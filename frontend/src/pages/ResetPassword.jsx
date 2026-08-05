import React, { useEffect, useMemo, useState } from 'react';
import { Eye, EyeOff, KeyRound, Lock, Mail, RefreshCcw } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { forgotPassword, resetPassword } from '../api/authApi';

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

const ResetPassword = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const initialEmail = useMemo(() => new URLSearchParams(location.search).get('email') || '', [location.search]);
  const [formData, setFormData] = useState({ email: initialEmail, code: '', password: '', confirmPassword: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(initialEmail ? 60 : 0);
  const passwordStrength = useMemo(() => getPasswordStrength(formData.password), [formData.password]);

  useEffect(() => {
    if (resendCooldown <= 0) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      setResendCooldown((current) => Math.max(0, current - 1));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [resendCooldown]);

  const handleResendOtp = async () => {
    const normalizedEmail = formData.email.trim();
    if (!normalizedEmail) {
      setError('Vui lòng nhập email để gửi lại OTP.');
      return;
    }
    if (resendCooldown > 0) {
      setError(`Vui lòng chờ ${resendCooldown} giây trước khi gửi lại OTP.`);
      return;
    }

    setResending(true);
    setError('');
    setSuccess('');

    try {
      const response = await forgotPassword(normalizedEmail);
      setSuccess(response.data?.message || 'Đã gửi lại mã OTP đặt lại mật khẩu.');
      setResendCooldown(60);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể gửi lại OTP. Vui lòng thử lại.');
    } finally {
      setResending(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!formData.email.trim()) {
      setError('Vui lòng nhập email đã yêu cầu đặt lại mật khẩu.');
      return;
    }

    if (!/^\d{6}$/.test(formData.code.trim())) {
      setError('Mã OTP phải gồm 6 chữ số.');
      return;
    }

    if (!formData.password || !formData.confirmPassword) {
      setError('Vui lòng nhập đầy đủ mật khẩu mới.');
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
    setSuccess('');

    try {
      const response = await resetPassword({
        email: formData.email.trim(),
        code: formData.code.trim(),
        newPassword: formData.password,
      });
      setSuccess(response.data?.message || 'Đặt lại mật khẩu thành công.');
      const nextPath = new URLSearchParams(location.search).get('next');
      const safeNextPath = nextPath?.startsWith('/') && !nextPath.startsWith('//') ? nextPath : null;
      const loginPath = safeNextPath ? `/login?next=${encodeURIComponent(safeNextPath)}` : '/login';
      setTimeout(() => navigate(loginPath, { replace: true }), 1200);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể đặt lại mật khẩu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="mb-7 text-center lg:text-left">
        <h1 className="mb-2 font-['Manrope'] text-[32px] font-[700] leading-[1.2] text-[#1A1C1C]">
          Tạo mật khẩu mới
        </h1>
        <p className="text-base leading-[1.6] text-[#584140]">
          Nhập mã OTP đã gửi qua email và tạo mật khẩu mới.
        </p>
      </div>

      {error && (
        <div className="mb-5 rounded border border-[#BA1A1A] bg-[#FFDAD6] px-4 py-3 text-sm text-[#93000A]">
          {error}
        </div>
      )}

      {success && (
        <div className="mb-5 rounded border border-[#c7e7d2] bg-[#edf8f1] px-4 py-3 text-sm text-[#185c37]">
          {success}
        </div>
      )}

      <form className="space-y-4" onSubmit={handleSubmit}>
        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="email">
            Email
          </label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-4 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="email"
              onChange={(event) => setFormData((current) => ({ ...current, email: event.target.value }))}
              placeholder="nhapemail@example.com"
              type="email"
              value={formData.email}
            />
          </div>
        </div>

        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="code">
            Mã OTP
          </label>
          <div className="relative">
            <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-4 text-base leading-[1.6] tracking-[0.18em] text-[#1A1C1C] outline-none transition-colors placeholder:tracking-normal placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="code"
              inputMode="numeric"
              maxLength={6}
              onChange={(event) => setFormData((current) => ({ ...current, code: event.target.value.replace(/\D/g, '').slice(0, 6) }))}
              placeholder="123456"
              type="text"
              value={formData.code}
            />
          </div>
          <button
            className="mt-3 inline-flex cursor-pointer items-center gap-2 rounded border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-[600] text-[#730014] transition-colors hover:bg-[#fff7f7] disabled:cursor-not-allowed disabled:opacity-70"
            disabled={resending || resendCooldown > 0}
            onClick={handleResendOtp}
            type="button"
          >
            <RefreshCcw size={14} />
            {resending
              ? 'Đang gửi lại...'
              : resendCooldown > 0
                ? `Gửi lại sau ${resendCooldown}s`
                : 'Gửi lại OTP'}
          </button>
        </div>

        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="password">
            Mật khẩu mới
          </label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-12 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="password"
              onChange={(event) => setFormData((current) => ({ ...current, password: event.target.value }))}
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
            <div className="mt-3 rounded-xl border border-[#ead8d4] bg-[#fff8f6] px-3 py-2 text-xs font-semibold leading-relaxed text-[#7a6461]">
              Mật khẩu cần ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.
            </div>
          </div>
        </div>

        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="confirmPassword">
            Xác nhận mật khẩu mới
          </label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-12 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
              id="confirmPassword"
              onChange={(event) => setFormData((current) => ({ ...current, confirmPassword: event.target.value }))}
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
          {loading ? 'Đang cập nhật...' : 'Lưu mật khẩu mới'}
        </button>
      </form>

      <div className="mt-7 text-center text-sm text-[#584140]">
        <Link className="font-semibold text-[#730014] transition-colors hover:text-[#8B1722]" to="/login">
          Quay lại đăng nhập
        </Link>
      </div>
    </>
  );
};

export default ResetPassword;
