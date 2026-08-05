import { useEffect, useMemo, useState } from 'react';
import { Archive, BookOpen, CheckCircle2, ChevronLeft, ChevronRight, Eye, Filter, Pencil, RefreshCw } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel } from '../../components/content-manager/ContentManagerUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import Pagination from '../../components/ui/Pagination';
import { useAppDialog } from '../../components/ui/AppDialog';
import ContentManagerCourseEditorPage from './ContentManagerCourseEditorPage';

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
  const { confirm: confirmDialog } = useAppDialog();
  const [searchParams, setSearchParams] = useSearchParams();
  const [courses, setCourses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filters, setFilters] = useState({ category: 'Tất cả', level: 'Tất cả', status: 'Tất cả', sort: 'newest' });
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [workingId, setWorkingId] = useState(null);

  const isCreateOpen = searchParams.get('new') === '1';
  const editingSlug = searchParams.get('edit');

  const handleCloseModal = () => {
    setSearchParams((prev) => {
      prev.delete('new');
      prev.delete('edit');
      return prev;
    });
  };

  const loadCourses = async (activeRef = { current: true }) => {
    setLoading(true);
    setError('');
    try {
      const loadCategories = async () => {
        try {
          return await courseApi.getManagedCourseCategories();
        } catch {
          return [];
        }
      };

      const [coursePage, categoryItems] = await Promise.all([
        courseApi.getManagedOnlineCourses({ page: 0, size: 500 }),
        loadCategories(),
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

  const changeCourseStatus = async (course, action) => {
    const publishing = action === 'PUBLISH';
    const prompt = publishing
      ? `Xuất bản khóa học "${course.title}"?`
      : `Lưu trữ khóa học "${course.title}"?`;
    if (!await confirmDialog(prompt, {
      title: publishing ? 'Xuất bản khóa học' : 'Lưu trữ khóa học',
      confirmLabel: publishing ? 'Xuất bản' : 'Lưu trữ',
      tone: publishing ? 'primary' : 'danger',
    })) return;
    setWorkingId(course.id);
    setError('');
    setSuccess('');
    try {
      const updated = publishing
        ? await courseApi.publishOnlineCourseDraft(course.id)
        : await courseApi.archiveOnlineCourse(course.id);
      setCourses((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setSuccess(publishing ? 'Đã xuất bản khóa học.' : 'Đã lưu trữ khóa học.');
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || (publishing ? 'Không thể xuất bản khóa học.' : 'Không thể lưu trữ khóa học.'));
    } finally {
      setWorkingId(null);
    }
  };

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
      {success ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-800">{success}</div> : null}

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
            <thead className="bg-[#fbf3f4] text-[11px] uppercase tracking-[0.12em] text-[#8e7371]">
              <tr>
                {['Tên khóa học', 'Danh mục', 'Trình độ', 'Bài học', 'Giờ học', 'Học phí', 'Trạng thái', 'Cập nhật lần cuối', 'Thao tác'].map((heading) => (
                  <th key={heading} className={`px-5 py-4 font-bold ${heading === 'Thao tác' ? 'text-left' : ''} ${heading === 'Bài học' ? 'text-center' : ''}`}>{heading}</th>
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
                    <td className="whitespace-nowrap px-5 py-4 text-left">
                      <div className="inline-flex items-center justify-start gap-2">
                        <Link
                          className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-[#fffafb] px-3 text-xs font-bold text-[#730014] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                          to={`/content-manager/courses/${course.slug}/preview`}
                        >
                          <Eye className="h-3.5 w-3.5" />
                          Xem trước
                        </Link>
                        <button
                          className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg border border-[#8b706e]/50 bg-white px-3 text-xs font-bold text-[#4b0009] whitespace-nowrap transition hover:bg-[#fff2f3] active:scale-95"
                          onClick={() => setSearchParams((prev) => { prev.set('edit', course.slug); return prev; })}
                          type="button"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                          Chỉnh sửa
                        </button>
                        <Link
                          className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#4b0009] px-3 text-xs font-bold text-white whitespace-nowrap transition hover:bg-[#730014] active:scale-95"
                          to={`/content-manager/courses/${course.slug}/builder`}
                        >
                          <BookOpen className="h-3.5 w-3.5" />
                          Biên soạn
                        </Link>
                        {course.status === 'DRAFT' || course.status === 'REJECTED' || course.status === 'PENDING_REVIEW' ? (
                          <button
                            className="inline-flex h-8 shrink-0 items-center justify-center gap-1.5 rounded-lg bg-[#730014] px-3 text-xs font-bold text-white whitespace-nowrap transition hover:bg-[#8a0018] disabled:opacity-50 active:scale-95"
                            disabled={workingId === course.id}
                            onClick={() => changeCourseStatus(course, 'PUBLISH')}
                            type="button"
                          >
                            <CheckCircle2 className="h-3.5 w-3.5" />
                            Xuất bản
                          </button>
                        ) : null}
                        {course.status === 'PUBLISHED' ? (
                          <button
                            aria-label={`Lưu trữ ${course.title}`}
                            className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-rose-200 bg-white text-rose-700 transition hover:bg-rose-50 disabled:opacity-50 active:scale-95"
                            disabled={workingId === course.id}
                            onClick={() => changeCourseStatus(course, 'ARCHIVE')}
                            title="Lưu trữ"
                            type="button"
                          >
                            <Archive className="h-3.5 w-3.5" />
                          </button>
                        ) : null}
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

        <div className="flex flex-col gap-3 border-t border-[#eef1f6] px-5 py-4 sm:flex-row sm:items-center sm:justify-between bg-[#fffafb]/25">
          <p className="text-sm text-[#69778a]">
            Hiển thị <span className="font-bold text-[#26364a]">{filteredCourses.length ? (page - 1) * pageSize + 1 : 0} - {Math.min(page * pageSize, filteredCourses.length)}</span> của <span className="font-bold text-[#26364a]">{filteredCourses.length}</span> khóa học
          </p>
          <Pagination
            page={page}
            totalPages={totalPages}
            onChange={setPage}
            totalItems={filteredCourses.length}
            pageSize={pageSize}
          />
        </div>
      </Panel>

      {isCreateOpen && (
        <EditorModal onClose={handleCloseModal}>
          <ContentManagerCourseEditorPage
            onClose={handleCloseModal}
            onSave={() => {
              loadCourses();
              handleCloseModal();
            }}
          />
        </EditorModal>
      )}

      {editingSlug && (
        <EditorModal onClose={handleCloseModal}>
          <ContentManagerCourseEditorPage
            slugOrId={editingSlug}
            onClose={handleCloseModal}
            onSave={() => {
              loadCourses();
            }}
          />
        </EditorModal>
      )}
    </div>
  );
}

function EditorModal({ children, onClose }) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden px-3 py-4 sm:px-6" role="dialog" aria-modal="true">
      <button
        aria-label="Đóng modal"
        className="absolute -inset-10 bg-[#1a0004]/45 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <div className="relative z-10 w-full max-w-[1200px] pointer-events-auto bg-[#fafafa] rounded-3xl border border-[#dcc0bf]/35 p-6 shadow-2xl overflow-y-auto max-h-[90vh]">
        {children}
      </div>
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
