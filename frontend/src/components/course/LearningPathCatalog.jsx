import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Route } from 'lucide-react';

const PAGE_SIZE = 3;

export default function LearningPathCatalog({ courses = [] }) {
  const [page, setPage] = useState(0);

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

  const totalPages = Math.max(1, Math.ceil(paths.length / PAGE_SIZE));
  const visiblePaths = paths.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  if (!paths.length) return null;

  return (
    <section className="mb-[80px]">
      <div className="mb-8 flex items-end justify-between">
        <div>
          <span className="mb-3 block text-[12px] font-extrabold uppercase leading-none tracking-[0.12em] text-[#4b0009]">
            Học theo từng bước
          </span>
          <h2 className="text-[32px] font-bold leading-[1.2]">
            Lộ trình tham khảo
          </h2>
        </div>
        <Link className="group flex items-center gap-2 text-[14px] font-semibold text-[#4b0009]" to="/learning-paths">
          <span className="group-hover:underline">Xem tất cả</span>
          <ArrowRight className="h-4 w-4" />
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {visiblePaths.map((path) => (
          <article
            className="flex min-h-[350px] flex-col overflow-hidden rounded-[24px] border border-[#dfbfbd]/30 bg-white p-5 shadow-sm transition-all duration-200 hover:-translate-y-1 hover:border-[#dfbfbd] hover:shadow-md"
            key={path.code}
          >
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

      {totalPages > 1 ? (
        <div className="mt-7 flex items-center justify-center gap-3">
          <button
            className="rounded-xl border border-[#dfbfbd] bg-white p-2 text-[#730014] transition hover:bg-[#fff4f5] disabled:cursor-not-allowed disabled:opacity-40"
            disabled={page === 0}
            onClick={() => setPage((current) => current - 1)}
            type="button"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <span className="text-sm font-bold text-[#584140]">
            Trang {page + 1}/{totalPages}
          </span>
          <button
            className="rounded-xl border border-[#dfbfbd] bg-white p-2 text-[#730014] transition hover:bg-[#fff4f5] disabled:cursor-not-allowed disabled:opacity-40"
            disabled={page === totalPages - 1}
            onClick={() => setPage((current) => current + 1)}
            type="button"
          >
            <ArrowRight className="h-4 w-4" />
          </button>
        </div>
      ) : null}
    </section>
  );
}
