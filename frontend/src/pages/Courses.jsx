import { useCallback, useDeferredValue, useEffect, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Route } from 'lucide-react';
import { getCurrentUser } from '../api/authApi';
import courseApi from '../api/courseApi';
import {
  CategoryTabs,
  CourseCatalog,
  CourseHero,
  CurrentCourse,
  PopularCourses,
} from '../components/course';
import RecommendedCoursesSection from '../components/course/RecommendedCoursesSection';
import LearningPathCatalog from '../components/course/LearningPathCatalog';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import { getStoredUser, hasAccessToken } from '../utils/auth';
import { mergeCourseRegistrations, normalizeCourse, normalizeEnrollment } from '../utils/courseModels';
import { EMPTY_PAGE, pageParams } from '../utils/pagination';

const PAGE_SIZE = 6;
const ENGLISH_CATEGORY_CODES = new Set(['IELTS', 'TOEIC', 'COMMUNICATION', 'FOUNDATION']);
const defaultFilters = {
  category: '',
  currentBand: '',
  targetBand: '',
  toeicTarget: '',
  skill: '',
  promotion: '',
};

const Courses = () => {
  const [allCourses, setAllCourses] = useState([]);
  const [learningPaths, setLearningPaths] = useState([]);
  const [coursePage, setCoursePage] = useState(EMPTY_PAGE);
  const [catalogPage, setCatalogPage] = useState(1);
  const [categories, setCategories] = useState([]);
  const [myEnrollments, setMyEnrollments] = useState([]);
  const [activeCategory, setActiveCategory] = useState('');
  const [keyword, setKeyword] = useState('');
  const [filters, setFilters] = useState(defaultFilters);
  const [initialLoading, setInitialLoading] = useState(true);
  const [coursesLoading, setCoursesLoading] = useState(true);
  const [error, setError] = useState('');
  const [backendRecommendations, setBackendRecommendations] = useState([]);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [recommendationError, setRecommendationError] = useState('');
  const [user, setUser] = useState(() => (hasAccessToken() ? getStoredUser() : null));
  const deferredKeyword = useDeferredValue(keyword.trim());

  const location = useLocation();

  const isAuthenticated = Boolean(user && hasAccessToken());

  useEffect(() => {
    if (location.hash === '#recommended') {
      const timer = setTimeout(() => {
        document.getElementById('recommended')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 400);
      return () => clearTimeout(timer);
    }
    return undefined;
  }, [location.hash]);

  useEffect(() => {
    let active = true;
    if (!hasAccessToken()) {
      setUser(null);
      setMyEnrollments([]);
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
    const loadCategories = async () => {
      try {
        const items = await courseApi.getOnlineCourseCategories();
        if (active) setCategories(items.filter((item) => ENGLISH_CATEGORY_CODES.has(item.code)));
      } catch {
        if (active) setCategories([]);
      }
    };

    loadCategories();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    const loadLearningPaths = async () => {
      try {
        const items = await courseApi.getLearningPathOffers();
        if (active) setLearningPaths(Array.isArray(items) ? items : []);
      } catch {
        if (active) setLearningPaths([]);
      }
    };

    loadLearningPaths();
    return () => {
      active = false;
    };
  }, []);

  const loadRecommendations = useCallback(async () => {
    if (!hasAccessToken()) {
      setBackendRecommendations([]);
      setRecommendationError('');
      return;
    }
    setRecommendationLoading(true);
    setRecommendationError('');
    try {
      const items = await courseApi.getRecommendedCourses();
      setBackendRecommendations(items.map(normalizeCourse));
    } catch {
      setBackendRecommendations([]);
      setRecommendationError('Không thể tải gợi ý cá nhân hóa. Vui lòng thử lại.');
    } finally {
      setRecommendationLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRecommendations();
  }, [loadRecommendations, user?.id]);

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
    setCoursesLoading(true);
    setError('');

    try {
      const params = pageParams(catalogPage, PAGE_SIZE);

      const selectedCategory = activeCategory || filters.category;
      if (deferredKeyword) params.keyword = deferredKeyword;
      if (selectedCategory) params.category = selectedCategory;
      if (filters.currentBand) params.currentBand = Number(filters.currentBand);
      if (filters.targetBand) params.targetBand = Number(filters.targetBand);
      if (filters.skill) params.skill = filters.skill;
      if (filters.toeicTarget) params.targetScore = Number(filters.toeicTarget);
      if (filters.promotion) params.promotion = filters.promotion;

      const [pageData, enrollments] = await Promise.all([
        courseApi.getOnlineCourses(params),
        loadMyEnrollments(),
      ]);

      const normalizedCourses = mergeCourseRegistrations((pageData.content || []).map(normalizeCourse), enrollments);
      setAllCourses(normalizedCourses);
      setCoursePage({ ...pageData, content: normalizedCourses });
    } catch (err) {
      setAllCourses([]);
      setCoursePage(EMPTY_PAGE);
      setError(
        err?.response?.status === 401
          ? 'Bạn cần đăng nhập để tải đầy đủ dữ liệu khóa học và trạng thái đăng ký.'
          : 'Không thể tải danh sách khóa học. Vui lòng thử lại.'
      );
    } finally {
      setCoursesLoading(false);
      setInitialLoading(false);
    }
  }, [activeCategory, catalogPage, deferredKeyword, filters.category, filters.currentBand, filters.promotion, filters.skill, filters.targetBand, filters.toeicTarget, loadMyEnrollments]);

  useEffect(() => {
    loadCourses();
  }, [loadCourses]);

  const visibleCourses = allCourses;

  const featuredCourses = useMemo(() => {
    const featured = visibleCourses.filter((course) => course.featured);
    return (featured.length ? featured : visibleCourses).slice(0, 4);
  }, [visibleCourses]);

  const recommendedCourses = useMemo(() => (
    isAuthenticated
      ? backendRecommendations
      : featuredCourses.slice(0, 3).map((course) => ({
        ...course,
        recommendationReason: 'Khóa học nổi bật để bạn tham khảo trước khi cập nhật hồ sơ học tập.',
      }))
  ), [backendRecommendations, featuredCourses, isAuthenticated]);

  const handleClearFilters = () => {
    setKeyword('');
    setActiveCategory('');
    setFilters(defaultFilters);
  };

  const handleFilterChange = ({ target }) => {
    const { name, value } = target;
    setFilters((current) => (
      name === 'category'
        ? { ...current, category: value, currentBand: '', targetBand: '', toeicTarget: '' }
        : { ...current, [name]: value }
    ));
  };

  const handleCategoryChange = (value) => {
    setActiveCategory(value);
    setFilters((current) => ({ ...current, category: '', currentBand: '', targetBand: '', toeicTarget: '' }));
  };

  return (
    <LearnerPageShell
      title="Thư viện khóa học"
      description="Tìm kiếm và khám phá các khóa học IELTS, TOEIC và tiếng Anh giao tiếp trực tuyến chất lượng cao từ EnglishLab."
    >
      {initialLoading ? (
        <BrandLoadingState message="Đang tải danh sách khóa học..." />
      ) : (
        <>
          <CourseHero user={user} registeredCount={myEnrollments.length} />
          <CurrentCourse enrollments={myEnrollments} isAuthenticated={isAuthenticated} />
          {error ? (
            <div className="mb-8 rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
              {error}
            </div>
          ) : null}
          {isAuthenticated ? (
            <div className="mb-8 flex flex-col gap-4 rounded-3xl border border-[#ead9db] bg-[linear-gradient(135deg,_#fffdfc,_#fff0f1)] px-6 py-6 shadow-sm transition-all duration-200 hover:border-[#dfbfbd] hover:shadow-md sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-4">
                <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-[#f0e3e4] bg-white text-[#8a0018] shadow-sm">
                  <Route className="h-5 w-5" />
                </span>
                <div>
                  <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">Lộ trình dành cho bạn</p>
                  <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#4b0009]">Xem giai đoạn đang học và khóa học tiếp theo</h2>
                </div>
              </div>
              <Link className="inline-flex shrink-0 items-center justify-center rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95" to="/learning-path">
                Mở lộ trình học
              </Link>
            </div>
          ) : null}
          <div id="recommended">
          <RecommendedCoursesSection
            courses={recommendedCourses}
            currentBand={user?.currentBand ?? null}
            loading={isAuthenticated ? recommendationLoading : coursesLoading}
            error={isAuthenticated ? recommendationError : error ? 'Không thể tải gợi ý khóa học. Vui lòng thử lại.' : ''}
            profileBased={isAuthenticated}
            onRetry={isAuthenticated ? loadRecommendations : loadCourses}
          />
          </div>
          <div className='mt-6'></div>
          <CategoryTabs activeCategory={activeCategory} categories={categories} onChange={handleCategoryChange} />
          <LearningPathCatalog paths={learningPaths} />
          <CategoryTabs activeCategory={activeCategory} categories={categories} onChange={handleCategoryChange} />
          <PopularCourses courses={featuredCourses} />
          <CategoryTabs activeCategory={activeCategory} categories={categories} onChange={handleCategoryChange} />
          <CourseCatalog
            courses={visibleCourses}
            keyword={keyword}
            filters={filters}
            onKeywordChange={setKeyword}
            onFilterChange={handleFilterChange}
            onClear={handleClearFilters}
            loading={coursesLoading}
            currentBand={user?.currentBand ?? null}
            categories={categories}
            selectedCategory={activeCategory || filters.category}
            serverPage={coursePage}
            onPageChange={setCatalogPage}
          />
        </>
      )}
    </LearnerPageShell>
  );
};

export default Courses;
