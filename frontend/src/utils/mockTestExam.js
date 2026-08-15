const TOEIC_PART_START = {
  LISTENING: { 1: 1, 2: 7, 3: 32, 4: 71 },
  READING: { 5: 101, 6: 131, 7: 147 },
};

export function parseJson(value, fallback = {}) {
  if (value && typeof value === 'object') {
    return value;
  }
  try {
    const parsed = JSON.parse(String(value || ''));
    return parsed && typeof parsed === 'object' ? parsed : fallback;
  } catch {
    return fallback;
  }
}

export function isToeicExamConfig(config = {}, test = null) {
  const examType = String(config?.examType || test?.examType || '').toUpperCase();
  if (examType === 'TOEIC') return true;
  const type = String(config?.type || '').toLowerCase();
  if (type.startsWith('toeic_')) return true;
  const key = String(config?.key || config?.sourceLabel || '').toLowerCase();
  if (key.includes('toeic')) return true;
  const title = String(config?.title || test?.title || '');
  return /\bTOEIC\b/i.test(title);
}

export function resolveMockConfig(test, config) {
  const skill = String(test.skill || 'MIXED').toUpperCase();
  if (skill !== 'MIXED') {
    const keyedConfig = config.sections?.[skill.toLowerCase()] || config[skill.toLowerCase()] || config;
    return { skill, config: normalizeObjectiveConfig(keyedConfig, skill) };
  }
  const sections = config.sections || config;
  if (sections?.toeic?.listening) {
    return { skill: 'LISTENING', config: normalizeObjectiveConfig(sections.toeic.listening, 'LISTENING') };
  }
  if (sections?.toeic?.reading) {
    return { skill: 'READING', config: normalizeObjectiveConfig(sections.toeic.reading, 'READING') };
  }
  for (const key of ['listening', 'reading', 'writing', 'speaking']) {
    if (sections?.[key]) {
      const resolvedSkill = key.toUpperCase();
      return { skill: resolvedSkill, config: normalizeObjectiveConfig(sections[key], resolvedSkill) };
    }
  }
  return { skill: 'READING', config };
}

function normalizeObjectiveConfig(config = {}, skill = '') {
  if (!['LISTENING', 'READING'].includes(skill)) return config;
  const parts = Array.isArray(config.parts) ? config.parts : [];
  const firstPartAudioUrl = parts.find((part) => part.audioUrl)?.audioUrl || '';
  if (!parts.length || parts.every((part) => Array.isArray(part.questionGroups))) {
    return skill === 'LISTENING' && firstPartAudioUrl && !config.audioUrl
      ? { ...config, audioUrl: firstPartAudioUrl }
      : config;
  }
  return {
    ...config,
    audioUrl: config.audioUrl || firstPartAudioUrl || '',
    parts: parts.map((part, index) => normalizeToeicStylePart(part, index, skill)),
  };
}

function normalizeToeicStylePart(part = {}, index = 0, skill = '') {
  const partNumber = Number(part.partNumber || part.part || index + 1);
  const start = TOEIC_PART_START[skill]?.[partNumber] || Number(part.startQuestion || 1);
  const sourceQuestions = Array.isArray(part.questions) && part.questions.length
    ? part.questions
    : Array.from({ length: Number(part.questionCount || 0) }, (_, questionIndex) => ({ number: start + questionIndex }));
  const questions = sourceQuestions.map((question, questionIndex) => normalizeToeicStyleQuestion(question, question.number || start + questionIndex));
  return {
    ...part,
    key: part.key || `toeic_${skill.toLowerCase()}_${partNumber}`,
    partNumber,
    title: part.title || `Part ${partNumber}`,
    questionRange: questions.length ? `Questions ${questions[0].number}-${questions[questions.length - 1].number}` : '',
    passage: part.passage || {
      title: part.title || `Part ${partNumber}`,
      paragraphs: part.description || part.instructions ? [{ text: part.description || part.instructions }] : [],
    },
    questionGroups: [{
      title: part.groupTitle || part.title || `Part ${partNumber}`,
      instructions: part.instructions || '',
      type: part.type || 'single_choice',
      questions,
    }],
  };
}

function normalizeToeicStyleQuestion(question = {}, fallbackNumber) {
  const options = Array.isArray(question.options) && question.options.length
    ? question.options.map((option, index) => normalizeToeicStyleOption(option, index))
    : ['A', 'B', 'C', 'D'].map((option, index) => normalizeToeicStyleOption(option, index));
  return {
    ...question,
    number: Number(question.number || question.id || fallbackNumber),
    prompt: question.prompt || question.question || question.text || `Câu ${fallbackNumber}`,
    options,
  };
}

function normalizeToeicStyleOption(option, index) {
  if (option && typeof option === 'object') {
    const value = String(option.value || option.key || String.fromCharCode(65 + index)).trim();
    return { value, label: option.label || option.text || value };
  }
  const text = String(option || '').trim();
  const match = text.match(/^([A-D])[\).:\s-]*(.*)$/i);
  if (match) {
    return { value: match[1].toUpperCase(), label: match[2] || match[1].toUpperCase() };
  }
  const value = String.fromCharCode(65 + index);
  return { value, label: text || value };
}
