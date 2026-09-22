import { describe, expect, it } from 'vitest';
import {
  normalizeAssessmentMaxScore,
  normalizeAssessmentPassingScore,
} from './ieltsBandScale';

describe('IELTS band score normalization', () => {
  const legacyWritingAssessment = {
    type: 'MODULE_TEST',
    skill: 'WRITING',
    aiEvaluationMode: 'RUBRIC_FEEDBACK',
    maxScore: 10,
  };

  it('normalizes legacy Writing module tests from score 10 to band 9', () => {
    expect(normalizeAssessmentMaxScore(legacyWritingAssessment)).toBe(9);
  });

  it('normalizes a passing band to the nearest half band', () => {
    expect(normalizeAssessmentPassingScore({
      ...legacyWritingAssessment,
      passingScore: 6.3,
    })).toBe(6.5);
  });
});
