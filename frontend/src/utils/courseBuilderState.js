const TRANSIENT_KEYS = new Set([
  'createdAt',
  'localKey',
  'tempId',
  'updatedAt',
]);

export const createCourseBuilderFingerprint = (course, assessments = []) => JSON.stringify({
  assessments: Array.isArray(assessments) ? assessments : [],
  modules: Array.isArray(course?.modules) ? course.modules : [],
}, (key, value) => (TRANSIENT_KEYS.has(key) ? undefined : value));
