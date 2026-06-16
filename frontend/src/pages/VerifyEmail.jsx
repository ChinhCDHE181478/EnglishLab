import React, { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, Mail, RefreshCcw } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { resendVerificationEmail, verifyEmail } from '../api/authApi';

const VerifyEmail = () => {
  const location = useLocation();
  const params = useMemo(() => new URLSearchParams(location.search), [location.search]);
  const token = params.get('token') || '';
  const initialEmail = params.get('email') || '';
  const sent = params.get('sent') === '1';
  const [email, setEmail] = useState(initialEmail);
  const [status, setStatus] = useState(token ? 'verifying' : 'idle');
  const [message, setMessage] = useState(
    sent ? 'Chúng tôi vừa gửi email xác thực. Hãy kiểm tra hộp thư của bạn để tiếp tục.' : ''
  );
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (!token) {
      return;
    }

    const runVerification = async () => {
      try {
        const response = await verifyEmail(token);
        setStatus('success');
        setMessage(response.data?.message || 'Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ.');
        setError('');
      } catch (err) {
        setStatus('error');
        setError(err.response?.data?.message || 'Không thể xác thực email. Vui lòng thử lại.');
      }
    };

    runVerification();
  }, [token]);

  const handleResend = async (event) => {
    event.preventDefault();

    if (!email.trim()) {
      setError('Vui lòng nhập email để gửi lại liên kết xác thực.');
      return;
    }

    setSending(true);
    setError('');
    try {
      const response = await resendVerificationEmail(email.trim());
      setMessage(response.data?.message || 'Đã gửi lại email xác thực.');
      setStatus('idle');
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể gửi lại email xác thực. Vui lòng thử lại.');
    } finally {
      setSending(false);
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
        <form className="space-y-5" onSubmit={handleResend}>
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
            className="flex w-full cursor-pointer items-center justify-center gap-2 rounded border border-transparent bg-[#730014] px-4 py-3 text-sm font-[600] leading-none tracking-[0.02em] text-white shadow-sm transition-all duration-200 hover:-translate-y-[2px] hover:bg-[#9E001F] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
            disabled={sending}
            type="submit"
          >
            <RefreshCcw size={16} />
            {sending ? 'Đang gửi lại...' : 'Gửi lại email xác thực'}
          </button>
        </form>
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
