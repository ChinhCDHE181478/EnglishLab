import { describe, expect, it } from 'vitest';
import {
  getEnrollmentRequestActions,
  getStaffEnrollmentLoadError,
  isAssignableClassroom,
  loadStaffEnrollmentData,
} from './enrollmentAssignment';

const today = new Date(2026, 6, 23);
const upcoming = {
  classroomStatus: 'UPCOMING',
  packageStatus: 'PUBLISHED',
  startDate: '2026-08-01',
  maxCapacity: 20,
  enrolledCount: 12,
};

describe('staff enrollment assignment', () => {
  it('allows a published future class that still has capacity', () => {
    expect(isAssignableClassroom(upcoming, today)).toBe(true);
  });

  it('is safe when used directly as an Array.filter callback', () => {
    const farFutureClassroom = { ...upcoming, startDate: '2999-08-01' };

    expect([farFutureClassroom].filter(isAssignableClassroom)).toEqual([farFutureClassroom]);
  });

  it('excludes active and already-started classes', () => {
    expect(isAssignableClassroom({ ...upcoming, classroomStatus: 'ACTIVE' }, today)).toBe(false);
    expect(isAssignableClassroom({ ...upcoming, startDate: '2026-07-22' }, today)).toBe(false);
    expect(isAssignableClassroom({ ...upcoming, startDate: '2026-07-23' }, today)).toBe(false);
  });

  it('excludes unpublished and full classes', () => {
    expect(isAssignableClassroom({ ...upcoming, packageStatus: 'DRAFT' }, today)).toBe(false);
    expect(isAssignableClassroom({ ...upcoming, enrolledCount: 20 }, today)).toBe(false);
  });

  it('shows consultation and assignment as two consecutive steps', () => {
    expect(getEnrollmentRequestActions('SUBMITTED')).toEqual({
      canCompleteConsultation: true,
      canAssign: false,
      canReject: true,
    });
    expect(getEnrollmentRequestActions('UNDER_STAFF_REVIEW')).toEqual({
      canCompleteConsultation: true,
      canAssign: false,
      canReject: true,
    });
    expect(getEnrollmentRequestActions('WAITING_FOR_CLASS')).toEqual({
      canCompleteConsultation: false,
      canAssign: true,
      canReject: true,
    });
  });

  it('keeps the request queue when loading classrooms fails', async () => {
    const classroomError = { response: { status: 500 } };
    const result = await loadStaffEnrollmentData(
      async () => [{ id: 1, status: 'SUBMITTED' }],
      async () => { throw classroomError; },
    );

    expect(result.requests).toEqual([{ id: 1, status: 'SUBMITTED' }]);
    expect(result.classrooms).toEqual([]);
    expect(result.requestError).toBeNull();
    expect(result.classroomError).toBe(classroomError);
  });

  it('explains forbidden staff access instead of returning a generic message', () => {
    const forbidden = { response: { status: 403, data: {} } };

    expect(getStaffEnrollmentLoadError(forbidden, 'requests')).toContain('quyền Staff');
    expect(getStaffEnrollmentLoadError(forbidden, 'classrooms')).toContain('quyền vận hành lớp học');
  });
});
