import React, { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, KeyRound, Mail, RefreshCcw } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { resendVerificationEmail, verifyEmail } from '../api/authApi';

const VerifyEmail = () => {
  const location = useLocation();
  const params = useMemo(() => new URLSearchParams(location.search), [location.search]);
  const initialEmail = params.get('email') || '';
  const sent = params.get('sent') === '1';
  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [status, setStatus] = useState('idle');
  const [message, setMessage] = useState(
    sent ? 'Chúng tôi vừa gửi mã xác thực. Hãy kiểm tra hộp thư và nhập mã để tiếp tục.' : ''
  );
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const [verifyingCode, setVerifyingCode] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(sent ? 60 : 0);

  useEffect(() => {
    if (resendCooldown <= 0) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      setResendCooldown((current) => Math.max(0, current - 1));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [resendCooldown]);

  const handleResend = async (event) => {
    event.preventDefault();

    if (!email.trim()) {
      setError('Vui lòng nhập email để gửi lại mã xác thực.');
      return;
    }
    if (resendCooldown > 0) {
      setError(`Vui lòng chờ ${resendCooldown} giây trước khi gửi lại OTP.`);
      return;
    }

    setSending(true);
    setError('');
    try {
      const response = await resendVerificationEmail(email.trim());
      setMessage(response.data?.message || 'Đã gửi lại mã xác thực.');
      setResendCooldown(60);
      setStatus('idle');
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể gửi lại mã xác thực. Vui lòng thử lại.');
    } finally {
      setSending(false);
    }
  };

  const handleVerifyCode = async (event) => {
    event.preventDefault();

    const normalizedEmail = email.trim();
    const normalizedCode = code.trim();
    if (!normalizedEmail) {
      setError('Vui lòng nhập email để xác thực.');
      return;
    }
    if (!/^\d{6}$/.test(normalizedCode)) {
      setError('Mã xác thực phải gồm 6 chữ số.');
      return;
    }

    setVerifyingCode(true);
    setError('');
    try {
      const response = await verifyEmail({ email: normalizedEmail, code: normalizedCode });
      setStatus('success');
      setMessage(response.data?.message || 'Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ.');
      setCode('');
    } catch (err) {
      setStatus('error');
      setError(err.response?.data?.message || 'Không thể xác thực email. Vui lòng thử lại.');
    } finally {
      setVerifyingCode(false);
    }
  };

  return (
    <>
      <div className="mb-7 text-center lg:text-left">
        <h1 className="mb-2 font-['Manrope'] text-[32px] font-[700] leading-[1.2] text-[#1A1C1C]">
          Xác thực email
        </h1>
        <p className="text-base leading-[1.6] text-[#584140]">
          Kích hoạt tài khoản để bắt đầu học trên EnglishLab.
        </p>
      </div>

      {message && (
        <div className="mb-5 rounded border border-[#c7e7d2] bg-[#edf8f1] px-4 py-3 text-sm text-[#185c37]">
          <div className="flex items-start gap-2">
            <CheckCircle2 className="mt-0.5" size={18} />
            <span>{message}</span>
          </div>
        </div>
      )}

      {error && (
        <div className="mb-5 rounded border border-[#BA1A1A] bg-[#FFDAD6] px-4 py-3 text-sm text-[#93000A]">
          {error}
        </div>
      )}

      {status === 'verifying' ? (
        <div className="rounded-2xl border border-[#E5E2E0] bg-[#faf8f8] px-4 py-6 text-sm text-[#584140]">
          Đang xác thực email của bạn...
        </div>
      ) : (
        <div className="space-y-5">
          <form className="space-y-5" onSubmit={handleVerifyCode}>
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

          <div>
            <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="code">
              Mã xác thực
            </label>
            <div className="relative">
              <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
              <input
                className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-4 text-base leading-[1.6] tracking-[0.18em] text-[#1A1C1C] outline-none transition-colors placeholder:tracking-normal placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                id="code"
                inputMode="numeric"
                maxLength={6}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="123456"
                type="text"
                value={code}
              />
            </div>
          </div>

          <button
            className="flex w-full cursor-pointer items-center justify-center gap-2 rounded border border-transparent bg-[#730014] px-4 py-3 text-sm font-[600] leading-none tracking-[0.02em] text-white shadow-sm transition-all duration-200 hover:-translate-y-[2px] hover:bg-[#9E001F] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
            disabled={verifyingCode}
            type="submit"
          >
            <CheckCircle2 size={16} />
            {verifyingCode ? 'Đang xác thực...' : 'Xác thực email'}
          </button>
          </form>

          <form onSubmit={handleResend}>
            <button
              className="flex w-full cursor-pointer items-center justify-center gap-2 rounded border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-[600] leading-none tracking-[0.02em] text-[#730014] shadow-sm transition-all duration-200 hover:-translate-y-[2px] hover:bg-[#fff7f7] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
              disabled={sending || resendCooldown > 0}
              type="submit"
            >
              <RefreshCcw size={16} />
              {sending
                ? 'Đang gửi lại...'
                : resendCooldown > 0
                  ? `Gửi lại sau ${resendCooldown}s`
                  : 'Gửi lại mã xác thực'}
            </button>
          </form>
        </div>
      )}

      <div className="mt-7 text-center text-sm text-[#584140]">
        <Link className="font-semibold text-[#730014] transition-colors hover:text-[#8B1722]" to="/login">
          Chuyển tới đăng nhập
        </Link>
      </div>
    </>
  );
};

export default VerifyEmail;
