import { describe, expect, it } from 'vitest';
import { buildLearningPathCheckout } from './learningPathCheckout';

describe('buildLearningPathCheckout', () => {
  it('keeps only paid courses the learner does not own', () => {
    const checkout = buildLearningPathCheckout({
      id: 9,
      code: 'TOEIC_PATH',
      name: 'TOEIC Path',
      discountPercent: 10,
      minimumCoursesForDiscount: 2,
      courses: [
        { courseId: 1, title: 'Owned', owned: true, currentPrice: 1_000_000 },
        { courseId: 2, title: 'Remaining', owned: false, currentPrice: 900_000, originalPrice: 1_000_000 },
        { courseId: 3, title: 'Free', owned: false, currentPrice: 0, originalPrice: 0 },
      ],
    });

    expect(checkout.learningPathId).toBe(9);
    expect(checkout.courses).toHaveLength(1);
    expect(checkout.courses[0]).toMatchObject({
      id: 2,
      title: 'Remaining',
      salePrice: 900_000,
      originalPrice: 1_000_000,
    });
  });

  it('returns an empty checkout when a path contains only free unowned courses', () => {
    const checkout = buildLearningPathCheckout({
      id: 10,
      code: 'FREE_PATH',
      name: 'Free Path',
      courses: [
        { courseId: 4, title: 'Free course', owned: false, currentPrice: 0 },
      ],
    });

    expect(checkout.courses).toEqual([]);
  });
});
