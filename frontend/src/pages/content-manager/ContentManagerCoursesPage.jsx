import { useEffect, useMemo, useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { Link } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { Panel, StatusBadge } from '../../components/content-manager/ContentManagerUi';

const categoryOptions = ['All', 'IELTS', 'TOEIC', 'COMMUNICATION', 'FOUNDATION', 'ONLINE'];
const levelOptions = ['All', 'BEGINNER', 'INTERMEDIATE', 'ADVANCED'];
const statusOptions = ['All', 'DRAFT', 'PUBLISHED', 'ARCHIVED'];
const sortOptions = [
  { label: 'Newest', value: 'newest' },
  { label: 'Oldest', value: 'oldest' },
  { label: 'Title A-Z', value: 'titleAsc' },
  { label: 'Title Z-A', value: 'titleDesc' },
  { label: 'Price High', value: 'priceDesc' },
  { label: 'Price Low', value: 'priceAsc' },
];

export default function ContentManagerCoursesPage() {
  const [courses, setCourses] = useState([]);
  const [filters, setFilters] = useState({ category: 'All', level: 'All', status: 'All', sort: 'newest' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    setLoading(true);
    courseApi.getManagedOnlineCourses({ page: 0, size: 200 })
      .then((page) => {
        if (!active) return;
        setCourses(page.content || []);
      })
      .catch(() => {
        if (active) setError('Không tải được danh sách khóa học từ backend.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const filteredCourses = useMemo(() => {
    const filtered = courses.filter((course) => {
      const categoryMatched = filters.category === 'All' || course.category === filters.category;
      const levelMatched = filters.level === 'All' || course.level === filters.level;
      const statusMatched = filters.status === 'All' || course.status === filters.status;
      return categoryMatched && levelMatched && statusMatched;
    });

    return [...filtered].sort((a, b) => {
      if (filters.sort === 'oldest') return new Date(a.createdAt || 0) - new Date(b.createdAt || 0);
      if (filters.sort === 'titleAsc') return String(a.title || '').localeCompare(String(b.title || ''));
      if (filters.sort === 'titleDesc') return String(b.title || '').localeCompare(String(a.title || ''));
      if (filters.sort === 'priceAsc') return Number(a.price || 0) - Number(b.price || 0);
      if (filters.sort === 'priceDesc') return Number(b.price || 0) - Number(a.price || 0);
      return new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0);
    });
  }, [courses, filters]);

  const updateFilter = (field) => (event) => setFilters((current) => ({ ...current, [field]: event.target.value }));

  return (
    <div className="space-y-6">
      {error ? <div className="rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-semibold text-[#93000a]">{error}</div> : null}

      <Panel className="p-5">
        <div className="flex flex-wrap items-center gap-3">
          <FilterSelect label="Category" onChange={updateFilter('category')} options={categoryOptions} value={filters.category} />
          <FilterSelect label="Level" onChange={updateFilter('level')} options={levelOptions} value={filters.level} />
          <FilterSelect label="Status" onChange={updateFilter('status')} options={statusOptions} value={filters.status} />
          <FilterSelect label="Sort" onChange={updateFilter('sort')} options={sortOptions} value={filters.sort} />
          <Link className="ml-auto rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white transition hover:bg-[#730014]" to="/content-manager/courses/new">
            Create New Course
          </Link>
        </div>
      </Panel>

      <Panel className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full text-left">
            <thead className="bg-[#fbf3f4] text-xs uppercase tracking-[0.18em] text-[#8e7371]">
              <tr>
                {['Course Title', 'Category', 'Level', 'Lessons', 'Hours', 'Price', 'Status', 'Last Updated', 'Actions'].map((heading) => (
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
              ) : filteredCourses.length ? (
                filteredCourses.map((course) => (
                  <tr key={course.id} className="bg-white transition hover:bg-[#fffafb]">
                    <td className="px-5 py-4">
                      <div>
                        <p className="font-semibold text-[#1a1c1c]">{course.title}</p>
                        <p className="text-sm text-[#584140]">{course.slug}</p>
                      </div>
                    </td>
                    <td className="px-5 py-4 text-sm">{course.categoryName || course.category}</td>
                    <td className="px-5 py-4 text-sm">{formatLabel(course.level)}</td>
                    <td className="px-5 py-4 text-sm">{course.totalLessons ?? 0}</td>
                    <td className="px-5 py-4 text-sm">{course.totalHours ?? 0}</td>
                    <td className="px-5 py-4 text-sm font-semibold">{formatPrice(course.price)}</td>
                    <td className="px-5 py-4"><StatusBadge label={course.status} /></td>
                    <td className="px-5 py-4 text-sm text-[#584140]">{formatDate(course.updatedAt)}</td>
                    <td className="px-5 py-4">
                      <div className="flex flex-wrap gap-2">
                        <Link className="rounded-xl border border-[#dfbfbd]/60 px-3 py-2 text-sm font-medium text-[#730014] transition hover:bg-[#fff2f3]" to={`/content-manager/courses/${course.slug}/edit`}>
                          Edit
                        </Link>
                        <Link className="rounded-xl bg-[#4b0009] px-3 py-2 text-sm font-medium text-white transition hover:bg-[#730014]" to={`/content-manager/courses/${course.slug}/builder`}>
                          Builder
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td className="px-5 py-10 text-center text-sm text-[#584140]" colSpan={9}>
                    No courses match the selected filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  );
}

function FilterSelect({ label, value, onChange, options }) {
  const [open, setOpen] = useState(false);
  const normalized = options.map((option) => (typeof option === 'string' ? { label: option, value: option } : option));
  const selected = normalized.find((option) => option.value === value) || normalized[0];

  return (
    <div className="relative">
      <button
        className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm text-[#584140] transition hover:border-[#730014]/40 hover:bg-[#fff7f7]"
        onBlur={() => window.setTimeout(() => setOpen(false), 120)}
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        <span>{label}:</span>
        <span className="font-semibold text-[#4b0009]">{selected?.label}</span>
        <ChevronDown className={`h-4 w-4 text-[#730014] transition ${open ? 'rotate-180' : ''}`} />
      </button>
      {open ? (
        <div className="absolute left-0 top-full z-50 mt-2 min-w-52 overflow-hidden rounded-2xl border border-[#dfbfbd]/75 bg-white p-1 shadow-[0_18px_45px_rgba(75,0,9,0.16)]">
          {normalized.map((option) => (
            <button
              key={option.value}
              className={`block w-full rounded-xl px-4 py-2.5 text-left text-sm font-semibold transition ${
                option.value === value ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-[#fff2f3]'
              }`}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => {
                onChange({ target: { value: option.value } });
                setOpen(false);
              }}
              type="button"
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
      <select className="sr-only" onChange={onChange} value={value}>
        {normalized.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
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
  return String(value).charAt(0) + String(value).slice(1).toLowerCase();
}
