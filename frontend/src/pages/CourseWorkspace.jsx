import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import AiAssessmentPanel from '../components/course-assessment/AiAssessmentPanel';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import WorkspaceFlashcards, { extractVocabularyTerms } from '../components/course-workspace/WorkspaceFlashcards';
import WorkspaceLessonPanel from '../components/course-workspace/WorkspaceLessonPanel';
import WorkspaceOverview from '../components/course-workspace/WorkspaceOverview';
import WorkspaceRightRail from '../components/course-workspace/WorkspaceRightRail';
import WorkspaceSidebar from '../components/course-workspace/WorkspaceSidebar';
import { useLearnerExperience } from '../context/LearnerExperienceContext';
import { hasAccessToken } from '../utils/auth';
import { findFallbackCourse, normalizeCourse, normalizeEnrollment } from '../utils/courseModels';
import { readEnrollments } from '../utils/learnerStore';
import { isAssessmentPassed } from '../utils/selfPacedHelpers';

const getLessonId = (module, lesson, lessonIndex) => lesson.id ?? `${module.id ?? module.title}-${lesson.title}-${lessonIndex}`;
const getAssessmentStepId = (moduleId) => `__ai_assessment__:${moduleId ?? 'course'}`;

const isTemporaryAssessmentError = (error) => {
  const status = error?.response?.status;
  return !status || status === 408 || status === 429 || status >= 500;
};

const CourseWorkspace = () => {
  const { slugOrId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const {
    isAuthenticated,
    lessonNotes,
    lessonFlags,
    recentLessons,
    assessmentQueue,
    saveLessonNote,
    updateLessonNote,
    removeLessonNote,
    toggleLessonReviewFlag,
    saveRecentLesson,
    addNotification,
    enqueueAssessmentSubmission,
    removeAssessmentQueueItem,
    markAssessmentQueueRetried,
    saveAssessmentDraft,
    clearAssessmentDraft,
    getAssessmentDraft,
    setCourseAssessmentsSnapshot,
  } = useLearnerExperience();
  const [course, setCourse] = useState(() => (location.state?.course ? normalizeCourse(location.state.course) : null));
  const [enrollment, setEnrollment] = useState(() => (location.state?.enrollment ? normalizeEnrollment(location.state.enrollment) : null));
  const [loading, setLoading] = useState(!course);
  const [error, setError] = useState('');
  const [activeLessonId, setActiveLessonId] = useState(null);
  const [completedLessonIds, setCompletedLessonIds] = useState(() => new Set());
  const [savingLessonId, setSavingLessonId] = useState(null);
  const [workspaceMode, setWorkspaceMode] = useState(() => (
    ['learn', 'flashcards'].includes(location.state?.workspaceMode)
      ? location.state.workspaceMode
      : localStorage.getItem(`englishlab.workspaceMode.${slugOrId}`) || 'learn'
  ));
  const [vocabularyCount, setVocabularyCount] = useState(0);
  const [assessments, setAssessments] = useState([]);
  const [retryingQueueId, setRetryingQueueId] = useState('');
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [rightPanelMode, setRightPanelMode] = useState('transcript');
  const [videoSeekRequest, setVideoSeekRequest] = useState(null);
  const activeLessonStorageKey = `englishlab.activeLesson.${slugOrId}`;

  const assessmentsByModule = useMemo(() => {
    const grouped = new Map();
    assessments.forEach((assessment) => {
      const key = String(assessment.moduleId ?? 'course');
      const bucket = grouped.get(key) || [];
      bucket.push(assessment);
      grouped.set(key, bucket);
    });
    return grouped;
  }, [assessments]);

  const moduleProgress = useMemo(() => {
    const modules = course?.modules || [];
    const progressByModule = new Map();
    let canEnterCurrentModule = true;

    modules.forEach((module, moduleIndex) => {
      const lessons = module.lessons || [];
      const lessonItemsForModule = lessons.map((lesson, lessonIndex) => ({
        id: getLessonId(module, lesson, lessonIndex),
        module,
        moduleIndex,
        lesson,
        lessonIndex,
      }));
      const lessonIds = lessonItemsForModule.map((item) => item.id);
      const lessonsCompleted = lessonIds.every((lessonId) => completedLessonIds.has(lessonId));
      const moduleAssessments = assessmentsByModule.get(String(module.id)) || [];
      const moduleAssessmentsPassed = moduleAssessments.every(isAssessmentPassed);
      const readyForNextModule = lessonsCompleted && (moduleAssessments.length === 0 || moduleAssessmentsPassed);

      progressByModule.set(String(module.id), {
        moduleUnlocked: canEnterCurrentModule,
        lessonsCompleted,
        moduleAssessmentsPassed,
        readyForNextModule,
        lessonItems: lessonItemsForModule,
      });

      canEnterCurrentModule = readyForNextModule;
    });

    return progressByModule;
  }, [assessmentsByModule, completedLessonIds, course?.modules]);

  const lessonItems = useMemo(() => {
    if (!course?.modules?.length) return [];

    return course.modules.flatMap((module) => {
      const moduleState = moduleProgress.get(String(module.id));
      const moduleUnlocked = moduleState?.moduleUnlocked ?? false;
      const moduleLessonItems = moduleState?.lessonItems || [];

      return moduleLessonItems.map((item, lessonIndex) => {
        const previousLessonId = lessonIndex > 0 ? moduleLessonItems[lessonIndex - 1]?.id : null;
        const isLocked = lessonIndex === 0
          ? !moduleUnlocked
          : !completedLessonIds.has(previousLessonId);

        return {
          ...item,
          isLocked,
        };
      });
    });
  }, [completedLessonIds, course?.modules, moduleProgress]);

  const hasAssessments = assessments.length > 0;

  const moduleAssessmentIds = useMemo(() => new Set(
    course?.modules
      ?.filter((module, moduleIndex) => {
        const moduleAssessments = assessmentsByModule.get(String(module.id)) || [];
        const courseLevelAssessments = assessmentsByModule.get('course') || [];
        return moduleAssessments.length || (moduleIndex === (course.modules.length - 1) && courseLevelAssessments.length);
      })
      .map((module) => String(module.id)) || []
  ), [assessmentsByModule, course?.modules]);

  const assessmentLockByModule = useMemo(() => {
    const lockMap = new Map();
    (course?.modules || []).forEach((module) => {
      const moduleState = moduleProgress.get(String(module.id));
      const moduleUnlocked = moduleState?.moduleUnlocked ?? false;
      const lessonsCompleted = moduleState?.lessonsCompleted ?? false;
      lockMap.set(String(module.id), !moduleUnlocked || !lessonsCompleted);
    });
    return lockMap;
  }, [course?.modules, moduleProgress]);

  const workspaceItems = useMemo(() => {
    if (!course?.modules?.length) return lessonItems.map((item) => ({ ...item, type: 'lesson' }));
    const courseLevelAssessments = assessmentsByModule.get('course') || [];

    return course.modules.flatMap((module, moduleIndex) => {
      const moduleLessons = (module.lessons || []).map((lesson, lessonIndex) => ({
        id: getLessonId(module, lesson, lessonIndex),
        module,
        moduleIndex,
        lesson,
        lessonIndex,
        type: 'lesson',
        isLocked: lessonItems.find((item) => String(item.id) === String(getLessonId(module, lesson, lessonIndex)))?.isLocked ?? false,
      }));

      const moduleAssessments = assessmentsByModule.get(String(module.id)) || [];
      const trailingAssessments = moduleIndex === course.modules.length - 1
        ? [...moduleAssessments, ...courseLevelAssessments]
        : moduleAssessments;

      if (!trailingAssessments.length) return moduleLessons;

      return [
        ...moduleLessons,
        {
          id: getAssessmentStepId(module.id),
          type: 'assessment',
          module,
          moduleIndex,
          isLocked: assessmentLockByModule.get(String(module.id)) ?? false,
          assessments: trailingAssessments,
          title: `Bài kiểm tra cuối module: ${module.title}`,
          description: 'Nộp bài viết hoặc câu trả lời để nhận góp ý theo tiêu chí đánh giá của module.',
        },
      ];
    });
  }, [assessmentLockByModule, assessmentsByModule, course?.modules, lessonItems]);

  const activeWorkspaceItem = useMemo(() => {
    if (!workspaceItems.length) return null;
    return workspaceItems.find((item) => String(item.id) === String(activeLessonId)) ?? workspaceItems[0];
  }, [activeLessonId, workspaceItems]);
  const isAssessmentMode = activeWorkspaceItem?.type === 'assessment';
  const activeLessonHasVideo = Boolean(activeWorkspaceItem?.lesson?.videoUrl);
  const hideRightRail = isAssessmentMode || workspaceMode === 'flashcards';
  const rightPanelVisible = Boolean(
    !hideRightRail
      && (
    rightPanelMode
      && (rightPanelMode !== 'transcript' || activeLessonHasVideo)
      )
  );

  const parsedVocabularyTerms = useMemo(() => extractVocabularyTerms(course), [course]);
  const flashcardCount = Math.max(parsedVocabularyTerms.length, vocabularyCount);
  const hasVocabularyTerms = flashcardCount > 0;
  const courseNotes = useMemo(() => lessonNotes.filter((item) => String(item.courseId) === String(course?.id)), [lessonNotes, course?.id]);
  const courseReviewFlags = useMemo(() => lessonFlags.filter((item) => String(item.courseId) === String(course?.id)), [lessonFlags, course?.id]);
  const courseRecentLessons = useMemo(() => recentLessons.filter((item) => String(item.courseId) === String(course?.id)), [recentLessons, course?.id]);
  const queuedItems = useMemo(() => assessmentQueue.filter((item) => String(item.courseId) === String(course?.id)), [assessmentQueue, course?.id]);
  const applyEnrollment = (nextEnrollment) => {
    const normalizedEnrollment = normalizeEnrollment(nextEnrollment);
    setEnrollment(normalizedEnrollment);
    setCompletedLessonIds(new Set(normalizedEnrollment.completedLessonIds));
  };

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [slugOrId]);

  useEffect(() => () => {
    window.getSelection?.()?.removeAllRanges?.();
    document.body.style.overflow = '';
  }, []);

  useEffect(() => {
    if (['learn', 'flashcards'].includes(location.state?.workspaceMode)) {
      setWorkspaceMode(location.state.workspaceMode);
      return;
    }
    setWorkspaceMode(localStorage.getItem(`englishlab.workspaceMode.${slugOrId}`) || 'learn');
  }, [slugOrId, location.state?.workspaceMode]);

  useEffect(() => {
    localStorage.setItem(`englishlab.workspaceMode.${slugOrId}`, workspaceMode);
  }, [slugOrId, workspaceMode]);

  useEffect(() => {
    if (!activeLessonId) return;
    localStorage.setItem(activeLessonStorageKey, String(activeLessonId));
  }, [activeLessonId, activeLessonStorageKey]);

  useEffect(() => {
    if (!workspaceItems.length) return;
    const activeLessonStillExists = workspaceItems.some((item) => String(item.id) === String(activeLessonId));
    const storedLessonId = localStorage.getItem(activeLessonStorageKey);

    if (!activeLessonId || !activeLessonStillExists) {
      const storedItem = workspaceItems.find((item) => String(item.id) === String(storedLessonId) && !item.isLocked);
      const firstUnlockedItem = workspaceItems.find((item) => !item.isLocked) || workspaceItems[0];
      setActiveLessonId(storedItem?.id || firstUnlockedItem?.id || workspaceItems[0].id);
      return;
    }

    const currentItem = workspaceItems.find((item) => String(item.id) === String(activeLessonId));
    if (currentItem?.isLocked) {
      const fallbackLesson = workspaceItems.find((item) => item.type === 'lesson' && !item.isLocked);
      if (fallbackLesson) setActiveLessonId(fallbackLesson.id);
    }
  }, [activeLessonId, workspaceItems]);

  useEffect(() => {
    if (course && !hasVocabularyTerms && workspaceMode === 'flashcards') {
      setWorkspaceMode('learn');
    }
  }, [course, hasVocabularyTerms, workspaceMode]);

  useEffect(() => {
    if (workspaceMode === 'flashcards' && rightPanelMode) {
      setRightPanelMode(null);
    }
  }, [rightPanelMode, workspaceMode]);

  useEffect(() => {
    let active = true;
    if (!course?.id || !hasAccessToken()) {
      setVocabularyCount(parsedVocabularyTerms.length);
      return undefined;
    }

    setVocabularyCount(parsedVocabularyTerms.length);
    courseApi.getVocabularyTerms(course.id)
      .then((terms) => {
        if (active) setVocabularyCount(Array.isArray(terms) ? terms.length : 0);
      })
      .catch(() => {
        if (active) setVocabularyCount(parsedVocabularyTerms.length);
      });

    return () => {
      active = false;
    };
  }, [course?.id, parsedVocabularyTerms.length]);

  useEffect(() => {
    if (!hasAccessToken()) {
      navigate('/login', { replace: true, state: { from: `/courses/${slugOrId}/learn` } });
      return undefined;
    }

    let active = true;
    const loadWorkspace = async () => {
      setLoading(true);
      setError('');
      try {
        const [courseResponse, myCourses] = await Promise.all([
          courseApi.getOnlineCourse(slugOrId),
          courseApi.getMyOnlineCourses(),
        ]);

        if (!active) return;

        const normalizedCourse = normalizeCourse({ ...courseResponse, registered: true });
        const localStoredEnrollments = readEnrollments().map(normalizeEnrollment);
        const matchedEnrollment = [...myCourses.map(normalizeEnrollment), ...localStoredEnrollments]
          .find((item) => item.courseSlug === normalizedCourse.slug || String(item.courseId) === String(normalizedCourse.id));

        if (!matchedEnrollment) {
          navigate(`/courses/${normalizedCourse.slug}`, { replace: true, state: { course: normalizedCourse } });
          return;
        }

        setCourse(normalizedCourse);
        applyEnrollment(matchedEnrollment);
        courseApi.getCourseAssessments(normalizedCourse.id)
          .then((items) => {
            if (!active) return;
            setAssessments(items);
            setCourseAssessmentsSnapshot(normalizedCourse.id, items);
          })
          .catch(() => {
            if (!active) return;
            setAssessments([]);
            setCourseAssessmentsSnapshot(normalizedCourse.id, []);
          });
      } catch {
        if (!active) return;
        const stateCourse = location.state?.course ? normalizeCourse(location.state.course) : null;
        const stateEnrollment = location.state?.enrollment ? normalizeEnrollment(location.state.enrollment) : null;
        const localEnrollment = readEnrollments()
          .map(normalizeEnrollment)
          .find((item) => item.courseSlug === slugOrId || String(item.courseId) === String(slugOrId));

        if (stateCourse?.registered) {
          setCourse(stateCourse);
          applyEnrollment(
            stateEnrollment
              ?? normalizeEnrollment({
                courseId: stateCourse.id,
                courseSlug: stateCourse.slug,
                courseTitle: stateCourse.title,
                thumbnailUrl: stateCourse.thumbnailUrl,
                progressPercent: stateCourse.progressPercent ?? 0,
              }),
          );
          return;
        }

        const fallback = findFallbackCourse(slugOrId);
        if (fallback && (location.state?.course?.registered || localEnrollment)) {
          setCourse(normalizeCourse({ ...fallback, registered: true }));
          applyEnrollment(localEnrollment || normalizeEnrollment({
            courseId: fallback.id,
            courseSlug: fallback.slug,
            courseTitle: fallback.title,
            thumbnailUrl: fallback.thumbnailUrl,
            progressPercent: 68,
          }));
        } else {
          setError('Không mở được không gian học của khóa học này.');
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    loadWorkspace();

    return () => {
      active = false;
    };
  }, [
    slugOrId,
    navigate,
    location.state?.course,
    location.state?.enrollment,
    location.state?.workspaceMode,
    setCourseAssessmentsSnapshot,
  ]);

  useEffect(() => {
    if (activeWorkspaceItem?.type !== 'lesson' || !course) return;
    saveRecentLesson({
      courseId: course.id,
      lessonId: activeWorkspaceItem.id,
      lessonTitle: activeWorkspaceItem.lesson?.title || '',
      courseTitle: course.title,
    });
  }, [activeWorkspaceItem, course, saveRecentLesson]);

  const handleSelectLesson = (lessonId) => {
    const targetItem = workspaceItems.find((item) => String(item.id) === String(lessonId));
    if (targetItem?.isLocked) return;
    setActiveLessonId(lessonId);
  };

  const handleToggleComplete = async (lessonId) => {
    if (!course?.id || !lessonId || savingLessonId) return;
    const shouldComplete = !completedLessonIds.has(lessonId);
    setSavingLessonId(lessonId);
    setError('');

    try {
      let nextEnrollment;
      try {
        nextEnrollment = await courseApi.updateLessonProgress(course.id, lessonId, shouldComplete);
      } catch (err) {
        const message = err?.response?.data?.message || '';
        if (!/not enrolled|chưa đăng ký|not registered/i.test(message)) throw err;
        await courseApi.registerOnlineCourse(course.id);
        nextEnrollment = await courseApi.updateLessonProgress(course.id, lessonId, shouldComplete);
      }
      applyEnrollment(nextEnrollment);
    } catch (err) {
      const message = err?.response?.data?.message || err?.response?.data?.error || err?.message;
      setError(message ? `Không lưu được tiến độ học tập: ${message}` : 'Không thể lưu tiến độ học tập. Vui lòng thử lại.');
    } finally {
      setSavingLessonId(null);
    }
  };

  const handleMoveLesson = (direction) => {
    if (!activeWorkspaceItem) return;
    const currentIndex = workspaceItems.findIndex((item) => String(item.id) === String(activeWorkspaceItem.id));
    let nextIndex = currentIndex + direction;
    while (nextIndex >= 0 && nextIndex < workspaceItems.length) {
      const nextItem = workspaceItems[nextIndex];
      if (!nextItem?.isLocked) {
        setActiveLessonId(nextItem.id);
        return;
      }
      nextIndex += direction;
    }
  };

  const handleSeekTranscript = (seconds) => {
    const parsed = Number(seconds);
    if (!Number.isFinite(parsed) || parsed < 0) return;
    setVideoSeekRequest({
      seconds: parsed,
      requestedAt: Date.now(),
    });
  };

  const refreshAssessments = async (courseId) => {
    if (!courseId) return;
    try {
      const items = await courseApi.getCourseAssessments(courseId);
      setAssessments(items);
      setCourseAssessmentsSnapshot(courseId, items);
    } catch {
      setAssessments([]);
      setCourseAssessmentsSnapshot(courseId, []);
    }
  };

  const handleSubmitAssessment = async (assessmentId, payload) => {
    try {
      const response = await courseApi.submitAssessment(assessmentId, payload);
      await refreshAssessments(course?.id);
      clearAssessmentDraft(assessmentId);
      removeAssessmentQueueItem(assessmentId);
      addNotification({
        type: 'learning',
        title: 'Nộp bài thành công',
        message: 'Bài làm của bạn đã được gửi thành công.',
        courseId: course?.id,
        courseTitle: course?.title,
        actionPath: `/courses/${course?.slug}/learn`,
      });
      return response;
    } catch (submissionError) {
      if (isTemporaryAssessmentError(submissionError)) {
        enqueueAssessmentSubmission({
          assessmentId,
          courseId: course?.id,
          lessonId: activeWorkspaceItem?.id || null,
          payload,
          assessmentTitle: activeWorkspaceItem?.title || 'Bài đánh giá AI',
        });
        addNotification({
          type: 'learning',
          title: 'Bài làm đã được lưu tạm thời',
          message: 'Hệ thống đang xử lý bài làm. Bạn không cần làm lại bài.',
          courseId: course?.id,
          courseTitle: course?.title,
          actionPath: `/courses/${course?.slug}/learn`,
        });
        throw new Error('Bài làm của bạn đã được lưu tạm thời. Hệ thống sẽ tự động gửi lại sau ít phút.');
      }

      throw new Error('Hiện chưa thể hoàn tất việc gửi bài. Vui lòng kiểm tra lại nội dung rồi thử lại.');
    }
  };

  const handleRetryQueueItem = async (item) => {
    if (!item || retryingQueueId) return;
    setRetryingQueueId(item.id);
    try {
      markAssessmentQueueRetried(item.id);
      const response = await courseApi.submitAssessment(item.assessmentId, item.payload);
      if (response) {
        await refreshAssessments(course?.id);
        removeAssessmentQueueItem(item.id);
        clearAssessmentDraft(item.assessmentId);
        addNotification({
          type: 'learning',
          title: 'Bài làm đã được gửi thành công',
          message: 'Bài làm đã được gửi lại thành công.',
          courseId: course?.id,
          courseTitle: course?.title,
          actionPath: `/courses/${course?.slug}/learn`,
        });
      }
    } catch {
      setError('Bài làm đã được lưu an toàn và vẫn đang chờ gửi lại khi hệ thống sẵn sàng.');
    } finally {
      setRetryingQueueId('');
    }
  };

  if (loading) {
    return (
      <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
        <CourseGlobalStyles />
        <Header />
        <main className="mx-auto max-w-[1320px] px-4 py-10 md:px-10">
          <div className="rounded-3xl border border-[#dfbfbd]/30 bg-white p-10 text-center text-[#584140]">Đang mở không gian học...</div>
        </main>
      </div>
    );
  }

  if (!course) {
    return (
      <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
        <CourseGlobalStyles />
        <Header />
        <main className="mx-auto max-w-[1320px] px-4 py-10 md:px-10">
          <div className="rounded-3xl border border-[#dfbfbd]/30 bg-white p-10 text-center text-[#93000a]">{error || 'Không tìm thấy khóa học.'}</div>
        </main>
      </div>
    );
  }

  return (
    <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto w-full max-w-[1880px] px-3 pb-10 pt-5 md:px-5 2xl:px-8">
        {queuedItems.length ? (
          <div className="mb-6 rounded-3xl border border-[#f2dfb3] bg-[#fff8e7] p-5 text-sm text-[#9a6700]">
            <p className="font-extrabold">Có {queuedItems.length} bài làm đang chờ gửi lại.</p>
            <p className="mt-2">Bài làm đã được lưu an toàn và sẽ được gửi lại khi hệ thống sẵn sàng.</p>
            <div className="mt-4 flex flex-wrap gap-3">
              {queuedItems.slice(0, 2).map((item) => (
                <button key={item.id} className="rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-extrabold text-white" onClick={() => handleRetryQueueItem(item)} type="button">
                  {retryingQueueId === item.id ? 'Đang gửi lại...' : 'Thử gửi lại'}
                </button>
              ))}
            </div>
          </div>
        ) : null}

        {error ? (
          <div className="mb-6 rounded-3xl border border-[#f0d4d7] bg-white p-5 text-sm font-semibold text-[#93000a]">
            {error}
          </div>
        ) : null}

        <div className="mb-4">
          <WorkspaceOverview
            course={course}
            enrollment={enrollment}
            hasVocabularyTerms={hasVocabularyTerms}
            workspaceMode={workspaceMode}
            onWorkspaceModeChange={setWorkspaceMode}
          />
        </div>

        <div className={`grid gap-4 xl:items-start ${
          hideRightRail
            ? sidebarCollapsed
              ? 'xl:grid-cols-[56px_minmax(0,1fr)]'
              : 'xl:grid-cols-[360px_minmax(0,1fr)]'
            : sidebarCollapsed
              ? rightPanelVisible ? 'xl:grid-cols-[56px_minmax(0,1fr)_420px]' : 'xl:grid-cols-[56px_minmax(0,1fr)_76px]'
              : rightPanelVisible ? 'xl:grid-cols-[360px_minmax(0,1fr)_420px]' : 'xl:grid-cols-[360px_minmax(0,1fr)_76px]'
        }`}
        >
          <WorkspaceSidebar
            course={course}
            activeLessonId={activeLessonId}
            assessmentLockByModule={assessmentLockByModule}
            assessmentModuleIds={moduleAssessmentIds}
            completedLessonIds={completedLessonIds}
            lessonItems={lessonItems}
            moduleProgress={moduleProgress}
            hasAssessments={hasAssessments}
            collapsed={sidebarCollapsed}
            onCollapse={() => setSidebarCollapsed(true)}
            onExpand={() => setSidebarCollapsed(false)}
            onSelectLesson={handleSelectLesson}
          />

          <div className="min-w-0 flex-1 space-y-6">
            {workspaceMode === 'flashcards' ? (
              <WorkspaceFlashcards course={course} totalTerms={flashcardCount} />
            ) : activeWorkspaceItem?.type === 'assessment' ? (
              <AiAssessmentPanel
                assessments={activeWorkspaceItem.assessments}
                moduleTitle={activeWorkspaceItem.module?.title}
                isLocked={activeWorkspaceItem.isLocked}
                onMoveStep={handleMoveLesson}
                onSubmitAssessment={handleSubmitAssessment}
                draftGetter={getAssessmentDraft}
                onDraftChange={saveAssessmentDraft}
                onClearDraft={clearAssessmentDraft}
              />
            ) : (
              <WorkspaceLessonPanel
                activeLessonItem={activeWorkspaceItem}
                completedLessonIds={completedLessonIds}
                lessonItems={lessonItems}
                savingLessonId={savingLessonId}
                canPersist={isAuthenticated}
                onMoveLesson={handleMoveLesson}
                onSelectLesson={handleSelectLesson}
                onToggleComplete={handleToggleComplete}
                onOpenNotes={() => setRightPanelMode('notes')}
                onSaveLessonNote={({ content, selectedText }) => saveLessonNote({
                  courseId: course.id,
                  lessonId: activeWorkspaceItem?.id,
                  content,
                  selectedText,
                  source: 'lesson',
                  lessonTitle: activeWorkspaceItem?.lesson?.title || activeWorkspaceItem?.title || '',
                  courseTitle: course.title,
                })}
                seekRequest={videoSeekRequest}
              />
            )}

          </div>

          {!hideRightRail ? (
            <div className="hidden min-w-0 self-stretch xl:block">
              <WorkspaceRightRail
                activeLesson={activeWorkspaceItem}
                mode={rightPanelVisible ? rightPanelMode : null}
                notes={courseNotes}
                reviewFlags={courseReviewFlags}
                recentLessons={courseRecentLessons}
                canPersist={isAuthenticated}
                onModeChange={(nextMode) => setRightPanelMode((current) => (current === nextMode ? null : nextMode))}
                onSeekTranscript={handleSeekTranscript}
                onSaveTranscriptNote={({ content, selectedText, transcriptStartSeconds }) => saveLessonNote({
                  courseId: course.id,
                  lessonId: activeWorkspaceItem?.id,
                  content,
                  selectedText,
                  transcriptStartSeconds,
                  source: 'transcript',
                  lessonTitle: activeWorkspaceItem?.lesson?.title || activeWorkspaceItem?.title || '',
                  courseTitle: course.title,
                })}
                onSaveManualNote={(content) => saveLessonNote({
                  courseId: course.id,
                  lessonId: activeWorkspaceItem?.id,
                  content,
                  lessonTitle: activeWorkspaceItem?.lesson?.title || activeWorkspaceItem?.title || '',
                  courseTitle: course.title,
                })}
                onUpdateNote={updateLessonNote}
                onDeleteNote={removeLessonNote}
                onToggleReviewFlag={() => toggleLessonReviewFlag({
                  courseId: course.id,
                  lessonId: activeWorkspaceItem?.id,
                  lessonTitle: activeWorkspaceItem?.lesson?.title || activeWorkspaceItem?.title || '',
                  courseTitle: course.title,
                })}
                onSelectRecentLesson={handleSelectLesson}
              />
            </div>
          ) : null}
        </div>
      </main>
    </div>
  );
};

export default CourseWorkspace;
