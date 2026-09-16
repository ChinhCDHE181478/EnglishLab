import { describe, expect, it } from 'vitest';
import { buildManagedCoursePayload } from './contentManagerCoursePayload';

describe('buildManagedCoursePayload', () => {
  it('removes IELTS and TOEIC scores from a General English course', () => {
    const payload = buildManagedCoursePayload({
      category: 'GENERAL',
      recommendedCurrentBandMin: 4.5,
      targetBand: 6.5,
      targetScore: '750',
    });

    expect(payload.recommendedCurrentBandMin).toBeNull();
    expect(payload.targetBand).toBeNull();
    expect(payload.targetScore).toBeNull();
  });

  it('keeps only the score fields belonging to the selected exam category', () => {
    const ielts = buildManagedCoursePayload({ category: 'IELTS', targetBand: 6.5, targetScore: '750' });
    const toeic = buildManagedCoursePayload({ category: 'TOEIC', targetBand: 6.5, targetScore: '750' });

    expect(ielts.targetBand).toBe(6.5);
    expect(ielts.targetScore).toBeNull();
    expect(toeic.targetBand).toBeNull();
    expect(toeic.targetScore).toBe('750');
  });
});
