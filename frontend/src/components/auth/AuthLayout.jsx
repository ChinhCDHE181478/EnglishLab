import React, { useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Link, Outlet, useLocation } from 'react-router-dom';
import loginMascot from '../../assets/login-mascot.jpg';

const quotes = {
  '/login': {
    title: 'Chào mừng bạn trở lại với EnglishLab!',
    text: 'Tiếp tục hành trình chinh phục tiếng Anh của bạn.',
  },
  '/register': {
    title: 'Bắt đầu hành trình với EnglishLab!',
    text: 'Tạo tài khoản để nhận gợi ý học cá nhân hóa và feedback AI tức thì.',
  },
  '/forgot-password': {
    title: 'Lấy lại quyền truy cập thật nhanh.',
    text: 'Chúng tôi sẽ gửi mã OTP đặt lại mật khẩu về đúng email của bạn.',
  },
  '/reset-password': {
    title: 'Một mật khẩu mới, một khởi đầu mới.',
    text: 'Đặt lại mật khẩu để tiếp tục học mà không làm mất tiến độ.',
  },
  '/verify-email': {
    title: 'Chỉ còn một bước để kích hoạt tài khoản.',
    text: 'Xác thực email để EnglishLab biết chắc đây là bạn.',
  },
};

const formVariants = {
  initial: { opacity: 0, x: 18, filter: 'blur(4px)' },
  animate: {
    opacity: 1,
    x: 0,
    filter: 'blur(0px)',
    transition: { duration: 0.34, ease: [0.22, 1, 0.36, 1] },
  },
  exit: {
    opacity: 0,
    x: -14,
    filter: 'blur(3px)',
    transition: { duration: 0.18, ease: [0.4, 0, 1, 1] },
  },
};

const Logo = () => (
  <Link className="flex cursor-pointer items-center gap-3" to="/" aria-label="Về trang chủ EnglishLab">
    <span className="flex h-9 w-8 items-center gap-1">
      <span className="h-8 w-3 bg-[#730014]" />
      <span className="h-6 w-3 bg-[#B39B9A]" />
    </span>

    <span className="font-['Manrope'] text-3xl font-[800] tracking-tighter text-[#1A1C1C]">
      English<span className="text-[#730014]">Lab</span>
    </span>
  </Link>
);

const AuthLayout = () => {
  const location = useLocation();
  const quote = quotes[location.pathname] || quotes['/login'];

  useEffect(() => {
    const handleMouseMove = (event) => {
      document.documentElement.style.setProperty('--cursor-x', `${event.clientX}px`);
      document.documentElement.style.setProperty('--cursor-y', `${event.clientY}px`);
    };

    window.addEventListener('mousemove', handleMouseMove);

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  return (
    <div className="relative flex min-h-screen w-full items-center justify-center overflow-x-hidden bg-[#F4F3F3] p-4 font-['Inter'] text-[#1A1C1C] lg:p-8">
      <style>
        {`
          .auth-cursor-glow {
            position: fixed;
            inset: 0;
            width: 100vw;
            height: 100vh;
            pointer-events: none;
            z-index: 0;
            background: radial-gradient(
              600px circle at var(--cursor-x, 50%) var(--cursor-y, 50%),
              rgba(115, 0, 20, 0.15),
              transparent 40%
            );
            transition: background 0.1s ease-out;
          }
        `}
      </style>

      <div className="auth-cursor-glow" />

      <motion.div
        className="relative z-10 flex min-h-[650px] w-full max-w-[1100px] flex-col overflow-hidden rounded-[32px] bg-white shadow-[0_20px_50px_rgba(0,0,0,0.3)] lg:flex-row"
        initial={{ opacity: 0, y: 16, scale: 0.88 }}
        animate={{ opacity: 1, y: 0, scale: 0.9 }}
        transition={{ duration: 0.38, ease: [0.22, 1, 0.36, 1] }}
      >
        <div className="relative hidden overflow-hidden bg-[#1A1C1C] lg:flex lg:w-1/2">
          <img
            alt="EnglishLab mascot working at a desk"
            className="breathe-animation h-full w-full object-cover object-center opacity-90"
            src={loginMascot}
          />

          <div className="absolute inset-0 z-10 bg-gradient-to-t from-[#730014]/60 to-transparent mix-blend-multiply" />

          <div className="absolute bottom-10 left-10 right-10 z-20">
            <div className="rounded-2xl border border-white/20 bg-white/10 p-6 shadow-lg backdrop-blur-md">
              <AnimatePresence mode="wait">
                <motion.div
                  key={location.pathname}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
                >
                  <p className="mb-2 font-['Manrope'] text-2xl font-[600] leading-[1.3] text-white">
                    "{quote.title}"
                  </p>
                  <p className="font-['Inter'] text-base leading-[1.6] text-white/90">
                    {quote.text}
                  </p>
                </motion.div>
              </AnimatePresence>
            </div>
          </div>
        </div>

        <div className="z-20 flex w-full flex-col items-center justify-center bg-white p-8 sm:p-12 lg:w-1/2 lg:p-16">
          <div className="w-full max-w-[400px]">
            <div className="mb-8 flex justify-center text-center lg:justify-start lg:text-left">
              <Logo />
            </div>

            <AnimatePresence mode="wait" initial={false}>
              <motion.div
                key={location.pathname}
                variants={formVariants}
                initial="initial"
                animate="animate"
                exit="exit"
              >
                <Outlet />
              </motion.div>
            </AnimatePresence>
          </div>
        </div>
      </motion.div>
    </div>
  );
};

export default AuthLayout;
