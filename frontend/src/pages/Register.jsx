import React, { useMemo, useState } from 'react';
import { Eye, EyeOff, Lock, Mail, User } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/authApi';

const passwordRequirements = [
  { id: 'length', label: 'Ít nhất 8 ký tự', test: (password) => password.length >= 8 },
  { id: 'uppercase', label: 'Có 1 chữ in hoa', test: (password) => /[A-Z]/.test(password) },
  { id: 'lowercase', label: 'Có 1 chữ in thường', test: (password) => /[a-z]/.test(password) },
  { id: 'number', label: 'Có 1 chữ số', test: (password) => /\d/.test(password) },
  { id: 'special', label: 'Có 1 ký tự đặc biệt', test: (password) => /[^A-Za-z0-9]/.test(password) },
];

const getPasswordStrength = (password) => {
  const passedRules = passwordRequirements.filter((rule) => rule.test(password)).length;

  if (!password) {
    return { label: 'Chưa nhập', color: '#d8c9c7', width: '0%', isValid: false };
  }
  if (passedRules <= 2) {
    return { label: 'Yếu', color: '#d14343', width: '25%', isValid: false };
  }
  if (passedRules === 3) {
    return { label: 'Trung bình', color: '#d98c1f', width: '55%', isValid: false };
  }
  if (passedRules === 4) {
    return { label: 'Khá', color: '#2f8f63', width: '78%', isValid: false };
  }
  return { label: 'Mạnh', color: '#167c4d', width: '100%', isValid: true };
};

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
  const passwordStrength = useMemo(() => getPasswordStrength(formData.password), [formData.password]);

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

    if (!passwordStrength.isValid) {
      setError('Mật khẩu phải có ít nhất 8 ký tự, gồm 1 chữ in hoa, 1 chữ in thường, 1 số và 1 ký tự đặc biệt.');
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
      navigate(`/verify-email?email=${encodeURIComponent(formData.email)}&sent=1`, { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.');
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
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-12 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="password"
              name="password"
              onChange={handleChange}
              placeholder="••••••••"
              type={showPassword ? 'text' : 'password'}
              value={formData.password}
            />
            <button
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              className="absolute inset-y-0 right-0 flex cursor-pointer items-center pr-3 text-[#584140]/50 transition-colors hover:text-[#730014]"
              onClick={() => setShowPassword((current) => !current)}
              type="button"
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>

          <div className="mt-3">
            <div className="flex items-center justify-between text-xs font-semibold">
              <span className="text-[#7a6461]">Độ mạnh mật khẩu</span>
              <span style={{ color: passwordStrength.color }}>{passwordStrength.label}</span>
            </div>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-[#f0e4e1]">
              <div
                className="h-full rounded-full transition-all duration-300"
                style={{ width: passwordStrength.width, backgroundColor: passwordStrength.color }}
              />
            </div>
            <div className="mt-3 grid gap-2 sm:grid-cols-2">
              {passwordRequirements.map((rule) => {
                const passed = rule.test(formData.password);
                return (
                  <div
                    key={rule.id}
                    className={`rounded-xl border px-3 py-2 text-xs font-semibold ${
                      passed
                        ? 'border-[#cce8d7] bg-[#eff8f2] text-[#1b6b45]'
                        : 'border-[#ead8d4] bg-[#fff8f6] text-[#7a6461]'
                    }`}
                  >
                    {rule.label}
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="confirmPassword">
            Xác nhận mật khẩu
          </label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-12 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="confirmPassword"
              name="confirmPassword"
              onChange={handleChange}
              placeholder="••••••••"
              type={showConfirm ? 'text' : 'password'}
              value={formData.confirmPassword}
            />
            <button
              aria-label={showConfirm ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              className="absolute inset-y-0 right-0 flex cursor-pointer items-center pr-3 text-[#584140]/50 transition-colors hover:text-[#730014]"
              onClick={() => setShowConfirm((current) => !current)}
              type="button"
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
          {loading ? 'Đang tạo tài khoản...' : 'Đăng ký'}
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
