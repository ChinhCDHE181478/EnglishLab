import React, { useState } from 'react';
import { Eye, EyeOff, Lock, Mail, User } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/authApi';

const Register = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!formData.fullName || !formData.email || !formData.password || !formData.confirmPassword) {
      setError('Vui lòng nhập đầy đủ thông tin.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      await register({
        fullName: formData.fullName,
        email: formData.email,
        password: formData.password,
      });
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng kí thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="mb-7 text-center lg:text-left">
        <h1 className="mb-2 font-['Manrope'] text-[32px] font-[700] leading-[1.2] text-[#1A1C1C]">
          Tạo tài khoản học tập
        </h1>
        <p className="text-base leading-[1.6] text-[#584140]">
          Nhập thông tin để bắt đầu luyện thi cùng EnglishLab.
        </p>
      </div>

      {error && (
        <div className="mb-5 rounded border border-[#BA1A1A] bg-[#FFDAD6] px-4 py-3 text-sm text-[#93000A]">
          {error}
        </div>
      )}

      <form className="space-y-4" onSubmit={handleSubmit}>
        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="fullName">
            Họ và tên
          </label>
          <div className="relative">
            <User className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-4 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="fullName"
              name="fullName"
              onChange={handleChange}
              placeholder="Nguyễn Văn A"
              type="text"
              value={formData.fullName}
            />
          </div>
        </div>

        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="email">
            Email
          </label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-4 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="email"
              name="email"
              onChange={handleChange}
              placeholder="nhapemail@example.com"
              type="email"
              value={formData.email}
            />
          </div>
        </div>

        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="password">
            Mật khẩu
          </label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-10 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="password"
              name="password"
              onChange={handleChange}
              placeholder="••••••••"
              type={showPassword ? 'text' : 'password'}
              value={formData.password}
            />
            <button
              className="absolute inset-y-0 right-0 flex cursor-pointer items-center pr-3 text-[#584140]/50 transition-colors hover:text-[#730014]"
              onClick={() => setShowPassword((current) => !current)}
              type="button"
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
        </div>

        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="confirmPassword">
            Xác nhận mật khẩu
          </label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-10 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="confirmPassword"
              name="confirmPassword"
              onChange={handleChange}
              placeholder="••••••••"
              type={showConfirm ? 'text' : 'password'}
              value={formData.confirmPassword}
            />
            <button
              className="absolute inset-y-0 right-0 flex cursor-pointer items-center pr-3 text-[#584140]/50 transition-colors hover:text-[#730014]"
              onClick={() => setShowConfirm((current) => !current)}
              type="button"
              aria-label={showConfirm ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            >
              {showConfirm ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
        </div>

        <button
          className="flex w-full cursor-pointer justify-center rounded border border-transparent bg-[#730014] px-4 py-3 text-sm font-[600] leading-none tracking-[0.02em] text-white shadow-sm transition-all duration-200 hover:-translate-y-[2px] hover:bg-[#9E001F] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
          disabled={loading}
          type="submit"
        >
          {loading ? 'Đang tạo tài khoản...' : 'Đăng kí'}
        </button>
      </form>

      <div className="mt-7 text-center text-sm text-[#584140]">
        Đã có tài khoản?{' '}
        <Link className="cursor-pointer font-semibold text-[#730014] transition-colors hover:text-[#8B1722]" to="/login">
          Quay lại đăng nhập
        </Link>
      </div>
    </>
  );
};

export default Register;
