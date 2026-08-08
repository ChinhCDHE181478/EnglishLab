import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Bell,
  BookCheck,
  BookOpenText,
  CalendarDays,
  ChevronDown,
  ClipboardList,
  CreditCard,
  GraduationCap,
  LifeBuoy,
  Layers3,
  LogOut,
  Menu,
  NotebookPen,
  School,
  ShoppingCart,
  UserRound,
  X,
} from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLearnerExperience } from '../../context/LearnerExperienceContext';
import classroomApi from '../../api/classroomApi';
import { canUseLearnerStudyTools, hasAccessToken, hasAnyUserRole } from '../../utils/auth';
import { commerceEventName, readCart } from '../../utils/commerceStore';

const studentNavItems = [
  { label: 'Khóa học', to: '/courses' },
  { label: 'Lộ trình học', to: '/learning-paths' },
  { label: 'Thi thử', to: '/mock-tests' },
  { label: 'Đăng ký học', to: '/opening-schedule' },
  { label: 'Về EnglishLab', to: '/about' },
];

const hasRole = (user, roles) => hasAnyUserRole(user, roles);

const getNavItemsByRole = (user) => {
  if (!user) return studentNavItems;
  const role = String(user.role || '').toUpperCase();
  if (role === 'TEACHER') {
    return [
      { label: 'Giảng dạy', to: '/teacher' },
      { label: 'Lịch dạy', to: '/teacher/schedule' },
      { label: 'Theo dõi yêu cầu', to: '/teacher/requests' },
      { label: 'Hồ sơ chuyên môn', to: '/teacher/professional-profile' },
    ];
  }
  if (role === 'STAFF') {
    return [
      { label: 'Bảng điều khiển', to: '/staff' },
      { label: 'Lớp học', to: '/staff/classrooms' },
      { label: 'Yêu cầu đăng ký', to: '/staff/enrollment-requests' },
      { label: 'Duyệt yêu cầu', to: '/staff/requests' },
      { label: 'Đăng ký học', to: '/opening-schedule' },
    ];
  }
  if (role === 'MANAGER' || role === 'ADMIN') {
    return [
      { label: 'Duyệt đề xuất lớp', to: '/manager/classroom-proposals' },
      { label: 'Ghi danh online', to: '/manager/online-enrollments' },
      { label: 'Đăng ký học', to: '/opening-schedule' },
    ];
  }
  if (role === 'CONTENT_MANAGER') {
    return [
      { label: 'Quản lý nội dung', to: '/content-manager' },
      { label: 'Đăng ký học', to: '/opening-schedule' },
    ];
  }
  return studentNavItems;
};

const getProfileItemsByRole = (user) => {
  if (!user) return [];
  const role = String(user.role || '').toUpperCase();
  // Staff roles already have primary navigation in the header; keep the avatar menu for account actions only.
  if (['TEACHER', 'STAFF', 'MANAGER', 'ADMIN', 'CONTENT_MANAGER'].includes(role)) {
    return [];
  }
  // Student
  return [
    { label: 'Khóa học', to: '/my-courses', icon: GraduationCap, group: 'learning' },
    { label: 'Lớp học', to: '/my-classrooms', icon: School, group: 'learning' },
    { label: 'Đăng ký', to: '/my-enrollment-requests', icon: ClipboardList, group: 'learning' },
    { label: 'Lịch học', to: '/my-schedule', icon: CalendarDays, group: 'learning' },
    { label: 'Flashcard', to: '/flashcards/practice', icon: Layers3, group: 'learning' },
    { label: 'Từ điển', to: '/dictionary', icon: BookOpenText, group: 'learning' },
    { label: 'Luyện tập', to: '/my-practice', icon: NotebookPen, group: 'learning' },
    { label: 'Bài tập', to: '/my-homework', icon: BookCheck, group: 'learning' },
    { label: 'Hồ sơ', to: '/profile', icon: UserRound, group: 'account' },
    { label: 'Giao dịch', to: '/transaction-history', icon: CreditCard, group: 'account' },
    { label: 'Hỗ trợ', to: '/support', icon: LifeBuoy, group: 'account' },
  ];
};

const getRoleLabel = (user) => {
  if (!user) return '';
  const role = String(user.role || '').toUpperCase();
  if (role === 'TEACHER') return 'Giáo viên';
  if (role === 'STAFF') return 'Nhân viên đào tạo';
  if (role === 'MANAGER' || role === 'ADMIN') return 'Quản lý';
  if (role === 'CONTENT_MANAGER') return 'Quản lý nội dung';
  return user.targetExam || 'Học viên EnglishLab';
};

const STAFF_ROLES = ['TEACHER', 'STAFF', 'CONTENT_MANAGER', 'MANAGER', 'ADMIN'];

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const { markAllNotificationsRead, unreadNotificationCount } = useLearnerExperience();
  const menuRef = useRef(null);
  const notificationRef = useRef(null);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [isNotificationMenuOpen, setIsNotificationMenuOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(() => readCart().length);
  const [apiUnreadCount, setApiUnreadCount] = useState(0);
  const [popoverNotifications, setPopoverNotifications] = useState([]);
  const [popoverLoading, setPopoverLoading] = useState(false);

  const shouldReloadWhenLeavingWorkspace = /\/courses\/[^/]+\/learn$/.test(location.pathname);
  const canUseStudentNotifications = canUseLearnerStudyTools(user);
  const displayUnreadCount = Math.max(apiUnreadCount, unreadNotificationCount);

  const loadPopoverNotifications = useCallback(async () => {
    setPopoverLoading(true);
    try {
      const items = await classroomApi.getStudentNotifications();
      setPopoverNotifications(items.map((n) => ({
        id: n.id,
        title: n.title,
        message: n.body,
        read: n.read,
        createdAt: n.createdAt,
        actionPath: n.actionPath,
      })));
    } catch {
      setPopoverNotifications([]);
    } finally {
      setPopoverLoading(false);
    }
  }, []);

  useEffect(() => {
    const syncCart = () => setCartCount(readCart().length);
    window.addEventListener('storage', syncCart);
    window.addEventListener(commerceEventName, syncCart);
    window.addEventListener('focus', syncCart);

    return () => {
      window.removeEventListener('storage', syncCart);
      window.removeEventListener(commerceEventName, syncCart);
      window.removeEventListener('focus', syncCart);
    };
  }, []);

  useEffect(() => {
    setIsProfileMenuOpen(false);
    setIsNotificationMenuOpen(false);
    setIsMobileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!isNotificationMenuOpen) return undefined;

    const handlePointerDown = (event) => {
      if (!notificationRef.current?.contains(event.target)) {
        setIsNotificationMenuOpen(false);
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setIsNotificationMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isNotificationMenuOpen]);

  useEffect(() => {
    if (!hasAccessToken() || !canUseStudentNotifications) {
      setApiUnreadCount(0);
      return undefined;
    }

    let active = true;
    const syncUnread = async () => {
      try {
        const count = await classroomApi.getUnreadNotificationCount();
        if (active) {
          setApiUnreadCount(Number(count?.count ?? count ?? 0));
        }
      } catch {
        if (active) setApiUnreadCount(0);
      }
    };

    syncUnread();
    const intervalId = window.setInterval(syncUnread, 60000);

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, [canUseStudentNotifications, location.pathname, user?.id]);

  useEffect(() => {
    if (!isProfileMenuOpen) return undefined;

    const handlePointerDown = (event) => {
      if (!menuRef.current?.contains(event.target)) {
        setIsProfileMenuOpen(false);
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setIsProfileMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isProfileMenuOpen]);

  const handleLogout = () => {
    logout();
    setIsProfileMenuOpen(false);
    if (shouldReloadWhenLeavingWorkspace) {
      window.location.assign('/');
      return;
    }
    navigate('/');
  };

  const navItems = useMemo(() => getNavItemsByRole(user), [user]);
  const profileMenuItems = useMemo(() => getProfileItemsByRole(user), [user]);
  const learningMenuItems = profileMenuItems.filter((item) => item.group === 'learning');
  const accountMenuItems = profileMenuItems.filter((item) => item.group === 'account');
  const isStaff = user && hasAnyUserRole(user, STAFF_ROLES);

  return (
    <header className="sticky top-0 z-50 w-full border-b border-[#dfbfbd]/30 bg-[#f9f9f9]/95 shadow-sm backdrop-blur-md">
      <div className="mx-auto flex h-20 w-full max-w-[1280px] items-center px-6 md:px-10">
        <Link className="flex shrink-0 items-center gap-2" to="/" reloadDocument={shouldReloadWhenLeavingWorkspace} aria-label="Trang chủ EnglishLab">
          <span className="flex h-8 w-7 items-center gap-1">
            <span className="h-7 w-3 rounded-[1px] bg-[#8a0018]" />
            <span className="h-5 w-2.5 rounded-[1px] bg-[#c45a64]" />
          </span>
          <span className="font-['Manrope'] text-xl font-extrabold tracking-tight text-[#2b2828]">
            English<span className="text-[#8a0018]">Lab</span>
          </span>
        </Link>

        <nav className="hidden flex-1 items-center justify-center gap-6 xl:flex xl:gap-9" aria-label="Điều hướng chính">
          {navItems.map((item) => {
            // If any nav item exactly matches the current path, use exact-only matching
            // to prevent parent paths (e.g. /teacher) from staying lit on child routes (e.g. /teacher/schedule)
            const anyExactMatch = navItems.some((n) => n.to && location.pathname === n.to);
            const isActive = item.to && (
              item.to === '/'
                ? location.pathname === '/'
                : anyExactMatch
                ? location.pathname === item.to
                : location.pathname.startsWith(item.to)
            );
            const baseClass = 'relative whitespace-nowrap text-sm font-bold transition-colors';
            const activeClass = 'text-[#8a0018] after:absolute after:-bottom-1 after:left-0 after:h-0.5 after:w-full after:rounded-full after:bg-[#8a0018]';
            const inactiveClass = 'text-[#6a5553] hover:text-[#8a0018]';
            return item.to ? (
              <Link
                key={item.label}
                className={`${baseClass} ${isActive ? activeClass : inactiveClass}`}
                to={item.to}
                reloadDocument={shouldReloadWhenLeavingWorkspace}
              >
                {item.label}
              </Link>
            ) : (
              <a
                key={item.label}
                className={`${baseClass} ${inactiveClass}`}
                href={item.href}
              >
                {item.label}
              </a>
            );
          })}
        </nav>

        {user ? (
          <div className="ml-auto flex shrink-0 items-center gap-3">
            {!isStaff && (
              <Link
                aria-label="Giỏ hàng"
                className="relative flex h-12 w-12 items-center justify-center rounded-full border border-[#dfbfbd]/60 bg-white text-[#4b0009] shadow-sm transition hover:-translate-y-0.5 hover:border-[#730014]/40 hover:bg-[#fff7f7]"
                to="/cart"
                reloadDocument={shouldReloadWhenLeavingWorkspace}
              >
                <ShoppingCart className="h-5 w-5" />
                {cartCount > 0 ? (
                  <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-[#c5162e] px-1 text-[11px] font-extrabold leading-none text-white">
                    {cartCount > 9 ? '9+' : cartCount}
                  </span>
                ) : null}
              </Link>
            )}

            {canUseStudentNotifications ? (
            <div className="relative" ref={notificationRef}>
              <button
                aria-label="Thông báo"
                className="relative flex h-12 w-12 items-center justify-center rounded-full border border-[#dfbfbd]/60 bg-white text-[#4b0009] shadow-sm transition hover:-translate-y-0.5 hover:border-[#730014]/40 hover:bg-[#fff7f7]"
                onClick={() => {
                  setIsNotificationMenuOpen((current) => {
                    const next = !current;
                    if (next && hasAccessToken() && canUseStudentNotifications) {
                      loadPopoverNotifications();
                    }
                    return next;
                  });
                  setIsProfileMenuOpen(false);
                }}
                type="button"
              >
                <Bell className="h-5 w-5" />
                {displayUnreadCount > 0 ? (
                  <span className="absolute right-2 top-2 h-2.5 w-2.5 rounded-full border-2 border-white bg-[#c5162e]" />
                ) : null}
              </button>

              {isNotificationMenuOpen ? (
                <div className="absolute -right-32 sm:-right-40 top-[calc(100%+12px)] z-[70] w-[min(360px,calc(100vw-32px))] rounded-[28px] border border-[#dfbfbd]/70 bg-white p-2 shadow-[0_20px_45px_rgba(75,0,9,0.18)]">
                  <div className="flex items-center justify-between border-b border-[#f1e4e5] px-4 py-3">
                    <div className="flex items-center gap-2">
                      <Bell className="h-4 w-4 text-[#8a0018]" />
                      <h3 className="font-['Manrope'] text-sm font-extrabold text-[#2b2828]">Thông báo</h3>
                      {displayUnreadCount > 0 ? (
                        <span className="rounded-full bg-[#fff1f2] px-2 py-0.5 text-[10px] font-black text-[#8a0018]">
                          {displayUnreadCount} mới
                        </span>
                      ) : null}
                    </div>
                    {displayUnreadCount > 0 ? (
                      <button
                        className="text-[11px] font-bold text-[#8a0018] hover:underline"
                        onClick={async () => {
                          markAllNotificationsRead();
                          setApiUnreadCount(0);
                          if (canUseStudentNotifications) {
                            try { await classroomApi.markAllNotificationsRead(); } catch {}
                          }
                        }}
                        type="button"
                      >
                        Đánh dấu đã đọc
                      </button>
                    ) : null}
                  </div>

                  <div className="max-h-[320px] overflow-y-auto overscroll-contain p-2 space-y-1">
                    {popoverLoading ? (
                      <div className="py-6 text-center text-xs font-semibold text-slate-400">Đang tải thông báo...</div>
                    ) : popoverNotifications.length === 0 ? (
                      <div className="py-6 text-center text-xs font-semibold text-slate-500">Chưa có thông báo mới</div>
                    ) : (
                      popoverNotifications.slice(0, 5).map((item) => (
                        <div
                          key={item.id}
                          className={`group flex items-start gap-3 rounded-2xl p-3 text-left transition cursor-pointer ${
                            !item.read ? 'bg-[#fff7f8] hover:bg-[#fff0f1]' : 'hover:bg-slate-50'
                          }`}
                          onClick={() => {
                            setIsNotificationMenuOpen(false);
                            if (item.actionPath) navigate(item.actionPath);
                            else navigate('/notifications');
                          }}
                        >
                          <span className={`mt-1.5 flex h-2 w-2 shrink-0 rounded-full ${!item.read ? 'bg-[#c5162e]' : 'bg-slate-200'}`} />
                          <div className="min-w-0 flex-1">
                            <p className="text-xs font-bold text-[#2b2828] group-hover:text-[#8a0018]">{item.title}</p>
                            <p className="mt-0.5 line-clamp-2 text-[11px] leading-relaxed text-[#6a5553]">{item.message}</p>
                            <time className="mt-1 block text-[10px] text-slate-400">
                              {item.createdAt ? new Date(item.createdAt).toLocaleString('vi-VN') : ''}
                            </time>
                          </div>
                        </div>
                      ))
                    )}
                  </div>

                  <div className="border-t border-[#f1e4e5] p-2">
                    <Link
                      className="block w-full rounded-2xl bg-[#fff1f2] py-2.5 text-center text-xs font-extrabold text-[#8a0018] transition hover:bg-[#fbe3e6]"
                      onClick={() => setIsNotificationMenuOpen(false)}
                      to="/notifications"
                      reloadDocument={shouldReloadWhenLeavingWorkspace}
                    >
                      Xem tất cả thông báo →
                    </Link>
                  </div>
                </div>
              ) : null}
            </div>
            ) : null}

            <div className="relative" ref={menuRef}>
              <button
                className="flex items-center gap-2 rounded-full border border-[#dfbfbd]/60 bg-white px-3 py-2 shadow-sm transition hover:-translate-y-0.5 hover:border-[#730014]/40 hover:bg-[#fff7f7]"
                onClick={() => setIsProfileMenuOpen((current) => !current)}
                type="button"
              >
                {user.avatarUrl ? (
                  <img
                    alt={user.fullName || 'Ảnh hồ sơ'}
                    className="h-8 w-8 rounded-full object-cover ring-1 ring-[#dfbfbd]/70"
                    src={user.avatarUrl}
                  />
                ) : (
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[#8a0018]/10 text-[#8a0018]">
                    <UserRound size={17} />
                  </span>
                )}
                <div className="hidden text-left leading-tight md:block">
                  <p className="max-w-[150px] truncate text-sm font-extrabold text-[#2b2828]">
                    {user.fullName || user.email}
                  </p>
                  <p className="max-w-[150px] truncate text-xs font-semibold text-[#6a5553]">
                    {getRoleLabel(user)}
                  </p>
                </div>
                <ChevronDown
                  className={`hidden h-4 w-4 text-[#6a5553] transition-transform md:block ${isProfileMenuOpen ? 'rotate-180' : ''}`}
                />
              </button>

              {isProfileMenuOpen ? (
                <div className="absolute right-0 top-[calc(100%+12px)] z-[70] max-h-[calc(100dvh-104px)] w-[min(360px,calc(100vw-24px))] overflow-y-auto overscroll-contain rounded-[28px] border border-[#dfbfbd]/70 bg-white shadow-[0_20px_45px_rgba(75,0,9,0.15)]">
                  <div className="border-b border-[#f1e4e5] px-5 py-4">
                    <p className="truncate text-sm font-extrabold text-[#2b2828]">{user.fullName || user.email}</p>
                    <p className="mt-1 truncate text-xs font-semibold text-[#6a5553]">{getRoleLabel(user)}</p>
                  </div>

                  <div className="p-3">
                    {learningMenuItems.length ? (
                      <>
                        <p className="px-2 pb-2 text-[10px] font-extrabold uppercase tracking-[0.16em] text-[#9b8582]">Học tập</p>
                        <div className="grid grid-cols-2 gap-1.5">
                          {learningMenuItems.map((item) => {
                            const Icon = item.icon;
                            return (
                              <Link
                                key={item.to}
                                className="flex min-w-0 items-center gap-2.5 rounded-2xl px-3 py-2.5 text-sm font-bold text-[#2b2828] transition hover:bg-[#fff3f4] hover:text-[#730014]"
                                to={item.to}
                                reloadDocument={shouldReloadWhenLeavingWorkspace}
                              >
                                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-[#fff1f3] text-[#8a0018]">
                                  <Icon className="h-4 w-4" />
                                </span>
                                <span className="truncate">{item.label}</span>
                              </Link>
                            );
                          })}
                        </div>
                      </>
                    ) : null}

                    {accountMenuItems.length ? (
                      <div className="mt-3 border-t border-[#f1e4e5] pt-3">
                        <p className="px-2 pb-2 text-[10px] font-extrabold uppercase tracking-[0.16em] text-[#9b8582]">Tài khoản</p>
                        <div className="grid grid-cols-3 gap-1">
                          {accountMenuItems.map((item) => {
                            const Icon = item.icon;
                            return (
                              <Link
                                key={item.to}
                                className="flex min-w-0 flex-col items-center gap-1.5 rounded-2xl px-2 py-2.5 text-center text-xs font-bold text-[#2b2828] transition hover:bg-[#fff3f4] hover:text-[#730014]"
                                to={item.to}
                                reloadDocument={shouldReloadWhenLeavingWorkspace}
                              >
                                <Icon className="h-4 w-4" />
                                <span className="truncate">{item.label}</span>
                              </Link>
                            );
                          })}
                        </div>
                      </div>
                    ) : null}

                    {!learningMenuItems.length && !accountMenuItems.length ? profileMenuItems.map((item) => (
                      <Link
                        key={item.to}
                        className="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-bold text-[#2b2828] transition hover:bg-[#fff3f4] hover:text-[#730014]"
                        to={item.to}
                        reloadDocument={shouldReloadWhenLeavingWorkspace}
                      >
                        {item.label}
                      </Link>
                    )) : null}
                    <button
                      className="mt-2 flex w-full items-center justify-center gap-2 border-t border-[#f1e4e5] px-4 pt-3 text-sm font-bold text-[#6a5553] transition hover:text-[#730014]"
                      onClick={handleLogout}
                      type="button"
                    >
                      <LogOut className="h-4 w-4" />
                      Đăng xuất
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
          </div>
        ) : (
          <div className="ml-auto flex shrink-0 items-center gap-4">
            <Link className="hidden text-sm font-extrabold text-[#8a0018] transition-colors hover:text-[#4b0009] md:inline" to="/login" reloadDocument={shouldReloadWhenLeavingWorkspace}>
              Đăng nhập
            </Link>
            <Link
              className="hidden rounded-[2px] bg-[#8a0018] px-7 py-3 text-sm font-extrabold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-[#650012] md:inline"
              to="/register"
              reloadDocument={shouldReloadWhenLeavingWorkspace}
            >
              Tạo tài khoản
            </Link>
          </div>
        )}

        <button
          aria-expanded={isMobileMenuOpen}
          aria-label={isMobileMenuOpen ? 'Đóng menu' : 'Mở menu'}
          className="ml-3 inline-flex text-[#1a1c1c] xl:hidden"
          onClick={() => setIsMobileMenuOpen((current) => !current)}
          type="button"
        >
          {isMobileMenuOpen ? <X size={28} /> : <Menu size={30} />}
        </button>
      </div>
      {isMobileMenuOpen ? (
        <nav className="max-h-[calc(100dvh-80px)] overflow-y-auto overscroll-contain border-t border-[#dfbfbd]/30 bg-white px-4 py-4 sm:px-5 xl:hidden" aria-label="Điều hướng trên thiết bị di động">
          <div className="mx-auto grid max-w-[1280px] gap-1 sm:grid-cols-2">
            {navItems.map((item) => (
              item.to ? (
                <Link
                  className="rounded-xl px-4 py-3 text-sm font-extrabold text-[#584140] hover:bg-[#fff3f4] hover:text-[#730014]"
                  key={item.label}
                  reloadDocument={shouldReloadWhenLeavingWorkspace}
                  to={item.to}
                >
                  {item.label}
                </Link>
              ) : (
                <a className="rounded-xl px-4 py-3 text-sm font-extrabold text-[#584140] hover:bg-[#fff3f4] hover:text-[#730014]" href={item.href} key={item.label}>
                  {item.label}
                </a>
              )
            ))}
            {!user ? (
              <>
                <Link className="rounded-xl px-4 py-3 text-sm font-extrabold text-[#730014] hover:bg-[#fff3f4]" to="/login">
                  Đăng nhập
                </Link>
                <Link className="rounded-xl bg-[#8a0018] px-4 py-3 text-center text-sm font-extrabold text-white hover:bg-[#650012]" to="/register">
                  Tạo tài khoản
                </Link>
              </>
            ) : null}
          </div>
        </nav>
      ) : null}
    </header>
  );
};

export default Header;
