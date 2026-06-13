import { useEffect, useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import CourseFooter from '../components/course/CourseFooter';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import CourseDetailHero from '../components/course-detail/CourseDetailHero';
import CourseModuleAccordion from '../components/course-detail/CourseModuleAccordion';
import StreakPreview from '../components/course-detail/StreakPreview';
import { clearSession, hasAccessToken } from '../utils/auth';
import { findFallbackCourse, normalizeCourse, normalizeEnrollment } from '../utils/courseModels';

const CourseDetail = () => {
  const { slugOrId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [course, setCourse] = useState(() => (location.state?.course ? normalizeCourse(location.state.course) : null));
  const [loading, setLoading] = useState(!course);
  const [error, setError] = useState('');
  const [purchasing, setPurchasing] = useState(false);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [slugOrId]);

  useEffect(() => {
    let active = true;

    if (course?.slug === slugOrId || String(course?.id) === String(slugOrId)) {
      setLoading(false);
      return undefined;
    }

    setLoading(true);
    setError('');

    Promise.all([
      courseApi.getOnlineCourse(slugOrId),
      hasAccessToken() ? courseApi.getMyOnlineCourses().catch(() => []) : Promise.resolve([]),
    ])
      .then(([response, enrollments]) => {
        if (!active) return;
        const normalized = normalizeCourse(response);
        const matchedEnrollment = enrollments
          .map(normalizeEnrollment)
          .find((item) => item.courseSlug === normalized.slug || String(item.courseId) === String(normalized.id));
        setCourse({ ...normalized, registered: Boolean(matchedEnrollment) });
      })
      .catch(() => {
        if (!active) return;
        const fallback = findFallbackCourse(slugOrId);
        if (fallback) {
          setCourse(normalizeCourse(fallback));
        } else {
          setError('Không tìm thấy khóa học bạn cần xem.');
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [slugOrId]);

  const handlePurchase = async () => {
    if (!course) return;

    if (!hasAccessToken()) {
      navigate('/login', { state: { from: `/courses/${course.slug}` } });
      return;
    }

    setPurchasing(true);
    setError('');

    try {
      const enrollment = await courseApi.registerOnlineCourse(course.id);
      navigate(`/courses/${course.slug}/learn`, {
        replace: true,
        state: {
          course: { ...course, registered: true, progressPercent: enrollment?.progressPercent ?? 0 },
          enrollment,
        },
      });
    } catch (err) {
      const status = err?.response?.status;
      if (status === 401 || status === 403) {
        clearSession();
        navigate('/login', { replace: true, state: { from: `/courses/${course.slug}` } });
        return;
      }

      if (status === 409) {
        navigate(`/courses/${course.slug}/learn`, {
          replace: true,
          state: { course: { ...course, registered: true } },
        });
        return;
      }

      setError(err?.response?.data?.message || 'Chưa thể ghi danh khóa học lúc này. Vui lòng thử lại.');
    } finally {
      setPurchasing(false);
    }
  };

  return (
    <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header hideTeacherLinks />
      <main className="mx-auto max-w-[1320px] px-4 pb-[80px] pt-8 md:px-10">
        <div className="mb-6">
          <Link className="group inline-flex items-center gap-2 text-sm font-bold text-[#8a0018]" to="/courses">
            <ArrowLeft className="h-4 w-4 shrink-0" />
            <span className="group-hover:underline">Quay lại danh sách khóa học</span>
          </Link>
        </div>
        {loading ? (
          <div className="rounded-3xl border border-[#dfbfbd]/30 bg-white p-10 text-center text-[#584140]">Đang tải chi tiết khóa học...</div>
        ) : !course ? (
          <div className="rounded-3xl border border-[#dfbfbd]/30 bg-white p-10 text-center text-[#93000a]">{error || 'Không tìm thấy khóa học.'}</div>
        ) : (
          <div className="space-y-8">
            <CourseDetailHero course={course} isAuthenticated={hasAccessToken()} purchasing={purchasing} onPurchase={handlePurchase} />
            {error ? (
              <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
                {error}
              </div>
            ) : null}
            <div className="space-y-8">
              <CourseModuleAccordion modules={course.modules} />
              <StreakPreview />
            </div>
          </div>
        )}
      </main>
      <CourseFooter />
    </div>
  );
};

export default CourseDetail;
