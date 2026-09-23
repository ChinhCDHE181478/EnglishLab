import { describe, expect, it } from 'vitest';
import {
  getEnrollmentRequestActions,
  getStaffEnrollmentLoadError,
  isAssignableClassroom,
  loadStaffEnrollmentData,
} from './enrollmentAssignment';

const today = new Date(2026, 6, 23);
const active = {
  classroomStatus: 'ACTIVE',
  instructorLedCourseStatus: 'PUBLISHED',
  startDate: '2026-08-01',
  endDate: '2026-12-01',
  capacity: 20,
  enrolledCount: 12,
};

describe('staff enrollment assignment', () => {
  it('allows published upcoming and active classes that still have capacity', () => {
    expect(isAssignableClassroom({ ...active, classroomStatus: 'UPCOMING' }, today)).toBe(true);
    expect(isAssignableClassroom(active, today)).toBe(true);
  });

  it('is safe when used directly as an Array.filter callback', () => {
    const farFutureClassroom = { ...active, startDate: '2999-08-01' };

    expect([farFutureClassroom].filter(isAssignableClassroom)).toEqual([farFutureClassroom]);
  });

  it('excludes unavailable statuses and ended classes', () => {
    expect(isAssignableClassroom({ ...active, classroomStatus: 'COMPLETED' }, today)).toBe(false);
    expect(isAssignableClassroom({ ...active, endDate: '2026-07-22' }, today)).toBe(false);
    expect(isAssignableClassroom({ ...active, endDate: '2026-07-23' }, today)).toBe(true);
  });

  it('excludes unpublished and full classes', () => {
    expect(isAssignableClassroom({ ...active, instructorLedCourseStatus: 'DRAFT' }, today)).toBe(false);
    expect(isAssignableClassroom({ ...active, enrolledCount: 20 }, today)).toBe(false);
  });

  it('requires an appointment before test result and assignment', () => {
    expect(getEnrollmentRequestActions('SUBMITTED')).toEqual({
      canSchedule: true,
      canCompleteTest: false,
      canAssign: false,
      canReject: true,
    });
    expect(getEnrollmentRequestActions('INVITATION_SENT')).toEqual({
      canSchedule: true,
      canCompleteTest: false,
      canAssign: false,
      canReject: true,
    });
    expect(getEnrollmentRequestActions('TEST_SCHEDULED')).toEqual({
      canSchedule: false,
      canCompleteTest: true,
      canAssign: false,
      canReject: true,
    });
    expect(getEnrollmentRequestActions('WAITING_FOR_CLASS')).toEqual({
      canSchedule: false,
      canCompleteTest: false,
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
