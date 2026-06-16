const storageKeys = {
  enrollments: 'englishlab_enrollments',
  notes: 'englishlab_lesson_notes',
  lessonFlags: 'englishlab_lesson_flags',
  recentLessons: 'englishlab_recent_lessons',
  assessmentDrafts: 'englishlab_assessment_drafts',
  assessmentQueue: 'englishlab_assessment_queue',
};

const safeParse = (value, fallback) => {
  try {
    return JSON.parse(value ?? '');
  } catch {
    return fallback;
  }
};

const readJson = (key, fallback) => {
  if (typeof window === 'undefined') return fallback;
  return safeParse(window.localStorage.getItem(key), fallback);
};

const writeJson = (key, value) => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(key, JSON.stringify(value));
};

const buildId = (prefix) => `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const nowIso = () => new Date().toISOString();

export const learnerStorageKeys = storageKeys;

export const readEnrollments = () => readJson(storageKeys.enrollments, []);

export const readLessonNotes = () => readJson(storageKeys.notes, []);
export const writeLessonNotes = (value) => writeJson(storageKeys.notes, value);

export const readLessonFlags = () => readJson(storageKeys.lessonFlags, []);
export const writeLessonFlags = (value) => writeJson(storageKeys.lessonFlags, value);

export const readRecentLessons = () => readJson(storageKeys.recentLessons, []);
export const writeRecentLessons = (value) => writeJson(storageKeys.recentLessons, value);

export const readAssessmentDrafts = () => readJson(storageKeys.assessmentDrafts, []);
export const writeAssessmentDrafts = (value) => writeJson(storageKeys.assessmentDrafts, value);

export const readAssessmentQueue = () => readJson(storageKeys.assessmentQueue, []);
export const writeAssessmentQueue = (value) => writeJson(storageKeys.assessmentQueue, value);

export const createLessonNote = ({
  courseId,
  lessonId,
  content,
  lessonTitle = '',
  courseTitle = '',
  selectedText = '',
  transcriptStartSeconds = null,
  source = 'manual',
}) => ({
  id: buildId('ghi-chu'),
  courseId,
  lessonId,
  content,
  lessonTitle,
  courseTitle,
  selectedText,
  transcriptStartSeconds,
  source,
  createdAt: nowIso(),
  updatedAt: nowIso(),
});

export const createAssessmentQueueItem = ({
  assessmentId,
  courseId,
  lessonId = null,
  payload,
  assessmentTitle = '',
}) => ({
  id: buildId('hang-cho-bai-lam'),
  assessmentId,
  courseId,
  lessonId,
  payload,
  assessmentTitle,
  createdAt: nowIso(),
  updatedAt: nowIso(),
  retryCount: 0,
});
