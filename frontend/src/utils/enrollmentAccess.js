export const isActiveOnlineEnrollment = (enrollment) => {
  if (!enrollment) return false;
  const status = String(enrollment.status || 'ACTIVE').toUpperCase();
  return status !== 'CANCELLED';
};

export const isActiveClassroomRegistration = (registration) => {
  if (!registration) return false;
  const status = String(registration.registrationStatus || registration.status || '').toUpperCase();
  return !['CANCELLED', 'REJECTED'].includes(status);
};

export const ACTIVE_CLASSROOM_REGISTRATION_STATUSES = new Set([
  'PENDING_CONFIRMATION',
  'PENDING_TUITION_PAYMENT',
  'DEPOSIT_PAID',
  'PARTIALLY_PAID',
  'FULLY_PAID',
  'ASSIGNED',
  'WAITLIST',
]);

export const hasClassroomPortalAccess = (registration) => {
  if (!registration) return false;
  const status = String(registration.registrationStatus || registration.status || '').toUpperCase();
  return ACTIVE_CLASSROOM_REGISTRATION_STATUSES.has(status);
};
