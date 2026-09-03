export const ENGLISH_EXAM_OPTIONS = [
  { label: 'IELTS', value: 'IELTS' },
  { label: 'TOEIC', value: 'TOEIC' },
  { label: 'General English', value: 'GENERAL_ENGLISH' },
];

export const IELTS_BAND_OPTIONS = Array.from({ length: 19 }, (_, index) => {
  const value = (index / 2).toFixed(1);
  return { label: `Band ${value}`, value };
});

export const TOEIC_SCORE_PRESETS = [350, 450, 550, 650, 750, 850, 900, 950];

export const CEFR_LEVEL_OPTIONS = [
  { label: 'A1 · Beginner', value: 'A1' },
  { label: 'A2 · Elementary', value: 'A2' },
  { label: 'B1 · Intermediate', value: 'B1' },
  { label: 'B2 · Upper-intermediate', value: 'B2' },
  { label: 'C1 · Advanced', value: 'C1' },
  { label: 'C2 · Proficient', value: 'C2' },
];

export const ENGLISH_SKILL_OPTIONS = [
  { label: 'Listening', value: 'LISTENING' },
  { label: 'Reading', value: 'READING' },
  { label: 'Writing', value: 'WRITING' },
  { label: 'Speaking', value: 'SPEAKING' },
  { label: 'Vocabulary', value: 'VOCABULARY' },
  { label: 'Grammar', value: 'GRAMMAR' },
  { label: 'Pronunciation', value: 'PRONUNCIATION' },
  { label: 'Communication', value: 'COMMUNICATION' },
];

const DEFAULT_PROFILES = {
  IELTS: {
    focusSkills: ['LISTENING', 'READING', 'WRITING', 'SPEAKING'],
    entryLevel: '4.0',
    targetBand: 6.5,
    targetScore: '',
  },
  TOEIC: {
    focusSkills: ['LISTENING', 'READING'],
    entryLevel: '450',
    targetBand: '',
    targetScore: 650,
  },
  GENERAL_ENGLISH: {
    focusSkills: ['LISTENING', 'READING', 'WRITING', 'SPEAKING'],
    entryLevel: 'A2',
    targetBand: '',
    targetScore: '',
  },
};

export const normalizeEnglishExamCategory = (value) => {
  const normalized = String(value || 'IELTS').trim().toUpperCase();
  if (['GENERAL', 'COMMUNICATION', 'FOUNDATION'].includes(normalized)) return 'GENERAL_ENGLISH';
  return ['IELTS', 'TOEIC', 'GENERAL_ENGLISH'].includes(normalized) ? normalized : 'GENERAL_ENGLISH';
};

export const getEnglishProfileDefaults = (examCategory) => {
  const normalized = normalizeEnglishExamCategory(examCategory);
  return { ...DEFAULT_PROFILES[normalized], focusSkills: [...DEFAULT_PROFILES[normalized].focusSkills] };
};

export const readEnglishFocusSkills = (value, examCategory) => {
  const skills = Array.isArray(value)
    ? value
    : String(value || '').split(',').map((item) => item.trim()).filter(Boolean);
  return skills.length ? skills : getEnglishProfileDefaults(examCategory).focusSkills;
};

export const normalizeEnglishEntryLevel = (value, examCategory) => {
  const normalizedCategory = normalizeEnglishExamCategory(examCategory);
  const fallback = getEnglishProfileDefaults(normalizedCategory).entryLevel;
  const text = String(value || '').trim().toUpperCase();

  if (normalizedCategory === 'IELTS') {
    const candidate = text.match(/(?:^|\s)([0-9](?:\.[05])?)(?:\s|$|-)/)?.[1];
    return isValidIeltsBand(candidate) ? Number(candidate).toFixed(1) : fallback;
  }
  if (normalizedCategory === 'TOEIC') {
    const candidate = text.match(/\d{2,3}/)?.[0];
    return isValidToeicScore(candidate) ? String(Number(candidate)) : fallback;
  }
  const cefr = text.match(/\b(A1|A2|B1|B2|C1|C2)\b/)?.[1];
  return cefr || fallback;
};

export const validateEnglishProgramProfile = (profile) => {
  const examCategory = normalizeEnglishExamCategory(profile.examCategory);
  if (!profile.focusSkills?.length) return 'Hãy chọn ít nhất một kỹ năng trọng tâm.';

  if (examCategory === 'IELTS') {
    const entryBand = Number(profile.entryLevel);
    const band = Number(profile.targetBand);
    if (!isValidIeltsBand(entryBand)) {
      return 'Band IELTS đầu vào phải từ 0 đến 9 và tăng theo bước 0.5.';
    }
    if (!Number.isFinite(band) || band < 0 || band > 9 || !Number.isInteger(band * 2)) {
      return 'Band IELTS phải từ 0 đến 9 và tăng theo bước 0.5.';
    }
    if (entryBand > band) return 'Band IELTS đầu vào không thể cao hơn band mục tiêu.';
    if (profile.targetScore !== '' && profile.targetScore != null) {
      return 'Chương trình IELTS không sử dụng thang điểm TOEIC.';
    }
  }
  if (examCategory === 'TOEIC') {
    const entryScore = Number(profile.entryLevel);
    const score = Number(profile.targetScore);
    if (!isValidToeicScore(entryScore)) {
      return 'Điểm TOEIC đầu vào phải từ 10 đến 990 và tăng theo bước 5.';
    }
    if (!Number.isInteger(score) || score < 10 || score > 990 || score % 5 !== 0) {
      return 'Điểm TOEIC phải từ 10 đến 990 và tăng theo bước 5.';
    }
    if (entryScore > score) return 'Điểm TOEIC đầu vào không thể cao hơn điểm mục tiêu.';
    if (profile.targetBand !== '' && profile.targetBand != null) {
      return 'Chương trình TOEIC không sử dụng band IELTS.';
    }
  }
  if (examCategory === 'GENERAL_ENGLISH' && !CEFR_LEVEL_OPTIONS.some((option) => option.value === profile.entryLevel)) {
    return 'Hãy chọn trình độ CEFR đầu vào từ A1 đến C2.';
  }
  if (examCategory === 'GENERAL_ENGLISH'
      && ((profile.targetBand !== '' && profile.targetBand != null)
        || (profile.targetScore !== '' && profile.targetScore != null))) {
    return 'General English dùng chuẩn đầu ra mô tả, không dùng điểm IELTS hoặc TOEIC.';
  }
  return '';
};

export const isValidIeltsBand = (value) => (
  Number.isFinite(Number(value))
  && Number(value) >= 0
  && Number(value) <= 9
  && Number.isInteger(Number(value) * 2)
);

export const isValidToeicScore = (value) => (
  Number.isInteger(Number(value))
  && Number(value) >= 10
  && Number(value) <= 990
  && Number(value) % 5 === 0
);
