import { useCallback, useEffect, useMemo, useState } from 'react';
import { getCurrentUser } from '../api/authApi';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import {
  CategoryTabs,
  CourseCatalog,
  CourseFooter,
  CourseGlobalStyles,
  CourseHero,
  CurrentCourse,
  FinalCourseCta,
  LearningPaths,
  PopularCourses,
  RecommendationBanner,
} from '../components/course';
import { getStoredUser, hasAccessToken } from '../utils/auth';
import { fallbackCourses, mergeCourseRegistrations, normalizeCourse, normalizeEnrollment } from '../utils/courseModels';

const PAGE_SIZE = 12;

const Courses = () => {
  const [courses, setCourses] = useState([]);
  const [myEnrollments, setMyEnrollments] = useState([]);
  const [activeCategory, setActiveCategory] = useState('');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [usingFallback, setUsingFallback] = useState(false);
  const [user, setUser] = useState(() => (hasAccessToken() ? getStoredUser() : null));

  const isAuthenticated = Boolean(user && hasAccessToken());

  useEffect(() => {
    let active = true;

    if (!hasAccessToken()) {
      setUser(null);
      setMyEnrollments([]);
      return undefined;
    }

    getCurrentUser()
      .then((response) => {
        if (!active) return;
        localStorage.setItem('user', JSON.stringify(response.data));
        window.dispatchEvent(new Event('englishlab:user-updated'));
        setUser(response.data);
      })
      .catch(() => {
        if (!active) return;
        setUser(getStoredUser());
      });

    return () => {
      active = false;
    };
  }, []);

  const getFilteredFallbackCourses = useCallback(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return fallbackCourses.filter((course) => {
      const matchedCategory = !activeCategory || course.category === activeCategory || activeCategory === 'ONLINE';
      const matchedKeyword =
        !normalizedKeyword ||
        course.title.toLowerCase().includes(normalizedKeyword) ||
        course.shortDescription.toLowerCase().includes(normalizedKeyword);
      return matchedCategory && matchedKeyword;
    });
  }, [activeCategory, keyword]);

  const loadMyEnrollments = useCallback(async () => {
    if (!hasAccessToken()) {
      setMyEnrollments([]);
      return [];
    }

    try {
      const enrollments = await courseApi.getMyOnlineCourses();
      const normalized = enrollments.map(normalizeEnrollment);
      setMyEnrollments(normalized);
      return normalized;
    } catch {
      setMyEnrollments([]);
      return [];
    }
  }, []);

  const loadCourses = useCallback(async () => {
    setLoading(true);
    setError('');
    setUsingFallback(false);

    try {
      const [pageData, enrollments] = await Promise.all([
        courseApi.getOnlineCourses({
          page: 0,
          size: PAGE_SIZE,
          category: activeCategory || undefined,
          keyword: keyword.trim() || undefined,
        }),
        loadMyEnrollments(),
      ]);

      const normalizedCourses = mergeCourseRegistrations((pageData.content || []).map(normalizeCourse), enrollments);
      setCourses(normalizedCourses);
    } catch (err) {
      setCourses(getFilteredFallbackCourses());
      setUsingFallback(true);
      setError(
        err?.response?.status === 401
          ? 'API danh sách khóa học đang yêu cầu đăng nhập. Trang vẫn hiển thị dữ liệu mẫu để không chặn trải nghiệm.'
          : 'Chưa kết nối được API khóa học. Trang đang hiển thị dữ liệu mẫu để kiểm tra giao diện.'
      );
    } finally {
      setLoading(false);
    }
  }, [activeCategory, keyword, loadMyEnrollments, getFilteredFallbackCourses]);

  useEffect(() => {
    const timeoutId = setTimeout(loadCourses, 250);
    return () => clearTimeout(timeoutId);
  }, [loadCourses]);

  const featuredCourses = useMemo(() => {
    const featured = courses.filter((course) => course.featured);
    return (featured.length ? featured : courses).slice(0, 4);
  }, [courses]);

  const handleClearFilters = () => {
    setKeyword('');
    setActiveCategory('');
  };

  return (
    <div id="top" className="course-page min-h-screen overflow-x-hidden bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header hideTeacherLinks />
      <main className="mx-auto max-w-[1320px] px-4 pb-[80px] pt-6 md:px-10">
        <CourseHero user={user} registeredCount={myEnrollments.length} />
        <CurrentCourse enrollments={myEnrollments} isAuthenticated={isAuthenticated} />
        <CategoryTabs activeCategory={activeCategory} onChange={setActiveCategory} />
        {error ? (
          <div className={`mb-8 rounded-2xl border px-5 py-4 text-sm font-semibold ${usingFallback ? 'border-[#dfbfbd]/40 bg-white text-[#584140]' : 'border-[#ba1a1a]/20 bg-[#ffdad6] text-[#93000a]'}`}>
            {error}
          </div>
        ) : null}
        <RecommendationBanner />
        <PopularCourses courses={featuredCourses} />
        <CourseCatalog
          courses={courses}
          keyword={keyword}
          onKeywordChange={setKeyword}
          onClear={handleClearFilters}
          loading={loading}
        />
        <LearningPaths />
        <FinalCourseCta />
      </main>
      <CourseFooter />
    </div>
  );
};

export default Courses;
