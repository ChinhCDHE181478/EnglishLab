import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Route } from 'lucide-react';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import CourseFooter from '../components/course/CourseFooter';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import Pagination, { usePagination } from '../components/ui/Pagination';
import { normalizeCourse } from '../utils/courseModels';

export default function LearningPathCatalogPage() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    window.scrollTo(0, 0);
    const load = async () => {
      try {
        const result = await courseApi.getOnlineCourses({ page: 0, size: 100 });
        setCourses((result.content || []).map(normalizeCourse));
      } catch (err) {
        console.error('Error loading courses:', err);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const paths = useMemo(() => {
    const groups = courses.reduce((acc, course) => {
      const code = String(course.learningPathCode || '').trim();
      if (!code) return acc;
      acc[code] ||= { code, name: course.learningPathName || code, courses: [] };
      acc[code].courses.push(course);
      return acc;
    }, {});
    
    return Object.values(groups).map((path) => ({
      ...path,
      courses: path.courses.sort((left, right) => Number(left.learningPathOrder || 0) - Number(right.learningPathOrder || 0))
    }));
  }, [courses]);

  const {
    page,
    setPage,
    totalPages,
    pageItems: pagePaths,
    totalItems,
  } = usePagination(paths, 10);

  return (
    <div className="min-h-screen bg-[#f9f9f9] text-[#2b2828]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto max-w-[1200px] px-4 py-10 md:px-8">
        <Link className="inline-flex items-center gap-2 text-sm font-bold text-[#730014] hover:underline" to="/courses">
          <ArrowLeft className="h-4 w-4" />
          Quay lại danh sách khóa học
        </Link>

        {/* Header Banner */}
        <section className="mt-6 overflow-hidden rounded-3xl border border-[#ead9db] bg-[linear-gradient(135deg,_#fffdfc,_#fff0f1)] p-6 shadow-sm md:p-9">
          <div className="flex items-center gap-4">
            <span className="flex h-14 w-14 items-center justify-center rounded-3xl bg-[#fff0f2] text-[#8a0018] shadow-sm">
              <Route className="h-7 w-7" />
            </span>
            <div>
              <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[#8a0018]">Danh mục lộ trình</p>
              <h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">
                Toàn bộ Lộ trình tham khảo
              </h1>
            </div>
          </div>
          <p className="mt-4 text-sm leading-relaxed text-[#584140]">
            Khám phá các lộ trình học tập được thiết kế khoa học từ các giáo viên giàu kinh nghiệm để giúp bạn chinh phục mục tiêu IELTS/TOEIC nhanh nhất.
          </p>
        </section>

        {loading ? (
          <p className="py-16 text-center text-[#584140] font-semibold">Đang tải danh sách lộ trình...</p>
        ) : !paths.length ? (
          <p className="py-16 text-center text-[#584140] font-semibold">Chưa có lộ trình tham khảo nào.</p>
        ) : (
          <>
            <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {pagePaths.map((path) => (
                <article key={path.code} className="flex min-h-[350px] flex-col overflow-hidden rounded-[24px] border border-[#dfbfbd]/30 bg-white p-5 shadow-sm transition duration-200 hover:-translate-y-1 hover:border-[#dfbfbd] hover:shadow-md">
                  <div className="flex h-36 items-center justify-center overflow-hidden rounded-2xl bg-[#fff0f2] shrink-0">
                    {path.courses[0]?.thumbnailUrl ? (
                      <img alt="" className="h-full w-full object-cover" src={path.courses[0].thumbnailUrl} />
                    ) : (
                      <Route className="h-10 w-10 text-[#8a0018]" />
                    )}
                  </div>
                  <p className="mt-5 text-[11px] font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">
                    {path.courses.length} khóa học · Lộ trình
                  </p>
                  <h3 className="mt-2 font-['Manrope'] text-xl font-extrabold leading-snug text-[#2b2828] line-clamp-1">
                    {path.name}
                  </h3>
                  <ol className="mt-4 space-y-2">
                    {path.courses.slice(0, 3).map((course, index) => (
                      <li className="flex items-center gap-2 text-sm text-[#584140]" key={course.id}>
                        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#fff0f2] text-xs font-extrabold text-[#8a0018]">
                          {course.learningPathOrder || index + 1}
                        </span>
                        <span className="truncate font-semibold">{course.title}</span>
                      </li>
                    ))}
                    {path.courses.length > 3 && (
                      <li className="text-xs font-semibold text-[#8c716f] pl-8">
                        + {path.courses.length - 3} khóa học khác...
                      </li>
                    )}
                  </ol>
                  <Link
                    className="mt-auto inline-flex items-center justify-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] active:scale-95 shadow-sm"
                    to={`/learning-paths/${encodeURIComponent(path.code)}`}
                  >
                    Xem lộ trình
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </article>
              ))}
            </div>

            <Pagination
              page={page}
              totalPages={totalPages}
              onChange={setPage}
              totalItems={totalItems}
              pageSize={10}
              className="mt-10"
              alwaysVisible
            />
          </>
        )}
      </main>
      <CourseFooter />
    </div>
  );
}
