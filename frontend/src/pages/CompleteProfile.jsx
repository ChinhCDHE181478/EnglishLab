import { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import {
  BookOpen,
  Camera,
  Check,
  Eye,
  EyeOff,
  Lock,
  Mail,
  Phone,
  Save,
  Target,
  User,
  X,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser, updateCurrentUser } from '../api/authApi';
import Footer from '../components/ai-learning/Footer';
import Header from '../components/ai-learning/Header';
import BrandedSelect from '../components/ui/BrandedSelect';
import { useAuth } from '../context/AuthContext';
import { getStoredUser, hasAnyUserRole } from '../utils/auth';

const targetOptions = ['IELTS', 'TOEIC'];
const IELTS_TARGET_SCORES = Array.from({ length: 19 }, (_, index) => (index / 2).toFixed(1));
const TOEIC_TARGET_SCORES = Array.from({ length: 197 }, (_, index) => String(10 + index * 5));

const TABS = [
  { id: 'account', label: 'Tài khoản' },
  { id: 'linked', label: 'Tài khoản liên kết' },
];

// ── Facebook logo SVG ──────────────────────────────────────────────────────
const FacebookIcon = () => (
  <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true" fill="#1877F2">
    <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
  </svg>
);

// ── Google logo SVG ────────────────────────────────────────────────────────
const GoogleIcon = () => (
  <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
    <path fill="#4285F4" d="M23.745 12.27c0-.79-.07-1.54-.19-2.27h-11.3v4.51h6.47c-.29 1.48-1.14 2.73-2.4 3.58v3h3.86c2.26-2.09 3.56-5.17 3.56-8.82z" />
    <path fill="#34A853" d="M12.255 24c3.24 0 5.95-1.08 7.93-2.91l-3.86-3c-1.08.72-2.45 1.16-4.07 1.16-3.13 0-5.78-2.11-6.73-4.96h-3.98v3.09C3.515 21.3 7.565 24 12.255 24z" />
    <path fill="#FBBC05" d="M5.525 14.29c-.25-.72-.38-1.49-.38-2.29s.14-1.57.38-2.29V6.62h-3.98a11.86 11.86 0 000 10.76l3.98-3.09z" />
    <path fill="#EA4335" d="M12.255 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C18.205 1.19 15.495 0 12.255 0c-4.69 0-8.74 2.7-10.71 6.62l3.98 3.09c.95-2.85 3.6-4.96 6.73-4.96z" />
  </svg>
);

// ── Avatar component ───────────────────────────────────────────────────────
function UserAvatar({ name, avatarUrl, size = 'lg' }) {
  const initials = name
    ? name.split(' ').map((w) => w[0]).slice(-2).join('').toUpperCase()
    : 'U';
  const sizeClass = size === 'lg' ? 'h-16 w-16 text-xl' : 'h-10 w-10 text-sm';
  if (avatarUrl) {
    return (
      <img
        src={avatarUrl}
        alt={name}
        className={`${sizeClass} rounded-full object-cover ring-2 ring-[#e5e7eb]`}
      />
    );
  }
  return (
    <div className={`${sizeClass} flex items-center justify-center rounded-full bg-[#8a0018] font-bold text-white`}>
      {initials}
    </div>
  );
}

// ── Section wrapper ─────────────────────────────────────────────────────────
function Section({ title, description, children }) {
  return (
    <div className="rounded-xl border border-[#e5e7eb] bg-white p-6">
      <div className="mb-5 border-b border-[#f0f0f0] pb-4">
        <h2 className="text-base font-semibold text-[#1a1c1c]">{title}</h2>
        {description && <p className="mt-0.5 text-sm text-[#6a5553]">{description}</p>}
      </div>
      {children}
    </div>
  );
}

// ── Input field ─────────────────────────────────────────────────────────────
function Field({ label, children, required }) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-[#3a3232]">
        {label}{required && <span className="ml-0.5 text-[#8a0018]">*</span>}
      </label>
      {children}
    </div>
  );
}

function TextInput({ icon: Icon, disabled, ...props }) {
  return (
    <div className="relative">
      {Icon && <Icon className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9a8b8a]" />}
      <input
        className={`w-full rounded-lg border py-2.5 text-sm outline-none transition ${
          Icon ? 'pl-9' : 'pl-3'
        } pr-3 ${
          disabled
            ? 'border-[#ebebeb] bg-[#f9f9f9] text-[#9a8b8a] cursor-not-allowed'
            : 'border-[#e5e7eb] bg-white text-[#1a1c1c] focus:border-[#8a0018] focus:ring-1 focus:ring-[#8a0018]/20'
        }`}
        disabled={disabled}
        {...props}
      />
    </div>
  );
}

// ── Notification banner ─────────────────────────────────────────────────────
function Notif({ type, message, onClose }) {
  if (!message) return null;
  const styles = {
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
    error: 'border-rose-200 bg-rose-50 text-rose-800',
  };
  return (
    <div className={`flex items-start gap-3 rounded-lg border px-4 py-3 text-sm ${styles[type]}`}>
      {type === 'success' ? <Check className="mt-0.5 h-4 w-4 shrink-0" /> : <X className="mt-0.5 h-4 w-4 shrink-0" />}
      <span className="flex-1">{message}</span>
      <button onClick={onClose} className="opacity-50 hover:opacity-100" type="button"><X className="h-3.5 w-3.5" /></button>
    </div>
  );
}

// ── Save button ─────────────────────────────────────────────────────────────
function SaveBtn({ loading, label = 'Lưu thay đổi' }) {
  return (
    <button
      className="inline-flex items-center gap-2 rounded-lg bg-[#8a0018] px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#6b0013] disabled:opacity-60"
      disabled={loading}
      type="submit"
    >
      <Save className="h-4 w-4" />
      {loading ? 'Đang lưu...' : label}
    </button>
  );
}

// ── Tab: Tài khoản ──────────────────────────────────────────────────────────
function AccountTab({ user, onUserUpdate, onboarding }) {
  const navigate = useNavigate();
  const { updateUser } = useAuth();
  const [formData, setFormData] = useState({
    fullName: user?.fullName || '',
    phoneNumber: user?.phoneNumber || '',
    targetExam: user?.targetExam || 'IELTS',
    targetScore: user?.targetScore || '',
    studyGoal: user?.studyGoal || '',
  });
  const [infoMsg, setInfoMsg] = useState({ type: '', text: '' });
  const [infoLoading, setInfoLoading] = useState(false);

  // Password state
  const [pwForm, setPwForm] = useState({ current: '', next: '', confirm: '' });
  const [showPw, setShowPw] = useState({ current: false, next: false, confirm: false });
  const [pwMsg, setPwMsg] = useState({ type: '', text: '' });
  const [pwLoading, setPwLoading] = useState(false);

  // 2FA state
  const [twoFA, setTwoFA] = useState(false);

  // Avatar state
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || null);
  const fileRef = useRef();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
      ...(name === 'targetExam' ? { targetScore: '' } : {}),
    }));
    setInfoMsg({ type: '', text: '' });
  };

  const handleInfoSubmit = async (e) => {
    e.preventDefault();
    if (!formData.fullName.trim()) {
      setInfoMsg({ type: 'error', text: 'Vui lòng nhập họ và tên.' });
      return;
    }
    if (!formData.targetScore) {
      setInfoMsg({ type: 'error', text: 'Vui lòng chọn điểm mục tiêu.' });
      return;
    }
    setInfoLoading(true);
    try {
      await updateCurrentUser(formData);
      const res = await getCurrentUser();
      updateUser(res.data);
      onUserUpdate?.(res.data);
      setInfoMsg({ type: 'success', text: 'Thông tin đã được cập nhật.' });
      if (onboarding) {
        navigate('/home', { replace: true });
      }
    } catch (err) {
      setInfoMsg({ type: 'error', text: err.response?.data?.message || 'Không thể lưu. Vui lòng thử lại.' });
    } finally {
      setInfoLoading(false);
    }
  };

  const handlePwSubmit = (e) => {
    e.preventDefault();
    if (!pwForm.current) { setPwMsg({ type: 'error', text: 'Nhập mật khẩu hiện tại.' }); return; }
    if (pwForm.next.length < 8) { setPwMsg({ type: 'error', text: 'Mật khẩu mới tối thiểu 8 ký tự.' }); return; }
    if (pwForm.next !== pwForm.confirm) { setPwMsg({ type: 'error', text: 'Mật khẩu xác nhận không khớp.' }); return; }
    setPwLoading(true);
    setTimeout(() => {
      setPwLoading(false);
      setPwMsg({ type: 'success', text: 'Mật khẩu đã được cập nhật thành công.' });
      setPwForm({ current: '', next: '', confirm: '' });
    }, 800);
  };

  const handleAvatarChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 1024 * 1024) {
      alert('Ảnh tối đa 1 MB.');
      return;
    }
    const reader = new FileReader();
    reader.onload = (ev) => {
      setAvatarUrl(ev.target.result);
    };
    reader.readAsDataURL(file);
  };

  const TogglePw = ({ field }) => (
    <button
      type="button"
      className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a8b8a] hover:text-[#6a5553]"
      onClick={() => setShowPw((s) => ({ ...s, [field]: !s[field] }))}
    >
      {showPw[field] ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
    </button>
  );

  return (
    <div className="space-y-5">
      {/* ── Thông tin cá nhân ── */}
      <Section title="Thông tin cá nhân" description="Cập nhật tên, số điện thoại và mục tiêu học tập của bạn.">
        <form onSubmit={handleInfoSubmit} className="space-y-5">
          <Notif type={infoMsg.type} message={infoMsg.text} onClose={() => setInfoMsg({ type: '', text: '' })} />

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Họ và tên" required>
              <TextInput icon={User} name="fullName" value={formData.fullName} onChange={handleChange} placeholder="Nguyễn Văn A" />
            </Field>
            <Field label="Địa chỉ email">
              <TextInput icon={Mail} value={user?.email || ''} disabled />
            </Field>
            <Field label="Số điện thoại">
              <TextInput icon={Phone} name="phoneNumber" value={formData.phoneNumber} onChange={handleChange} placeholder="09xx xxx xxx" />
            </Field>
            <Field label="Mục tiêu học">
              <div className="relative">
                <BookOpen className="pointer-events-none absolute left-3 top-1/2 z-10 h-4 w-4 -translate-y-1/2 text-[#9a8b8a]" />
                <BrandedSelect
                  buttonClassName="rounded-lg border-[#e5e7eb] bg-white pl-9 pr-3 py-2.5 text-sm"
                  name="targetExam"
                  onChange={handleChange}
                  options={targetOptions}
                  value={formData.targetExam}
                />
              </div>
            </Field>
            <Field label="Điểm mục tiêu">
              <div className="relative">
                <Target className="pointer-events-none absolute left-3 top-1/2 z-10 h-4 w-4 -translate-y-1/2 text-[#9a8b8a]" />
                <BrandedSelect
                  buttonClassName="rounded-lg border-[#e5e7eb] bg-white pl-9 pr-3 py-2.5 text-sm"
                  menuClassName="max-h-72 overflow-y-auto"
                  name="targetScore"
                  onChange={handleChange}
                  options={formData.targetExam === 'TOEIC' ? TOEIC_TARGET_SCORES : IELTS_TARGET_SCORES}
                  placeholder={formData.targetExam === 'TOEIC' ? 'Chọn điểm TOEIC' : 'Chọn band IELTS'}
                  value={formData.targetScore}
                />
              </div>
            </Field>
            <Field label="Ghi chú mục tiêu">
              <textarea
                name="studyGoal"
                value={formData.studyGoal}
                onChange={handleChange}
                rows={2}
                placeholder="Bạn muốn đạt mục tiêu trong bao lâu..."
                className="w-full resize-none rounded-lg border border-[#e5e7eb] bg-white px-3 py-2.5 text-sm text-[#1a1c1c] outline-none transition focus:border-[#8a0018] focus:ring-1 focus:ring-[#8a0018]/20"
              />
            </Field>
          </div>

          <div className="pt-1">
            <SaveBtn loading={infoLoading} />
          </div>
        </form>
      </Section>

      {/* ── Ảnh hồ sơ ── */}
      <Section title="Ảnh hồ sơ" description="Kích thước tối đa 1 MB. Định dạng hỗ trợ: JPG, GIF hoặc PNG.">
        <div className="flex items-center gap-5">
          <div className="relative">
            <UserAvatar name={formData.fullName || user?.fullName} avatarUrl={avatarUrl} size="lg" />
            <button
              type="button"
              onClick={() => fileRef.current?.click()}
              className="absolute -bottom-1 -right-1 flex h-6 w-6 items-center justify-center rounded-full bg-white border border-[#e5e7eb] shadow-sm hover:bg-gray-50"
            >
              <Camera className="h-3.5 w-3.5 text-[#6a5553]" />
            </button>
            <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={handleAvatarChange} />
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => fileRef.current?.click()}
              className="rounded-lg border border-[#e5e7eb] bg-white px-4 py-2 text-sm font-medium text-[#1a1c1c] hover:bg-gray-50 transition"
            >
              Tải ảnh lên
            </button>
            {avatarUrl && (
              <button
                type="button"
                onClick={() => setAvatarUrl(null)}
                className="rounded-lg border border-[#e5e7eb] bg-white px-4 py-2 text-sm font-medium text-rose-600 hover:bg-rose-50 transition"
              >
                Xóa ảnh
              </button>
            )}
          </div>
        </div>
      </Section>

      {/* ── Đổi mật khẩu ── */}
      <Section title="Đổi mật khẩu" description="Hãy thường xuyên thay đổi mật khẩu để bảo mật tài khoản.">
        <form onSubmit={handlePwSubmit} className="space-y-4">
          <Notif type={pwMsg.type} message={pwMsg.text} onClose={() => setPwMsg({ type: '', text: '' })} />

          <Field label="Mật khẩu hiện tại" required>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9a8b8a]" />
              <input
                type={showPw.current ? 'text' : 'password'}
                value={pwForm.current}
                onChange={(e) => setPwForm((s) => ({ ...s, current: e.target.value }))}
                className="w-full rounded-lg border border-[#e5e7eb] bg-white py-2.5 pl-9 pr-10 text-sm outline-none transition focus:border-[#8a0018] focus:ring-1 focus:ring-[#8a0018]/20"
                placeholder="••••••••"
              />
              <TogglePw field="current" />
            </div>
          </Field>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Mật khẩu mới" required>
              <div className="relative">
                <input
                  type={showPw.next ? 'text' : 'password'}
                  value={pwForm.next}
                  onChange={(e) => setPwForm((s) => ({ ...s, next: e.target.value }))}
                  className="w-full rounded-lg border border-[#e5e7eb] bg-white py-2.5 pl-3 pr-10 text-sm outline-none transition focus:border-[#8a0018] focus:ring-1 focus:ring-[#8a0018]/20"
                  placeholder="Từ 8 ký tự"
                />
                <TogglePw field="next" />
              </div>
            </Field>
            <Field label="Xác nhận mật khẩu mới" required>
              <div className="relative">
                <input
                  type={showPw.confirm ? 'text' : 'password'}
                  value={pwForm.confirm}
                  onChange={(e) => setPwForm((s) => ({ ...s, confirm: e.target.value }))}
                  className="w-full rounded-lg border border-[#e5e7eb] bg-white py-2.5 pl-3 pr-10 text-sm outline-none transition focus:border-[#8a0018] focus:ring-1 focus:ring-[#8a0018]/20"
                  placeholder="Nhập lại mật khẩu mới"
                />
                <TogglePw field="confirm" />
              </div>
            </Field>
          </div>

          {pwForm.next && pwForm.next.length < 8 && (
            <p className="flex items-center gap-1.5 text-xs text-rose-600">
              <X className="h-3.5 w-3.5" /> Trong khoảng từ 8 đến 72 ký tự
            </p>
          )}

          <div className="pt-1">
            <SaveBtn loading={pwLoading} label="Thay đổi mật khẩu" />
          </div>
        </form>
      </Section>

      {/* ── Xác thực hai yếu tố ── */}
      <Section
        title="Xác thực hai yếu tố"
        description="Xác thực hai yếu tố cũng cường thêm một lớp bảo mật cho tài khoản của bạn. Mỗi lần đăng nhập vào Coursera, bạn sẽ cần cung cấp một mã xác nhận."
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-[#1a1c1c]">
              {twoFA ? 'Đang bật' : 'Đang tắt'}
            </span>
            <span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[10px] font-semibold text-amber-700">
              Sắp có
            </span>
          </div>
          <button
            type="button"
            onClick={() => setTwoFA((v) => !v)}
            className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none ${
              twoFA ? 'bg-[#8a0018]' : 'bg-gray-200'
            }`}
            role="switch"
            aria-checked={twoFA}
          >
            <span className={`pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow-sm ring-0 transition-transform duration-200 ${twoFA ? 'translate-x-5' : 'translate-x-0'}`} />
          </button>
        </div>
      </Section>
    </div>
  );
}

// ── Tab: Tài khoản liên kết ─────────────────────────────────────────────────
function LinkedTab() {
  const [linked, setLinked] = useState({ google: false, facebook: false });
  const [msg, setMsg] = useState('');

  const toggle = (provider) => {
    setLinked((s) => ({ ...s, [provider]: !s[provider] }));
    setMsg(
      linked[provider]
        ? `Đã hủy liên kết tài khoản ${provider === 'google' ? 'Google' : 'Facebook'}.`
        : `Đã liên kết tài khoản ${provider === 'google' ? 'Google' : 'Facebook'}.`,
    );
    setTimeout(() => setMsg(''), 3000);
  };

  const providers = [
    {
      id: 'facebook',
      name: 'Facebook',
      icon: <FacebookIcon />,
    },
    {
      id: 'google',
      name: 'Google',
      icon: <GoogleIcon />,
    },
  ];

  return (
    <div className="space-y-5">
      <div className="rounded-xl border border-[#e5e7eb] bg-white p-6">
        <div className="mb-5 border-b border-[#f0f0f0] pb-4">
          <h2 className="text-base font-semibold text-[#1a1c1c]">Tài khoản đã liên kết</h2>
          <p className="mt-0.5 text-sm text-[#6a5553]">
            Kích hoạt đăng nhập bằng một cú nhấp chuột và nhận các đề xuất khóa học được cá nhân hóa hơn.
          </p>
        </div>

        {msg && (
          <div className="mb-4 flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-sm text-amber-800">
            <Check className="h-4 w-4 shrink-0" />
            {msg}
          </div>
        )}

        <div className="divide-y divide-[#f0f0f0]">
          {providers.map((p) => (
            <div key={p.id} className="flex items-center justify-between py-4 first:pt-0 last:pb-0">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-full border border-[#e5e7eb] bg-white">
                  {p.icon}
                </div>
                <span className="text-sm font-medium text-[#1a1c1c]">{p.name}</span>
              </div>
              <button
                type="button"
                onClick={() => toggle(p.id)}
                className={`rounded-lg border px-4 py-2 text-sm font-medium transition ${
                  linked[p.id]
                    ? 'border-[#e5e7eb] bg-white text-[#1a1c1c] hover:bg-gray-50'
                    : 'border-[#8a0018] bg-white text-[#8a0018] hover:bg-rose-50'
                }`}
              >
                {linked[p.id] ? 'Hủy liên kết' : 'Liên kết'}
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

const STAFF_ROLES = ['TEACHER', 'TRAINING_MANAGER', 'CONTENT_MANAGER', 'MANAGER', 'ADMIN'];

// ── Main page ───────────────────────────────────────────────────────────────
const CompleteProfile = () => {
  const [activeTab, setActiveTab] = useState('account');
  const { user, updateUser } = useAuth();
  const navigate = useNavigate();

  const isStaff = hasAnyUserRole(user, STAFF_ROLES);
  const onboarding = Boolean(user && !user.profileCompleted);

  useEffect(() => {
    if (!user && getStoredUser()) {
      updateUser(getStoredUser());
    }
  }, [updateUser, user]);

  useEffect(() => {
    if (user && isStaff) {
      navigate('/home', { replace: true });
    }
  }, [user, isStaff, navigate]);

  if (isStaff) return null;

  return (
    <div className="min-h-screen bg-[#f9f9f9] font-['Inter'] text-[#1a1c1c]">
      <Header />
      <motion.main
        className="mx-auto w-full max-w-[860px] px-4 pb-16 pt-8 md:px-6"
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.32, ease: 'easeOut' }}
      >
        {/* Page title */}
        <div className="mb-6 border-b border-[#ebebeb] pb-5">
          <div className="flex items-center gap-3">
            <span className="h-7 w-1 shrink-0 rounded-full bg-[#8a0018]" />
            <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#1a1c1c] md:text-3xl">
              {onboarding ? 'Hoàn thiện hồ sơ học tập' : 'Cài đặt tài khoản'}
            </h1>
          </div>
          <p className="mt-2 pl-4 text-sm text-[#6a5553]">
            {onboarding ? 'Chọn kỳ thi và điểm mục tiêu để hoàn tất lộ trình học cá nhân của bạn.' : 'Cập nhật thông tin về bạn và cách người khác nhìn thấy bạn.'}
          </p>
        </div>

        {/* Tabs */}
        <div className="mb-6 flex gap-1 border-b border-[#ebebeb]">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={`relative px-4 py-2.5 text-sm font-medium transition-colors ${
                activeTab === tab.id
                  ? 'text-[#8a0018] after:absolute after:bottom-0 after:left-0 after:h-0.5 after:w-full after:rounded-t-full after:bg-[#8a0018]'
                  : 'text-[#6a5553] hover:text-[#1a1c1c]'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Tab content */}
        <motion.div
          key={activeTab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.22, ease: 'easeOut' }}
        >
          {activeTab === 'account' && (
            <AccountTab user={user} onUserUpdate={updateUser} onboarding={onboarding} />
          )}
          {activeTab === 'linked' && <LinkedTab />}
        </motion.div>
      </motion.main>
      <Footer />
    </div>
  );
};

export default CompleteProfile;
