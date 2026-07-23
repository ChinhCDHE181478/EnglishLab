import { describe, expect, it } from 'vitest';
import {
  canUseLearnerStudyTools,
  getUserRoles,
  hasAnyUserRole,
  needsPlacementTest,
  needsProfileCompletion,
} from './auth';

describe('role helpers', () => {
  it('normalizes primary and assigned roles without duplicates', () => {
    expect(getUserRoles({ role: 'staff', roles: ['LEARNER', 'STAFF'] }))
      .toEqual(['LEARNER', 'STAFF']);
  });

  it('recognizes STAFF independently from the legacy TRAINING_MANAGER role', () => {
    const staff = { role: 'STAFF' };

    expect(hasAnyUserRole(staff, ['STAFF'])).toBe(true);
    expect(hasAnyUserRole(staff, ['TRAINING_MANAGER'])).toBe(false);
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
    expect(canUseLearnerStudyTools({ role: 'TRAINING_MANAGER' })).toBe(false);
  });
});
