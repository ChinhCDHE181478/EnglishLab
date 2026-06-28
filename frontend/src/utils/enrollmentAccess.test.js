import { describe, expect, it } from 'vitest';
import {
  ACTIVE_CLASSROOM_REGISTRATION_STATUSES,
  hasClassroomPortalAccess,
  isActiveClassroomRegistration,
  isActiveOnlineEnrollment,
} from './enrollmentAccess';

describe('enrollmentAccess', () => {
  it('denies online enrollment when status is missing', () => {
    expect(isActiveOnlineEnrollment({ id: 1, enrollmentId: 2 })).toBe(false);
    expect(isActiveOnlineEnrollment(null)).toBe(false);
  });

  it('allows only ACTIVE and COMPLETED online enrollments', () => {
    expect(isActiveOnlineEnrollment({ status: 'ACTIVE' })).toBe(true);
    expect(isActiveOnlineEnrollment({ status: 'completed' })).toBe(true);
    expect(isActiveOnlineEnrollment({ status: 'CANCELLED' })).toBe(false);
    expect(isActiveOnlineEnrollment({ status: 'PENDING' })).toBe(false);
  });

  it('denies classroom registration when status is missing', () => {
    expect(isActiveClassroomRegistration({ id: 1 })).toBe(false);
  });

  it('blocks cancelled and rejected classroom registrations', () => {
    expect(isActiveClassroomRegistration({ registrationStatus: 'CANCELLED' })).toBe(false);
    expect(isActiveClassroomRegistration({ registrationStatus: 'REJECTED' })).toBe(false);
    expect(isActiveClassroomRegistration({ registrationStatus: 'ASSIGNED' })).toBe(true);
  });

  it('grants classroom portal access only for active registration statuses', () => {
    for (const status of ACTIVE_CLASSROOM_REGISTRATION_STATUSES) {
      expect(hasClassroomPortalAccess({ registrationStatus: status })).toBe(true);
    }
    expect(hasClassroomPortalAccess({ registrationStatus: 'CANCELLED' })).toBe(false);
  });
});
