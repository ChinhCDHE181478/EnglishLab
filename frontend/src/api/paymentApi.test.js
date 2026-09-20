import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./axiosClient', () => ({
  default: {
    post: vi.fn(),
  },
}));

import axiosClient from './axiosClient';
import paymentApi from './paymentApi';

describe('payment checkout API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends the complete checkout snapshot when creating a PayOS link', async () => {
    axiosClient.post.mockResolvedValue({ data: { checkoutUrl: 'https://pay.payos.vn/web/123' } });
    const snapshot = {
      displayedOriginalAmount: 1_000_000,
      displayedSystemDiscountAmount: 100_000,
      displayedLearningPathDiscountAmount: 50_000,
      displayedCouponDiscountAmount: 50_000,
      finalAmount: 800_000,
    };

    await paymentApi.createPayosLink([10], 'SAVE50', [], 7, snapshot);

    expect(axiosClient.post).toHaveBeenCalledWith('/api/student/payments/payos/link', {
      courseIds: [10],
      classroomOfferingIds: [],
      learningPathId: 7,
      couponCode: 'SAVE50',
      ...snapshot,
    });
  });

  it('keeps classroom tuition requests backward compatible without a course snapshot', async () => {
    axiosClient.post.mockResolvedValue({ data: { checkoutUrl: 'https://pay.payos.vn/web/456' } });

    await paymentApi.createPayosLink([], '', [12]);

    expect(axiosClient.post).toHaveBeenCalledWith('/api/student/payments/payos/link', {
      courseIds: [],
      classroomOfferingIds: [12],
      learningPathId: null,
      couponCode: '',
    });
  });
});
