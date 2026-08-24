import { useEffect, useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { getCurrentUser } from '../api/authApi';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import CourseFooter from '../components/course/CourseFooter';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import CourseDetailHero from '../components/course-detail/CourseDetailHero';
import CourseDiscussionSection from '../components/course-detail/CourseDiscussionSection';
import CourseModuleAccordion from '../components/course-detail/CourseModuleAccordion';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import { getStoredUser, hasAccessToken } from '../utils/auth';
import { normalizeCourse, normalizeEnrollment } from '../utils/courseModels';

const CourseDetail = () => {
  const { slugOrId } = useParams();
  const location = useLocation();
  const [course, setCourse] = useState(() => (location.state?.course ? normalizeCourse(location.state.course) : null));
  const [user, setUser] = useState(() => (hasAccessToken() ? getStoredUser() : null));
  const [loading, setLoading] = useState(!course);
  const [error, setError] = useState('');

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [slugOrId]);

  useEffect(() => {
    let active = true;
    if (!hasAccessToken()) {
      setUser(null);
      return undefined;
    }

    const loadCurrentUser = async () => {
      try {
        const response = await getCurrentUser();
        if (!active) return;
        localStorage.setItem('user', JSON.stringify(response.data));
        window.dispatchEvent(new Event('englishlab:user-updated'));
        setUser(response.data);
      } catch {
        if (!active) return;
        setUser(getStoredUser());
      }
    };

    loadCurrentUser();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');

    const loadEnrollments = async () => {
      if (!hasAccessToken()) return { items: [], failed: false };
      try {
        return { items: await courseApi.getMyOnlineCourses(), failed: false };
      } catch {
        return { items: [], failed: true };
      }
    };

    const loadCourse = async () => {
      try {
        const [response, enrollmentResult] = await Promise.all([
          courseApi.getOnlineCourse(slugOrId),
          loadEnrollments(),
        ]);
        if (!active) return;

        const normalized = normalizeCourse(response);
        const normalizedEnrollments = enrollmentResult.items.map(normalizeEnrollment);
        const matchedEnrollment = normalizedEnrollments.find(
          (item) => item.courseSlug === normalized.slug || String(item.courseId) === String(normalized.id),
        );

        setCourse({
          ...normalized,
          registered: Boolean(matchedEnrollment),
          enrollmentAccessCheckFailed: enrollmentResult.failed,
          enrollmentId: matchedEnrollment?.id ?? null,
          enrollmentStatus: matchedEnrollment?.status ?? null,
          progressPercent: matchedEnrollment?.progressPercent ?? normalized.progressPercent,
        });
        if (enrollmentResult.failed) {
          setError('Không thể kiểm tra quyền truy cập khóa học. Vui lòng tải lại trang.');
        }
      } catch {
        if (!active) return;
        setCourse(null);
        setError('Không tìm thấy khóa học bạn cần xem hoặc máy chủ chưa trả dữ liệu chi tiết.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadCourse();

    return () => {
      active = false;
    };
  }, [slugOrId]);

  return (
    <div className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto max-w-[1320px] px-4 pb-[80px] pt-8 md:px-10">
        <div className="mb-6">
          <Link className="group inline-flex items-center gap-2 text-sm font-bold text-[#8a0018]" to="/courses">
            <ArrowLeft className="h-4 w-4 shrink-0" />
            <span className="group-hover:underline">Quay lại danh sách khóa học</span>
          </Link>
        </div>
        {location.state?.accessMessage ? (
          <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-900">
            {location.state.accessMessage}
          </div>
        ) : null}
        {loading ? (
          <BrandLoadingState className="rounded-3xl" message="Đang tải chi tiết khóa học..." />
        ) : !course ? (
          <div className="rounded-3xl border border-[#dfbfbd]/30 bg-white p-10 text-center text-[#93000a]">
            {error || 'Không tìm thấy khóa học.'}
          </div>
        ) : (
          <div className="space-y-8">
            <CourseDetailHero course={course} currentBand={user?.currentBand ?? null} />
            {error ? (
              <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
                {error}
              </div>
            ) : null}
            <CourseModuleAccordion modules={course.modules} />
            <CourseDiscussionSection courseId={course.id} />
          </div>
        )}
      </main>
      <CourseFooter />
    </div>
  );
};

export default CourseDetail;
