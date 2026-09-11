import { describe, expect, it } from 'vitest';
import { getEffectiveCoursePrice, isFreeCourse } from './courseModels';

describe('course pricing helpers', () => {
  it('treats an explicit zero sale price as free', () => {
    const course = { price: 1_000_000, salePrice: 0 };

    expect(getEffectiveCoursePrice(course)).toBe(0);
    expect(isFreeCourse(course)).toBe(true);
  });

  it('keeps a positive effective price payable', () => {
    const course = { price: 1_000_000, salePrice: 750_000 };

    expect(getEffectiveCoursePrice(course)).toBe(750_000);
    expect(isFreeCourse(course)).toBe(false);
  });
});
