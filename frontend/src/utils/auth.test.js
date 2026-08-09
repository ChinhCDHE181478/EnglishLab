import { describe, expect, it } from 'vitest';
import {
  canUseLearnerStudyTools,
  getDefaultAuthenticatedPath,
  getUserRoles,
  hasAnyUserRole,
  needsPlacementTest,
  needsProfileCompletion,
} from './auth';

describe('role helpers', () => {
  it.each([
    ['ADMIN', '/admin'],
    ['MANAGER', '/manager/classroom-proposals'],
    ['CONTENT_MANAGER', '/content-manager/dashboard'],
    ['STAFF', '/staff'],
    ['TEACHER', '/teacher'],
  ])('returns the workspace landing page for %s', (role, expectedPath) => {
    expect(getDefaultAuthenticatedPath({ role })).toBe(expectedPath);
  });

  it('keeps a fully onboarded learner on the learner home page', () => {
    const learner = {
      role: 'LEARNER',
      profileCompleted: true,
      fullName: 'Học viên',
      phoneNumber: '0900000000',
      targetExam: 'IELTS',
      targetScore: '6.5',
    };

    expect(getDefaultAuthenticatedPath(learner)).toBe('/home');
  });

  it('normalizes primary and assigned roles without duplicates', () => {
    expect(getUserRoles({ role: 'staff', roles: ['LEARNER', 'STAFF'] }))
      .toEqual(['LEARNER', 'STAFF']);
  });

  it('recognizes the STAFF role', () => {
    const staff = { role: 'STAFF' };

    expect(hasAnyUserRole(staff, ['STAFF'])).toBe(true);
  });

  it('does not force staff accounts through learner onboarding', () => {
    const staff = {
      role: 'STAFF',
      profileCompleted: false,
      placementTestCompleted: false,
    };

    expect(needsProfileCompletion(staff)).toBe(false);
    expect(needsPlacementTest(staff)).toBe(false);
  });

  it('only syncs learner study tools for roles accepted by the student API', () => {
    expect(canUseLearnerStudyTools({ role: 'LEARNER' })).toBe(true);
    expect(canUseLearnerStudyTools({ roles: ['STAFF', 'LEARNER'] })).toBe(true);
    expect(canUseLearnerStudyTools({ role: 'CONTENT_MANAGER' })).toBe(true);
    expect(canUseLearnerStudyTools({ role: 'STAFF' })).toBe(false);
    expect(canUseLearnerStudyTools({ role: 'TEACHER' })).toBe(false);
  });
});
