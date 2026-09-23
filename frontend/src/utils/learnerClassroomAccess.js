export const hasAssignedClassroomAccess = (classroom) => (
  classroom?.registrationStatus === 'ASSIGNED'
  && classroom?.hasClassAccess === true
);

const VISIBLE_REGISTRATION_STATUSES = new Set([
  'PENDING_TUITION_PAYMENT',
  'DEPOSIT_PAID',
  'PARTIALLY_PAID',
  'FULLY_PAID',
  'ASSIGNED',
  'SUSPENDED',
]);

export const hasVisibleClassroomRegistration = (classroom) => (
  VISIBLE_REGISTRATION_STATUSES.has(classroom?.registrationStatus)
);

export const onlyVisibleClassrooms = (classrooms) => (
  Array.isArray(classrooms) ? classrooms.filter(hasVisibleClassroomRegistration) : []
);
