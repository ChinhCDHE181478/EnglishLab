import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, BookOpen, CheckCircle2, ChevronDown, FileText, Info, Lock, MessageCircle, Star, StickyNote, Trophy, X } from 'lucide-react';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import CourseFooter from '../components/course/CourseFooter';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import CourseDiscussionSection from '../components/course-detail/CourseDiscussionSection';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import { hasAccessToken } from '../utils/auth';
import { normalizeCourse, normalizeEnrollment } from '../utils/courseModels';
import { isActiveOnlineEnrollment } from '../utils/enrollmentAccess';
import { isAssessmentPassed } from '../utils/selfPacedHelpers';
import { resolveScoreCap } from '../utils/ieltsBandScale';

const getLessonId = (module, lesson, lessonIndex) => lesson.id ?? `${module.id ?? module.title}-${lesson.title}-${lessonIndex}`;
const getAssessmentStepId = (moduleId) => `__ai_assessment__:${moduleId ?? 'course'}`;

const navItems = [
  { id: 'materials', label: 'Tài liệu khóa học', icon: BookOpen },
  { id: 'grades', label: 'Điểm', icon: Trophy },
  { id: 'notes', label: 'Ghi chú', icon: StickyNote },
  { id: 'forums', label: 'Diễn đàn thảo luận', icon: MessageCircle },
  { id: 'info', label: 'Thông tin khóa học', icon: Info },
];

const countLessons = (modules = []) => modules.reduce((total, module) => total + (module.lessons?.length || 0), 0);

const formatDateTime = (value) => {
  if (!value) return 'Không có';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
};

const formatScore = (submission, assessment) => {
  if (!submission?.id) return '-';
  if (submission.aiScore == null || submission.aiScore === '') return 'Đã nộp';
  const score = Number(submission.aiScore);
  const max = resolveScoreCap(assessment) ?? Number(assessment?.maxScore) ?? 9;
  if (!Number.isFinite(score)) return '-';
  return `${Number.isInteger(score) ? score : score.toFixed(1)}/${Number.isInteger(max) ? max : max.toFixed(1)}`;
};

const getAssessmentStatus = (assessment) => {
  const status = String(assessment?.latestSubmission?.status || '');
  if (!assessment?.latestSubmission?.id) return { label: 'Chưa nộp', done: false };
  if (isAssessmentPassed(assessment)) return { label: 'Đã đạt', done: true };
  if (status === 'NEEDS_IMPROVEMENT') return { label: 'Cần cải thiện', done: false };
  return { label: 'Đã nộp', done: true };
};

const CourseHome = () => {
  const { slugOrId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('materials');
  const [materialsExpanded, setMaterialsExpanded] = useState(true);
  const [openModuleId, setOpenModuleId] = useState(null);
  const [course, setCourse] = useState(() => (location.state?.course ? normalizeCourse(location.state.course) : null));
  const [enrollment, setEnrollment] = useState(() => (location.state?.enrollment ? normalizeEnrollment(location.state.enrollment) : null));
  const [loading, setLoading] = useState(!course);
  const [error, setError] = useState('');
  const [assessments, setAssessments] = useState([]);
  const [completion, setCompletion] = useState(null);
  const [certificate, setCertificate] = useState(null);
  const [ratingInfo, setRatingInfo] = useState(null);
  const [showRatingForm, setShowRatingForm] = useState(false);
  const [selectedRating, setSelectedRating] = useState(0);
  const [ratingComment, setRatingComment] = useState('');
  const [ratingSaving, setRatingSaving] = useState(false);
  const [ratingError, setRatingError] = useState('');

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [slugOrId]);

  useEffect(() => {
    let active = true;
    const hasCourse = course && (course.slug === slugOrId || String(course.id) === String(slugOrId));
    if (!hasCourse) {
      setLoading(true);
    }
    setError('');

    const loadEnrollments = async () => {
      if (!hasAccessToken()) return [];
      try {
        return await courseApi.getMyOnlineCourses();
      } catch {
        return [];
      }
    };

    const loadOptionalCourseData = async (loader, fallback) => {
      try {
        return await loader();
      } catch {
        return fallback;
      }
    };

    const loadCourseHome = async () => {
      try {
        const [courseResponse, enrollments] = await Promise.all([
          courseApi.getOnlineCourse(slugOrId),
          loadEnrollments(),
        ]);
        if (!active) return;
        const normalizedCourse = normalizeCourse(courseResponse);
        const normalizedEnrollments = enrollments.map(normalizeEnrollment);
        const matchedEnrollment = normalizedEnrollments.find(
          (item) => item.courseSlug === normalizedCourse.slug || String(item.courseId) === String(normalizedCourse.id),
        );

        if (hasAccessToken() && (!matchedEnrollment || !isActiveOnlineEnrollment(matchedEnrollment))) {
          navigate(`/courses/${normalizedCourse.slug}`, {
            replace: true,
            state: {
              course: normalizedCourse,
              accessMessage: 'Bạn cần đăng ký khóa học (hoặc đăng ký lại nếu đã hủy) để vào trang học.',
            },
          });
          return;
        }

        setCourse({ ...normalizedCourse, registered: Boolean(matchedEnrollment) });
        setEnrollment(matchedEnrollment || null);
        setOpenModuleId(normalizedCourse.modules?.[0]?.id ?? normalizedCourse.modules?.[0]?.title ?? null);
        if (hasAccessToken()) {
          const [assessmentItems, completionResponse, certificateResponse, ratingResponse] = await Promise.all([
            loadOptionalCourseData(() => courseApi.getCourseAssessments(normalizedCourse.id), []),
            matchedEnrollment ? loadOptionalCourseData(() => courseApi.getCourseCompletion(normalizedCourse.id), null) : null,
            matchedEnrollment ? loadOptionalCourseData(() => courseApi.getCourseCertificate(normalizedCourse.id), null) : null,
            matchedEnrollment ? loadOptionalCourseData(() => courseApi.getMyCourseRating(normalizedCourse.id), null) : null,
          ]);
          if (active) setAssessments(Array.isArray(assessmentItems) ? assessmentItems : []);
          if (active) setCompletion(completionResponse);
          if (active) setCertificate(certificateResponse);
          if (active && ratingResponse) {
            setRatingInfo(ratingResponse);
            setSelectedRating(ratingResponse.myRating || 0);
            setRatingComment(ratingResponse.myComment || '');
            setCourse((current) => current ? {
              ...current,
              averageRating: ratingResponse.averageRating,
              reviewCount: ratingResponse.reviewCount,
            } : current);
          }
        } else {
          setAssessments([]);
          setCompletion(null);
          setCertificate(null);
          setRatingInfo(null);
        }
      } catch {
        if (!active) return;
        setCourse(null);
        setCompletion(null);
        setCertificate(null);
        setRatingInfo(null);
        setError('Không mở được trang tổng quan khóa học. Vui lòng thử lại.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadCourseHome();

    return () => {
      active = false;
    };
  }, [slugOrId, navigate]);

  const completedLessonIds = useMemo(() => new Set(enrollment?.completedLessonIds || []), [enrollment?.completedLessonIds]);
  const totalLessons = countLessons(course?.modules || []);
  const completedCount = completedLessonIds.size;
  const progressPercent = Math.round(Number(completion?.progressPercent ?? enrollment?.progressPercent ?? course?.progressPercent ?? 0));
  const courseCompleted = Boolean(completion?.eligibleForCertificate);
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
    const result = new Map();
    let previousModulesReady = true;

    modules.forEach((module, moduleIndex) => {
      const lessons = module.lessons || [];
      const moduleAssessments = [
        ...(assessmentsByModule.get(String(module.id)) || []),
        ...(moduleIndex === modules.length - 1 ? (assessmentsByModule.get('course') || []) : []),
      ];
      const lessonsCompleted = lessons.length > 0 && lessons.every((lesson, lessonIndex) => completedLessonIds.has(getLessonId(module, lesson, lessonIndex)));
      const assessmentsPassed = moduleAssessments.length === 0 || moduleAssessments.every(isAssessmentPassed);
      const unlocked = previousModulesReady;

      result.set(String(module.id ?? module.title ?? moduleIndex), {
        unlocked,
        lessonsCompleted,
        assessmentsPassed,
        moduleAssessments,
        readyForNextModule: lessonsCompleted && assessmentsPassed,
      });

      previousModulesReady = previousModulesReady && lessonsCompleted && assessmentsPassed;
    });

    return result;
  }, [assessmentsByModule, completedLessonIds, course?.modules]);

  const openLesson = (module, lesson, lessonIndex) => {
    if (!course?.slug) return;
    const lessonId = getLessonId(module, lesson, lessonIndex);
    localStorage.setItem(`englishlab.activeLesson.${course.slug}`, String(lessonId));
    navigate(`/courses/${course.slug}/learn`, { state: { course, enrollment, workspaceMode: 'learn' } });
  };

  const openAssessment = (module) => {
    if (!course?.slug) return;
    localStorage.setItem(`englishlab.activeLesson.${course.slug}`, getAssessmentStepId(module?.id));
    navigate(`/courses/${course.slug}/learn`, { state: { course, enrollment, workspaceMode: 'learn' } });
  };

  const showLockedAssessmentMessage = () => {
    setError('Bạn cần hoàn thành toàn bộ bài học trong mô-đun trước khi mở bài đánh giá cuối mô-đun.');
  };

  const openRatingForm = () => {
    setRatingError('');
    setShowRatingForm(true);
  };

  const saveRating = async () => {
    if (!selectedRating) {
      setRatingError('Vui lòng chọn số sao trước khi gửi.');
      return;
    }
    setRatingSaving(true);
    setRatingError('');
    try {
      const response = await courseApi.saveCourseRating(course.id, { rating: selectedRating, comment: ratingComment });
      setRatingInfo(response);
      setCourse((current) => ({ ...current, averageRating: response.averageRating, reviewCount: response.reviewCount }));
      setShowRatingForm(false);
    } catch (requestError) {
      setRatingError(requestError?.response?.data?.message || 'Không thể lưu đánh giá. Vui lòng thử lại.');
    } finally {
      setRatingSaving(false);
    }
  };

  const openFirstLesson = () => {
    const firstModule = course?.modules?.[0];
    const firstLesson = firstModule?.lessons?.[0];
    if (!firstModule || !firstLesson) return;
    openLesson(firstModule, firstLesson, 0);
  };

  const renderMaterials = () => (
    <div>
      <div className="mb-6 rounded-lg border border-[#f1dfb8] bg-[#f2e8cf] p-6">
        <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#9e001f]">EnglishLab course home</p>
        <h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#730014]">{course.title}</h1>
        <p className="mt-2 max-w-3xl text-sm leading-7 text-[#1a1c1c]">{course.shortDescription || course.description}</p>
        <div className="mt-5 flex flex-wrap gap-3">
          <button
            className="rounded bg-[#730014] px-4 py-2 text-sm font-bold text-white transition hover:bg-[#9e001f] disabled:cursor-not-allowed disabled:opacity-50"
            disabled={!course.modules?.[0]?.lessons?.[0]}
            onClick={openFirstLesson}
            type="button"
          >
            Bắt đầu bài đầu tiên
          </button>
          <Link className="rounded border border-[#730014] px-4 py-2 text-sm font-bold text-[#730014] transition hover:bg-white/60" to={`/courses/${course.slug}`}>
            Xem chi tiết khóa học
          </Link>
        </div>
      </div>

      <div className="mb-5 flex items-center justify-between gap-4">
        <div>
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c]">Nội dung khóa học</h2>
          <p className="mt-1 text-sm text-[#6b7280]">{completedCount}/{totalLessons} bài học đã hoàn thành • {progressPercent}% tiến độ</p>
        </div>
        <span className="rounded bg-[#fff1f3] px-3 py-1 text-xs font-bold text-[#730014]">
          {courseCompleted ? 'Hoàn thành' : 'Đang học'}
        </span>
      </div>

      {progressPercent >= 100 ? (
        <div className="mb-5 flex flex-col gap-3 border border-[#e5d7d9] bg-white px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
          <h3 className="text-lg font-extrabold text-[#1a1c1c]">Đánh giá khóa học này</h3>
          <button className="inline-flex items-center gap-4 self-start text-sm font-extrabold text-[#730014] sm:self-auto" onClick={openRatingForm} type="button">
            Đánh giá khóa học này
            <span className="flex gap-0.5 text-[#e11d48]">
              {[1, 2, 3, 4, 5].map((star) => <Star className="h-5 w-5" fill={star <= (ratingInfo?.myRating || 0) ? 'currentColor' : 'none'} key={star} />)}
            </span>
          </button>
        </div>
      ) : null}

      {showRatingForm ? (
        <div aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="dialog">
          <div className="w-full max-w-lg border border-[#e5d7d9] bg-white p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4">
              <div><h3 className="text-xl font-extrabold text-[#1a1c1c]">Đánh giá khóa học</h3><p className="mt-1 text-sm text-[#4b5563]">Bạn có thể sửa đánh giá của mình bất cứ lúc nào.</p></div>
              <button aria-label="Đóng" className="text-[#4b5563]" onClick={() => setShowRatingForm(false)} type="button"><X className="h-5 w-5" /></button>
            </div>
            <div className="mt-6 flex gap-2" role="radiogroup" aria-label="Số sao đánh giá">
              {[1, 2, 3, 4, 5].map((star) => <button aria-checked={selectedRating === star} aria-label={`${star} sao`} className={`transition hover:scale-110 ${star <= selectedRating ? 'text-[#e11d48]' : 'text-[#d1d5db]'}`} key={star} onClick={() => setSelectedRating(star)} role="radio" type="button"><Star className="h-8 w-8" fill={star <= selectedRating ? 'currentColor' : 'none'} /></button>)}
            </div>
            <label className="mt-5 block text-sm font-bold text-[#1a1c1c]" htmlFor="course-rating-comment">Nhận xét (không bắt buộc)</label>
            <textarea className="mt-2 min-h-28 w-full border border-[#d1d5db] p-3 text-sm outline-none focus:border-[#730014]" id="course-rating-comment" maxLength={2000} onChange={(event) => setRatingComment(event.target.value)} placeholder="Chia sẻ trải nghiệm học của bạn" value={ratingComment} />
            {ratingError ? <p className="mt-3 text-sm font-semibold text-[#93000a]">{ratingError}</p> : null}
            <div className="mt-6 flex justify-end gap-3"><button className="border border-[#d1d5db] px-4 py-2 text-sm font-bold" onClick={() => setShowRatingForm(false)} type="button">Hủy</button><button className="bg-[#730014] px-4 py-2 text-sm font-bold text-white disabled:opacity-50" disabled={ratingSaving} onClick={saveRating} type="button">{ratingSaving ? 'Đang lưu...' : 'Gửi đánh giá'}</button></div>
          </div>
        </div>
      ) : null}

      <div className="space-y-4">
        {(course.modules || []).map((module, moduleIndex) => {
          const moduleKey = module.id ?? module.title ?? moduleIndex;
          const isOpen = String(openModuleId) === String(moduleKey);
          const lessons = module.lessons || [];
          const moduleState = moduleProgress.get(String(moduleKey)) || {};
          const moduleAssessments = moduleState.moduleAssessments || [];
          const moduleDone = Boolean(moduleState.lessonsCompleted && moduleState.assessmentsPassed);

          return (
            <section className="border border-[#e5e7eb] bg-white" key={moduleKey}>
              <button
                className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left"
                onClick={() => setOpenModuleId(isOpen ? null : moduleKey)}
                type="button"
              >
                <div>
                  <p className="text-xs font-bold text-[#6b7280]">Mô-đun {moduleIndex + 1}</p>
                  <h3 className="mt-1 font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">{module.title}</h3>
                </div>
                <div className="flex items-center gap-3">
                  {moduleDone ? <span className="rounded bg-[#fff1f3] px-2 py-1 text-xs font-bold text-[#730014]">Hoàn thành</span> : null}
                  <ChevronDown className={`h-5 w-5 text-[#730014] transition ${isOpen ? 'rotate-180' : ''}`} />
                </div>
              </button>

              {isOpen ? (
                <div className="border-t border-[#e5e7eb] px-5 py-2">
                  {lessons.map((lesson, lessonIndex) => {
                    const lessonId = getLessonId(module, lesson, lessonIndex);
                    const completed = completedLessonIds.has(lessonId);
                    const previousLesson = lessons[lessonIndex - 1];
                    const previousLessonId = previousLesson ? getLessonId(module, previousLesson, lessonIndex - 1) : null;
                    const locked = !moduleState.unlocked || Boolean(previousLessonId && !completedLessonIds.has(previousLessonId));
                    return (
                      <button
                        className={`flex w-full items-center justify-between gap-4 border-b border-[#f3f4f6] px-3 py-4 text-left transition last:border-b-0 ${
                          locked ? 'cursor-not-allowed opacity-60' : 'hover:bg-[#f7f8fb]'
                        }`}
                        disabled={locked}
                        key={lessonId}
                        onClick={() => openLesson(module, lesson, lessonIndex)}
                        type="button"
                      >
                        <div className="flex min-w-0 items-start gap-4">
                          {completed ? (
                            <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-[#730014]" />
                          ) : locked ? (
                            <Lock className="mt-0.5 h-5 w-5 shrink-0 text-[#6b7280]" />
                          ) : (
                            <span className="mt-0.5 h-5 w-5 shrink-0 rounded-full border border-[#9bb0cf] bg-white" />
                          )}
                          <div className="min-w-0">
                            <p className="font-semibold text-[#1a1c1c]">{lesson.title}</p>
                            <p className="mt-1 text-sm text-[#4b5563]">{lesson.videoUrl ? 'Video' : 'Bài đọc'} • {lesson.durationMinutes || 10} phút</p>
                          </div>
                        </div>
                        {locked ? (
                          <span className="shrink-0 rounded border border-[#d1d5db] px-4 py-2 text-xs font-bold text-[#6b7280]">Đã khóa</span>
                        ) : moduleIndex === 0 && lessonIndex === 0 ? (
                          <span className="shrink-0 rounded bg-[#730014] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#9e001f]">Bắt đầu</span>
                        ) : (
                          <span className="shrink-0 rounded border border-[#dfbfbd] px-4 py-2 text-xs font-bold text-[#730014]">Mở bài</span>
                        )}
                      </button>
                    );
                  })}
                  {moduleAssessments.map((assessment, assessmentIndex) => {
                    const passed = isAssessmentPassed(assessment);
                    const locked = !moduleState.unlocked || !moduleState.lessonsCompleted;
                    return (
                      <button
                        className={`flex w-full items-center justify-between gap-4 border-b border-[#f3f4f6] px-3 py-4 text-left transition last:border-b-0 ${
                          locked ? 'cursor-not-allowed opacity-60' : 'hover:bg-[#fff7f7]'
                        }`}
                        key={assessment.id ?? `${moduleKey}-assessment-${assessmentIndex}`}
                        onClick={() => (locked ? showLockedAssessmentMessage() : openAssessment(module))}
                        type="button"
                      >
                        <div className="flex min-w-0 items-start gap-4">
                          {passed ? (
                            <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-[#730014]" />
                          ) : locked ? (
                            <Lock className="mt-0.5 h-5 w-5 shrink-0 text-[#6b7280]" />
                          ) : (
                            <span className="mt-0.5 h-5 w-5 shrink-0 rounded-full border border-[#9bb0cf] bg-white" />
                          )}
                          <div className="min-w-0">
                            <p className="font-semibold text-[#1a1c1c]">{assessment.title || `Bài thi module ${moduleIndex + 1}`}</p>
                            <p className="mt-1 text-sm text-[#4b5563]">Bài đánh giá • {assessment.skill || 'AI assessment'}</p>
                          </div>
                        </div>
                        <span className={`shrink-0 rounded border px-4 py-2 text-xs font-bold ${
                          locked ? 'border-[#d1d5db] text-[#6b7280]' : 'border-[#dfbfbd] text-[#730014]'
                        }`}>
                          {locked ? 'Đã khóa' : 'Làm bài'}
                        </span>
                      </button>
                    );
                  })}
                </div>
              ) : null}
            </section>
          );
        })}
      </div>
    </div>
  );

  const renderGrades = () => (
    <div>
      <h1 className="mb-6 font-['Manrope'] text-3xl font-extrabold text-[#730014]">Điểm</h1>
      <div className="mb-6 flex items-center gap-4 border border-[#e5e7eb] bg-white p-6">
        <div className="relative">
          <FileText className="h-10 w-10 text-[#4b5563]" />
          {courseCompleted ? (
            <CheckCircle2 className="absolute -bottom-1 -right-1 h-5 w-5 rounded-full bg-white text-[#730014]" />
          ) : null}
        </div>
        <p className="text-sm font-semibold text-[#1a1c1c]">
          {courseCompleted
            ? `Bạn đã hoàn thành ${progressPercent}% nội dung khóa học và đủ điều kiện nhận chứng nhận.`
            : completion?.statusReason || 'Hoàn thành các bài đánh giá để đủ điều kiện nhận chứng nhận.'}
        </p>
      </div>

      <div className="overflow-hidden border border-[#e5e7eb] bg-white">
        <table className="w-full border-collapse text-left text-sm">
          <thead className="border-b border-[#e5e7eb] bg-white">
            <tr>
              <th className="px-6 py-4">Mục</th>
              <th className="px-6 py-4">Trạng thái</th>
              <th className="px-6 py-4">Đến hạn</th>
              <th className="px-6 py-4">Trọng số</th>
              <th className="px-6 py-4">Điểm</th>
            </tr>
          </thead>
          <tbody>
            {assessments.length ? assessments.map((assessment) => {
              const status = getAssessmentStatus(assessment);
              const weight = assessment.weightPercent ?? assessment.weight ?? (assessments.length ? Math.round(100 / assessments.length) : 100);
              return (
                <tr className="border-b border-[#e5e7eb] last:border-b-0" key={assessment.id}>
                  <td className="px-6 py-5">
                    <div className="flex items-start gap-4">
                      {status.done ? (
                        <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-[#730014]" />
                      ) : (
                        <Lock className="mt-0.5 h-5 w-5 shrink-0 rounded-full bg-[#f3f4f6] p-0.5 text-[#6b7280]" />
                      )}
                      <div>
                        <button
                          className="text-left font-semibold text-[#0056d2] transition hover:text-[#730014]"
                          onClick={() => {
                            const module = (course.modules || []).find((item) => String(item.id) === String(assessment.moduleId)) || (course.modules || [])[0];
                            if (module) openAssessment(module);
                          }}
                          type="button"
                        >
                          {assessment.title || 'Bài đánh giá'}
                        </button>
                        <p className="mt-1 text-xs text-[#4b5563]">{assessment.moduleTitle || 'Bài tập được chấm điểm'}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-5">
                    <span className="inline-flex items-center gap-2">
                      <Info className="h-4 w-4 text-[#1a1c1c]" />
                      {status.label}
                    </span>
                  </td>
                  <td className="px-6 py-5">{formatDateTime(assessment.dueAt || assessment.dueDate)}</td>
                  <td className="px-6 py-5">{weight}%</td>
                  <td className="px-6 py-5">{formatScore(assessment.latestSubmission, assessment)}</td>
                </tr>
              );
            }) : (
              <tr>
                <td className="px-6 py-8 text-center text-[#6b7280]" colSpan={5}>
                  Khóa học này chưa có bài đánh giá được cấu hình.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );

  const renderNotes = () => (
    <div className="flex min-h-[560px] flex-col">
      <div className="mb-14 flex items-center justify-between gap-4">
        <h1 className="font-['Manrope'] text-3xl font-extrabold text-[#730014]">Ghi chú</h1>
        <button className="rounded border border-[#730014] px-4 py-2 text-sm font-bold text-[#730014]" type="button">Lọc: Tất cả ghi chú</button>
      </div>
      <div className="py-16 text-center text-[#1a1c1c]">
        <StickyNote className="mx-auto mb-4 h-14 w-14 text-[#9ca3af]" />
        <p>Bạn chưa thêm bất kỳ ghi chú nào. Ghi chú có thể được tạo trong không gian học.</p>
      </div>
    </div>
  );

  const renderForums = () => (
    <div className="flex min-h-[560px] flex-col">
      <h1 className="mb-6 font-['Manrope'] text-3xl font-extrabold text-[#730014]">Diễn đàn thảo luận</h1>
      <CourseDiscussionSection courseId={course.id} />
    </div>
  );

  const renderInfo = () => (
    <div className="flex min-h-[560px] flex-col">
      <div className="max-w-4xl">
        <h1 className="font-['Manrope'] text-3xl font-extrabold text-[#730014]">{course.title}</h1>
        <p className="mt-2 text-[#6b7280]">EnglishLab</p>
        <p className="mt-6 text-sm leading-7 text-[#1a1c1c]">{course.description}</p>
      </div>

      <div className="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div className="border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#6b7280]">Trình độ</p>
          <p className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{course.level || 'Đang cập nhật'}</p>
        </div>
        <div className="border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#6b7280]">Mục tiêu band</p>
          <p className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{course.targetBand || course.targetScore || 'Đang cập nhật'}</p>
        </div>
        <div className="border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#6b7280]">Bài học</p>
          <p className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{totalLessons}</p>
        </div>
        <div className="border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#6b7280]">Bài đánh giá</p>
          <p className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">{assessments.length}</p>
        </div>
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <section className="border border-[#e5e7eb] bg-white p-5">
          <h2 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">Thông tin chi tiết</h2>
          <table className="mt-4 w-full text-sm">
            <tbody>
              <tr className="border-b border-[#e5e7eb]">
                <td className="w-52 bg-[#f9fafb] p-4 font-semibold">Danh mục</td>
                <td className="p-4">{course.categoryName || 'Trực tuyến'}</td>
              </tr>
              <tr className="border-b border-[#e5e7eb]">
                <td className="bg-[#f9fafb] p-4 font-semibold">Thời lượng</td>
                <td className="p-4">{course.duration || `${course.totalHours || 0} giờ`}</td>
              </tr>
              <tr className="border-b border-[#e5e7eb]">
                <td className="bg-[#f9fafb] p-4 font-semibold">Lộ trình</td>
                <td className="p-4">{course.learningPathName || 'Đang cập nhật'}</td>
              </tr>
              <tr>
                <td className="bg-[#f9fafb] p-4 font-semibold">Đầu ra mục tiêu</td>
                <td className="p-4">{course.targetOutcome || 'Đang cập nhật mục tiêu đầu ra.'}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <aside className="space-y-4">
          <div className="border border-[#e5e7eb] bg-white p-5">
            <h3 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">Đánh giá khóa học</h3>
            <p className="mt-3 text-sm text-[#4b5563]">
              {Number(course.averageRating || 0) > 0
                ? `${Number(course.averageRating).toFixed(1)}/5 từ ${Number(course.reviewCount || 0).toLocaleString('vi-VN')} lượt đánh giá.`
                : 'Chưa có dữ liệu đánh giá khóa học.'}
            </p>
          </div>

          <div className="border border-[#e5e7eb] bg-white p-5">
            <h3 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">Chứng nhận</h3>
            <p className="mt-3 text-sm leading-6 text-[#4b5563]">
              {certificate?.eligible
                ? `Đủ điều kiện nhận chứng nhận. Mã xác thực: ${certificate.verificationCode || 'đang cập nhật'}.`
                : certificate?.message || completion?.statusReason || 'Hoàn thành toàn bộ bài học và bài đánh giá để nhận chứng nhận.'}
            </p>
          </div>
        </aside>
      </div>
    </div>
  );

  const renderContent = () => {
    if (!course) return null;
    if (activeTab === 'grades') return renderGrades();
    if (activeTab === 'notes') return renderNotes();
    if (activeTab === 'forums') return renderForums();
    if (activeTab === 'info') return renderInfo();
    return renderMaterials();
  };

  return (
    <div className="course-page flex min-h-[100dvh] flex-col bg-white text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      {loading ? (
        <main className="mx-auto flex min-h-[calc(100dvh-80px)] w-full max-w-[1320px] flex-1 items-center px-4 py-10 md:px-10">
          <BrandLoadingState className="w-full" message="Đang tải trang khóa học..." />
        </main>
      ) : error || !course ? (
        <main className="mx-auto flex min-h-[calc(100dvh-80px)] w-full max-w-[1320px] flex-1 items-center px-4 py-10 md:px-10">
          <div className="w-full border border-[#f0d4d7] p-10 text-center font-semibold text-[#93000a]">{error || 'Không tìm thấy khóa học.'}</div>
        </main>
      ) : (
        <div className="mx-auto flex min-h-[calc(100dvh-80px)] w-full max-w-[1440px] flex-1">
          <aside className="hidden w-[320px] shrink-0 border-r border-[#e5e7eb] bg-white py-6 lg:block">
            <div className="border-b border-[#e5e7eb] px-6 pb-6">
              <Link className="mb-5 inline-flex items-center gap-2 text-sm font-bold text-[#730014] transition hover:text-[#9e001f]" to={`/courses/${course.slug}`}>
                <ArrowLeft className="h-4 w-4" />
                Quay lại chi tiết khóa học
              </Link>
              <span className="mb-3 block font-['Manrope'] text-2xl font-extrabold text-[#730014]">EnglishLab</span>
              <h2 className="text-sm font-extrabold leading-5 text-[#1a1c1c]">{course.title}</h2>
              <p className="mt-1 text-xs text-[#6b7280]">EnglishLab</p>
            </div>
            <nav className="mt-6">
              <div>
                <button
                  className={`flex w-full items-center gap-3 px-6 py-3 text-left text-sm font-extrabold transition ${
                    activeTab === 'materials' ? 'text-[#1a1c1c]' : 'text-[#1a1c1c] hover:bg-[#fff7f7]'
                  }`}
                  onClick={() => {
                    if (activeTab !== 'materials') {
                      setActiveTab('materials');
                      setMaterialsExpanded(true);
                      return;
                    }
                    setMaterialsExpanded((current) => !current);
                  }}
                  type="button"
                >
                  <ChevronDown className={`h-4 w-4 transition ${materialsExpanded ? 'rotate-180 text-[#1a1c1c]' : '-rotate-90 text-[#6b7280]'}`} />
                  Tài liệu khóa học
                </button>
                {materialsExpanded ? (
                  <ul className="mt-1 space-y-1 pr-5">
                    {(course.modules || []).map((module, index) => {
                      const moduleLessons = module.lessons || [];
                      const moduleKey = module.id ?? module.title ?? index;
                      const activeModule = String(openModuleId) === String(moduleKey);
                      const moduleState = moduleProgress.get(String(moduleKey)) || {};
                      const moduleDone = Boolean(moduleState.lessonsCompleted && moduleState.assessmentsPassed);
                      const moduleLocked = !moduleState.unlocked;

                      return (
                        <li key={moduleKey}>
                          <button
                            className={`flex w-full items-center gap-3 border-l-4 px-6 py-3 text-left text-sm transition ${
                              moduleLocked
                                ? 'cursor-not-allowed border-transparent text-[#9ca3af]'
                                : activeModule
                                  ? 'border-[#730014] bg-[#fff4f5] font-semibold text-[#1a1c1c]'
                                  : 'border-transparent text-[#1a1c1c] hover:bg-[#fff9f9]'
                            }`}
                            disabled={moduleLocked}
                            onClick={() => {
                              setActiveTab('materials');
                              setOpenModuleId(moduleKey);
                            }}
                            type="button"
                          >
                            {moduleDone ? (
                              <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-[#730014] text-white">
                                <CheckCircle2 className="h-3.5 w-3.5" />
                              </span>
                            ) : moduleLocked ? (
                              <Lock className="h-5 w-5 shrink-0 text-[#9ca3af]" />
                            ) : (
                              <span className="h-5 w-5 shrink-0 rounded-full border border-[#9bb0cf] bg-white" />
                            )}
                            Mô-đun {index + 1}
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                ) : null}
              </div>

              {navItems.filter((item) => item.id !== 'materials').map((item) => {
                const Icon = item.icon;
                const active = activeTab === item.id;
                return (
                  <button
                    className={`flex w-full items-center gap-3 border-l-4 px-6 py-3 text-left text-sm transition ${
                      active
                        ? 'border-[#730014] bg-[#fff4f5] font-bold text-[#1a1c1c]'
                        : 'border-transparent text-[#1a1c1c] hover:bg-[#fff9f9]'
                    }`}
                    key={item.id}
                    onClick={() => setActiveTab(item.id)}
                    type="button"
                  >
                    <Icon className="h-4 w-4" />
                    {item.label}
                  </button>
                );
              })}
            </nav>
          </aside>
          <main className="flex min-w-0 flex-1 flex-col px-4 py-8 md:px-10 lg:px-12">
            <Link className="mb-4 inline-flex items-center gap-2 text-sm font-bold text-[#730014] transition hover:text-[#9e001f] lg:hidden" to={`/courses/${course.slug}`}>
              <ArrowLeft className="h-4 w-4" />
              Quay lại chi tiết khóa học
            </Link>
            <div className="mb-5 flex flex-wrap gap-2 lg:hidden">
              {navItems.map((item) => (
                <button
                  className={`rounded border px-3 py-2 text-sm font-bold ${activeTab === item.id ? 'border-[#730014] bg-[#f2e8cf] text-[#730014]' : 'border-[#e5e7eb] text-[#4b5563]'}`}
                  key={item.id}
                  onClick={() => setActiveTab(item.id)}
                  type="button"
                >
                  {item.label}
                </button>
              ))}
            </div>
            <div className="flex-1">
              {renderContent()}
            </div>
          </main>
        </div>
      )}
      <CourseFooter />
    </div>
  );
};

export default CourseHome;
