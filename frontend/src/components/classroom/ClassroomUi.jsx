import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import BrandLoadingState from '../ui/BrandLoadingState';
import {
  Calendar,
  Clock,
  MapPin,
  Video,
  User as UserIcon,
  CheckCircle2,
  XCircle,
  AlertCircle,
  AlertTriangle,
  ChevronRight,
  ChevronLeft,
  Filter,
  Search,
  Plus,
  Trash2,
  Edit3,
  ExternalLink,
  BookOpen,
  FileText,
  Award,
  DollarSign,
  Users,
  TrendingUp,
  ArrowRight,
  Lock,
  Unlock,
  MoreVertical,
  Info,
  Check,
  X,
  ChevronDown,
  Play,
  RefreshCw,
} from 'lucide-react';

// ==========================================
// 1. LOADING & SKELETON STATES
// ==========================================

export const ClassroomLoadingState = ({ message = 'Đang tải dữ liệu...' }) => (
  <BrandLoadingState message={message} />
);

export const LoadingSkeleton = ({ count = 3, type = 'card' }) => {
  const items = Array.from({ length: count });

  if (type === 'list') {
    return (
      <div className="space-y-4 w-full animate-pulse">
        {items.map((_, i) => (
          <div key={i} className="flex items-center justify-between rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 rounded-2xl bg-gray-100"></div>
              <div className="space-y-2">
                <div className="h-4 w-48 rounded bg-gray-200"></div>
                <div className="h-3 w-32 rounded bg-gray-100"></div>
              </div>
            </div>
            <div className="h-8 w-24 rounded-xl bg-gray-100"></div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 w-full animate-pulse">
      {items.map((_, i) => (
        <div key={i} className="flex flex-col rounded-xl border border-[#e5e7eb] bg-white p-6 shadow-sm">
          <div className="h-4 w-1/3 rounded bg-gray-200 mb-4"></div>
          <div className="h-6 w-3/4 rounded bg-gray-200 mb-2"></div>
          <div className="h-4 w-1/2 rounded bg-gray-100 mb-6"></div>
          <div className="space-y-2 mb-6">
            <div className="h-3 w-full rounded bg-gray-100"></div>
            <div className="h-3 w-5/6 rounded bg-gray-100"></div>
          </div>
          <div className="mt-auto pt-4 border-t border-gray-50 flex justify-between items-center">
            <div className="h-4 w-20 rounded bg-gray-100"></div>
            <div className="h-8 w-24 rounded-xl bg-gray-200"></div>
          </div>
        </div>
      ))}
    </div>
  );
};

// ==========================================
// 2. ERROR & EMPTY STATES
// ==========================================

export const ClassroomErrorState = ({ message, onRetry }) => (
  <div className="flex min-h-[400px] flex-1 flex-col items-center justify-center rounded-xl border border-[#f0d4d7] bg-[#fffafb] px-6 py-16 text-center shadow-sm">
    <div className="flex h-16 w-16 items-center justify-center rounded-full bg-[#fff1f3] text-[#93000a]">
      <AlertCircle className="h-8 w-8" />
    </div>
    <h3 className="mt-6 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Đã xảy ra lỗi</h3>
    <p className="mt-3 max-w-md text-sm leading-7 text-[#584140]">{message || 'Không thể kết nối tới máy chủ. Vui lòng thử lại.'}</p>
    {onRetry ? (
      <button
        className="mt-8 inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-6 py-3.5 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95"
        onClick={onRetry}
        type="button"
      >
        <RefreshCw className="h-4 w-4" />
        Thử lại
      </button>
    ) : null}
  </div>
);

export const ClassroomEmptyState = ({
  title,
  description,
  actionLabel,
  actionTo,
  onAction,
  icon: Icon = BookOpen,
}) => (
  <div className="flex min-h-[400px] flex-1 flex-col items-center justify-center rounded-xl border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center shadow-sm">
    <div className="flex h-16 w-16 items-center justify-center rounded-full bg-[#fffafb] border border-[#dfbfbd]/30 text-[#730014]">
      <Icon className="h-8 w-8" />
    </div>
    <h3 className="mt-6 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{title || 'Danh sách trống'}</h3>
    {description ? <p className="mx-auto mt-3 max-w-lg text-sm leading-7 text-[#584140]">{description}</p> : null}
    {actionLabel && actionTo ? (
      <div className="mt-8">
        <Link
          className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95"
          to={actionTo}
        >
          {actionLabel}
          <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
    ) : null}
    {actionLabel && onAction ? (
      <div className="mt-8">
        <button
          className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white shadow-md transition hover:bg-[#730014] hover:shadow-lg active:scale-95"
          onClick={onAction}
          type="button"
        >
          {actionLabel}
          <ArrowRight className="h-4 w-4" />
        </button>
      </div>
    ) : null}
  </div>
);

// ==========================================
// 3. BADGES & STATUSES
// ==========================================

export const StatusBadge = ({ status }) => {
  const configMap = {
    // Registration Statuses
    PENDING_CONFIRMATION: { text: 'Chờ xác nhận', bg: 'bg-amber-50 border-amber-100 text-amber-700' },
    PENDING_TUITION_PAYMENT: { text: 'Chờ học phí', bg: 'bg-orange-50 border-orange-100 text-orange-700' },
    DEPOSIT_PAID: { text: 'Đã đặt cọc', bg: 'bg-blue-50 border-blue-100 text-blue-700' },
    PARTIALLY_PAID: { text: 'Thanh toán một phần', bg: 'bg-indigo-50 border-indigo-100 text-indigo-700' },
    FULLY_PAID: { text: 'Đã thanh toán đủ', bg: 'bg-emerald-50 border-emerald-100 text-emerald-700' },
    ASSIGNED: { text: 'Đã xếp lớp', bg: 'bg-teal-50 border-teal-100 text-teal-700 font-extrabold' },
    WAITLIST: { text: 'Chờ xếp lớp', bg: 'bg-purple-50 border-purple-100 text-purple-700' },
    REJECTED: { text: 'Từ chối', bg: 'bg-rose-50 border-rose-100 text-rose-700' },
    CANCELLED: { text: 'Đã hủy', bg: 'bg-gray-100 border-gray-200 text-gray-500' },

    // Classroom Offering Statuses
    DRAFT: { text: 'Bản nháp', bg: 'bg-gray-50 border-gray-100 text-gray-600' },
    UPCOMING: { text: 'Sắp khai giảng', bg: 'bg-blue-50 border-blue-100 text-blue-700' },
    OPEN: { text: 'Đang mở đăng ký', bg: 'bg-emerald-50 border-emerald-100 text-emerald-700' },
    FULL: { text: 'Đã đủ chỗ', bg: 'bg-amber-50 border-amber-100 text-amber-700' },
    ACTIVE: { text: 'Đang học', bg: 'bg-rose-50 border-rose-100 text-[#730014] font-extrabold' },
    IN_PROGRESS: { text: 'Đang học', bg: 'bg-rose-50 border-rose-100 text-[#730014] font-extrabold' },
    COMPLETED: { text: 'Đã kết thúc', bg: 'bg-emerald-50 border-emerald-100 text-emerald-700' },
    CLOSED: { text: 'Đã đóng', bg: 'bg-gray-100 border-gray-200 text-gray-500' },

    // Change Request Statuses
    PENDING: { text: 'Chờ duyệt', bg: 'bg-amber-50 border-amber-100 text-amber-700' },
    APPROVED: { text: 'Đã duyệt', bg: 'bg-emerald-50 border-emerald-100 text-emerald-700' },
    APPLIED: { text: 'Đã áp dụng', bg: 'bg-teal-50 border-teal-100 text-teal-700' },

    // Homework Submission Statuses
    NOT_SUBMITTED: { text: 'Chưa nộp', bg: 'bg-amber-50 border-amber-100 text-amber-700' },
    SUBMITTED: { text: 'Đã nộp', bg: 'bg-blue-50 border-blue-100 text-blue-700' },
    GRADED: { text: 'Đã chấm', bg: 'bg-emerald-50 border-emerald-100 text-emerald-700 font-extrabold' },
    OVERDUE: { text: 'Quá hạn', bg: 'bg-rose-50 border-rose-100 text-rose-700 font-bold animate-pulse' },
  };

  const config = configMap[status] || { text: status || 'Đang cập nhật', bg: 'bg-gray-50 border-gray-100 text-gray-600' };

  return (
    <span className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-bold tracking-wide ${config.bg}`}>
      {config.text}
    </span>
  );
};

export const ClassroomTypeBadge = ({ mode }) => {
  const isVirtual = mode === 'VIRTUAL';
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-extrabold tracking-wide ${
      isVirtual
        ? 'bg-purple-50 border-purple-100 text-purple-700'
        : 'bg-rose-50 border-rose-100 text-[#730014]'
    }`}>
      {isVirtual ? <Video className="h-3.5 w-3.5" /> : <MapPin className="h-3.5 w-3.5" />}
      {isVirtual ? 'Trực tuyến' : 'Tại trung tâm'}
    </span>
  );
};

// ==========================================
// 4. NAVIGATION & TAB BARS
// ==========================================

export const ClassroomTabBar = ({ tabs, activeTab, onChange }) => (
  <div className="flex flex-wrap gap-2 border-b border-gray-100 pb-2">
    {tabs.map((tab) => {
      const isActive = activeTab === tab.id;
      return (
        <button
          key={tab.id}
          className={`relative rounded-2xl px-5 py-3 text-sm font-extrabold transition-all duration-200 ${
            isActive
              ? 'bg-[#4b0009] text-white shadow-md shadow-[#4b0009]/10'
              : 'bg-white text-[#584140] hover:bg-[#fff3f4] hover:text-[#730014] border border-gray-100'
          }`}
          onClick={() => onChange(tab.id)}
          type="button"
        >
          {tab.label}
          {tab.badge != null ? (
            <span className={`ml-2 rounded-full px-2 py-0.5 text-[10px] font-bold ${
              isActive ? 'bg-white text-[#4b0009]' : 'bg-[#fff1f3] text-[#730014]'
            }`}>
              {tab.badge}
            </span>
          ) : null}
        </button>
      );
    })}
  </div>
);

// ==========================================
// 5. PAGE HERO & STATS CARDS
// ==========================================

export const PageHero = ({ title, subtitle, stats = [], action }) => (
  <section className="rounded-[28px] border border-gray-200/80 bg-white p-6 md:p-8 shadow-[0_10px_35px_rgba(0,0,0,0.015)] transition-all duration-300 hover:shadow-[0_15px_45px_rgba(75,0,9,0.035)]">
    <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
      <div className="space-y-1.5">
        <div className="flex items-center gap-3">
          <span className="h-6 w-1 shrink-0 rounded-full bg-[#8a0018]" />
          <h1 className="font-['Manrope'] text-xl font-extrabold tracking-tight text-[#1a1c1c] md:text-2xl leading-snug">
            {title}
          </h1>
        </div>
        {subtitle ? (
          <p className="text-xs leading-relaxed text-[#584140] pl-4">{subtitle}</p>
        ) : null}
        {action ? <div className="pt-2 pl-4">{action}</div> : null}
      </div>

      {stats.length ? (
        <div className="flex flex-wrap gap-x-8 gap-y-3 shrink-0">
          {stats.map((stat, idx) => (
            <StatCard key={idx} {...stat} />
          ))}
        </div>
      ) : null}
    </div>
  </section>
);

export const StatCard = ({ label, value, icon: Icon, trend, color = 'rose', glass: _glass }) => {
  const accentMap = {
    rose:    'border-b-[#8a0018] text-[#8a0018]',
    emerald: 'border-b-emerald-600 text-emerald-700',
    blue:    'border-b-blue-500 text-blue-600',
    amber:   'border-b-amber-500 text-amber-700',
    purple:  'border-b-purple-500 text-purple-700',
  };
  const accent = accentMap[color] || accentMap.rose;

  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-xs font-semibold text-[#9a8b8a]">{label}</span>
      <div className="flex items-baseline gap-1.5">
        <span className={`font-['Manrope'] text-2xl font-extrabold ${accent.split(' ')[1]}`}>
          {value}
        </span>
        {trend ? (
          <span className="flex items-center gap-0.5 text-[10px] font-semibold text-emerald-600">
            <TrendingUp className="h-3 w-3" />{trend}
          </span>
        ) : null}
      </div>
    </div>
  );
};

// ==========================================
// 6. PROGRESS RING & PROGRESS BAR
// ==========================================

export const ProgressBar = ({ percent, label, showPercent = true, size = 'md' }) => {
  const clampedPercent = Math.min(100, Math.max(0, percent || 0));
  const heightMap = {
    sm: 'h-1.5',
    md: 'h-2.5',
    lg: 'h-4',
  };

  return (
    <div className="w-full">
      {label || showPercent ? (
        <div className="mb-2 flex items-center justify-between text-sm">
          {label ? <span className="font-extrabold text-[#584140]">{label}</span> : <span></span>}
          {showPercent ? <span className="font-extrabold text-[#730014]">{clampedPercent}%</span> : null}
        </div>
      ) : null}
      <div className={`w-full overflow-hidden rounded-full bg-gray-100 ${heightMap[size]}`}>
        <div
          className="h-full rounded-full bg-gradient-to-r from-[#4b0009] to-[#730014] transition-all duration-500 ease-out"
          style={{ width: `${clampedPercent}%` }}
        ></div>
      </div>
    </div>
  );
};

export const ProgressRing = ({ percent, size = 80, strokeWidth = 8, label }) => {
  const clampedPercent = Math.min(100, Math.max(0, percent || 0));
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (clampedPercent / 100) * circumference;

  return (
    <div className="flex flex-col items-center justify-center">
      <div className="relative" style={{ width: size, height: size }}>
        <svg className="h-full w-full -rotate-90">
          {/* Background circle */}
          <circle
            className="text-gray-100"
            strokeWidth={strokeWidth}
            stroke="currentColor"
            fill="transparent"
            r={radius}
            cx={size / 2}
            cy={size / 2}
          />
          {/* Progress circle */}
          <circle
            className="text-[#730014] transition-all duration-500 ease-out"
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            strokeLinecap="round"
            stroke="currentColor"
            fill="transparent"
            r={radius}
            cx={size / 2}
            cy={size / 2}
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">{clampedPercent}%</span>
        </div>
      </div>
      {label ? <span className="mt-2 text-xs font-bold text-[#8b706e] uppercase tracking-wider">{label}</span> : null}
    </div>
  );
};

// ==========================================
// 7. TIMELINE & SCHEDULE CARD
// ==========================================

export const Timeline = ({ children }) => (
  <div className="relative border-l-2 border-[#dfbfbd]/30 pl-6 ml-4 space-y-8 py-2">
    {children}
  </div>
);

export const ScheduleCard = ({
  title,
  subtitle,
  date,
  time,
  mode,
  status,
  teacher,
  location,
  cta,
  active = false,
}) => (
  <div className={`relative rounded-xl border bg-white p-6 shadow-sm transition-all duration-300 hover:shadow-md ${
    active ? 'border-[#730014] ring-2 ring-[#730014]/5' : 'border-[#e5e7eb]'
  }`}>
    {/* Timeline dot connector */}
    <div className={`absolute -left-[33px] top-8 h-4 w-4 rounded-full border-2 bg-white transition-all duration-300 ${
      active ? 'border-[#730014] scale-125 ring-4 ring-[#730014]/10' : 'border-[#dfbfbd]'
    }`}></div>

    <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div className="space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <ClassroomTypeBadge mode={mode} />
          <StatusBadge status={status} />
          {active ? (
            <span className="rounded-full bg-rose-100 px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wider text-[#730014] animate-pulse">
              Đang diễn ra
            </span>
          ) : null}
        </div>

        <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{title}</h3>
        {subtitle ? <p className="text-sm text-[#584140]">{subtitle}</p> : null}

        <div className="grid gap-x-6 gap-y-2 text-sm text-[#584140] sm:grid-cols-2">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-[#8b706e]" />
            <span>{date}</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock className="h-4 w-4 text-[#8b706e]" />
            <span>{time}</span>
          </div>
          {teacher ? (
            <div className="flex items-center gap-2">
              <UserIcon className="h-4 w-4 text-[#8b706e]" />
              <span>Giảng viên: <strong className="text-[#2b2828]">{teacher}</strong></span>
            </div>
          ) : null}
          {location ? (
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-[#8b706e]" />
              <span className="truncate">{location}</span>
            </div>
          ) : null}
        </div>
      </div>

      {cta ? <div className="mt-4 flex flex-shrink-0 items-center md:mt-0">{cta}</div> : null}
    </div>
  </div>
);

// ==========================================
// 8. DRAWER & MODAL DIALOGS
// ==========================================

export const DetailDrawer = ({ isOpen, onClose, title, children }) => {
  const drawerRef = useRef(null);

  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === 'Escape') onClose();
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleEscape);
    }
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm transition-opacity duration-300" onClick={onClose}></div>

      {/* Drawer Panel */}
      <div
        ref={drawerRef}
        className="relative z-10 flex h-full w-full max-w-[550px] flex-col bg-white shadow-2xl transition-transform duration-300 ease-out animate-slide-in"
      >
        <header className="flex items-center justify-between border-b border-gray-100 px-6 py-5">
          <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{title}</h2>
          <button
            className="rounded-full p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition"
            onClick={onClose}
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
        </header>

        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {children}
        </div>
      </div>
    </div>
  );
};

export const ConfirmModal = ({ isOpen, onClose, onConfirm, title, message, confirmLabel = 'Xác nhận', cancelLabel = 'Hủy', danger = false }) => {
  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === 'Escape') onClose();
    };
    if (isOpen) {
      window.addEventListener('keydown', handleEscape);
    }
    return () => window.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose}></div>

      {/* Modal Dialog */}
      <div className="relative z-10 w-full max-w-md rounded-xl border border-gray-100 bg-white p-6 shadow-2xl animate-scale-in">
        <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{title}</h3>
        <p className="mt-3 text-sm leading-6 text-[#584140]">{message}</p>

        <div className="mt-6 flex justify-end gap-3">
          <button
            className="rounded-2xl border border-gray-200 bg-white px-5 py-3 text-sm font-extrabold text-[#584140] transition hover:bg-gray-50 active:scale-95"
            onClick={onClose}
            type="button"
          >
            {cancelLabel}
          </button>
          <button
            className={`rounded-2xl px-5 py-3 text-sm font-extrabold text-white shadow-md transition hover:shadow-lg active:scale-95 ${
              danger ? 'bg-[#93000a] hover:bg-[#b3000f]' : 'bg-[#4b0009] hover:bg-[#730014]'
            }`}
            onClick={onConfirm}
            type="button"
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

// ==========================================
// 9. FILTER & SEARCH BARS
// ==========================================

export const FilterBar = ({ onSearch, searchPlaceholder = 'Tìm kiếm...', filters = [], activeFilters = {}, onFilterChange }) => {
  const [query, setQuery] = useState('');

  const handleSearchSubmit = (e) => {
    event.preventDefault();
    onSearch?.(query);
  };

  return (
    <div className="rounded-xl border border-[#e5e7eb] bg-white p-5 shadow-sm">
      <form className="flex flex-col gap-4 md:flex-row md:items-center" onSubmit={handleSearchSubmit}>
        {onSearch ? (
          <div className="relative flex-1">
            <input
              type="text"
              className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/50 py-3 pl-11 pr-4 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
              placeholder={searchPlaceholder}
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                onSearch?.(e.target.value);
              }}
            />
            <Search className="absolute left-4 top-3.5 h-4.5 w-4.5 text-[#8b706e]" />
          </div>
        ) : null}

        {filters.length ? (
          <div className="flex flex-wrap items-center gap-3">
            {filters.map((filter) => (
              <div key={filter.id} className="flex items-center gap-2">
                <span className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">{filter.label}:</span>
                <select
                  className="rounded-xl border border-gray-200 bg-white px-3 py-2 text-xs font-bold text-[#584140] outline-none focus:border-[#730014]"
                  value={activeFilters[filter.id] || ''}
                  onChange={(e) => onFilterChange?.(filter.id, e.target.value)}
                >
                  <option value="">Tất cả</option>
                  {filter.options.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>
            ))}
          </div>
        ) : null}
      </form>
    </div>
  );
};

// ==========================================
// 10. TUITION STATUS CARD
// ==========================================

export const TuitionStatusCard = ({
  due,
  paid,
  remaining,
  settlementType,
  settlementLabel,
  settlementNote,
  settlementStatus,
}) => {
  const formatPrice = (val) => {
    if (val == null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(val);
  };

  const hasPendingSettlement = settlementType
    && settlementType !== 'NONE'
    && (!settlementStatus || settlementStatus === 'PENDING');
  const isFullyPaid = remaining <= 0 && !hasPendingSettlement;

  return (
    <div className={`rounded-xl border p-6 shadow-sm ${
      isFullyPaid ? 'border-emerald-100 bg-emerald-50/20' : 'border-rose-100 bg-rose-50/10'
    }`}>
      <div className="flex items-center justify-between mb-4">
        <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828] flex items-center gap-2">
          <DollarSign className="h-5 w-5 text-[#730014]" />
          Trạng thái học phí
        </h4>
        <span className={`rounded-full px-3 py-1 text-xs font-extrabold ${
          isFullyPaid ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-[#730014]'
        }`}>
          {hasPendingSettlement
            ? (settlementType === 'NEED_REFUND' ? 'Cần hoàn tiền' : 'Cần xử lý học phí')
            : (isFullyPaid ? 'Đã hoàn thành' : 'Chưa hoàn thành')}
        </span>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl bg-white p-4 border border-gray-100">
          <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Cần thanh toán</p>
          <p className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{formatPrice(due)}</p>
        </div>
        <div className="rounded-2xl bg-white p-4 border border-gray-100">
          <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Đã thanh toán</p>
          <p className="mt-1 font-['Manrope'] text-xl font-extrabold text-emerald-700">{formatPrice(paid)}</p>
        </div>
        <div className="rounded-2xl bg-white p-4 border border-gray-100">
          <p className="text-xs font-bold text-[#8b706e] uppercase tracking-wider">Còn lại</p>
          <p className={`mt-1 font-['Manrope'] text-xl font-extrabold ${remaining > 0 ? 'text-[#730014]' : 'text-gray-500'}`}>
            {formatPrice(Math.max(0, remaining))}
          </p>
        </div>
      </div>

      {settlementType && settlementType !== 'NONE' ? (
        <div className="mt-4 rounded-2xl bg-white border border-[#dfbfbd]/30 p-4">
          <p className="text-xs font-bold text-[#730014] uppercase tracking-wider flex items-center gap-1">
            <Info className="h-3.5 w-3.5" />
            Xử lý học phí dư/thiếu: {settlementLabel}
          </p>
          {settlementNote ? <p className="mt-1 text-sm text-[#584140]">{settlementNote}</p> : null}
        </div>
      ) : null}
    </div>
  );
};

// ==========================================
// 11. CONFLICT PANEL
// ==========================================

export const ConflictPanel = ({ conflictResult }) => {
  if (!conflictResult) return null;

  const hasConflict = conflictResult.hasBlockingConflict || conflictResult.hasConflict || (conflictResult.conflicts && conflictResult.conflicts.length > 0);

  if (!hasConflict) {
    return (
      <div className="rounded-xl border border-emerald-100 bg-emerald-50/20 p-5 flex items-start gap-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100 text-emerald-700 flex-shrink-0">
          <CheckCircle2 className="h-5 w-5" />
        </div>
        <div>
          <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Không phát hiện xung đột</h4>
          <p className="mt-1 text-sm text-emerald-800">Lịch học hoàn toàn hợp lệ, không bị trùng lặp với giáo viên, phòng học hay học viên.</p>
        </div>
      </div>
    );
  }

  const conflicts = conflictResult.conflicts || [];

  return (
    <div className="rounded-xl border border-rose-100 bg-rose-50/20 p-5 space-y-4">
      <div className="flex items-start gap-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-rose-100 text-[#93000a] flex-shrink-0">
          <AlertTriangle className="h-5 w-5" />
        </div>
        <div>
          <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828] text-[#93000a]">Phát hiện xung đột lịch học</h4>
          <p className="mt-1 text-sm text-rose-800">Cảnh báo: Lịch học được đề xuất bị trùng lặp với lịch hiện có.</p>
        </div>
      </div>

      <div className="space-y-2 border-t border-rose-100/30 pt-3">
        {conflicts.map((item, idx) => (
          <div key={idx} className="rounded-2xl bg-white border border-rose-100/50 p-4 text-sm text-[#584140]">
            <p className="font-extrabold text-[#2b2828] flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-[#93000a]"></span>
              Loại xung đột: {item.conflictTypeLabel || item.conflictType}
            </p>
            <p className="mt-1 pl-3.5 text-xs text-[#8b706e]">{item.description}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

// ==========================================
// 12. REQUEST STATUS TIMELINE
// ==========================================

export const RequestStatusTimeline = ({ status, createdAt, reviewedAt, reviewerName, reviewNote }) => {
  const steps = [
    { id: 'SUBMITTED', label: 'Đã gửi yêu cầu', desc: 'Giáo viên gửi đề xuất thay đổi', done: true, date: createdAt },
    { id: 'PENDING', label: 'Chờ duyệt', desc: 'Đang chờ điều phối đào tạo xem xét', done: status !== 'PENDING', date: null },
    {
      id: 'REVIEWED',
      label: status === 'REJECTED' ? 'Bị từ chối' : 'Đã duyệt & áp dụng',
      desc: status === 'REJECTED' ? `Từ chối bởi ${reviewerName || 'điều phối đào tạo'}` : `Phê duyệt bởi ${reviewerName || 'điều phối đào tạo'}`,
      done: ['APPROVED', 'REJECTED', 'APPLIED'].includes(status),
      date: reviewedAt,
      error: status === 'REJECTED',
    },
  ];

  return (
    <div className="space-y-6">
      <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Tiến trình xử lý</h4>
      <div className="relative border-l-2 border-gray-100 pl-6 ml-3 space-y-6 py-1">
        {steps.map((step, idx) => {
          const isError = step.error;
          const isDone = step.done;
          return (
            <div key={idx} className="relative">
              {/* Dot */}
              <div className={`absolute -left-[31px] top-1.5 h-3 w-3 rounded-full border bg-white ${
                isError
                  ? 'border-rose-500 ring-4 ring-rose-100'
                  : isDone
                    ? 'border-emerald-500 ring-4 ring-emerald-100'
                    : 'border-gray-300'
              }`}></div>
              <div>
                <p className={`text-sm font-extrabold ${isError ? 'text-rose-600' : isDone ? 'text-emerald-700' : 'text-gray-500'}`}>
                  {step.label}
                </p>
                <p className="text-xs text-[#8b706e] mt-0.5">{step.desc}</p>
                {step.date ? (
                  <p className="text-[10px] text-gray-400 mt-1">
                    {new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(step.date))}
                  </p>
                ) : null}
              </div>
            </div>
          );
        })}
      </div>

      {status === 'REJECTED' && reviewNote ? (
        <div className="rounded-2xl border border-rose-100 bg-rose-50/20 p-4">
          <p className="text-xs font-bold text-rose-700 uppercase tracking-wider">Lý do từ chối</p>
          <p className="mt-1 text-sm text-[#584140]">{reviewNote}</p>
        </div>
      ) : null}
      {status === 'APPLIED' && reviewNote ? (
        <div className="rounded-2xl border border-emerald-100 bg-emerald-50/20 p-4">
          <p className="text-xs font-bold text-emerald-700 uppercase tracking-wider">Phản hồi duyệt</p>
          <p className="mt-1 text-sm text-[#584140]">{reviewNote}</p>
        </div>
      ) : null}
    </div>
  );
};

// ==========================================
// 13. LEARNER AVATAR ROW & ACTION MENU
// ==========================================

export const LearnerAvatarRow = ({ learners = [], max = 4 }) => {
  const displayLearners = learners.slice(0, max);
  const extra = learners.length - max;

  return (
    <div className="flex items-center -space-x-2 overflow-hidden">
      {displayLearners.map((learner, idx) => {
        const name = learner.studentName || learner.studentEmail || 'H';
        const initial = name.charAt(0).toUpperCase();
        return (
          <div
            key={idx}
            className="inline-flex h-8 w-8 items-center justify-center rounded-full border-2 border-white bg-gradient-to-br from-[#4b0009] to-[#730014] text-[10px] font-extrabold text-white shadow-sm"
            title={name}
          >
            {initial}
          </div>
        );
      })}
      {extra > 0 ? (
        <div className="inline-flex h-8 w-8 items-center justify-center rounded-full border-2 border-white bg-gray-100 text-[10px] font-extrabold text-gray-500 shadow-sm">
          +{extra}
        </div>
      ) : null}
    </div>
  );
};

export const ActionMenu = ({ actions = [] }) => {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  if (!actions.length) return null;

  return (
    <div ref={menuRef} className="relative inline-block text-left">
      <button
        className="rounded-full p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600 transition"
        onClick={() => setIsOpen(!isOpen)}
        type="button"
      >
        <MoreVertical className="h-5 w-5" />
      </button>

      {isOpen ? (
        <div className="absolute right-0 mt-2 z-20 w-48 origin-top-right rounded-2xl border border-gray-100 bg-white p-1.5 shadow-xl animate-scale-in">
          {actions.map((act, idx) => (
            <button
              key={idx}
              className={`flex w-full items-center gap-2 rounded-xl px-3 py-2.5 text-left text-xs font-bold transition ${
                act.danger
                  ? 'text-[#93000a] hover:bg-rose-50'
                  : 'text-[#584140] hover:bg-[#fff3f4] hover:text-[#730014]'
              }`}
              onClick={() => {
                setIsOpen(false);
                act.onClick();
              }}
              type="button"
            >
              {act.icon ? <act.icon className="h-4 w-4" /> : null}
              {act.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
};

// ==========================================
// 14. LARK JOIN BUTTON (BACKWARD COMPATIBILITY)
// ==========================================

export const LarkJoinButton = ({ url, label = 'Tham gia Lark', className = '', onBlocked, onClick }) => {
  const handleClick = () => {
    if (onClick) {
      onClick();
      return;
    }
    const popup = window.open(url, '_blank', 'noopener,noreferrer');
    if (!popup) {
      onBlocked?.('Trình duyệt đã chặn cửa sổ mới. Hãy cho phép popup hoặc mở liên kết thủ công.');
    }
  };

  if (!url) return null;

  return (
    <div className="flex flex-wrap items-center gap-3">
      <button
        className={`rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] ${className}`}
        onClick={handleClick}
        type="button"
      >
        {label}
      </button>
      <a className="text-sm font-semibold text-[#730014] underline" href={url} rel="noreferrer" target="_blank">
        Mở liên kết thủ công
      </a>
    </div>
  );
};
