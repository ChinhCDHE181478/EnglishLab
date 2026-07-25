import { describe, expect, it } from 'vitest';
import { hasAssignedClassroomAccess, onlyAssignedClassrooms } from './learnerClassroomAccess';

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

  it('lọc dữ liệu cũ khỏi danh sách Lớp học của tôi', () => {
    const assigned = {
      id: 1,
      registrationStatus: 'ASSIGNED',
      hasClassAccess: true,
    };

    expect(onlyAssignedClassrooms([
      assigned,
      {
        id: 2,
        registrationStatus: 'PENDING_TUITION_PAYMENT',
        hasClassAccess: false,
      },
    ])).toEqual([assigned]);
    expect(onlyAssignedClassrooms(null)).toEqual([]);
  });
});
