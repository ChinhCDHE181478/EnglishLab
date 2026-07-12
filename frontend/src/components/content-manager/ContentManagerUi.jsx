import { useEffect, useRef, useState } from 'react';
import { ChevronDown, ChevronRight, LogOut, Plus } from 'lucide-react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { clearSession, getStoredUser } from '../../utils/auth';
import { contentManagerNav, contentManagerPageMeta } from './contentManagerConfig';
import BrandedSelect from '../ui/BrandedSelect';

export function ContentManagerLayout({ children }) {
  const location = useLocation();
  const navigate = useNavigate();
  const meta = resolveMeta(location.pathname);
  const crumbs = location.pathname.replace('/content-manager/', '').split('/').filter(Boolean);
  const [bootLoading, setBootLoading] = useState(true);
  const [currentUser, setCurrentUser] = useState(() => getStoredUser());
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const sidebarNavRef = useRef(null);
  const sidebarScrollTopRef = useRef(0);

  useEffect(() => {
    const timerId = window.setTimeout(() => {
      setBootLoading(false);
    }, 650);

    return () => {
      window.clearTimeout(timerId);
    };
  }, []);

  useEffect(() => {
    setCurrentUser(getStoredUser());
    setAccountMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    const restoreScrollPosition = () => {
      if (sidebarNavRef.current) {
        sidebarNavRef.current.scrollTop = sidebarScrollTopRef.current;
      }
    };
    const frameId = window.requestAnimationFrame(() => {
      window.requestAnimationFrame(restoreScrollPosition);
    });
    const timeoutId = window.setTimeout(restoreScrollPosition, 80);
    return () => {
      window.cancelAnimationFrame(frameId);
      window.clearTimeout(timeoutId);
    };
  }, [location.pathname]);

  const handleLogout = () => {
    clearSession();
    navigate('/login', { replace: true });
  };

  const displayName =
    currentUser?.fullName || currentUser?.username || currentUser?.email || 'EnglishLab Admin';
  const displayRole = formatRoleLabel(currentUser?.role || 'CONTENT_MANAGER');
  const avatarLetter = displayName.charAt(0).toUpperCase();
  const mobileNavOptions = contentManagerNav.flatMap((section) =>
    section.items.map((item) => ({ label: item.label, value: item.href })),
  );
  const mobileNavValue = resolveMobileNavValue(location.pathname, mobileNavOptions);

  return (
    <div className="min-h-screen bg-[#f8fafc] font-['Inter'] text-slate-800 antialiased">
      <div
        className="pointer-events-none fixed inset-0 opacity-[0.02]"
        style={{
          backgroundImage: 'radial-gradient(#4b0009 1px, transparent 1px)',
          backgroundSize: '24px 24px',
        }}
      />

      <aside className="fixed inset-y-0 left-0 z-30 hidden w-[270px] flex-col overflow-hidden border-r border-slate-200 bg-[#4b0009] text-white shadow-xl lg:flex">
        <div className="shrink-0 border-b border-white/10 bg-[#4b0009] px-6 pb-5 pt-7">
          <p className="bg-gradient-to-r from-white to-pink-200 bg-clip-text font-['Manrope'] text-2xl font-black tracking-tight text-transparent">
            EnglishLab
          </p>
          <p className="mt-1.5 text-[10px] font-bold uppercase tracking-[0.2em] text-white/50">
            Hệ thống quản lý nội dung
          </p>
        </div>

        <div
          className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-6 [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden"
          onScroll={(event) => {
            sidebarScrollTopRef.current = event.currentTarget.scrollTop;
          }}
          ref={sidebarNavRef}
        >
          <nav className="space-y-7">
            {contentManagerNav.map((section) => (
              <div key={section.title}>
                <p className="mb-3 px-4 text-[10px] font-bold uppercase tracking-[0.25em] text-white/40">
                  {section.title}
                </p>
                <div className="space-y-1.5">
                  {section.items.map((item) => {
                    const Icon = item.icon;
                    return (
                      <NavLink
                        key={item.href}
                        className={({ isActive }) =>
                          `group flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all duration-200 ${
                            isActive
                              ? 'bg-white font-semibold text-[#4b0009] shadow-md shadow-black/10'
                              : 'text-white/70 hover:bg-white/10 hover:text-white'
                          }`
                        }
                        onPointerDown={() => {
                          sidebarScrollTopRef.current = sidebarNavRef.current?.scrollTop ?? 0;
                        }}
                        onMouseDown={(event) => {
                          sidebarScrollTopRef.current = sidebarNavRef.current?.scrollTop ?? 0;
                          // Prevent browser focus from scrolling the sidebar back to the active link.
                          event.preventDefault();
                        }}
                        to={item.href}
                      >
                        <Icon className="h-[18px] w-[18px] transition-transform duration-200 group-hover:scale-105" />
                        <span>{item.label}</span>
                      </NavLink>
                    );
                  })}
                </div>
              </div>
            ))}
          </nav>
        </div>
      </aside>

      <div className="lg:ml-[270px]">
        <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/80 backdrop-blur-md">
          <div className="mx-auto flex max-w-[1680px] items-center justify-between gap-4 px-4 py-3.5 sm:px-6 lg:px-8">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-1.5 text-xs font-medium text-slate-500">
                <Link className="transition hover:text-[#730014]" to="/content-manager/dashboard">
                  Quản lý nội dung
                </Link>
                {crumbs.map((crumb, index) => {
                  const isLast = index === crumbs.length - 1;
                  const href = resolveCrumbHref(crumbs, index);
                  const label = formatCrumbLabel(crumbs, index);
                  return (
                    <span key={`${crumb}-${index}`} className="inline-flex items-center gap-1.5 capitalize">
                      <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                      {isLast || !href ? (
                        <span className="font-semibold text-slate-800">{label}</span>
                      ) : (
                        <Link className="transition hover:text-[#730014]" to={href}>
                          {label}
                        </Link>
                      )}
                    </span>
                  );
                })}
              </div>

            </div>

            <div className="hidden items-center gap-3 sm:flex">
              <div className="relative">
                <button
                  className="flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-1.5 pr-4 text-left shadow-sm transition-all duration-200 hover:border-slate-300 hover:shadow-md"
                  onBlur={() => window.setTimeout(() => setAccountMenuOpen(false), 150)}
                  onClick={() => setAccountMenuOpen((current) => !current)}
                  type="button"
                >
                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-[#730014] to-[#4b0009] text-sm font-bold text-white shadow-inner">
                    {avatarLetter}
                  </div>
                  <div className="hidden text-left md:block">
                    <p className="text-xs font-semibold leading-tight text-slate-800">{displayName}</p>
                    <p className="mt-0.5 text-[10px] font-medium uppercase tracking-wider text-slate-400">{displayRole}</p>
                  </div>
                  <ChevronDown
                    className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${accountMenuOpen ? 'rotate-180' : ''}`}
                  />
                </button>

                {accountMenuOpen ? (
                  <div className="absolute right-0 top-full z-50 mt-2 w-56 overflow-hidden rounded-xl border border-slate-200 bg-white p-1 shadow-xl">
                    <div className="border-b border-slate-100 px-3 py-2 md:hidden">
                      <p className="text-sm font-semibold text-slate-800">{displayName}</p>
                    </div>
                    <button
                      className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-left text-sm font-medium text-rose-600 transition hover:bg-rose-50"
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={handleLogout}
                      type="button"
                    >
                      <LogOut className="h-4 w-4" />
                      Đăng xuất
                    </button>
                  </div>
                ) : null}
              </div>
            </div>
          </div>
          <div className="mx-auto flex max-w-[1680px] items-center gap-3 border-t border-slate-100 px-4 py-2.5 sm:px-6 lg:hidden">
            <div className="min-w-0 flex-1">
              <BrandedSelect
                onChange={(event) => navigate(event.target.value)}
                options={mobileNavOptions}
                value={mobileNavValue}
              />
            </div>
            <button
              aria-label="Đăng xuất"
              className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white text-rose-600 transition hover:bg-rose-50 active:scale-95"
              onClick={handleLogout}
              title="Đăng xuất"
              type="button"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </header>

        <main className="mx-auto max-w-[1680px] px-4 py-6 sm:px-6 lg:px-8">
          {bootLoading ? (
            <section className="space-y-8">
              <div>
                <div className="h-10 w-72 animate-pulse rounded-2xl bg-[#f1e3e4]" />
                <div className="mt-3 h-5 w-full max-w-3xl animate-pulse rounded-2xl bg-[#f6ecec]" />
              </div>
              <ContentManagerLoadingState message="Đang tải dữ liệu quản lý..." />
            </section>
          ) : (
            <>
              <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl">
                    {meta.title}
                  </h1>
                  {meta.subtitle ? (
                    <p className="mt-2 max-w-4xl text-sm leading-relaxed text-slate-500">
                      {meta.subtitle}
                    </p>
                  ) : null}
                </div>
                {location.pathname === '/content-manager/courses' ? (
                  <Link
                    className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]"
                    to="/content-manager/courses/new"
                  >
                    <Plus className="h-4 w-4" />
                    Tạo khóa học mới
                  </Link>
                ) : null}
                {location.pathname === '/content-manager/flashcards' ? (
                  <Link
                    className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-[0.98]"
                    to="/content-manager/flashcards?new=1"
                  >
                    <Plus className="h-4 w-4" />
                    Tạo bộ thẻ mới
                  </Link>
                ) : null}
              </div>
              <div className="min-h-[500px]">
                {children}
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  );
}

function resolveMobileNavValue(pathname, options) {
  const exact = options.find((option) => option.value === pathname);
  if (exact) return exact.value;
  if (pathname.startsWith('/content-manager/courses/')) return '/content-manager/courses';
  return options.find((option) => pathname.startsWith(`${option.value}/`))?.value || '/content-manager/dashboard';
}

function resolveMeta(pathname) {
  if (contentManagerPageMeta[pathname]) {
    return contentManagerPageMeta[pathname];
  }

  if (/^\/content-manager\/courses\/[^/]+\/edit$/.test(pathname)) {
    return contentManagerPageMeta['/content-manager/courses/:slugOrId/edit'];
  }

  if (/^\/content-manager\/courses\/[^/]+\/builder$/.test(pathname)) {
    return contentManagerPageMeta['/content-manager/courses/:slugOrId/builder'];
  }

  if (pathname.startsWith('/content-manager/flashcards/')) {
    return contentManagerPageMeta['/content-manager/flashcards'];
  }

  return contentManagerPageMeta['/content-manager/dashboard'];
}

function formatRoleLabel(role) {
  return String(role || '')
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function resolveCrumbHref(crumbs, index) {
  const current = crumbs[index];
  const previous = crumbs[index - 1];
  const fullPath = crumbs.slice(0, index + 1);

  if (current === 'dashboard') return '/content-manager/dashboard';
  if (current === 'courses') return '/content-manager/courses';
  if (current === 'classrooms') return '/content-manager/classrooms';
  if (current === 'materials') return '/content-manager/materials';
  if (current === 'flashcards') return '/content-manager/flashcards';
  if (current === 'discount-codes') return '/content-manager/discount-codes';
  if (current === 'categories') return '/content-manager/categories';
  if (current === 'learning-paths' || current === 'syllabus') return '/content-manager/learning-paths';
  if (current === 'mock-exams') return '/content-manager/mock-exams';
  if (current === 'rubrics') return '/content-manager/rubrics';
  if (current === 'publication') return '/content-manager/publication';
  if (current === 'analytics') return '/content-manager/analytics';
  if (current === 'listening') return '/content-manager/listening';
  if (current === 'reading') return '/content-manager/reading';
  if (current === 'writing') return '/content-manager/writing';
  if (current === 'speaking') return '/content-manager/speaking';

  if (crumbs[0] === 'flashcards') {
    if (index === 1) {
      return `/content-manager/flashcards/${current}`;
    }
    return null;
  }

  if (previous === 'courses') {
    return `/content-manager/courses/${current}/edit`;
  }

  if (current === 'edit' || current === 'builder' || current === 'new') {
    return null;
  }

  return `/content-manager/${fullPath.join('/')}`;
}

function formatCrumbLabel(crumbs, index) {
  const current = crumbs[index];
  const previous = crumbs[index - 1];

  const dictionary = {
    dashboard: 'Tổng quan',
    courses: 'Khóa học online',
    classrooms: 'Tài liệu lớp học',
    'discount-codes': 'Mã giảm giá',
    categories: 'Danh mục khóa học',
    syllabus: 'Lộ trình học',
    'learning-paths': 'Lộ trình học',
    materials: 'Kho học liệu',
    flashcards: 'Thẻ ghi nhớ',
    'discussion-moderation': 'Kiểm duyệt thảo luận',
    listening: 'Luyện nghe',
    reading: 'Luyện đọc',
    writing: 'Luyện viết',
    speaking: 'Luyện nói',
    'mock-exams': 'Ngân hàng đề thi thử',
    rubrics: 'Rubrics chấm điểm',
    publication: 'Hàng chờ xuất bản',
    analytics: 'Phân tích nội dung',
    edit: 'Chỉnh sửa khóa học',
    builder: 'Biên soạn nội dung',
    new: 'Tạo khóa học',
    modules: 'Mô-đun',
  };

  if (crumbs[0] === 'flashcards' && index === 1) {
    return current.replace(/-/g, ' ');
  }

  if (previous === 'courses' && !dictionary[current]) {
    return current.replace(/-/g, ' ');
  }

  return dictionary[current] || current.replace(/-/g, ' ');
}

function translateManagerStatusLabel(label) {
  const map = {
    DRAFT: 'Bản nháp',
    Draft: 'Bản nháp',
    PUBLISHED: 'Đã xuất bản',
    Published: 'Đã xuất bản',
    ARCHIVED: 'Lưu trữ',
    Archived: 'Lưu trữ',
    'Pending review': 'Chờ duyệt',
    'PENDING REVIEW': 'Chờ duyệt',
    Ready: 'Sẵn sàng',
    READY: 'Sẵn sàng',
    Processed: 'Đã xử lý',
    PROCESSED: 'Đã xử lý',
    Encoding: 'Đang mã hóa',
    ENCODING: 'Đang mã hóa',
    ACTIVE: 'Đang hoạt động',
    INACTIVE: 'Tạm ngừng',
  };

  return map[label] || label;
}

export function Panel({ children, className = '', ...props }) {
  return (
    <section
      className={`rounded-2xl border border-slate-200 bg-white shadow-sm ${className}`}
      {...props}
    >
      {children}
    </section>
  );
}

export function ContentManagerLoadingState({ message = 'Đang tải dữ liệu...' }) {
  return (
    <div className="flex min-h-[400px] flex-1 flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white px-6 py-16 text-center shadow-sm">
      <div className="relative flex h-16 w-16 items-center justify-center">
        <div className="absolute h-12 w-12 animate-spin rounded-full border-4 border-slate-200 border-t-[#730014]" />
        <div className="h-6 w-6 rounded-full bg-[#4b0009]/10" />
      </div>
      <p className="mt-6 animate-pulse font-['Manrope'] text-base font-extrabold text-slate-700">
        {message}
      </p>
    </div>
  );
}

export function StatusBadge({ label }) {
  const translatedLabel = translateManagerStatusLabel(label);
  const tone = {
    'Bản nháp': 'bg-slate-200 text-slate-700',
    'Đã xuất bản': 'bg-emerald-100 text-emerald-700',
    'Lưu trữ': 'bg-rose-100 text-rose-700',
    'Chờ duyệt': 'bg-amber-100 text-amber-700',
    'Sẵn sàng': 'bg-emerald-100 text-emerald-700',
    'Đã xử lý': 'bg-emerald-100 text-emerald-700',
    'Đang mã hóa': 'bg-amber-100 text-amber-700',
    'Đang hoạt động': 'bg-emerald-100 text-emerald-700',
    'Tạm ngừng': 'bg-slate-200 text-slate-700',
  };

  return (
    <span
      className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${tone[translatedLabel] ?? 'bg-[#fff2f3] text-[#730014]'}`}
    >
      {translatedLabel}
    </span>
  );
}

export function SectionTitle({ title, action, onAction }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <h2 className="font-['Manrope'] text-lg font-extrabold text-slate-900">{title}</h2>
      {action ? (
        <button className="text-sm font-semibold text-[#730014]" onClick={onAction} type="button">
          {action}
        </button>
      ) : null}
    </div>
  );
}

export function FilterChip({ label }) {
  return (
    <div className="inline-flex items-center rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-600 shadow-sm">
      {label}
    </div>
  );
}

export function TextField({ label, value, onChange, textarea = false, rows = 4 }) {
  return (
    <label className="block">
      <span className="mb-2 block text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">
        {label}
      </span>
      {textarea ? (
        <textarea
          className="min-h-0 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#730014] focus:bg-white"
          onChange={onChange}
          rows={rows}
          value={value}
        />
      ) : (
        <input
          className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#730014] focus:bg-white"
          onChange={onChange}
          value={value}
        />
      )}
    </label>
  );
}
