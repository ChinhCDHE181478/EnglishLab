import { useEffect, useMemo, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, StatusBadge } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';

const levelOptions = ['Tất cả', 'BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
const statusOptions = ['Tất cả', 'DRAFT', 'PUBLISHED', 'ARCHIVED'];
const pageSize = 10;
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
    <div className="space-y-6">
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

      <Panel className="p-5">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
          <label className="block">
            <span className="mb-2 block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">Tìm kiếm</span>
            <input
              className="w-full rounded-[18px] border border-[#dfbfbd]/75 bg-white px-4 py-3 text-sm text-[#1a1c1c] outline-none focus:border-[#730014]"
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tên khóa học hoặc slug"
              value={keyword}
            />
          </label>
          <FilterSelect label="Danh mục" onChange={updateFilter('category')} options={categoryOptions} value={filters.category} />
          <FilterSelect label="Trình độ" onChange={updateFilter('level')} options={levelOptions} value={filters.level} />
          <FilterSelect label="Trạng thái" onChange={updateFilter('status')} options={statusOptions} value={filters.status} />
          <FilterSelect label="Sắp xếp" onChange={updateFilter('sort')} options={sortOptions} value={filters.sort} />
        </div>
        <div className="mt-4 flex flex-wrap justify-end gap-3">
          <button
            className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-semibold text-[#730014]"
            onClick={() => loadCourses()}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
          <Link className="rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" to="/content-manager/courses/new">
            Tạo khóa học mới
          </Link>
        </div>
      </Panel>

      <Panel className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-xs uppercase tracking-[0.18em] text-[#8e7371]">
              <tr>
                {['Tên khóa học', 'Danh mục', 'Trình độ', 'Bài học', 'Giờ học', 'Học phí', 'Trạng thái', 'Cập nhật lần cuối', 'Thao tác'].map((heading) => (
                  <th key={heading} className="px-5 py-4 font-semibold">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e3e4]">
              {loading ? (
                Array.from({ length: 4 }).map((_, index) => (
                  <tr key={index}>
                    {Array.from({ length: 9 }).map((__, cellIndex) => (
                      <td key={cellIndex} className="px-5 py-4">
                        <div className="h-4 animate-pulse rounded bg-[#f3e5e7]" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : visibleCourses.length ? (
                visibleCourses.map((course) => (
                  <tr key={course.id} className="bg-white transition hover:bg-[#fffafb]">
                    <td className="px-5 py-4">
                      <div>
                        <p className="font-semibold text-[#1a1c1c]">{course.title}</p>
                        <p className="text-sm text-[#584140]">{course.slug}</p>
                      </div>
                    </td>
                    <td className="px-5 py-4 text-sm">{formatCategory(course.categoryName || course.category)}</td>
                    <td className="px-5 py-4 text-sm">{formatLabel(course.level)}</td>
                    <td className="px-5 py-4 text-sm">{course.totalLessons ?? 0}</td>
                    <td className="px-5 py-4 text-sm">{course.totalHours ?? 0}</td>
                    <td className="px-5 py-4 text-sm font-semibold">{formatPrice(course.price)}</td>
                    <td className="px-5 py-4"><StatusBadge label={course.status} /></td>
                    <td className="px-5 py-4 text-sm text-[#584140]">{formatDate(course.updatedAt)}</td>
                    <td className="px-5 py-4">
                      <div className="flex flex-wrap gap-2">
                        <Link className="rounded-xl border border-[#dfbfbd]/60 px-3 py-2 text-sm font-medium text-[#730014] transition hover:bg-[#fff2f3]" to={`/content-manager/courses/${course.slug}/edit`}>
                          Chỉnh sửa
                        </Link>
                        <Link className="rounded-xl bg-[#4b0009] px-3 py-2 text-sm font-medium text-white transition hover:bg-[#730014]" to={`/content-manager/courses/${course.slug}/builder`}>
                          Biên soạn nội dung
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
      </Panel>

      {totalPages > 1 ? (
        <div className="flex items-center justify-center gap-3">
          <button className="rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page <= 1} onClick={() => setPage((current) => current - 1)} type="button">Trang trước</button>
          <span className="text-sm font-semibold text-[#584140]">Trang {page} / {totalPages}</span>
          <button className="rounded-xl border border-[#dfbfbd] bg-white px-4 py-2 text-sm font-bold text-[#730014] disabled:opacity-40" disabled={page >= totalPages} onClick={() => setPage((current) => current + 1)} type="button">Trang sau</button>
        </div>
      ) : null}
    </div>
  );
}

function FilterSelect({ label, value, onChange, options }) {
  const normalized = options.map((option) => (typeof option === 'string' ? { label: option, value: option } : option));

  return (
    <div>
      <span className="mb-2 block text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{label}</span>
      <BrandedSelect onChange={onChange} options={normalized} value={value} />
    </div>
  );
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
