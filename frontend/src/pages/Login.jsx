import React, { useEffect, useState } from 'react';
import { Eye, EyeOff, Lock, Mail } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { login, loginWithFacebook, loginWithGoogle } from '../api/authApi';
import { hasAnyUserRole, isContentManagerUser, needsProfileCompletion } from '../utils/auth';

const GOOGLE_CLIENT_ID = '550203681762-29kpjelfmfu7q62qfgh72qft0lgfun3f.apps.googleusercontent.com';
const FACEBOOK_APP_ID = import.meta.env.VITE_FACEBOOK_APP_ID;

const GoogleIcon = () => (
  <svg className="mr-2 h-5 w-5" viewBox="0 0 24 24" aria-hidden="true">
    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
    <path fill="#FBBC05" d="M5.84 14.1c-.22-.66-.35-1.36-.35-2.1s.13-1.44.35-2.1V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l3.66-2.84z" />
    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06L5.84 9.9C6.71 7.3 9.14 5.38 12 5.38z" />
  </svg>
);

const FacebookIcon = () => (
  <svg className="mr-2 h-5 w-5" viewBox="0 0 24 24" aria-hidden="true">
    <path
      d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"
      fill="#1877F2"
    />
  </svg>
);

const Login = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const [googleReady, setGoogleReady] = useState(false);
  const [facebookLoading, setFacebookLoading] = useState(false);

  const resolvePostLoginPath = (user) => {
    if (isContentManagerUser(user)) return '/content-manager/dashboard';
    if (hasAnyUserRole(user, ['TRAINING_MANAGER'])) return '/training-manager/classroom-registrations';
    if (hasAnyUserRole(user, ['TEACHER'])) return '/teacher';
    if (needsProfileCompletion(user)) return '/complete-profile';
    return '/home';
  };

  const saveSession = (response) => {
    const { accessToken, user } = response.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('user', JSON.stringify(user));
    navigate(resolvePostLoginPath(user), { replace: true });
  };

  useEffect(() => {
    const scriptSelector = 'script[src="https://accounts.google.com/gsi/client"]';
    const existingScript = document.querySelector(scriptSelector);

    if (!existingScript) {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => setGoogleReady(true);
      document.body.appendChild(script);
    } else {
      setGoogleReady(true);
    }

    if (FACEBOOK_APP_ID && !document.querySelector('script[src="https://connect.facebook.net/en_US/sdk.js"]')) {
      window.fbAsyncInit = () => {
        window.FB.init({
          appId: FACEBOOK_APP_ID,
          cookie: true,
          xfbml: false,
          version: 'v20.0',
        });
      };

      const script = document.createElement('script');
      script.src = 'https://connect.facebook.net/en_US/sdk.js';
      script.async = true;
      script.defer = true;
      document.body.appendChild(script);
    }

    return undefined;
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!formData.email || !formData.password) {
      setError('Vui lòng nhập email và mật khẩu.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await login(formData);

      if (rememberMe) {
        localStorage.setItem('rememberMe', 'true');
      } else {
        localStorage.removeItem('rememberMe');
      }

      saveSession(response);
    } catch (err) {
      setError(err.response?.data?.message || 'Đăng nhập thất bại. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  const handleFacebookLogin = () => {
    setError('');

    if (!FACEBOOK_APP_ID) {
      setError('Chưa cấu hình VITE_FACEBOOK_APP_ID cho Facebook Login.');
      return;
    }

    if (!window.FB) {
      setError('Facebook SDK chưa sẵn sàng. Vui lòng thử lại sau vài giây.');
      return;
    }

    setFacebookLoading(true);
    window.FB.login(
      async (fbResponse) => {
        if (!fbResponse.authResponse?.accessToken) {
          setFacebookLoading(false);
          return;
        }

        try {
          const response = await loginWithFacebook(fbResponse.authResponse.accessToken);
          saveSession(response);
        } catch (err) {
          setError(err.response?.data?.message || 'Đăng nhập Facebook thất bại. Vui lòng thử lại.');
        } finally {
          setFacebookLoading(false);
        }
      },
      { scope: 'public_profile,email' }
    );
  };

  const handleGoogleLogin = () => {
    setError('');

    if (!googleReady || !window.google?.accounts?.oauth2) {
      setError('Google SDK chưa sẵn sàng. Vui lòng thử lại sau vài giây.');
      return;
    }

    setGoogleLoading(true);
    const tokenClient = window.google.accounts.oauth2.initTokenClient({
      client_id: GOOGLE_CLIENT_ID,
      scope: 'openid email profile',
      prompt: 'select_account',
      callback: async (tokenResponse) => {
        if (tokenResponse.error || !tokenResponse.access_token) {
          setGoogleLoading(false);
          setError('Đăng nhập Google thất bại. Vui lòng thử lại.');
          return;
        }

        try {
          const response = await loginWithGoogle(tokenResponse.access_token);
          saveSession(response);
        } catch (err) {
          setError(err.response?.data?.message || 'Đăng nhập Google thất bại. Vui lòng thử lại.');
        } finally {
          setGoogleLoading(false);
        }
      },
      error_callback: () => {
        setGoogleLoading(false);
      },
    });

    tokenClient.requestAccessToken();
  };

  return (
    <>
      <div className="mb-8 text-center lg:text-left">
        <h1 className="mb-2 font-['Manrope'] text-[32px] font-[700] leading-[1.2] text-[#1A1C1C]">
          Đăng nhập vào hệ thống
        </h1>
        <p className="font-['Inter'] text-base leading-[1.6] text-[#584140]">
          Vui lòng nhập thông tin để truy cập tài khoản.
        </p>
      </div>

      {error && (
        <div className="mb-6 rounded border border-[#BA1A1A] bg-[#FFDAD6] px-4 py-3 text-sm text-[#93000A]">
          {error}
        </div>
      )}

      <form className="space-y-6" onSubmit={handleSubmit}>
        <div>
          <label className="mb-2 block text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#1A1C1C]" htmlFor="email">
            Email
          </label>
          <div className="relative">
            <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-[#584140]/50">
              <Mail size={20} />
            </span>
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-4 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors duration-200 placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
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
            <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-[#584140]/50">
              <Lock size={20} />
            </span>
            <input
              className="w-full rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-10 text-base leading-[1.6] text-[#1A1C1C] outline-none transition-colors duration-200 placeholder:text-[#584140]/50 focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
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
        </div>

        <div className="flex items-center justify-between">
          <label className="flex cursor-pointer items-center" htmlFor="remember-me">
            <input
              checked={rememberMe}
              className="h-4 w-4 cursor-pointer rounded border-[#E5E2E0] accent-[#730014] focus:ring-[#730014]"
              id="remember-me"
              onChange={(event) => setRememberMe(event.target.checked)}
              type="checkbox"
            />
            <span className="ml-2 block text-sm text-[#584140]">Ghi nhớ đăng nhập</span>
          </label>

          <Link className="cursor-pointer text-sm font-semibold text-[#730014] transition-colors hover:text-[#8B1722]" to="/forgot-password">
            Quên mật khẩu?
          </Link>
        </div>

        <button
          className="flex w-full cursor-pointer justify-center rounded border border-transparent bg-[#730014] px-4 py-3 text-sm font-[600] leading-none tracking-[0.02em] text-white shadow-sm transition-all duration-200 hover:-translate-y-[2px] hover:bg-[#9E001F] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0"
          disabled={loading}
          type="submit"
        >
          {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>

      <div className="relative mt-8">
        <div aria-hidden="true" className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-[#E5E2E0]" />
        </div>
        <div className="relative flex justify-center text-sm">
          <span className="bg-white px-2 text-xs font-[600] uppercase leading-none tracking-[0.1em] text-[#584140]">
            Hoặc tiếp tục với
          </span>
        </div>
      </div>

      <div className="mt-8 grid grid-cols-2 gap-4">
        <button
          className="group flex min-h-[42px] cursor-pointer items-center justify-center rounded border border-[#E5E2E0] bg-white px-4 py-2.5 text-sm font-[600] leading-none text-[#1A1C1C] shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-[#730014]/30 hover:bg-[#fff8f7] hover:shadow-md active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-70"
          disabled={googleLoading || facebookLoading || loading}
          onClick={handleGoogleLogin}
          type="button"
        >
          <GoogleIcon />
          {googleLoading ? 'Đang xử lý...' : 'Google'}
        </button>
        <button
          className="group flex min-h-[42px] cursor-pointer items-center justify-center rounded border border-[#E5E2E0] bg-white px-4 py-2.5 text-sm font-[600] leading-none text-[#1A1C1C] shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-[#1877F2]/30 hover:bg-blue-50 hover:shadow-md active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-70"
          disabled={googleLoading || facebookLoading || loading}
          onClick={handleFacebookLogin}
          type="button"
        >
          <FacebookIcon />
          {facebookLoading ? 'Đang mở...' : 'Facebook'}
        </button>
      </div>

      <div className="mt-10 text-center">
        <p className="text-sm text-[#584140]">
          Bạn chưa có tài khoản?{' '}
          <Link className="cursor-pointer font-semibold text-[#730014] transition-colors hover:text-[#8B1722]" to="/register">
            Đăng ký ngay
          </Link>
        </p>
      </div>
    </>
  );
};

export default Login;
