import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
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
  PopularCourses,
} from '../components/course';
import RecommendedCoursesSection from '../components/course/RecommendedCoursesSection';
import { getStoredUser, hasAccessToken } from '../utils/auth';
import { mergeCourseRegistrations, normalizeCourse, normalizeEnrollment } from '../utils/courseModels';
import { recommendCoursesForLearner } from '../utils/selfPacedHelpers';

const PAGE_SIZE = 100;
const defaultFilters = {
  category: '',
  currentBand: '',
  targetBand: '',
  skill: '',
  promotion: '',
};

const deriveLearnerTargetBand = (user) => {
  if (!user || String(user.targetExam || '').toUpperCase() !== 'IELTS') {
    return null;
  }
  const parsed = Number(user.targetScore);
  return Number.isFinite(parsed) ? parsed : null;
};

const Courses = () => {
  const [allCourses, setAllCourses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [myEnrollments, setMyEnrollments] = useState([]);
  const [activeCategory, setActiveCategory] = useState('');
  const [keyword, setKeyword] = useState('');
  const [filters, setFilters] = useState(defaultFilters);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
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

  useEffect(() => {
    let active = true;
    courseApi.getOnlineCourseCategories()
      .then((items) => {
        if (active) setCategories(items);
      })
      .catch(() => {
        if (active) setCategories([]);
      });
    return () => {
      active = false;
    };
  }, []);

  const loadMyEnrollments = useCallback(async () => {
    if (!hasAccessToken()) {
      setMyEnrollments([]);
      return [];
    }

    try {
      const enrollments = await courseApi.getMyOnlineCourses();
      const normalized = enrollments
        .map(normalizeEnrollment)
        .filter((enrollment) => enrollment.courseId && enrollment.courseSlug);
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

    try {
      const params = {
        page: 0,
        size: PAGE_SIZE,
      };

      const selectedCategory = activeCategory || filters.category;
      if (keyword.trim()) params.keyword = keyword.trim();
      if (selectedCategory) params.category = selectedCategory;
      if (filters.currentBand) params.currentBand = Number(filters.currentBand);
      if (filters.targetBand) params.targetBand = Number(filters.targetBand);
      if (filters.skill) params.skill = filters.skill;

      const [pageData, enrollments] = await Promise.all([
        courseApi.getOnlineCourses(params),
        loadMyEnrollments(),
      ]);

      const normalizedCourses = mergeCourseRegistrations((pageData.content || []).map(normalizeCourse), enrollments);
      setAllCourses(normalizedCourses);
    } catch (err) {
      setAllCourses([]);
      setError(
        err?.response?.status === 401
          ? 'Bạn cần đăng nhập để tải đầy đủ dữ liệu khóa học và trạng thái đăng ký.'
          : 'Không thể tải danh sách khóa học. Vui lòng thử lại.'
      );
    } finally {
      setLoading(false);
    }
  }, [activeCategory, filters.category, filters.currentBand, filters.skill, filters.targetBand, keyword, loadMyEnrollments]);

  useEffect(() => {
    loadCourses();
  }, [loadCourses]);

  const visibleCourses = useMemo(() => {
    if (filters.promotion === 'promotion') {
      return allCourses.filter((course) => Number(course.discountPercent || 0) > 0);
    }
    if (filters.promotion === 'standard') {
      return allCourses.filter((course) => Number(course.discountPercent || 0) <= 0);
    }
    return allCourses;
  }, [allCourses, filters.promotion]);

  const featuredCourses = useMemo(() => {
    const featured = visibleCourses.filter((course) => course.featured);
    return (featured.length ? featured : visibleCourses).slice(0, 4);
  }, [visibleCourses]);

  const recommendedCourses = useMemo(
    () => recommendCoursesForLearner({
      courses: allCourses,
      enrollments: myEnrollments,
      currentBand: user?.currentBand ?? null,
      targetBand: deriveLearnerTargetBand(user),
    }),
    [allCourses, myEnrollments, user],
  );

  const handleClearFilters = () => {
    setKeyword('');
    setActiveCategory('');
    setFilters(defaultFilters);
  };

  const handleFilterChange = ({ target }) => {
    const { name, value } = target;
    setFilters((current) => ({ ...current, [name]: value }));
  };

  return (
    <div id="top" className="course-page min-h-screen overflow-x-hidden bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto max-w-[1320px] px-4 pb-[80px] pt-6 md:px-10">
        <CourseHero user={user} registeredCount={myEnrollments.length} />
        <CategoryTabs activeCategory={activeCategory} categories={categories} onChange={setActiveCategory} />
        <CurrentCourse enrollments={myEnrollments} isAuthenticated={isAuthenticated} />
        {error ? (
          <div className="mb-8 rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
            {error}
          </div>
        ) : null}
        {isAuthenticated ? (
          <div className="mb-8 flex flex-col gap-4 rounded-3xl border border-[#ead9db] bg-[#fffdfc] px-6 py-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">Lộ trình dành cho bạn</p>
              <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#4b0009]">Xem bước đang học và khóa học tiếp theo</h2>
            </div>
            <Link className="inline-flex shrink-0 items-center justify-center rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]" to="/learning-path">Mở lộ trình học</Link>
          </div>
        ) : null}
        <RecommendedCoursesSection
          courses={recommendedCourses}
          currentBand={user?.currentBand ?? null}
          loading={loading}
          error={error ? 'Không thể tải gợi ý khóa học. Vui lòng thử lại.' : ''}
          onRetry={loadCourses}
        />
        <PopularCourses courses={featuredCourses} />
        <CourseCatalog
          courses={visibleCourses}
          keyword={keyword}
          filters={filters}
          onKeywordChange={setKeyword}
          onFilterChange={handleFilterChange}
          onClear={handleClearFilters}
          loading={loading}
          currentBand={user?.currentBand ?? null}
          categories={categories}
        />
      </main>
      <CourseFooter />
    </div>
  );
};

export default Courses;
