const storageKeys = {
  enrollments: 'englishlab_enrollments',
  notes: 'englishlab_lesson_notes',
  lessonFlags: 'englishlab_lesson_flags',
  recentLessons: 'englishlab_recent_lessons',
  assessmentDrafts: 'englishlab_assessment_drafts',
  assessmentQueue: 'englishlab_assessment_queue',
  notifications: 'englishlab_notifications',
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
const DAY_MS = 24 * 60 * 60 * 1000;
const ASSESSMENT_DRAFT_TTL_MS = 14 * DAY_MS;
const ASSESSMENT_QUEUE_TTL_MS = 7 * DAY_MS;
const MAX_ASSESSMENT_DRAFTS = 20;
const MAX_ASSESSMENT_QUEUE_ITEMS = 10;

const isFreshEnough = (item, ttlMs) => {
  const timestamp = Date.parse(item?.updatedAt || item?.createdAt || '');
  if (!Number.isFinite(timestamp)) return true;
  return Date.now() - timestamp <= ttlMs;
};

const stripLargeAudioPayload = (payload = {}) => {
  if (!payload || typeof payload !== 'object') return payload;
  const { audioBase64, submittedAudioBase64, ...rest } = payload;
  return rest;
};

const sanitizeAssessmentDraft = (draft) => {
  if (!draft || typeof draft !== 'object') return null;
  const { audioBase64, submittedAudioBase64, ...rest } = draft;
  return rest;
};

const sanitizeAssessmentQueueItem = (item) => {
  if (!item || typeof item !== 'object') return null;
  return {
    ...item,
    payload: stripLargeAudioPayload(item.payload),
  };
};

const normalizeLimitedList = (items, { ttlMs, maxItems, sanitizer }) => (
  Array.isArray(items)
    ? items
      .map(sanitizer)
      .filter(Boolean)
      .filter((item) => isFreshEnough(item, ttlMs))
      .sort((left, right) => Date.parse(right.updatedAt || right.createdAt || '') - Date.parse(left.updatedAt || left.createdAt || ''))
      .slice(0, maxItems)
    : []
);

export const learnerStorageKeys = storageKeys;

export const readEnrollments = () => readJson(storageKeys.enrollments, []);

export const readLessonNotes = () => readJson(storageKeys.notes, []);
export const writeLessonNotes = (value) => writeJson(storageKeys.notes, value);

export const readLessonFlags = () => readJson(storageKeys.lessonFlags, []);
export const writeLessonFlags = (value) => writeJson(storageKeys.lessonFlags, value);

export const readRecentLessons = () => readJson(storageKeys.recentLessons, []);
export const writeRecentLessons = (value) => writeJson(storageKeys.recentLessons, value);

export const readAssessmentDrafts = () => normalizeLimitedList(readJson(storageKeys.assessmentDrafts, []), {
  ttlMs: ASSESSMENT_DRAFT_TTL_MS,
  maxItems: MAX_ASSESSMENT_DRAFTS,
  sanitizer: sanitizeAssessmentDraft,
});
export const writeAssessmentDrafts = (value) => writeJson(storageKeys.assessmentDrafts, normalizeLimitedList(value, {
  ttlMs: ASSESSMENT_DRAFT_TTL_MS,
  maxItems: MAX_ASSESSMENT_DRAFTS,
  sanitizer: sanitizeAssessmentDraft,
}));

export const readAssessmentQueue = () => normalizeLimitedList(readJson(storageKeys.assessmentQueue, []), {
  ttlMs: ASSESSMENT_QUEUE_TTL_MS,
  maxItems: MAX_ASSESSMENT_QUEUE_ITEMS,
  sanitizer: sanitizeAssessmentQueueItem,
});
export const writeAssessmentQueue = (value) => writeJson(storageKeys.assessmentQueue, normalizeLimitedList(value, {
  ttlMs: ASSESSMENT_QUEUE_TTL_MS,
  maxItems: MAX_ASSESSMENT_QUEUE_ITEMS,
  sanitizer: sanitizeAssessmentQueueItem,
}));

export const readNotifications = () => readJson(storageKeys.notifications, []);
export const writeNotifications = (value) => writeJson(storageKeys.notifications, value);

export const createLearnerNotification = ({
  title,
  message,
  type = 'success',
  actionPath = '',
  courseId = null,
  courseTitle = '',
}) => ({
  id: buildId('thong-bao'),
  title,
  message,
  type,
  actionPath,
  courseId,
  courseTitle,
  read: false,
  createdAt: nowIso(),
});

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
  payload: stripLargeAudioPayload(payload),
  assessmentTitle,
  createdAt: nowIso(),
  updatedAt: nowIso(),
  retryCount: 0,
});
