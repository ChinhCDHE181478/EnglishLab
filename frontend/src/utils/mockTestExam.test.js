import { describe, expect, it } from 'vitest';
import { isToeicExamConfig } from './mockTestExam';

describe('isToeicExamConfig', () => {
  it('detects examType TOEIC', () => {
    expect(isToeicExamConfig({ examType: 'TOEIC' })).toBe(true);
  });

  it('detects toeic_ type keys', () => {
    expect(isToeicExamConfig({ type: 'toeic_listening_exam' })).toBe(true);
    expect(isToeicExamConfig({ type: 'ielts_listening_exam' })).toBe(false);
  });

  it('detects TOEIC titles on the test object', () => {
    expect(isToeicExamConfig({}, { title: 'TOEIC Mock Test 2026 Collection Listening Practice Test 2' })).toBe(true);
    expect(isToeicExamConfig({}, { title: 'IELTS Mock Test 2026 January Listening Practice Test 1' })).toBe(false);
  });
});
