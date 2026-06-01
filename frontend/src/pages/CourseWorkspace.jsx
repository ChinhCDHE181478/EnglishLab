import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import WorkspaceFlashcards, { extractVocabularyTerms } from '../components/course-workspace/WorkspaceFlashcards';
import WorkspaceLessonPanel from '../components/course-workspace/WorkspaceLessonPanel';
import WorkspaceOverview from '../components/course-workspace/WorkspaceOverview';
import WorkspaceRightRail from '../components/course-workspace/WorkspaceRightRail';
import WorkspaceSidebar from '../components/course-workspace/WorkspaceSidebar';
import { hasAccessToken } from '../utils/auth';
import { findFallbackCourse, normalizeCourse, normalizeEnrollment } from '../utils/courseModels';

const getLessonId = (module, lesson, lessonIndex) => lesson.id ?? `${module.id ?? module.title}-${lesson.title}-${lessonIndex}`;

const CourseWorkspace = () => {
  const { slugOrId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [course, setCourse] = useState(() => (location.state?.course ? normalizeCourse(location.state.course) : null));
  const [enrollment, setEnrollment] = useState(() => (location.state?.enrollment ? normalizeEnrollment(location.state.enrollment) : null));
  const [loading, setLoading] = useState(!course);
  const [error, setError] = useState('');
  const [activeLessonId, setActiveLessonId] = useState(null);
  const [completedLessonIds, setCompletedLessonIds] = useState(() => new Set());
  const [savingLessonId, setSavingLessonId] = useState(null);
  const [workspaceMode, setWorkspaceMode] = useState(() => localStorage.getItem(`englishlab.workspaceMode.${slugOrId}`) || 'learn');
  const [vocabularyCount, setVocabularyCount] = useState(0);

  const lessonItems = useMemo(() => {
    if (!course?.modules?.length) return [];

    return course.modules.flatMap((module, moduleIndex) =>
      (module.lessons || []).map((lesson, lessonIndex) => ({
        id: getLessonId(module, lesson, lessonIndex),
        module,
        moduleIndex,
        lesson,
        lessonIndex,
      }))
    );
  }, [course]);

  const activeLessonItem = useMemo(() => {
    if (!lessonItems.length) return null;
    return lessonItems.find((item) => String(item.id) === String(activeLessonId)) ?? lessonItems[0];
  }, [activeLessonId, lessonItems]);

  const parsedVocabularyTerms = useMemo(() => extractVocabularyTerms(course), [course]);
  const flashcardCount = Math.max(parsedVocabularyTerms.length, vocabularyCount);
  const hasVocabularyTerms = flashcardCount > 0;

  const applyEnrollment = (nextEnrollment) => {
    const normalizedEnrollment = normalizeEnrollment(nextEnrollment);
    setEnrollment(normalizedEnrollment);
    setCompletedLessonIds(new Set(normalizedEnrollment.completedLessonIds));
  };

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [slugOrId]);

  useEffect(() => {
    setWorkspaceMode(localStorage.getItem(`englishlab.workspaceMode.${slugOrId}`) || 'learn');
  }, [slugOrId]);

  useEffect(() => {
    localStorage.setItem(`englishlab.workspaceMode.${slugOrId}`, workspaceMode);
  }, [slugOrId, workspaceMode]);

  useEffect(() => {
    if (!lessonItems.length) return;

    const activeLessonStillExists = lessonItems.some((item) => String(item.id) === String(activeLessonId));
    if (!activeLessonId || !activeLessonStillExists) {
      setActiveLessonId(lessonItems[0].id);
    }
  }, [activeLessonId, lessonItems]);

  useEffect(() => {
    if (course && !hasVocabularyTerms && workspaceMode === 'flashcards') {
      setWorkspaceMode('learn');
    }
  }, [course, hasVocabularyTerms, workspaceMode]);

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
        const matchedEnrollment = myCourses
          .map(normalizeEnrollment)
          .find((item) => item.courseSlug === normalizedCourse.slug || String(item.courseId) === String(normalizedCourse.id));

        if (!matchedEnrollment) {
          navigate(`/courses/${normalizedCourse.slug}`, { replace: true, state: { course: normalizedCourse } });
          return;
        }

        setCourse(normalizedCourse);
        applyEnrollment(matchedEnrollment);
      } catch {
        if (!active) return;
        const stateCourse = location.state?.course ? normalizeCourse(location.state.course) : null;
        const stateEnrollment = location.state?.enrollment ? normalizeEnrollment(location.state.enrollment) : null;

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
              })
          );
          return;
        }

        const fallback = findFallbackCourse(slugOrId);
        if (fallback && location.state?.course?.registered) {
          setCourse(normalizeCourse({ ...fallback, registered: true }));
          applyEnrollment(normalizeEnrollment({ courseId: fallback.id, courseSlug: fallback.slug, courseTitle: fallback.title, thumbnailUrl: fallback.thumbnailUrl, progressPercent: 68 }));
        } else {
          setError('Không mở được workspace của khóa học này.');
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    loadWorkspace();

    return () => {
      active = false;
    };
  }, [slugOrId, navigate, location.state]);

  const handleSelectLesson = (lessonId) => {
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
        if (!/not enrolled|chưa đăng ký|not registered/i.test(message)) {
          throw err;
        }

        await courseApi.registerOnlineCourse(course.id);
        nextEnrollment = await courseApi.updateLessonProgress(course.id, lessonId, shouldComplete);
      }
      applyEnrollment(nextEnrollment);
    } catch (err) {
      const status = err?.response?.status;
      const message = err?.response?.data?.message || err?.response?.data?.error || err?.message;
      setError(message ? `Không lưu được tiến độ bài học: ${message}` : `Không lưu được tiến độ bài học${status ? ` (${status})` : ''}. Vui lòng thử lại.`);
    } finally {
      setSavingLessonId(null);
    }
  };

  const handleMoveLesson = (direction) => {
    if (!activeLessonItem) return;

    const currentIndex = lessonItems.findIndex((item) => String(item.id) === String(activeLessonItem.id));
    const nextItem = lessonItems[currentIndex + direction];
    if (nextItem) {
      setActiveLessonId(nextItem.id);
    }
  };

  if (loading) {
    return (
      <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
        <CourseGlobalStyles />
        <Header hideTeacherLinks />
        <main className="mx-auto max-w-[1320px] px-4 py-10 md:px-10">
          <div className="rounded-3xl border border-[#dfbfbd]/30 bg-white p-10 text-center text-[#584140]">Đang mở workspace khóa học...</div>
        </main>
      </div>
    );
  }

  if (!course || !enrollment) {
    return (
      <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
        <CourseGlobalStyles />
        <Header hideTeacherLinks />
        <main className="mx-auto max-w-[1320px] px-4 py-10 md:px-10">
          <div className="rounded-3xl border border-[#dfbfbd]/30 bg-white p-10 text-center text-[#93000a]">{error || 'Bạn chưa có quyền vào khóa học này.'}</div>
        </main>
      </div>
    );
  }

  return (
    <div className="course-page min-h-screen bg-[#faf9f8] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header hideTeacherLinks />
      <div className="mx-auto flex max-w-[1600px]">
        <WorkspaceSidebar
          activeLessonId={activeLessonItem?.id}
          completedLessonIds={completedLessonIds}
          course={course}
          onSelectLesson={handleSelectLesson}
        />
        <main className="min-w-0 flex-1 px-4 py-8 md:px-8">
          <div className="mb-6">
            <Link className="group inline-flex items-center gap-2 text-sm font-bold text-[#8a0018]" to={`/courses/${course.slug}`} state={{ course }}>
              <ArrowLeft className="h-4 w-4 shrink-0" />
              <span className="group-hover:underline">Quay lại chi tiết khóa học</span>
            </Link>
          </div>
          {error ? (
            <div className="mb-6 rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
              {error}
            </div>
          ) : null}
          {hasVocabularyTerms ? (
            <div className="mb-6 inline-flex rounded-2xl bg-[#f4f3f3] p-1">
              <button
                className={`inline-flex cursor-pointer items-center gap-2 rounded-xl px-5 py-3 text-sm font-extrabold transition ${workspaceMode === 'learn' ? 'bg-white text-[#8a0018] shadow-sm' : 'text-[#584140] hover:text-[#8a0018]'}`}
                type="button"
                onClick={() => setWorkspaceMode('learn')}
              >
                <span className="material-symbols-outlined text-[18px]">school</span>
                Bài học
              </button>
              <button
                className={`inline-flex cursor-pointer items-center gap-2 rounded-xl px-5 py-3 text-sm font-extrabold transition ${workspaceMode === 'flashcards' ? 'bg-white text-[#8a0018] shadow-sm' : 'text-[#584140] hover:text-[#8a0018]'}`}
                type="button"
                onClick={() => setWorkspaceMode('flashcards')}
              >
                <span className="material-symbols-outlined text-[18px]">style</span>
                Flashcards
                <span className="rounded-full bg-[#fff0f1] px-2 py-0.5 text-xs text-[#8a0018]">{flashcardCount}</span>
              </button>
            </div>
          ) : null}
          <div className="grid items-start gap-8 xl:grid-cols-[1fr_340px]">
            <div className="space-y-8">
              {workspaceMode === 'flashcards' ? (
                <WorkspaceFlashcards course={course} />
              ) : (
                <>
                  <WorkspaceOverview course={course} enrollment={enrollment} />
                  <WorkspaceLessonPanel
                    activeLessonItem={activeLessonItem}
                    completedLessonIds={completedLessonIds}
                    course={course}
                    lessonItems={lessonItems}
                    savingLessonId={savingLessonId}
                    onMoveLesson={handleMoveLesson}
                    onSelectLesson={handleSelectLesson}
                    onToggleComplete={handleToggleComplete}
                  />
                </>
              )}
            </div>
            <WorkspaceRightRail enrollment={enrollment} />
          </div>
        </main>
      </div>
    </div>
  );
};

export default CourseWorkspace;
