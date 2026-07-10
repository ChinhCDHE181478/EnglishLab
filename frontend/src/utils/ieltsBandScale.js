export const IELTS_MAX_BAND = 9;

const toNumber = (value) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

export const usesBandScale = (assessment) => {
  const skill = String(assessment?.skill || '').toUpperCase();
  const type = String(assessment?.type || '').toUpperCase();
  const mode = String(assessment?.aiEvaluationMode || '').toUpperCase();

  if (skill === 'LISTENING' || skill === 'READING') {
    return false;
  }
  if (mode === 'ESTIMATED_BAND') {
    return true;
  }
  if (type !== 'MODULE_TEST' && type !== 'MOCK_TEST') {
    return false;
  }
  return true;
};

export const resolveScoreCap = (assessment) => {
  const configured = toNumber(assessment?.maxScore);
  if (!usesBandScale(assessment)) {
    return configured;
  }
  if (configured == null || configured > IELTS_MAX_BAND) {
    return IELTS_MAX_BAND;
  }
  return configured;
};

export const clampBand = (value) => {
  const parsed = toNumber(value);
  if (parsed == null) {
    return null;
  }
  const bounded = Math.max(0, Math.min(IELTS_MAX_BAND, parsed));
  return Math.round(bounded * 2) / 2;
};

export const normalizeAssessmentMaxScore = (assessment) => {
  const raw = toNumber(assessment?.maxScore);
  if (raw == null) {
    return usesBandScale(assessment) ? IELTS_MAX_BAND : null;
  }
  if (usesBandScale(assessment) && raw > IELTS_MAX_BAND) {
    return IELTS_MAX_BAND;
  }
  return raw;
};

export const normalizeBandThreshold = (assessment, raw) => {
  const parsed = toNumber(raw);
  if (parsed == null) {
    return null;
  }
  return usesBandScale(assessment) ? clampBand(parsed) : parsed;
};

export const normalizeAssessmentPassingScore = (assessment) => {
  const raw = toNumber(assessment?.passingScore);
  if (raw == null) {
    return null;
  }
  return usesBandScale(assessment) ? clampBand(raw) : raw;
};
