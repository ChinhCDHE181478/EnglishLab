import React, { useState } from 'react';
import { ArrowLeft, Mail } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { forgotPassword } from '../api/authApi';

const ForgotPassword = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!email.trim()) {
      setError('Vui lòng nhập email đã đăng ký.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await forgotPassword(email.trim());
      navigate(`/reset-password?email=${encodeURIComponent(email.trim())}`, { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể gửi email lúc này. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="mb-7 text-center lg:text-left">
        <h1 className="mb-2 font-['Manrope'] text-[32px] font-[700] leading-[1.2] text-[#1A1C1C]">
          Quên mật khẩu
        </h1>
        <p className="text-base leading-[1.6] text-[#584140]">
          Nhập email của bạn, EnglishLab sẽ gửi mã OTP đặt lại mật khẩu.
        </p>
      </div>

      {error && (
        <div className="mb-5 rounded border border-[#BA1A1A] bg-[#FFDAD6] px-4 py-3 text-sm text-[#93000A]">
          {error}
        </div>
      )}

      <form className="space-y-5" onSubmit={handleSubmit}>
        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="email">
            Email
          </label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-4 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="email"
              onChange={(event) => setEmail(event.target.value)}
              placeholder="nhapemail@example.com"
              type="email"
              value={email}
            />
          </div>
        </div>

        <button
          className="flex w-full cursor-pointer justify-center rounded border border-transparent bg-[#730014] px-4 py-3 text-sm font-[600] leading-none tracking-[0.02em] text-white shadow-sm transition-all duration-200 hover:-translate-y-[2px] hover:bg-[#9E001F] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
          disabled={loading}
          type="submit"
        >
          {loading ? 'Đang gửi email...' : 'Gửi mã OTP đặt lại mật khẩu'}
        </button>
      </form>

      <div className="mt-7 text-center text-sm text-[#584140]">
        <Link className="inline-flex items-center gap-2 font-semibold text-[#730014] transition-colors hover:text-[#8B1722]" to="/login">
          <ArrowLeft size={16} />
          Quay lại đăng nhập
        </Link>
      </div>
    </>
  );
};

export default ForgotPassword;
