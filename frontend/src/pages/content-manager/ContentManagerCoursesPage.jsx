import { useEffect, useMemo, useState } from 'react';
import { BookOpen, ChevronLeft, ChevronRight, Filter, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';

const levelOptions = ['Tất cả', 'BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
const statusOptions = ['Tất cả', 'DRAFT', 'PUBLISHED', 'ARCHIVED'];
const pageSize = 5;
const sortOptions = [
  { label: 'Mới nhất', value: 'newest' },
  { label: 'Cũ nhất', value: 'oldest' },
  { label: 'Tên A-Z', value: 'titleAsc' },
  { label: 'Tên Z-A', value: 'titleDesc' },
  { label: 'Giá cao đến thấp', value: 'priceDesc' },
  { label: 'Giá thấp đến cao', value: 'priceAsc' },
];

export default function ContentManagerCoursesPage() {
  const [courses, setCourses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filters, setFilters] = useState({ category: 'Tất cả', level: 'Tất cả', status: 'Tất cả', sort: 'newest' });
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadCourses = async (activeRef = { current: true }) => {
    setLoading(true);
    setError('');
    try {
      const [coursePage, categoryItems] = await Promise.all([
        courseApi.getManagedOnlineCourses({ page: 0, size: 500 }),
        courseApi.getManagedCourseCategories().catch(() => []),
      ]);
      if (!activeRef.current) return;
      setCourses(coursePage.content || []);
      setCategories(categoryItems);
    } catch {
      if (activeRef.current) setError('Không tải được danh sách khóa học từ backend.');
    } finally {
      if (activeRef.current) setLoading(false);
    }
  };

  useEffect(() => {
    const activeRef = { current: true };
    loadCourses(activeRef);

    return () => {
      activeRef.current = false;
    };
  }, []);

  const filteredCourses = useMemo(() => {
    const filtered = courses.filter((course) => {
      const searchValue = keyword.trim().toLocaleLowerCase('vi');
      const keywordMatched = !searchValue
        || String(course.title || '').toLocaleLowerCase('vi').includes(searchValue)
        || String(course.slug || '').toLocaleLowerCase('vi').includes(searchValue);
      const categoryMatched = filters.category === 'Tất cả' || course.category === filters.category;
      const levelMatched = filters.level === 'Tất cả' || course.level === filters.level;
      const statusMatched = filters.status === 'Tất cả' || course.status === filters.status;
      return keywordMatched && categoryMatched && levelMatched && statusMatched;
    });

    return [...filtered].sort((a, b) => {
      if (filters.sort === 'oldest') return new Date(a.createdAt || 0) - new Date(b.createdAt || 0);
      if (filters.sort === 'titleAsc') return String(a.title || '').localeCompare(String(b.title || ''));
      if (filters.sort === 'titleDesc') return String(b.title || '').localeCompare(String(a.title || ''));
      if (filters.sort === 'priceAsc') return Number(a.price || 0) - Number(b.price || 0);
      if (filters.sort === 'priceDesc') return Number(b.price || 0) - Number(a.price || 0);
      return new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0);
    });
  }, [courses, filters, keyword]);

  const categoryOptions = useMemo(
    () => [
      { label: 'Tất cả', value: 'Tất cả' },
      ...categories.map((category) => ({ label: category.name, value: category.code })),
    ],
    [categories],
  );
  const totalPages = Math.max(1, Math.ceil(filteredCourses.length / pageSize));
  const visibleCourses = filteredCourses.slice((page - 1) * pageSize, page * pageSize);

  useEffect(() => {
    setPage(1);
  }, [filters, keyword]);

  useEffect(() => {
    if (page > totalPages) setPage(totalPages);
  }, [page, totalPages]);

  const updateFilter = (field) => (event) => setFilters((current) => ({ ...current, [field]: event.target.value }));

  return (
    <div className="space-y-5">
      {error ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">
          <span>{error}</span>
          <button
            className="inline-flex items-center gap-2 rounded-xl border border-[#93000a]/25 bg-white/70 px-3 py-2"
            onClick={() => loadCourses()}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Thử lại
          </button>
        </div>
      ) : null}

      <Panel className="rounded-xl border-[#e9d7d6]/80 bg-white p-4 shadow-sm">
        <div className="grid gap-3 xl:grid-cols-[minmax(320px,1fr)_170px_160px_160px_160px_44px]">
          <label className="block">
            <span className="relative block">
              <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#c2acab]" />
              <input
                className="h-11 w-full rounded-lg border border-[#ecdedd] bg-[#fffafb] py-2 pl-10 pr-4 text-sm text-[#1a1c1c] outline-none transition focus:border-[#730014] focus:bg-white"
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Tìm theo tiêu đề, giảng viên hoặc học phần..."
                value={keyword}
              />
            </span>
          </label>
          <FilterSelect compact prefix="Danh mục" onChange={updateFilter('category')} options={categoryOptions} value={filters.category} />
          <FilterSelect compact prefix="Trình độ" onChange={updateFilter('level')} options={levelOptions} value={filters.level} />
          <FilterSelect compact prefix="Trạng thái" onChange={updateFilter('status')} options={statusOptions} value={filters.status} />
          <FilterSelect compact prefix="Sắp xếp" onChange={updateFilter('sort')} options={sortOptions} value={filters.sort} />
          <div>
            <button
              aria-label="Làm mới danh sách khóa học"
              className="inline-flex h-11 w-11 items-center justify-center rounded-lg border border-[#ecdedd] bg-white text-[#730014] transition hover:bg-[#fff2f3]"
              onClick={() => loadCourses()}
              type="button"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </Panel>

      <Panel className="overflow-hidden rounded-xl border-[#e9d7d6]/80 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="min-w-[1040px] w-full text-left">
            <thead className="bg-[#eef4ff] text-[11px] uppercase tracking-[0.12em] text-[#6c7a8d]">
              <tr>
                {['Tên khóa học', 'Danh mục', 'Trình độ', 'Bài học', 'Giờ học', 'Học phí', 'Trạng thái', 'Cập nhật lần cuối', 'Thao tác'].map((heading) => (
                  <th key={heading} className={`px-5 py-4 font-bold ${heading === 'Thao tác' ? 'text-right' : ''} ${heading === 'Bài học' ? 'text-center' : ''}`}>{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#eef1f6]">
              {loading ? (
                Array.from({ length: pageSize }).map((_, index) => (
                  <tr key={index}>
                    {Array.from({ length: 9 }).map((__, cellIndex) => (
                      <td key={cellIndex} className="px-5 py-5">
                        <div className="h-4 animate-pulse rounded bg-[#eef1f6]" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : visibleCourses.length ? (
                visibleCourses.map((course) => (
                  <tr key={course.id} className="bg-white transition hover:bg-[#fbfdff]">
                    <td className="px-5 py-5">
                      <div className="flex min-w-[260px] items-center gap-4">
                        <CourseThumb course={course} />
                        <div className="min-w-0">
                          <p className="max-w-[260px] overflow-hidden text-sm font-extrabold leading-5 text-[#26364a] [display:-webkit-box] [-webkit-box-orient:vertical] [-webkit-line-clamp:2]">{course.title}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-5 text-sm text-[#26364a]">{formatCategory(course.categoryName || course.category)}</td>
                    <td className="px-5 py-5 text-sm"><LevelBadge level={course.level} /></td>
                    <td className="px-5 py-5 text-center text-sm font-bold text-[#26364a]">{formatLessonCount(course)}</td>
                    <td className="px-5 py-5 text-sm font-bold text-[#26364a]">{course.totalHours ?? 0}h</td>
                    <td className="px-5 py-5 text-sm font-extrabold text-[#26364a]">{formatPrice(course.price)}</td>
                    <td className="px-5 py-5"><StatusPill status={course.status} /></td>
                    <td className="px-5 py-5 text-sm text-[#69778a]">{formatDate(course.updatedAt)}</td>
                    <td className="px-5 py-4">
                      <div className="flex items-center justify-end gap-2">
                        <Link className="rounded-lg border border-[#8b706e]/60 bg-white px-3 py-2 text-xs font-bold leading-4 text-[#4b0009] transition hover:bg-[#fff2f3]" to={`/content-manager/courses/${course.slug}/edit`}>
                          Chỉnh sửa
                        </Link>
                        <Link className="rounded-lg bg-[#4b0009] px-4 py-2 text-xs font-bold leading-4 text-white transition hover:bg-[#730014]" to={`/content-manager/courses/${course.slug}/builder`}>
                          Biên soạn
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td className="px-5 py-10 text-center text-sm text-[#584140]" colSpan={9}>
                    Không có khóa học nào khớp với bộ lọc đã chọn.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-col gap-3 border-t border-[#eef1f6] px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-sm text-[#69778a]">
            Hiển thị <span className="font-bold text-[#26364a]">{filteredCourses.length ? (page - 1) * pageSize + 1 : 0} - {Math.min(page * pageSize, filteredCourses.length)}</span> của <span className="font-bold text-[#26364a]">{filteredCourses.length}</span> khóa học
          </p>
          <div className="flex items-center gap-2">
            <button className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[#e3e8f0] bg-white text-[#69778a] transition hover:bg-[#f7f9fc] disabled:opacity-35" disabled={page <= 1} onClick={() => setPage((current) => current - 1)} type="button">
              <ChevronLeft className="h-4 w-4" />
            </button>
            {buildPageItems(page, totalPages).map((item, index) => (
              item === 'dots' ? (
                <span className="px-1 text-sm text-[#69778a]" key={`${item}-${index}`}>...</span>
              ) : (
                <button
                  className={`inline-flex h-9 w-9 items-center justify-center rounded-lg text-sm font-bold transition ${item === page ? 'bg-[#4b0009] text-white' : 'text-[#69778a] hover:bg-[#f7f9fc]'}`}
                  key={item}
                  onClick={() => setPage(item)}
                  type="button"
                >
                  {item}
                </button>
              )
            ))}
            <button className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[#e3e8f0] bg-white text-[#69778a] transition hover:bg-[#f7f9fc] disabled:opacity-35" disabled={page >= totalPages} onClick={() => setPage((current) => current + 1)} type="button">
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </Panel>
    </div>
  );
}

function FilterSelect({ compact = false, label, prefix, value, onChange, options }) {
  const normalized = options.map((option) => {
    const normalizedOption = typeof option === 'string' ? { label: option, value: option } : option;
    return {
      ...normalizedOption,
      label: compact && prefix ? `${prefix}: ${normalizedOption.label}` : normalizedOption.label,
    };
  });

  return (
    <div>
      {label ? <span className="mb-2 block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</span> : null}
      <BrandedSelect buttonClassName={compact ? 'h-11 rounded-lg border-[#ecdedd] bg-white py-2 text-sm shadow-none' : undefined} onChange={onChange} options={normalized} value={value} />
    </div>
  );
}

function CourseThumb({ course }) {
  if (course.thumbnailUrl) {
    return (
      <img
        alt={course.title || 'Khóa học'}
        className="h-12 w-12 shrink-0 rounded-lg border border-[#e3e8f0] bg-[#f7f9fc] object-cover"
        src={course.thumbnailUrl}
      />
    );
  }

  return (
    <span className="inline-flex h-12 w-12 shrink-0 items-center justify-center rounded-lg border border-[#e3e8f0] bg-[#f7f9fc] text-[#730014]">
      <BookOpen className="h-5 w-5" />
    </span>
  );
}

function LevelBadge({ level }) {
  const normalized = String(level || '').toUpperCase();
  const label = formatLabel(level);
  const tone = normalized === 'ADVANCED'
    ? 'bg-[#f1ecec] text-[#4b0009]'
    : normalized === 'INTERMEDIATE'
      ? 'bg-[#eef3ff] text-[#53627a]'
      : 'bg-[#f4f4f5] text-[#69778a]';

  return (
    <span className={`inline-flex rounded-md px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-[0.08em] ${tone}`}>
      {label}
    </span>
  );
}

function StatusPill({ status }) {
  const label = formatLabel(status);
  const normalized = String(status || '').toUpperCase();
  const tone = normalized === 'PUBLISHED'
    ? 'bg-emerald-100 text-emerald-700'
    : normalized === 'DRAFT'
      ? 'bg-amber-100 text-amber-700'
      : 'bg-slate-100 text-slate-700';

  return (
    <span className={`inline-flex whitespace-nowrap rounded-full px-3 py-1 text-[11px] font-bold ${tone}`}>
      {label}
    </span>
  );
}

function formatLessonCount(course) {
  const modules = course.totalModules ?? course.moduleCount;
  const lessons = course.totalLessons ?? 0;
  if (modules) return `${modules} / ${lessons}`;
  return String(lessons);
}

function buildPageItems(currentPage, totalPages) {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index + 1);
  const items = [1];
  if (currentPage > 3) items.push('dots');
  const start = Math.max(2, currentPage - 1);
  const end = Math.min(totalPages - 1, currentPage + 1);
  for (let item = start; item <= end; item += 1) items.push(item);
  if (currentPage < totalPages - 2) items.push('dots');
  items.push(totalPages);
  return items;
}

function formatPrice(value) {
  const amount = Number(value || 0);
  if (!amount) return 'Miễn phí';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(amount);
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatLabel(value) {
  if (!value) return '-';
  const normalized = String(value).toUpperCase();
  const labels = {
    BEGINNER: 'Cơ bản',
    INTERMEDIATE: 'Trung cấp',
    ADVANCED: 'Nâng cao',
    DRAFT: 'Nháp',
    PUBLISHED: 'Đã xuất bản',
    ARCHIVED: 'Lưu trữ',
  };
  return labels[normalized] || (String(value).charAt(0) + String(value).slice(1).toLowerCase());
}

function formatCategory(value) {
  const labels = {
    COMMUNICATION: 'Giao tiếp',
    FOUNDATION: 'Nền tảng',
    ONLINE: 'Online',
  };
  return labels[String(value || '').toUpperCase()] || value || '-';
}
