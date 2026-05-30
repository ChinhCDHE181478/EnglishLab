import React, { useEffect, useState } from 'react';
import { LogOut, Menu, UserRound } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { getStoredUser, clearSession } from '../../utils/auth';

const navItems = ['Khóa học', 'IELTS', 'TOEIC', 'Giáo viên', 'Lịch khai giảng', 'Về trung tâm'];

const Header = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(() => getStoredUser());

  useEffect(() => {
    const syncUser = () => setUser(getStoredUser());
    window.addEventListener('storage', syncUser);
    window.addEventListener('focus', syncUser);
    window.addEventListener('englishlab:user-updated', syncUser);

    return () => {
      window.removeEventListener('storage', syncUser);
      window.removeEventListener('focus', syncUser);
      window.removeEventListener('englishlab:user-updated', syncUser);
    };
  }, []);

  const handleLogout = () => {
    clearSession();
    setUser(null);
    navigate('/');
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-[#dfbfbd]/30 bg-[#f9f9f9]/95 shadow-sm backdrop-blur-md">
      <div className="mx-auto flex h-20 w-full max-w-[1280px] items-center px-6 md:px-10">
        <Link className="flex shrink-0 cursor-pointer items-center gap-2" to="/" aria-label="EnglishLab home">
          <span className="flex h-8 w-7 items-center gap-1">
            <span className="h-7 w-3 rounded-[1px] bg-[#8a0018]" />
            <span className="h-5 w-2.5 rounded-[1px] bg-[#c45a64]" />
          </span>
          <span className="font-['Manrope'] text-xl font-extrabold tracking-tight text-[#2b2828]">
            English<span className="text-[#8a0018]">Lab</span>
          </span>
        </Link>

        <nav className="flex flex-1 items-center justify-center gap-6 xl:gap-9" aria-label="Main navigation">
          {navItems.map((item) => (
            <a
              key={item}
              className="cursor-pointer whitespace-nowrap text-sm font-bold text-[#6a5553] transition-colors hover:text-[#8a0018]"
              href="#"
            >
              {item}
            </a>
          ))}
        </nav>

        {user ? (
          <div className="ml-auto flex shrink-0 items-center gap-3">
            <div className="flex items-center gap-2 rounded-full border border-[#dfbfbd]/60 bg-white px-3 py-2 shadow-sm">
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[#8a0018]/10 text-[#8a0018]">
                <UserRound size={17} />
              </span>
              <div className="hidden leading-tight lg:block">
                <p className="max-w-[150px] truncate text-sm font-extrabold text-[#2b2828]">
                  {user.fullName || user.email}
                </p>
                <p className="max-w-[150px] truncate text-xs font-semibold text-[#6a5553]">
                  {user.targetExam || user.email}
                </p>
              </div>
            </div>
            <button
              className="flex cursor-pointer items-center gap-2 rounded-[2px] border border-[#8a0018]/25 px-4 py-3 text-sm font-extrabold text-[#8a0018] transition-all hover:-translate-y-0.5 hover:bg-[#8a0018]/5"
              onClick={handleLogout}
              type="button"
            >
              <LogOut size={16} />
              Đăng xuất
            </button>
          </div>
        ) : (
          <div className="ml-auto flex shrink-0 items-center gap-5 xl:gap-7">
            <Link className="cursor-pointer text-sm font-extrabold text-[#8a0018] transition-colors hover:text-[#4b0009]" to="/login">
              Đăng nhập
            </Link>
            <Link
              className="cursor-pointer rounded-[2px] bg-[#8a0018] px-7 py-3 text-sm font-extrabold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-[#650012]"
              to="/register"
            >
              Đăng ký tư vấn
            </Link>
          </div>
        )}

        <button className="ml-auto hidden cursor-pointer text-[#1a1c1c]" type="button" aria-label="Mở menu">
          <Menu size={30} />
        </button>
      </div>
    </header>
  );
};

export default Header;
