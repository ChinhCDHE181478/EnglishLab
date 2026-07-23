export const hasAssignedClassroomAccess = (classroom) => (
  classroom?.registrationStatus === 'ASSIGNED'
  && classroom?.hasClassAccess === true
);

export const onlyAssignedClassrooms = (classrooms) => (
  Array.isArray(classrooms) ? classrooms.filter(hasAssignedClassroomAccess) : []
);
