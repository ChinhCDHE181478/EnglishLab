export const IELTS_MAX_SCORE = 9;
export const TOEIC_MAX_SCORE = 990;

const parseConfig = (value) => {
  try {
    const parsed = JSON.parse(String(value || ''));
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
};

export function resolveAssessmentExamType(assessment = {}) {
  const explicit = String(assessment.examCategory || assessment.examType || '').toUpperCase();
  if (explicit === 'IELTS' || explicit === 'TOEIC') return explicit;

  const config = parseConfig(assessment.uiConfigJson);
  const configured = String(config.examType || '').toUpperCase();
  if (configured === 'IELTS' || configured === 'TOEIC') return configured;
  if (String(config.type || '').toLowerCase().startsWith('toeic_')) return 'TOEIC';
  return /\bTOEIC\b/i.test(String(assessment.title || '')) ? 'TOEIC' : 'IELTS';
}

export function getMockExamMaxScore(assessment = {}) {
  return resolveAssessmentExamType(assessment) === 'TOEIC' ? TOEIC_MAX_SCORE : IELTS_MAX_SCORE;
}

export function isIeltsModuleAssessment(assessment = {}) {
  const mode = String(assessment.aiEvaluationMode || '').toUpperCase();
  return resolveAssessmentExamType(assessment) === 'IELTS' && mode !== 'NONE';
}
