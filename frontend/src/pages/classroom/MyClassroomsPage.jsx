import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  BookOpen,
  Calendar,
  Clock,
  User,
  DollarSign,
  Award,
  ArrowRight,
  ChevronRight,
  Info,
  CheckCircle2,
  AlertTriangle,
  Lock,
  MapPin,
  Video,
  TrendingUp,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  LoadingSkeleton,
  ClassroomTabBar,
  ClassroomTypeBadge,
  StatusBadge,
} from '../../components/classroom/ClassroomUi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDate,
  formatClassroomPrice,
  formatTuitionSettlement,
} from '../../utils/classroomHelpers';
import { getStoredUser, hasAccessToken } from '../../utils/auth';

// ─── Tab definitions ────────────────────────────────────────────────────────
const learnerTabs = [
  { id: 'all', label: 'Tất cả' },
  { id: 'active', label: 'Đang học' },
  { id: 'upcoming', label: 'Sắp khai giảng' },
  { id: 'pending', label: 'Chờ xếp lớp' },
  { id: 'completed', label: 'Đã kết thúc' },
];

// ─── Status helpers ──────────────────────────────────────────────────────────
const startOfToday = () => {
  const t = new Date(); t.setHours(0, 0, 0, 0); return t;
};
const parseDate = (v) => {
  if (!v) return null;
  const d = new Date(v); if (Number.isNaN(d.getTime())) return null;
  d.setHours(0, 0, 0, 0); return d;
};
const daysUntil = (dateStr) => {
  const d = parseDate(dateStr); if (!d) return null;
  return Math.round((d - startOfToday()) / 86400000);
};

const isActiveClass = (item) => {
  if (item.classroomStatus === 'ACTIVE') {
    const s = parseDate(item.startDate); return !s || s <= startOfToday();
  }
  return ['IN_PROGRESS', 'OPEN'].includes(item.classroomStatus);
};
const isUpcomingClass = (item) => {
  if (['UPCOMING', 'DRAFT'].includes(item.classroomStatus)) return true;
  if (item.classroomStatus === 'ACTIVE') {
    const s = parseDate(item.startDate); return Boolean(s && s > startOfToday());
  }
  return false;
};
const isCompletedClass = (item) => ['COMPLETED', 'CANCELLED', 'CLOSED'].includes(item.classroomStatus);
const isPendingClass = (item) =>
  ['PENDING_CONFIRMATION', 'PENDING_TUITION_PAYMENT', 'WAITLIST', 'DEPOSIT_PAID', 'PARTIALLY_PAID'].includes(item.registrationStatus);

// ─── Derive a concise status descriptor for the card ─────────────────────────
const getCardStatusInfo = (classroom) => {
  const days = daysUntil(classroom.startDate);
  if (isActiveClass(classroom)) {
    const end = daysUntil(classroom.endDate);
    if (end != null && end > 0) return { text: `Còn ${end} ngày`, color: 'text-emerald-700 bg-emerald-50 border-emerald-100' };
    return { text: 'Đang diễn ra', color: 'text-emerald-700 bg-emerald-50 border-emerald-100' };
  }
  if (isUpcomingClass(classroom)) {
    if (days != null && days > 0) return { text: `Khai giảng sau ${days} ngày`, color: 'text-amber-700 bg-amber-50 border-amber-100' };
    if (days === 0) return { text: 'Khai giảng hôm nay!', color: 'text-[#730014] bg-rose-50 border-rose-100' };
  }
  if (isCompletedClass(classroom)) return { text: 'Đã kết thúc', color: 'text-gray-500 bg-gray-50 border-gray-100' };
  if (isPendingClass(classroom)) return { text: 'Chờ xác nhận', color: 'text-blue-700 bg-blue-50 border-blue-100' };
  return { text: formatClassroomDate(classroom.startDate), color: 'text-gray-500 bg-gray-50 border-gray-100' };
};

// ─── Card accent color based on class lifecycle ───────────────────────────────
const getCardAccent = (classroom) => {
  if (isActiveClass(classroom)) return 'border-l-4 border-l-emerald-500';
  if (isUpcomingClass(classroom)) return 'border-l-4 border-l-amber-400';
  if (isCompletedClass(classroom)) return 'border-l-4 border-l-gray-200';
  if (isPendingClass(classroom)) return 'border-l-4 border-l-blue-300';
  return 'border-l-4 border-l-gray-100';
};

// ─── Component ────────────────────────────────────────────────────────────────
export default function MyClassroomsPage() {
  const [activeTab, setActiveTab] = useState('all');
  const [classrooms, setClassrooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const isAuthenticated = Boolean(hasAccessToken() && getStoredUser());

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getMyClassrooms();
      setClassrooms(data);
    } catch (err) {
      setClassrooms([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách lớp của bạn.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isAuthenticated) { setLoading(false); return; }
    loadClassrooms();
  }, [isAuthenticated]);

  const filteredClassrooms = useMemo(() => classrooms.filter((item) => {
    if (activeTab === 'active') return isActiveClass(item);
    if (activeTab === 'upcoming') return isUpcomingClass(item);
    if (activeTab === 'completed') return isCompletedClass(item);
    if (activeTab === 'pending') return isPendingClass(item);
    return true;
  }), [activeTab, classrooms]);

  // Summary stats for mini banner
  const counts = useMemo(() => ({
    active: classrooms.filter(isActiveClass).length,
    upcoming: classrooms.filter(isUpcomingClass).length,
    pending: classrooms.filter(isPendingClass).length,
    completed: classrooms.filter(isCompletedClass).length,
  }), [classrooms]);

  return (
    <LearnerPageShell
      description="Theo dõi lớp đang học, lịch buổi học, bài tập và điểm số tại một nơi."
      title="Lớp của tôi"
    >
      {!isAuthenticated ? (
        <ClassroomEmptyState
          icon={Lock}
          actionLabel="Đăng nhập"
          actionTo="/login"
          description="Bạn cần đăng nhập để xem các lớp đã ghi danh."
          title="Chưa đăng nhập"
        />
      ) : loading ? (
        <LoadingSkeleton count={4} type="list" />
      ) : error ? (
        <ClassroomErrorState message={error} onRetry={loadClassrooms} />
      ) : !classrooms.length ? (
        <ClassroomEmptyState
          icon={BookOpen}
          actionLabel="Xem danh mục lớp học"
          actionTo="/opening-schedule"
          description="Bạn chưa đăng ký lớp học nào. Khám phá các khóa học IELTS / TOEIC ngay bây giờ."
          title="Chưa có lớp học"
        />
      ) : (
        <div className="space-y-6">

          {/* Snapshot banner */}
          {classrooms.length > 0 && (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <SnapCard label="Đang học" value={counts.active} accent="emerald" icon={<BookOpen className="h-4 w-4" />} />
              <SnapCard label="Sắp khai giảng" value={counts.upcoming} accent="amber" icon={<Calendar className="h-4 w-4" />} />
              <SnapCard label="Chờ xác nhận" value={counts.pending} accent="blue" icon={<Clock className="h-4 w-4" />} />
              <SnapCard label="Đã kết thúc" value={counts.completed} accent="gray" icon={<Award className="h-4 w-4" />} />
            </div>
          )}

          {/* Filter Tabs */}
          <ClassroomTabBar activeTab={activeTab} onChange={setActiveTab} tabs={learnerTabs} />

          {/* Classroom List */}
          {filteredClassrooms.length ? (
            <div className="space-y-4">
              {filteredClassrooms.map((classroom, idx) => (
                <motion.div
                  key={classroom.id}
                  initial={{ opacity: 0, y: 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.32, delay: Math.min(idx * 0.07, 0.42), ease: 'easeOut' }}
                >
                  <ClassroomCard classroom={classroom} />
                </motion.div>
              ))}
            </div>
          ) : (
            <ClassroomEmptyState
              description="Không có lớp học nào khớp với bộ lọc này."
              title="Không có lớp phù hợp"
            />
          )}
        </div>
      )}
    </LearnerPageShell>
  );
}

// ─── Snapshot stat mini-card ─────────────────────────────────────────────────
function SnapCard({ label, value, accent, icon }) {
  const colors = {
    emerald: 'border-emerald-100 bg-emerald-50/60 text-emerald-800',
    amber: 'border-amber-100 bg-amber-50/60 text-amber-800',
    blue: 'border-blue-100 bg-blue-50/60 text-blue-800',
    gray: 'border-gray-100 bg-gray-50/60 text-gray-600',
  };
  return (
    <div className={`rounded-lg border px-4 py-3 ${colors[accent]}`}>
      <div className="flex items-center justify-between mb-1">
        <span className="text-[10px] font-semibold uppercase tracking-wider opacity-60">{label}</span>
        <span className="opacity-40">{icon}</span>
      </div>
      <p className="font-['Manrope'] text-2xl font-extrabold">{value}</p>
    </div>
  );
}

// ─── Classroom card ───────────────────────────────────────────────────────────
function ClassroomCard({ classroom }) {
  const isClassActive = isActiveClass(classroom);
  const isClassCompleted = isCompletedClass(classroom);
  const isWaiting = !classroom.hasClassAccess;
  const isVirtual = classroom.deliveryMode === 'VIRTUAL';

  const tuitionDue = classroom.tuitionAmountDue ?? 0;
  const tuitionPaid = classroom.tuitionAmountPaid ?? 0;
  const tuitionRemaining = Math.max(0, tuitionDue - tuitionPaid);
  const isFullyPaid = tuitionDue > 0 && tuitionRemaining === 0;

  const statusInfo = getCardStatusInfo(classroom);
  const accentClass = getCardAccent(classroom);

  return (
    <article className={`group overflow-hidden rounded-xl border border-[#e5e7eb] bg-white shadow-sm transition-shadow hover:shadow-md ${accentClass}`}>
      <div className="p-5 md:p-6">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">

          {/* ── Left: Main Info ── */}
          <div className="flex-1 min-w-0 space-y-3">
            {/* Badge row */}
            <div className="flex flex-wrap items-center gap-2">
              <ClassroomTypeBadge mode={classroom.deliveryMode} />
              <StatusBadge status={classroom.registrationStatus || classroom.classroomStatus} />
              {/* Lifecycle time pill */}
              <span className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-extrabold ${statusInfo.color}`}>
                {statusInfo.text}
              </span>
            </div>

            {/* Class name */}
            <h3 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828] leading-tight group-hover:text-[#730014] transition-colors line-clamp-2">
              {classroom.title}
            </h3>

            {/* Key info row */}
            <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-[#584140]">
              <span className="flex items-center gap-1.5">
                <User className="h-3.5 w-3.5 text-[#730014]" />
                <strong className="text-[#2b2828]">{classroom.primaryTeacherName || 'Đang cập nhật'}</strong>
              </span>
              <span className="flex items-center gap-1.5">
                <Calendar className="h-3.5 w-3.5 text-[#730014]" />
                {formatClassroomDate(classroom.startDate)} – {formatClassroomDate(classroom.endDate)}
              </span>
              {isVirtual ? (
                <span className="flex items-center gap-1.5 text-purple-700">
                  <Video className="h-3.5 w-3.5" />
                  <span className="font-bold">Trực tuyến</span>
                </span>
              ) : (
                <span className="flex items-center gap-1.5">
                  <MapPin className="h-3.5 w-3.5 text-[#730014]" />
                  {classroom.offlineAddress || 'Cơ sở Hà Nội'}
                </span>
              )}
            </div>

            {/* Progress bar for active classes */}
            {isClassActive && classroom.hasClassAccess && classroom.progressPercent != null && (
              <div className="pt-1">
                <div className="flex items-center justify-between text-[10px] font-bold text-[#8b706e] mb-1.5">
                  <span className="flex items-center gap-1"><TrendingUp className="h-3 w-3" /> Tiến độ khóa học</span>
                  <span>{classroom.progressPercent}%</span>
                </div>
                <div className="h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
                  <div
                    className="h-full rounded-full bg-[#4b0009] transition-all"
                    style={{ width: `${classroom.progressPercent}%` }}
                  />
                </div>
              </div>
            )}

            {/* Waiting notice */}
            {isWaiting && !isClassCompleted && (
              <div className="inline-flex items-center gap-1.5 rounded-xl border border-amber-100 bg-amber-50/60 px-3 py-1.5 text-[10px] font-bold text-amber-800">
                <Info className="h-3.5 w-3.5 flex-shrink-0" />
                Bạn sẽ truy cập đầy đủ nội dung sau khi Training Manager xếp lớp
              </div>
            )}
          </div>

          {/* ── Right: Tuition + CTA ── */}
          <div className="flex flex-col items-end gap-3 flex-shrink-0">
            {/* Tuition status pill */}
            {tuitionDue > 0 && (
              <div className={`rounded-2xl border px-3 py-2 text-right ${
                isFullyPaid
                  ? 'border-emerald-100 bg-emerald-50/50'
                  : 'border-amber-100 bg-amber-50/50'
              }`}>
                <div className="flex items-center gap-1 justify-end">
                  <DollarSign className={`h-3.5 w-3.5 ${isFullyPaid ? 'text-emerald-700' : 'text-amber-700'}`} />
                  <span className={`text-[10px] font-extrabold uppercase tracking-wider ${isFullyPaid ? 'text-emerald-700' : 'text-amber-700'}`}>
                    {isFullyPaid ? 'Đã thanh toán đủ' : 'Còn thiếu học phí'}
                  </span>
                </div>
                {!isFullyPaid && (
                  <p className="text-xs font-extrabold text-[#2b2828] mt-0.5">
                    {formatClassroomPrice(tuitionRemaining)}
                  </p>
                )}
                {isFullyPaid && (
                  <p className="text-xs font-extrabold text-emerald-700 flex items-center justify-end gap-0.5 mt-0.5">
                    <CheckCircle2 className="h-3 w-3" />
                    {formatClassroomPrice(tuitionPaid)}
                  </p>
                )}
                {formatTuitionSettlement(classroom.tuitionSettlementType, classroom.tuitionSettlementTypeLabel) && (
                  <p className="text-[10px] font-bold text-[#730014] mt-1">
                    {formatTuitionSettlement(classroom.tuitionSettlementType, classroom.tuitionSettlementTypeLabel)}
                  </p>
                )}
              </div>
            )}

            {/* CTA */}
            {classroom.hasClassAccess ? (
              <Link
                className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] hover:shadow active:scale-95"
                to={`/my-classrooms/${classroom.id}`}
              >
                Vào lớp học
                <ChevronRight className="h-3.5 w-3.5" />
              </Link>
            ) : isClassCompleted ? (
              <Link
                className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-5 py-2.5 text-xs font-extrabold text-gray-600 transition hover:bg-gray-50 active:scale-95"
                to={`/opening-schedule/${classroom.slug || classroom.id}`}
              >
                Xem lớp
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            ) : (
              <Link
                className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd] bg-white px-5 py-2.5 text-xs font-extrabold text-[#4b0009] transition hover:bg-[#fff3f4] active:scale-95"
                to={`/opening-schedule/${classroom.slug || classroom.id}`}
              >
                Chi tiết đăng ký
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            )}
          </div>
        </div>
      </div>
    </article>
  );
}
