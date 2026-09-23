import { describe, expect, it } from 'vitest';
import {
  hasAssignedClassroomAccess,
  hasVisibleClassroomRegistration,
  onlyVisibleClassrooms,
} from './learnerClassroomAccess';

describe('learnerClassroomAccess', () => {
  it('chỉ nhận lớp đã được Staff xếp và đã có quyền học', () => {
    expect(hasAssignedClassroomAccess({
      registrationStatus: 'ASSIGNED',
      hasClassAccess: true,
    })).toBe(true);

    [
      'PENDING_CONFIRMATION',
      'PENDING_TUITION_PAYMENT',
      'DEPOSIT_PAID',
      'PARTIALLY_PAID',
      'FULLY_PAID',
      'WAITLIST',
      'CANCELLED',
    ].forEach((registrationStatus) => {
      expect(hasAssignedClassroomAccess({
        registrationStatus,
        hasClassAccess: false,
      })).toBe(false);
    });
  });

  it('hiển thị lớp cần thanh toán nhưng vẫn ẩn hồ sơ chưa được chọn lớp', () => {
    const assigned = {
      id: 1,
      registrationStatus: 'ASSIGNED',
      hasClassAccess: true,
    };
    const awaitingPayment = {
      id: 2,
      registrationStatus: 'PENDING_TUITION_PAYMENT',
      hasClassAccess: false,
    };

    expect(hasVisibleClassroomRegistration(awaitingPayment)).toBe(true);
    expect(onlyVisibleClassrooms([
      assigned,
      awaitingPayment,
      { id: 3, registrationStatus: 'WAITLIST', hasClassAccess: false },
    ])).toEqual([assigned, awaitingPayment]);
    expect(onlyVisibleClassrooms(null)).toEqual([]);
  });
});
