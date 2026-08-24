import { useEffect, useMemo, useState } from 'react';
import { AlertCircle, Check, CheckCircle2, Circle, Eye, EyeOff, KeyRound, Lock, NotebookPen } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { changeCurrentUserPassword } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';
import { needsPlacementTest } from '../../utils/auth';

const passwordRequirements = [
  { id: 'length', label: 'Ít nhất 8 ký tự', test: (password) => password.length >= 8 && password.length <= 72 },
  { id: 'uppercase', label: '1 chữ in hoa (A-Z)', test: (password) => /[A-Z]/.test(password) },
  { id: 'lowercase', label: '1 chữ in thường (a-z)', test: (password) => /[a-z]/.test(password) },
  { id: 'number', label: '1 chữ số (0-9)', test: (password) => /\d/.test(password) },
  { id: 'special', label: '1 ký tự đặc biệt (!@#$...)', test: (password) => /[^A-Za-z0-9]/.test(password) },
];

const getPasswordStrength = (password) => {
  if (!password) {
    return { score: 0, label: 'Chưa nhập', color: '#cbd5e1', bg: 'bg-slate-100 text-slate-500 border-slate-200' };
  }
  const passedRules = passwordRequirements.filter((rule) => rule.test(password)).length;
  if (passedRules <= 2) {
    return { score: 1, label: 'Yếu', color: '#e11d48', bg: 'bg-rose-50 text-rose-700 border-rose-200' };
  }
  if (passedRules === 3) {
    return { score: 2, label: 'Trung bình', color: '#f59e0b', bg: 'bg-amber-50 text-amber-700 border-amber-200' };
  }
  if (passedRules === 4) {
    return { score: 3, label: 'Khá', color: '#10b981', bg: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
  }
  return { score: 4, label: 'Mạnh', color: '#059669', bg: 'bg-emerald-100 text-emerald-800 border-emerald-300' };
};

export default function AccountOnboardingPrompts() {
  const navigate = useNavigate();
  const { updateUser, user } = useAuth();
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [placementVisible, setPlacementVisible] = useState(false);

  const reminderKey = user?.id ? `englishlab.placement-reminder.dismissed.${user.id}` : '';
  const requiresPassword = Boolean(user && user.passwordSet === false);
  const requiresPlacement = needsPlacementTest(user);

  const passwordStrength = useMemo(() => getPasswordStrength(password), [password]);
  const isMatch = Boolean(confirmPassword && password === confirmPassword);
  const isMismatch = Boolean(confirmPassword && password !== confirmPassword);

  useEffect(() => {
    if (!requiresPlacement || requiresPassword || !reminderKey) {
      setPlacementVisible(false);
      return;
    }
    setPlacementVisible(sessionStorage.getItem(reminderKey) !== 'true');
  }, [reminderKey, requiresPassword, requiresPlacement]);

  const savePassword = async (event) => {
    event.preventDefault();
    setError('');

    const passedRules = passwordRequirements.filter((rule) => rule.test(password)).length;
    if (passedRules < 5) {
      setError('Mật khẩu cần đáp ứng đầy đủ tất cả các yêu cầu bảo mật bên dưới.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    setSaving(true);
    try {
      await changeCurrentUserPassword({ newPassword: password, confirmPassword });
      updateUser({ ...user, passwordSet: true });
      setPassword('');
      setConfirmPassword('');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Không thể thiết lập mật khẩu. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  if (requiresPassword) {
    return (
      <Modal>
        {/* Header Badge & Icon */}
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-[#f5d0d3] bg-gradient-to-br from-[#fff0f1] to-[#ffe5e7] text-[#730014] shadow-sm">
            <KeyRound size={22} />
          </div>
          <div>
            <span className="inline-block rounded-md border border-[#f8d7da] bg-[#fff0f1] px-2.5 py-0.5 text-[11px] font-extrabold uppercase tracking-[0.14em] text-[#730014]">
              Bảo mật tài khoản
            </span>
            <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
              Thiết lập mật khẩu
            </h2>
          </div>
        </div>

        <p className="mt-3 text-sm leading-relaxed text-[#584140]">
          Tạo mật khẩu an toàn để lần sau bạn có thể dễ dàng đăng nhập bằng email và mật khẩu.
        </p>

        <form className="mt-6 space-y-5" onSubmit={savePassword}>
          {/* Mật khẩu mới */}
          <div className="space-y-2">
            <label className="block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
              Mật khẩu mới *
            </label>
            <div className="relative">
              <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                autoComplete="new-password"
                className="w-full rounded-2xl border border-[#dfbfbd]/70 bg-[#fcfbfb] pl-10 pr-11 py-3 text-sm font-medium text-slate-900 outline-none transition duration-150 focus:border-[#730014] focus:bg-white focus:ring-4 focus:ring-[#730014]/10"
                onChange={(event) => {
                  setPassword(event.target.value);
                  setError('');
                }}
                placeholder="Nhập mật khẩu mới..."
                type={showPassword ? 'text' : 'password'}
                value={password}
              />
              <button
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-[#730014]"
                onClick={() => setShowPassword((prev) => !prev)}
                type="button"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            {/* Strength Meter Bar */}
            {password ? (
              <div className="mt-3 rounded-2xl border border-slate-100 bg-slate-50/70 p-3.5 space-y-2.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-600">Độ mạnh mật khẩu</span>
                  <span className={`rounded-md border px-2 py-0.5 text-[11px] font-extrabold ${passwordStrength.bg}`}>
                    {passwordStrength.label}
                  </span>
                </div>

                {/* 4 segmented bar */}
                <div className="grid grid-cols-4 gap-1.5">
                  {[1, 2, 3, 4].map((level) => (
                    <div
                      key={level}
                      className="h-1.5 rounded-full transition-all duration-300"
                      style={{
                        backgroundColor: level <= passwordStrength.score ? passwordStrength.color : '#e2e8f0',
                      }}
                    />
                  ))}
                </div>

                {/* Requirements checklist chips */}
                <div className="mt-2.5 grid grid-cols-1 gap-1.5 sm:grid-cols-2">
                  {passwordRequirements.map((rule) => {
                    const passed = rule.test(password);
                    return (
                      <div
                        key={rule.id}
                        className={`flex items-center gap-1.5 rounded-xl border px-2.5 py-1.5 text-[11px] font-medium transition ${
                          passed
                            ? 'border-emerald-200 bg-emerald-50/80 text-emerald-800'
                            : 'border-slate-200 bg-white text-slate-500'
                        }`}
                      >
                        {passed ? (
                          <Check className="h-3.5 w-3.5 shrink-0 text-emerald-600" />
                        ) : (
                          <Circle className="h-3.5 w-3.5 shrink-0 text-slate-300" />
                        )}
                        <span className="truncate">{rule.label}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            ) : null}
          </div>

          {/* Xác nhận mật khẩu */}
          <div className="space-y-2">
            <label className="block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
              Xác nhận mật khẩu *
            </label>
            <div className="relative">
              <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                autoComplete="new-password"
                className={`w-full rounded-2xl border bg-[#fcfbfb] pl-10 pr-11 py-3 text-sm font-medium text-slate-900 outline-none transition duration-150 focus:bg-white focus:ring-4 ${
                  isMismatch
                    ? 'border-rose-300 focus:border-rose-600 focus:ring-rose-600/10'
                    : isMatch
                    ? 'border-emerald-300 focus:border-emerald-600 focus:ring-emerald-600/10'
                    : 'border-[#dfbfbd]/70 focus:border-[#730014] focus:ring-[#730014]/10'
                }`}
                onChange={(event) => {
                  setConfirmPassword(event.target.value);
                  setError('');
                }}
                placeholder="Nhập lại mật khẩu mới..."
                type={showConfirmPassword ? 'text' : 'password'}
                value={confirmPassword}
              />
              <button
                aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-[#730014]"
                onClick={() => setShowConfirmPassword((prev) => !prev)}
                type="button"
              >
                {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>

            {/* Confirm password feedback indicator */}
            {confirmPassword ? (
              <div className="flex items-center gap-1.5 text-xs">
                {isMatch ? (
                  <span className="inline-flex items-center gap-1 font-semibold text-emerald-700">
                    <CheckCircle2 className="h-3.5 w-3.5" /> Mật khẩu xác nhận đã khớp
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 font-semibold text-rose-600">
                    <AlertCircle className="h-3.5 w-3.5" /> Mật khẩu chưa trùng khớp
                  </span>
                )}
              </div>
            ) : null}
          </div>

          {/* Error notice */}
          {error ? (
            <div className="flex items-start gap-2.5 rounded-2xl border border-rose-200 bg-rose-50 p-3.5 text-xs font-bold text-rose-800">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
              <span>{error}</span>
            </div>
          ) : null}

          {/* Action button */}
          <button
            className="w-full rounded-2xl bg-gradient-to-r from-[#730014] to-[#8a0018] px-5 py-3.5 text-sm font-bold text-white shadow-md shadow-[#730014]/15 transition duration-150 hover:shadow-lg hover:shadow-[#730014]/25 active:scale-[0.99] disabled:cursor-wait disabled:opacity-60"
            disabled={saving}
            type="submit"
          >
            {saving ? (
              <span className="flex items-center justify-center gap-2">
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                Đang lưu mật khẩu...
              </span>
            ) : (
              'Lưu mật khẩu'
            )}
          </button>
        </form>
      </Modal>
    );
  }

  if (!placementVisible) return null;
  return (
    <Modal closeable onClose={() => {
      sessionStorage.setItem(reminderKey, 'true');
      setPlacementVisible(false);
    }}>
      <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#fff0f1] text-[#730014]"><NotebookPen size={21} /></div>
      <p className="mt-5 text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Đánh giá đầu vào</p>
      <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Hoàn thành bài placement test</h2>
      <p className="mt-2 text-sm leading-6 text-[#584140]">Kết quả giúp EnglishLab gợi ý lộ trình và lớp học phù hợp với bạn.</p>
      <div className="mt-6 flex flex-wrap justify-end gap-3">
        <button className="rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-sm font-bold text-[#584140] transition hover:bg-[#fff5f5]" onClick={() => {
          sessionStorage.setItem(reminderKey, 'true');
          setPlacementVisible(false);
        }} type="button">Nhắc tôi sau</button>
        <button className="rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#8a0018]" onClick={() => {
          setPlacementVisible(false);
          navigate('/placement-test');
        }} type="button">Làm bài ngay</button>
      </div>
    </Modal>
  );
}

function Modal({ children, closeable = false, onClose }) {
  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-[#210005]/45 px-4 backdrop-blur-sm animate-fade-in" role="dialog" aria-modal="true">
      <div className="w-full max-w-lg rounded-3xl border border-[#dfbfbd]/60 bg-white p-6 sm:p-7 shadow-2xl">
        {closeable ? <button aria-label="Đóng" className="float-right rounded-lg px-2 py-1 text-sm font-bold text-[#584140] transition hover:bg-[#fff5f5]" onClick={onClose} type="button">Đóng</button> : null}
        {children}
      </div>
    </div>
  );
}
