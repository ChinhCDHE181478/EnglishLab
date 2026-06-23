import { useEffect, useRef, useState } from 'react';
import { ChevronDown, ChevronRight, LogOut } from 'lucide-react';
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
  const mobileNavOptions = contentManagerNav.flatMap((section) =>
    section.items.map((item) => ({ label: item.label, value: item.href })),
  );
  const mobileNavValue = resolveMobileNavValue(location.pathname, mobileNavOptions);

  return (
    <div className="min-h-screen bg-[#f9f9f9] font-['Inter'] text-[#1a1c1c]">
      <div
        className="pointer-events-none fixed inset-0 opacity-[0.045]"
        style={{
          backgroundImage: 'radial-gradient(#4b0009 0.6px, transparent 0.6px)',
          backgroundSize: '34px 34px',
        }}
      />

      <aside className="fixed inset-y-0 left-0 z-30 hidden w-[280px] flex-col overflow-hidden border-r border-[#dfbfbd]/55 bg-[#4b0009] text-white lg:flex">
        <div className="shrink-0 bg-[#4b0009] px-5 pb-5 pt-6">
          <p className="font-['Manrope'] text-2xl font-extrabold">EnglishLab</p>
          <p className="mt-1 text-xs uppercase tracking-[0.24em] text-white/60">Quản lý nội dung</p>
        </div>

        <div
          className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 pb-6 [scrollbar-color:rgba(255,255,255,0.28)_transparent] [scrollbar-width:thin]"
          onScroll={(event) => {
            sidebarScrollTopRef.current = event.currentTarget.scrollTop;
          }}
          ref={sidebarNavRef}
        >
          <nav className="space-y-6">
            {contentManagerNav.map((section) => (
              <div key={section.title}>
                <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-[0.22em] text-white/35">
                  {section.title}
                </p>
                <div className="space-y-1">
                  {section.items.map((item) => {
                    const Icon = item.icon;
                    return (
                      <NavLink
                        key={item.href}
                        className={({ isActive }) =>
                          `flex items-center gap-3 rounded-2xl px-3 py-3 text-sm transition ${
                            isActive
                              ? 'bg-white text-[#4b0009] shadow-[0_14px_32px_rgba(0,0,0,0.22)]'
                              : 'text-white/70 hover:bg-white/8 hover:text-white'
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
                        <Icon className="h-4 w-4" />
                        <span className="font-medium">{item.label}</span>
                      </NavLink>
                    );
                  })}
                </div>
              </div>
            ))}
          </nav>
        </div>
      </aside>

      <div className="lg:ml-[280px]">
        <header className="sticky top-0 z-20 border-b border-[#dfbfbd]/50 bg-[#f9f9f9]/95 backdrop-blur">
          <div className="mx-auto flex max-w-[1680px] items-center gap-4 px-4 py-4 sm:px-6 lg:px-8">
            <div className="min-w-0 flex-1">
              <div className="mb-2 flex flex-wrap items-center gap-2 text-sm text-[#584140]">
                <Link className="font-medium text-[#730014] hover:underline" to="/content-manager/dashboard">
                  Quản lý nội dung
                </Link>
                {crumbs.map((crumb, index) => {
                  const isLast = index === crumbs.length - 1;
                  const href = resolveCrumbHref(crumbs, index);
                  const label = formatCrumbLabel(crumbs, index);
                  return (
                    <span key={`${crumb}-${index}`} className="inline-flex items-center gap-2 capitalize">
                      <ChevronRight className="h-4 w-4 text-[#aa8e8d]" />
                      {isLast || !href ? (
                        <span className="font-semibold text-[#1a1c1c]">{label}</span>
                      ) : (
                        <Link className="font-medium text-[#730014] hover:underline" to={href}>
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
                  className="flex items-center gap-3 rounded-2xl border border-[#dfbfbd]/60 bg-white px-4 py-3 text-left shadow-sm transition hover:border-[#730014]/30 hover:bg-[#fffafb]"
                  onBlur={() => window.setTimeout(() => setAccountMenuOpen(false), 120)}
                  onClick={() => setAccountMenuOpen((current) => !current)}
                  type="button"
                >
                  <div className="text-right">
                    <p className="text-sm font-semibold text-[#1a1c1c]">{displayName}</p>
                    <p className="text-xs uppercase tracking-[0.14em] text-[#584140]">{displayRole}</p>
                  </div>
                  <ChevronDown
                    className={`h-4 w-4 text-[#730014] transition ${accountMenuOpen ? 'rotate-180' : ''}`}
                  />
                </button>

                {accountMenuOpen ? (
                  <div className="absolute right-0 top-full z-50 mt-2 min-w-[220px] overflow-hidden rounded-2xl border border-[#dfbfbd]/75 bg-white p-1.5 shadow-[0_18px_45px_rgba(75,0,9,0.16)]">
                    <button
                      className="flex w-full items-center gap-2 rounded-xl px-4 py-3 text-left text-sm font-semibold text-[#730014] transition hover:bg-[#fff2f3]"
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
          <div className="mx-auto flex max-w-[1680px] items-center gap-3 border-t border-[#dfbfbd]/35 px-4 py-3 sm:px-6 lg:hidden">
            <div className="min-w-0 flex-1">
              <BrandedSelect
                onChange={(event) => navigate(event.target.value)}
                options={mobileNavOptions}
                value={mobileNavValue}
              />
            </div>
            <button
              aria-label="Đăng xuất"
              className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border border-[#dfbfbd]/65 bg-white text-[#730014]"
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
              <section className="mb-8">
                <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-[-0.03em] text-[#4b0009] sm:text-4xl">
                  {meta.title}
                </h1>
                <p className="mt-2 max-w-3xl text-sm leading-6 text-[#584140] sm:text-base">
                  {meta.subtitle}
                </p>
              </section>
              {children}
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
    listening: 'Luyện nghe',
    reading: 'Luyện đọc',
    writing: 'Luyện viết',
    speaking: 'Luyện nói',
    'mock-exams': 'Ngân hàng đề thi thử',
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
      className={`rounded-[28px] border border-[#dfbfbd]/55 bg-white shadow-[0_16px_40px_rgba(75,0,9,0.05)] ${className}`}
      {...props}
    >
      {children}
    </section>
  );
}

export function ContentManagerLoadingState({ message = 'Đang tải dữ liệu...' }) {
  return (
    <div className="flex min-h-[400px] flex-1 flex-col items-center justify-center rounded-[28px] border border-[#dfbfbd]/55 bg-white px-6 py-16 text-center shadow-[0_16px_40px_rgba(75,0,9,0.05)]">
      <div className="relative flex h-16 w-16 items-center justify-center">
        <div className="absolute h-12 w-12 animate-spin rounded-full border-4 border-[#dfbfbd]/30 border-t-[#730014]" />
        <div className="h-6 w-6 rounded-full bg-[#4b0009]/10" />
      </div>
      <p className="mt-6 animate-pulse font-['Manrope'] text-base font-extrabold text-[#730014]">
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
      <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">{title}</h2>
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
    <div className="inline-flex items-center rounded-2xl border border-[#dfbfbd]/65 bg-white px-4 py-3 text-sm text-[#584140]">
      {label}
    </div>
  );
}

export function TextField({ label, value, onChange, textarea = false, rows = 4 }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-[#8b706e]">
        {label}
      </span>
      {textarea ? (
        <textarea
          className="min-h-0 w-full rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-sm text-[#1a1c1c] outline-none focus:border-[#730014]"
          onChange={onChange}
          rows={rows}
          value={value}
        />
      ) : (
        <input
          className="w-full rounded-2xl border border-[#dfbfbd]/65 bg-[#fcfbfb] px-4 py-3 text-sm text-[#1a1c1c] outline-none focus:border-[#730014]"
          onChange={onChange}
          value={value}
        />
      )}
    </label>
  );
}
