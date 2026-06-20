import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { getStoredUser, hasAccessToken } from '../utils/auth';
import {
  createAssessmentQueueItem,
  createLearnerNotification,
  createLessonNote,
  learnerStorageKeys,
  readAssessmentDrafts,
  readAssessmentQueue,
  readLessonFlags,
  readLessonNotes,
  readNotifications,
  readRecentLessons,
  writeAssessmentDrafts,
  writeAssessmentQueue,
  writeLessonFlags,
  writeLessonNotes,
  writeNotifications,
  writeRecentLessons,
} from '../utils/learnerStore';

const LearnerExperienceContext = createContext(null);
const TOAST_EXIT_DURATION_MS = 260;

const ToastViewport = ({ toasts, onDismiss }) => (
  <div className="pointer-events-none fixed right-4 top-24 z-[70] flex w-[calc(100%-2rem)] max-w-[420px] flex-col gap-3 sm:right-6 sm:w-full">
    {toasts.map((toast) => (
      <div
        key={toast.id}
        className={`pointer-events-auto w-full max-w-[420px] rounded-2xl border px-4 py-3 shadow-[0_18px_40px_rgba(75,0,9,0.18)] ${
          toast.type === 'error'
            ? 'border-[#f0b4b4] bg-[#fff1f1] text-[#7a0010]'
            : 'border-[#dfbfbd] bg-white text-[#4b0009]'
        } ${toast.isLeaving ? 'animate-toast-out' : 'animate-toast-in'}`}
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-sm font-extrabold">{toast.title}</p>
            <p className="mt-1 text-sm leading-6 text-[#584140]">{toast.message}</p>
          </div>
          <button className="text-xs font-bold text-[#730014]" onClick={() => onDismiss(toast.id)} type="button">
            Đóng
          </button>
        </div>
      </div>
    ))}
  </div>
);

export const LearnerExperienceProvider = ({ children }) => {
  const [lessonNotes, setLessonNotes] = useState(() => readLessonNotes());
  const [lessonFlags, setLessonFlags] = useState(() => readLessonFlags());
  const [recentLessons, setRecentLessons] = useState(() => readRecentLessons());
  const [assessmentDrafts, setAssessmentDrafts] = useState(() => readAssessmentDrafts());
  const [assessmentQueue, setAssessmentQueue] = useState(() => readAssessmentQueue());
  const [notifications, setNotifications] = useState(() => readNotifications());
  const [courseAssessmentSnapshots, setCourseAssessmentSnapshots] = useState({});
  const [user, setUser] = useState(() => getStoredUser());
  const [toasts, setToasts] = useState([]);
  const toastTimers = useRef(new Map());

  useEffect(() => { writeLessonNotes(lessonNotes); }, [lessonNotes]);
  useEffect(() => { writeLessonFlags(lessonFlags); }, [lessonFlags]);
  useEffect(() => { writeRecentLessons(recentLessons); }, [recentLessons]);
  useEffect(() => { writeAssessmentDrafts(assessmentDrafts); }, [assessmentDrafts]);
  useEffect(() => { writeAssessmentQueue(assessmentQueue); }, [assessmentQueue]);
  useEffect(() => { writeNotifications(notifications); }, [notifications]);

  useEffect(() => {
    const syncStorage = (event) => {
      if (!event.key || event.key === learnerStorageKeys.notes) setLessonNotes(readLessonNotes());
      if (!event.key || event.key === learnerStorageKeys.lessonFlags) setLessonFlags(readLessonFlags());
      if (!event.key || event.key === learnerStorageKeys.recentLessons) setRecentLessons(readRecentLessons());
      if (!event.key || event.key === learnerStorageKeys.assessmentDrafts) setAssessmentDrafts(readAssessmentDrafts());
      if (!event.key || event.key === learnerStorageKeys.assessmentQueue) setAssessmentQueue(readAssessmentQueue());
      if (!event.key || event.key === learnerStorageKeys.notifications) setNotifications(readNotifications());
    };

    const syncUser = () => setUser(getStoredUser());

    window.addEventListener('storage', syncStorage);
    window.addEventListener('focus', syncUser);
    window.addEventListener('englishlab:user-updated', syncUser);

    return () => {
      window.removeEventListener('storage', syncStorage);
      window.removeEventListener('focus', syncUser);
      window.removeEventListener('englishlab:user-updated', syncUser);
    };
  }, []);

  useEffect(() => () => {
    toastTimers.current.forEach((timeoutId) => window.clearTimeout(timeoutId));
    toastTimers.current.clear();
  }, []);

  const dismissToast = (toastId) => {
    const timeoutId = toastTimers.current.get(toastId);
    if (timeoutId) {
      window.clearTimeout(timeoutId);
      toastTimers.current.delete(toastId);
    }
    setToasts((current) => current.map((toast) => (
      toast.id === toastId ? { ...toast, isLeaving: true } : toast
    )));
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== toastId));
    }, TOAST_EXIT_DURATION_MS);
  };

  const pushToast = ({ title, message, type = 'success' }) => {
    const toast = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      title,
      message,
      type,
      isLeaving: false,
    };
    setToasts((current) => [...current, toast]);
    const timeoutId = window.setTimeout(() => dismissToast(toast.id), 3200);
    toastTimers.current.set(toast.id, timeoutId);
  };

  const saveLessonNote = ({
    courseId,
    lessonId,
    content,
    lessonTitle = '',
    courseTitle = '',
    selectedText = '',
    transcriptStartSeconds = null,
    source = 'manual',
  }) => {
    const note = createLessonNote({
      courseId,
      lessonId,
      content,
      lessonTitle,
      courseTitle,
      selectedText,
      transcriptStartSeconds,
      source,
    });
    setLessonNotes((current) => [note, ...current]);
    pushToast({ title: 'Đã lưu ghi chú', message: 'Ghi chú của bạn đã được lưu.' });
    return note;
  };

  const updateLessonNote = (noteId, content) => {
    setLessonNotes((current) => current.map((note) => (
      note.id === noteId
        ? { ...note, content, updatedAt: new Date().toISOString() }
        : note
    )));
  };

  const removeLessonNote = (noteId) => {
    setLessonNotes((current) => current.filter((note) => note.id !== noteId));
    pushToast({ title: 'Đã xóa ghi chú', message: 'Ghi chú đã được xóa khỏi bài học.' });
  };

  const toggleLessonReviewFlag = ({ courseId, lessonId, lessonTitle = '', courseTitle = '' }) => {
    const exists = lessonFlags.some((item) => String(item.lessonId) === String(lessonId));
    if (exists) {
      setLessonFlags((current) => current.filter((item) => String(item.lessonId) !== String(lessonId)));
      return false;
    }

    setLessonFlags((current) => [{
      id: `hoc-lai-${lessonId}`,
      courseId,
      lessonId,
      lessonTitle,
      courseTitle,
      createdAt: new Date().toISOString(),
    }, ...current]);
    return true;
  };

  const saveRecentLesson = useCallback(({ courseId, lessonId, lessonTitle = '', courseTitle = '' }) => {
    const nextItem = {
      id: `gan-day-${lessonId}`,
      courseId,
      lessonId,
      lessonTitle,
      courseTitle,
      createdAt: new Date().toISOString(),
    };
    setRecentLessons((current) => [
      nextItem,
      ...current.filter((item) => String(item.lessonId) !== String(lessonId)),
    ].slice(0, 12));
  }, []);

  const addNotification = ({ title, message, type = 'success', actionPath = '', courseId = null, courseTitle = '' }) => {
    pushToast({ title, message, type });
    const notification = createLearnerNotification({
      title,
      message,
      type,
      actionPath,
      courseId,
      courseTitle,
    });
    setNotifications((current) => {
      const duplicate = current.some((item) => (
        item.title === notification.title
        && item.message === notification.message
        && String(item.courseId || '') === String(notification.courseId || '')
        && item.actionPath === notification.actionPath
      ));
      return duplicate ? current : [notification, ...current].slice(0, 50);
    });
  };

  const markAllNotificationsRead = useCallback(() => {
    setNotifications((current) => {
      if (current.every((notification) => notification.read)) {
        return current;
      }
      return current.map((notification) => ({ ...notification, read: true }));
    });
  }, []);

  const saveAssessmentDraft = useCallback((draft) => {
    if (!draft?.assessmentId) return;
    setAssessmentDrafts((current) => {
      const filtered = current.filter((item) => String(item.assessmentId) !== String(draft.assessmentId));
      const nextDraft = { ...draft, updatedAt: new Date().toISOString() };
      const previousDraft = current.find((item) => String(item.assessmentId) === String(draft.assessmentId));
      if (previousDraft
        && previousDraft.submittedText === nextDraft.submittedText
        && previousDraft.submittedAudioUrl === nextDraft.submittedAudioUrl
        && JSON.stringify(previousDraft.objectiveDraft || null) === JSON.stringify(nextDraft.objectiveDraft || null)) {
        return current;
      }
      return [...filtered, nextDraft];
    });
  }, []);

  const clearAssessmentDraft = useCallback((assessmentId) => {
    setAssessmentDrafts((current) => current.filter((item) => String(item.assessmentId) !== String(assessmentId)));
  }, []);

  const getAssessmentDraft = useCallback((assessmentId) =>
    assessmentDrafts.find((item) => String(item.assessmentId) === String(assessmentId)) || null, [assessmentDrafts]);

  const enqueueAssessmentSubmission = ({ assessmentId, courseId, lessonId = null, payload, assessmentTitle = '' }) => {
    const existing = assessmentQueue.find((item) => String(item.assessmentId) === String(assessmentId));
    if (existing) {
      setAssessmentQueue((current) => current.map((item) => (
        item.id === existing.id
          ? {
            ...item,
            courseId,
            lessonId,
            payload,
            assessmentTitle,
            retryCount: Number(item.retryCount || 0),
            updatedAt: new Date().toISOString(),
          }
          : item
      )));
      return existing.id;
    }

    const queueItem = createAssessmentQueueItem({ assessmentId, courseId, lessonId, payload, assessmentTitle });
    setAssessmentQueue((current) => [queueItem, ...current]);
    return queueItem.id;
  };

  const removeAssessmentQueueItem = (itemIdOrAssessmentId) => {
    setAssessmentQueue((current) => current.filter((item) =>
      item.id !== itemIdOrAssessmentId && String(item.assessmentId) !== String(itemIdOrAssessmentId)
    ));
  };

  const markAssessmentQueueRetried = (itemIdOrAssessmentId) => {
    setAssessmentQueue((current) => current.map((item) => (
      item.id === itemIdOrAssessmentId || String(item.assessmentId) === String(itemIdOrAssessmentId)
        ? { ...item, retryCount: Number(item.retryCount || 0) + 1, updatedAt: new Date().toISOString() }
        : item
    )));
  };

  const setAssessmentSnapshot = useCallback((courseId, assessments) => {
    setCourseAssessmentSnapshots((current) => ({
      ...current,
      [courseId]: Array.isArray(assessments) ? assessments : [],
    }));
  }, []);

  const getCourseAssessmentsSnapshot = useCallback((courseId) => courseAssessmentSnapshots[courseId] || [], [courseAssessmentSnapshots]);

  const value = useMemo(() => ({
    user,
    notifications,
    unreadNotificationCount: notifications.filter((notification) => !notification.read).length,
    isAuthenticated: hasAccessToken(),
    lessonNotes,
    lessonFlags,
    recentLessons,
    assessmentDrafts,
    assessmentQueue,
    addNotification,
    markAllNotificationsRead,
    saveLessonNote,
    updateLessonNote,
    removeLessonNote,
    toggleLessonReviewFlag,
    saveRecentLesson,
    saveAssessmentDraft,
    clearAssessmentDraft,
    getAssessmentDraft,
    enqueueAssessmentSubmission,
    removeAssessmentQueueItem,
    markAssessmentQueueRetried,
    setCourseAssessmentsSnapshot: setAssessmentSnapshot,
    getCourseAssessmentsSnapshot,
  }), [
    user,
    lessonNotes,
    lessonFlags,
    recentLessons,
    assessmentDrafts,
    assessmentQueue,
    notifications,
    courseAssessmentSnapshots,
  ]);

  return (
    <LearnerExperienceContext.Provider value={value}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismissToast} />
    </LearnerExperienceContext.Provider>
  );
};

export const useLearnerExperience = () => {
  const context = useContext(LearnerExperienceContext);
  if (!context) {
    throw new Error('useLearnerExperience must be used within LearnerExperienceProvider.');
  }
  return context;
};
