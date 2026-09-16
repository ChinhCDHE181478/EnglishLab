export const buildLessonWorkspacePath = (courseSlug, lessonId) => {
  const path = `/courses/${encodeURIComponent(String(courseSlug || ''))}/learn`;
  if (lessonId == null || lessonId === '') return path;
  return `${path}?lessonId=${encodeURIComponent(String(lessonId))}`;
};

export const getRequestedLessonId = (search = '') => {
  const lessonId = new URLSearchParams(search).get('lessonId');
  return lessonId?.trim() || null;
};
