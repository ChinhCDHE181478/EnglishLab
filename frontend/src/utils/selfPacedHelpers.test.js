import { describe, expect, it } from 'vitest';
import { resolveCourseTargetDisplay } from './selfPacedHelpers';

describe('resolveCourseTargetDisplay', () => {
  it('formats IELTS targets with one decimal place', () => {
    expect(resolveCourseTargetDisplay({ category: 'IELTS', targetBand: 7 })).toEqual({
      label: 'Band mục tiêu',
      value: 'Band 7.0',
    });
  });

  it('uses score wording for TOEIC without a decimal suffix', () => {
    expect(resolveCourseTargetDisplay({ category: 'TOEIC', targetScore: '800' })).toEqual({
      label: 'Điểm mục tiêu',
      value: '800',
    });
  });

  it('uses outcome wording for non-exam categories', () => {
    expect(resolveCourseTargetDisplay({ category: 'COMMUNICATION', targetScore: 'CEFR B1-B2' })).toEqual({
      label: 'Chuẩn đầu ra',
      value: 'CEFR B1-B2',
    });
  });
});
