import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Bell, ChevronDown, LogOut, Menu, ShoppingCart, UserRound } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLearnerExperience } from '../../context/LearnerExperienceContext';
import classroomApi from '../../api/classroomApi';
import { hasAccessToken, hasAnyUserRole } from '../../utils/auth';
import { commerceEventName, readCart } from '../../utils/commerceStore';

const studentNavItems = [
  { label: 'Khóa học', to: '/courses' },
  { label: 'IELTS', href: '/#courses' },
  { label: 'TOEIC', href: '/#courses' },
  { label: 'Lịch khai giảng', to: '/opening-schedule' },
  { label: 'Về EnglishLab', href: '/#testimonials' },
];

const hasRole = (user, roles) => hasAnyUserRole(user, roles);

const getNavItemsByRole = (user) => {
  if (!user) return studentNavItems;
  const role = String(user.role || '').toUpperCase();
  if (role === 'TEACHER') {
    return [
      { label: 'Giảng dạy', to: '/teacher' },
      { label: 'Lịch dạy', to: '/teacher/schedule' },
      { label: 'Yêu cầu thay đổi', to: '/teacher/requests' },
    ];
  }
  if (role === 'TRAINING_MANAGER') {
    return [
      { label: 'Bảng điều khiển', to: '/training-manager' },
      { label: 'Lớp học', to: '/training-manager/classrooms' },
      { label: 'Hàng đợi đăng ký', to: '/training-manager/registrations' },
      { label: 'Duyệt yêu cầu', to: '/training-manager/requests' },
      { label: 'Lịch khai giảng', to: '/opening-schedule' },
    ];
  }
  if (role === 'MANAGER' || role === 'ADMIN') {
    return [
      { label: 'Bảng điều khiển', to: '/training-manager' },
      { label: 'Lớp học', to: '/training-manager/classrooms' },
      { label: 'Hàng đợi đăng ký', to: '/training-manager/registrations' },
      { label: 'Duyệt yêu cầu', to: '/training-manager/requests' },
      { label: 'Giảng dạy', to: '/teacher' },
      { label: 'Lịch khai giảng', to: '/opening-schedule' },
    ];
  }
  if (role === 'CONTENT_MANAGER') {
    return [
      { label: 'Quản lý nội dung', to: '/content-manager' },
      { label: 'Lịch khai giảng', to: '/opening-schedule' },
    ];
  }
  return studentNavItems;
};

const getProfileItemsByRole = (user) => {
  if (!user) return [];
  const role = String(user.role || '').toUpperCase();
  if (role === 'TEACHER') {
    return [
      { label: 'Giảng dạy', to: '/teacher' },
      { label: 'Lịch dạy', to: '/teacher/schedule' },
      { label: 'Yêu cầu thay đổi', to: '/teacher/requests' },
    ];
  }
  if (role === 'TRAINING_MANAGER') {
    return [
      { label: 'Bảng điều khiển', to: '/training-manager' },
      { label: 'Hàng đợi đăng ký', to: '/training-manager/registrations' },
      { label: 'Duyệt yêu cầu thay đổi', to: '/training-manager/requests' },
    ];
  }
  if (role === 'MANAGER' || role === 'ADMIN') {
    return [
      { label: 'Bảng điều khiển', to: '/training-manager' },
      { label: 'Lớp học', to: '/training-manager/classrooms' },
      { label: 'Hàng đợi đăng ký', to: '/training-manager/registrations' },
      { label: 'Duyệt yêu cầu thay đổi', to: '/training-manager/requests' },
      { label: 'Giảng dạy', to: '/teacher' },
    ];
  }
  if (role === 'CONTENT_MANAGER') {
    return [
      { label: 'Quản lý nội dung', to: '/content-manager' },
    ];
  }
  // Student
  return [
    { label: 'Khóa học của tôi', to: '/my-courses' },
    { label: 'Luyện flashcard', to: '/flashcards/practice' },
    { label: 'Lớp của tôi', to: '/my-classrooms' },
    { label: 'Lịch học', to: '/my-schedule' },
    { label: 'Luyện tập', to: '/my-practice' },
    { label: 'Bài tập', to: '/my-homework' },
    { label: 'Hồ sơ', to: '/profile' },
    { label: 'Lịch sử giao dịch', to: '/transaction-history' },
  ];
};

const getRoleLabel = (user) => {
  if (!user) return '';
  const role = String(user.role || '').toUpperCase();
  if (role === 'TEACHER') return 'Giáo viên';
  if (role === 'TRAINING_MANAGER') return 'Quản lý đào tạo';
  if (role === 'MANAGER' || role === 'ADMIN') return 'Quản lý';
  if (role === 'CONTENT_MANAGER') return 'Quản lý nội dung';
  return user.targetExam || 'Học viên EnglishLab';
};

const STAFF_ROLES = ['TEACHER', 'TRAINING_MANAGER', 'CONTENT_MANAGER', 'MANAGER', 'ADMIN'];

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const { markAllNotificationsRead, unreadNotificationCount } = useLearnerExperience();
  const menuRef = useRef(null);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(() => readCart().length);
  const [apiUnreadCount, setApiUnreadCount] = useState(0);
  const shouldReloadWhenLeavingWorkspace = /\/courses\/[^/]+\/learn$/.test(location.pathname);
  const displayUnreadCount = Math.max(apiUnreadCount, unreadNotificationCount);

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
  }, [location.pathname]);

  useEffect(() => {
    if (!hasAccessToken()) {
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
  }, [location.pathname, user?.id]);

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

            <Link
              aria-label="Thông báo"
              className="relative flex h-12 w-12 items-center justify-center rounded-full border border-[#dfbfbd]/60 bg-white text-[#4b0009] shadow-sm transition hover:-translate-y-0.5 hover:border-[#730014]/40 hover:bg-[#fff7f7]"
              onClick={markAllNotificationsRead}
              to="/notifications"
              reloadDocument={shouldReloadWhenLeavingWorkspace}
            >
              <Bell className="h-5 w-5" />
              {displayUnreadCount > 0 ? (
                <span className="absolute right-2 top-2 h-2.5 w-2.5 rounded-full border-2 border-white bg-[#c5162e]" />
              ) : null}
            </Link>

            <div className="relative" ref={menuRef}>
              <button
                className="flex items-center gap-2 rounded-full border border-[#dfbfbd]/60 bg-white px-3 py-2 shadow-sm transition hover:-translate-y-0.5 hover:border-[#730014]/40 hover:bg-[#fff7f7]"
                onClick={() => setIsProfileMenuOpen((current) => !current)}
                type="button"
              >
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[#8a0018]/10 text-[#8a0018]">
                  <UserRound size={17} />
                </span>
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
                <div className="absolute right-0 top-[calc(100%+12px)] z-[70] w-[240px] overflow-hidden rounded-[28px] border border-[#dfbfbd]/70 bg-white shadow-[0_20px_45px_rgba(75,0,9,0.15)]">
                  <div className="border-b border-[#f1e4e5] px-5 py-4">
                    <p className="truncate text-sm font-extrabold text-[#2b2828]">{user.fullName || user.email}</p>
                    <p className="mt-1 truncate text-xs font-semibold text-[#6a5553]">{getRoleLabel(user)}</p>
                  </div>

                  <div className="p-2">
                    {profileMenuItems.map((item) => (
                      <Link
                        key={item.to}
                        className="flex w-full items-center rounded-2xl px-4 py-3 text-sm font-bold text-[#2b2828] transition hover:bg-[#fff3f4] hover:text-[#730014]"
                        to={item.to}
                        reloadDocument={shouldReloadWhenLeavingWorkspace}
                      >
                        {item.label}
                      </Link>
                    ))}
                    <button
                      className="flex w-full items-center gap-2 rounded-2xl px-4 py-3 text-left text-sm font-bold text-[#2b2828] transition hover:bg-[#fff3f4] hover:text-[#730014]"
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
              Đăng ký tư vấn
            </Link>
          </div>
        )}

        <button className="ml-3 inline-flex text-[#1a1c1c] xl:hidden" type="button" aria-label="Mở menu">
          <Menu size={30} />
        </button>
      </div>
    </header>
  );
};

export default Header;
