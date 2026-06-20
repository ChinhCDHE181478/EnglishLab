import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Bell, ChevronDown, LogOut, Menu, ShoppingCart, UserRound } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useLearnerExperience } from '../../context/LearnerExperienceContext';
import { clearSession, getStoredUser } from '../../utils/auth';
import { commerceEventName, readCart } from '../../utils/commerceStore';

const navItems = [
  { label: 'Khóa học', to: '/courses' },
  { label: 'IELTS', href: '/#courses' },
  { label: 'TOEIC', href: '/#courses' },
  { label: 'Lịch khai giảng', href: '/#cta' },
  { label: 'Về EnglishLab', href: '/#testimonials' },
];

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { markAllNotificationsRead, unreadNotificationCount } = useLearnerExperience();
  const menuRef = useRef(null);
  const [user, setUser] = useState(() => getStoredUser());
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(() => readCart().length);
  const shouldReloadWhenLeavingWorkspace = /\/courses\/[^/]+\/learn$/.test(location.pathname);

  useEffect(() => {
    const syncUser = () => setUser(getStoredUser());
    const syncCart = () => setCartCount(readCart().length);
    window.addEventListener('storage', syncUser);
    window.addEventListener('focus', syncUser);
    window.addEventListener('englishlab:user-updated', syncUser);
    window.addEventListener('storage', syncCart);
    window.addEventListener(commerceEventName, syncCart);
    window.addEventListener('focus', syncCart);

    return () => {
      window.removeEventListener('storage', syncUser);
      window.removeEventListener('focus', syncUser);
      window.removeEventListener('englishlab:user-updated', syncUser);
      window.removeEventListener('storage', syncCart);
      window.removeEventListener(commerceEventName, syncCart);
      window.removeEventListener('focus', syncCart);
    };
  }, []);

  useEffect(() => {
    setIsProfileMenuOpen(false);
  }, [location.pathname]);

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
    clearSession();
    window.dispatchEvent(new Event('englishlab:user-updated'));
    setUser(null);
    setIsProfileMenuOpen(false);
    if (shouldReloadWhenLeavingWorkspace) {
      window.location.assign('/');
      return;
    }
    navigate('/');
  };

  const profileMenuItems = useMemo(
    () => [
      { label: 'Khóa học của tôi', to: '/my-courses' },
      { label: 'Hồ sơ', to: '/profile' },
      { label: 'Lịch sử giao dịch', to: '/transaction-history' },
    ],
    [],
  );

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
          {navItems.map((item) => (
            item.to ? (
              <Link
                key={item.label}
                className="whitespace-nowrap text-sm font-bold text-[#6a5553] transition-colors hover:text-[#8a0018]"
                to={item.to}
                reloadDocument={shouldReloadWhenLeavingWorkspace}
              >
                {item.label}
              </Link>
            ) : (
              <a
                key={item.label}
                className="whitespace-nowrap text-sm font-bold text-[#6a5553] transition-colors hover:text-[#8a0018]"
                href={item.href}
              >
                {item.label}
              </a>
            )
          ))}
        </nav>

        {user ? (
          <div className="ml-auto flex shrink-0 items-center gap-3">
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

            <Link
              aria-label="Thông báo"
              className="relative flex h-12 w-12 items-center justify-center rounded-full border border-[#dfbfbd]/60 bg-white text-[#4b0009] shadow-sm transition hover:-translate-y-0.5 hover:border-[#730014]/40 hover:bg-[#fff7f7]"
              onClick={markAllNotificationsRead}
              to="/notifications"
              reloadDocument={shouldReloadWhenLeavingWorkspace}
            >
              <Bell className="h-5 w-5" />
              {unreadNotificationCount > 0 ? (
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
                    {user.targetExam || 'Học viên EnglishLab'}
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
                    <p className="mt-1 truncate text-xs font-semibold text-[#6a5553]">{user.targetExam || 'Học viên EnglishLab'}</p>
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
