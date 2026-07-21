import { useEffect, useRef, useState } from 'react';
import { CalendarDays, CheckSquare, ChevronDown, ChevronRight, ClipboardList, LayoutDashboard, LifeBuoy, LogOut, Settings2, Video } from 'lucide-react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { clearSession, getStoredUser } from '../../utils/auth';
import BrandedSelect from '../ui/BrandedSelect';

const trainingOperationsNav = [
  {
    title: 'Vận hành đào tạo',
    items: [
      { label: 'Bảng điều khiển', href: '/staff', icon: LayoutDashboard, end: true, staffOnly: true },
      { label: 'Lớp học', href: '/staff/classrooms', icon: CalendarDays, staffOnly: true },
      { label: 'Yêu cầu đăng ký', href: '/staff/enrollment-requests', icon: ClipboardList, staffOnly: true },
      { label: 'Đề xuất mở lớp', href: '/staff/classroom-proposals', icon: CalendarDays, staffOnly: true },
      { label: 'Hàng đợi đăng ký', href: '/staff/registrations', icon: ClipboardList, staffOnly: true },
      { label: 'Duyệt yêu cầu', href: '/staff/requests', icon: CheckSquare, staffOnly: true },
      { label: 'Hạ tầng lớp học', href: '/staff/infrastructure', icon: Settings2, staffOnly: true },
      { label: 'Quản lý ghi hình', href: '/staff/recordings', icon: Video, staffOnly: true },
      { label: 'Duyệt đề xuất lớp', href: '/manager/classroom-proposals', icon: CalendarDays, managerOnly: true },
      { label: 'Duyệt khóa học', href: '/manager/course-approvals', icon: CheckSquare, managerOnly: true },
      { label: 'Duyệt nội dung', href: '/manager/content-approvals', icon: CheckSquare, managerOnly: true },
      { label: 'Ghi danh online', href: '/manager/online-enrollments', icon: ClipboardList, managerOnly: true },
      { label: 'Support ticket', href: '/staff/support-tickets', managerHref: '/manager/support-tickets', icon: LifeBuoy },
    ],
  },
];

function resolvePageMeta(pathname) {
  if (pathname === '/staff' || pathname === '/staff/') {
    return {
      title: 'Việc cần làm hôm nay',
      subtitle: 'Tổng hợp đăng ký chờ xử lý, yêu cầu thay đổi và lớp cần can thiệp trước khai giảng.',
    };
  }
  if (pathname.startsWith('/staff/classrooms/') && pathname !== '/staff/classrooms') {
    return {
      title: 'Quản lý lớp học',
      subtitle: 'Xử lý hàng đợi đăng ký, học viên, lịch học và công bố khai giảng theo từng cohort.',
    };
  }
  if (pathname.startsWith('/staff/registrations') || pathname.startsWith('/staff/classroom-registrations')) {
    return {
      title: 'Hàng đợi đăng ký và học phí',
      subtitle: 'Xác nhận ghi danh, ghi nhận học phí, xếp lớp — mặc định chỉ hiển thị hồ sơ cần xử lý.',
    };
  }
  if (pathname.startsWith('/staff/enrollment-requests')) {
    return {
      title: 'Yêu cầu đăng ký và phân lớp',
      subtitle: 'Kiểm tra placement eligibility, chốt trình độ và quản lý waiting pool trước khi đề xuất lớp.',
    };
  }
  if (pathname.startsWith('/staff/classroom-proposals')) {
    return {
      title: 'Đề xuất mở lớp',
      subtitle: 'Gom waiting pool thành bản nháp, kiểm tra lịch và gửi Manager duyệt trước khi tạo lớp chính thức.',
    };
  }
  if (pathname.startsWith('/staff/requests')) {
    return {
      title: 'Duyệt yêu cầu thay đổi',
      subtitle: 'Xem diff thay đổi, kiểm tra trùng lịch và phê duyệt đề xuất từ giảng viên.',
    };
  }
  if (pathname.startsWith('/staff/infrastructure')) {
    return {
      title: 'Hạ tầng lớp học',
      subtitle: 'Quản lý cơ sở, phòng học và mẫu lịch để mở lớp không bị trùng lịch hoặc thiếu dữ liệu vận hành.',
    };
  }
  if (pathname.startsWith('/manager/course-approvals')) {
    return {
      title: 'Duyệt khóa học và phiên bản',
      subtitle: 'Rà soát lần xuất bản đầu tiên cùng các phiên bản cập nhật v2, v3.',
    };
  }
  if (pathname.startsWith('/manager/content-approvals')) {
    return {
      title: 'Duyệt nội dung và phiên bản',
      subtitle: 'Kiểm duyệt v2, v3 cùng tài liệu và giáo trình do Content Manager gửi lên.',
    };
  }
  if (pathname.startsWith('/manager/classroom-proposals')) {
    return {
      title: 'Duyệt đề xuất mở lớp',
      subtitle: 'Kiểm tra học viên, lịch và nguồn lực trước khi tạo classroom chính thức trong một transaction.',
    };
  }
  if (pathname.startsWith('/staff/support-tickets') || pathname.startsWith('/manager/support-tickets')) {
    return {
      title: 'Support Ticket',
      subtitle: 'Theo dõi và xử lý yêu cầu hỗ trợ từ học viên.',
    };
  }
  return {
    title: 'Mở lớp và lịch khai giảng',
    subtitle: 'Tạo cohort mới và mở trang quản lý từng lớp để vận hành.',
  };
}

export default function TrainingManagerLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState(() => getStoredUser());
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const sidebarNavRef = useRef(null);
  const sidebarScrollTopRef = useRef(0);
  const meta = resolvePageMeta(location.pathname);
  const role = String(currentUser?.role || '').toUpperCase();
  const isManager = ['MANAGER', 'ADMIN'].includes(role);
  const isStaff = ['STAFF', 'TRAINING_MANAGER', 'ADMIN'].includes(role);
  const visibleTrainingManagerNav = trainingOperationsNav.map((section) => ({
    ...section,
    items: section.items
      .filter((item) => {
        if (item.managerOnly && !isManager) return false;
        if (item.staffOnly && !isStaff) return false;
        return true;
      })
      .map((item) => (isManager && item.managerHref ? { ...item, href: item.managerHref } : item)),
  }));
  const crumbs = location.pathname.replace(/^\/(staff|manager)\/?/, '').split('/').filter(Boolean);
  const mobileNavOptions = visibleTrainingManagerNav.flatMap((section) => section.items.map((item) => ({
    label: item.label,
    value: item.href,
  })));
  const mobileNavValue = mobileNavOptions.find((option) => option.value === location.pathname)?.value
    || mobileNavOptions.find((option) => location.pathname.startsWith(`${option.value}/`))?.value
    || (isManager ? '/manager/course-approvals' : '/staff');

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
    const frameId = window.requestAnimationFrame(() => window.requestAnimationFrame(restoreScrollPosition));
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

  const displayName = currentUser?.fullName || currentUser?.username || currentUser?.email || 'Nhân viên đào tạo';
  const avatarLetter = displayName.charAt(0).toUpperCase();
  const hidePageShell = /^\/staff\/classrooms\/\d+/.test(location.pathname);
  const homeHref = isManager ? '/manager/course-approvals' : '/staff';
  const roleLabel = isManager ? 'Quản lý' : 'Nhân viên đào tạo';

  return (
    <div className="min-h-screen bg-[#f8fafc] font-['Inter'] text-slate-800 antialiased">
      <div
        className="pointer-events-none fixed inset-0 opacity-[0.02]"
        style={{ backgroundImage: 'radial-gradient(#4b0009 1px, transparent 1px)', backgroundSize: '24px 24px' }}
      />

      <aside className="fixed inset-y-0 left-0 z-10 hidden w-[270px] flex-col overflow-hidden border-r border-slate-200 bg-[#4b0009] text-white shadow-xl lg:flex">
        <div className="shrink-0 border-b border-white/10 bg-[#4b0009] px-6 pb-5 pt-7">
          <p className="bg-gradient-to-r from-white to-pink-200 bg-clip-text font-['Manrope'] text-2xl font-black tracking-tight text-transparent">
            EnglishLab
          </p>
          <p className="mt-1.5 text-[10px] font-bold uppercase tracking-[0.2em] text-white/50">
            Vận hành đào tạo
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
            {visibleTrainingManagerNav.map((section) => (
              <div key={section.title}>
                <p className="mb-3 px-4 text-[10px] font-bold uppercase tracking-[0.25em] text-white/40">
                  {section.title}
                </p>
                <div className="space-y-1.5">
                  {section.items.map((item) => {
                    const Icon = item.icon || LayoutDashboard;
                    return (
                      <NavLink
                        className={({ isActive }) => `group flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all duration-200 ${
                          isActive
                            ? 'bg-white font-semibold text-[#4b0009] shadow-md shadow-black/10'
                            : 'text-white/70 hover:bg-white/10 hover:text-white'
                        }`}
                        end={item.end}
                        key={item.href}
                        onMouseDown={(event) => {
                          sidebarScrollTopRef.current = sidebarNavRef.current?.scrollTop ?? 0;
                          event.preventDefault();
                        }}
                        onPointerDown={() => {
                          sidebarScrollTopRef.current = sidebarNavRef.current?.scrollTop ?? 0;
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

      <div className="relative lg:ml-[270px] z-20">
        <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/80 backdrop-blur-md">
          <div className="mx-auto flex max-w-[1680px] items-center justify-between gap-4 px-4 py-3.5 sm:px-6 lg:px-8">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-1.5 text-xs font-medium text-slate-500">
                <Link className="transition hover:text-[#730014]" to={homeHref}>
                  Vận hành đào tạo
                </Link>
                {crumbs.map((crumb, index) => (
                  <span className="inline-flex items-center gap-1.5 capitalize" key={`${crumb}-${index}`}>
                    <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
                    <span className={index === crumbs.length - 1 ? 'font-semibold text-slate-800' : ''}>
                      {formatCrumbLabel(crumb)}
                    </span>
                  </span>
                ))}
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
                    <p className="mt-0.5 text-[10px] font-medium uppercase tracking-wider text-slate-400">{roleLabel}</p>
                  </div>
                  <ChevronDown className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${accountMenuOpen ? 'rotate-180' : ''}`} />
                </button>

                {accountMenuOpen ? (
                  <div className="absolute right-0 top-full z-50 mt-2 w-56 overflow-hidden rounded-xl border border-slate-200 bg-white p-1 shadow-xl">
                    <button
                      className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-left text-sm font-medium text-rose-600 transition hover:bg-rose-50"
                      onClick={handleLogout}
                      onMouseDown={(event) => event.preventDefault()}
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
          {!hidePageShell ? (
            <div className="mb-6">
              <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl">
                {meta.title}
              </h1>
              {meta.subtitle ? (
                <p className="mt-2 max-w-4xl text-sm leading-relaxed text-slate-500">
                  {meta.subtitle}
                </p>
              ) : null}
            </div>
          ) : null}

          <div className={hidePageShell ? '' : 'min-h-[500px]'}>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}

function formatCrumbLabel(value) {
  const dictionary = {
    classrooms: 'Lớp học',
    registrations: 'Hàng đợi',
    'classroom-registrations': 'Hàng đợi',
    requests: 'Yêu cầu',
    infrastructure: 'Hạ tầng',
    'classroom-proposals': 'Đề xuất lớp',
    'course-approvals': 'Duyệt khóa học',
    'content-approvals': 'Duyệt nội dung',
  };
  if (/^\d+$/.test(value)) {
    return `Lớp #${value}`;
  }
  return dictionary[value] || value.replace(/-/g, ' ');
}
