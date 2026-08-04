import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  BookOpen,
  Calendar,
  User,
  DollarSign,
  Award,
  ChevronRight,
  Lock,
  MapPin,
  Video,
  TrendingUp,
  RefreshCw,
  Building,
  Search
} from 'lucide-react';
import BrandedSelect from '../../components/ui/BrandedSelect';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  LoadingSkeleton,
} from '../../components/classroom/ClassroomUi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDate,
  formatClassroomPrice,
  formatTuitionSettlement,
} from '../../utils/classroomHelpers';
import { getStoredUser, hasAccessToken } from '../../utils/auth';
import { onlyAssignedClassrooms } from '../../utils/learnerClassroomAccess';

// ─── Tab definitions ────────────────────────────────────────────────────────
const learnerTabs = [
  { id: 'all', label: 'Tất cả lớp học' },
  { id: 'active', label: 'Đang diễn ra' },
  { id: 'upcoming', label: 'Sắp khai giảng' },
  { id: 'completed', label: 'Đã hoàn thành' },
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

// ─── Custom Minimalist Status configuration ───────────────────────────────────
const getMinimalistStatusInfo = (classroom) => {
  const days = daysUntil(classroom.startDate);
  if (isActiveClass(classroom)) {
    const end = daysUntil(classroom.endDate);
    if (end != null && end > 0) return { text: `Còn ${end} ngày`, dotColor: 'bg-emerald-500', textColor: 'text-emerald-700', badgeBg: 'bg-emerald-50 border-emerald-100' };
    return { text: 'Đang hoạt động', dotColor: 'bg-emerald-500', textColor: 'text-emerald-700', badgeBg: 'bg-emerald-50 border-emerald-100' };
  }
  if (isUpcomingClass(classroom)) {
    if (days != null && days > 0) return { text: `Khai giảng sau ${days} ngày`, dotColor: 'bg-amber-500', textColor: 'text-amber-700', badgeBg: 'bg-amber-50 border-amber-100' };
    if (days === 0) return { text: 'Khai giảng hôm nay!', dotColor: 'bg-rose-500 animate-ping', textColor: 'text-[#730014]', badgeBg: 'bg-rose-50 border-rose-100' };
  }
  if (isCompletedClass(classroom)) return { text: 'Đã hoàn thành', dotColor: 'bg-gray-400', textColor: 'text-gray-600', badgeBg: 'bg-gray-50 border-gray-150' };
  return { text: formatClassroomDate(classroom.startDate), dotColor: 'bg-gray-400', textColor: 'text-gray-600', badgeBg: 'bg-gray-50 border-gray-150' };
};

// ─── Component ────────────────────────────────────────────────────────────────
export default function MyClassroomsPage() {
  const [activeTab, setActiveTab] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [classrooms, setClassrooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const isAuthenticated = Boolean(hasAccessToken() && getStoredUser());

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getMyClassrooms();
      setClassrooms(onlyAssignedClassrooms(data));
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

  const assignedClassrooms = onlyAssignedClassrooms(classrooms);
  const filteredClassrooms = (() => {
    const query = searchQuery.trim().toLowerCase();
    return assignedClassrooms.filter((item) => {
      if (activeTab === 'active' && !isActiveClass(item)) return false;
      if (activeTab === 'upcoming' && !isUpcomingClass(item)) return false;
      if (activeTab === 'completed' && !isCompletedClass(item)) return false;
      if (!query) return true;
      return String(item.title || item.classroomTitle || '').toLowerCase().includes(query) ||
        String(item.code || '').toLowerCase().includes(query) ||
        String(item.courseTitle || '').toLowerCase().includes(query);
    });
  })();

  const { page, setPage, totalPages, pageItems: paginatedClassrooms, totalItems } = usePagination(
    filteredClassrooms,
    6,
    `${activeTab}-${searchQuery}`
  );

  const counts = {
    all: assignedClassrooms.length,
    active: assignedClassrooms.filter(isActiveClass).length,
    upcoming: assignedClassrooms.filter(isUpcomingClass).length,
    completed: assignedClassrooms.filter(isCompletedClass).length,
  };

  const learnerTabOptions = learnerTabs.map((tab) => ({
    label: `${tab.label} (${counts[tab.id] ?? 0})`,
    value: tab.id,
  }));

  return (
    <LearnerPageShell
      description="Xem danh sách lớp đang diễn ra, thời khóa biểu, tiến độ và cập nhật chi tiết học phí cá nhân."
      title="Lớp học của tôi"
    >
      {!isAuthenticated ? (
        <div className="flex flex-1 flex-col items-center justify-center py-16">
          <ClassroomEmptyState
            icon={Lock}
            actionLabel="Đăng nhập hệ thống"
            actionTo="/login"
            description="Bạn phải đăng nhập để xem các lớp đã được Nhân viên đào tạo xếp cho mình."
            title="Quyền truy cập bị giới hạn"
          />
        </div>
      ) : loading ? (
        <div className="space-y-6 flex-1">
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-24 w-full animate-pulse rounded-[24px] border border-gray-100 bg-white/60 p-4" />
            ))}
          </div>
          <LoadingSkeleton count={3} type="card" />
        </div>
      ) : error ? (
        <div className="flex flex-1 flex-col items-center justify-center py-12">
          <ClassroomErrorState message={error} onRetry={loadClassrooms} />
        </div>
      ) : classrooms.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center py-16">
          <ClassroomEmptyState
            icon={BookOpen}
            actionLabel="Đăng ký học và nhận tư vấn"
            actionTo="/opening-schedule#dang-ky-tu-van"
            description="Bạn chưa được xếp vào lớp học nào. Hãy gửi form đăng ký để Nhân viên đào tạo liên hệ, tư vấn và xếp lớp phù hợp."
            title="Chưa được xếp lớp"
          />
        </div>
      ) : (
        <div className="space-y-8 flex-1">
          
          {/* Flat Minimal Counter Cards */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <GlassCounterCard label="Đang tham gia" value={counts.active} dotColor="bg-emerald-500" icon={<BookOpen className="h-5 w-5" />} />
            <GlassCounterCard label="Sắp diễn ra" value={counts.upcoming} dotColor="bg-amber-500" icon={<Calendar className="h-5 w-5" />} />
            <GlassCounterCard label="Đã kết thúc" value={counts.completed} dotColor="bg-gray-400" icon={<Award className="h-5 w-5" />} />
          </div>

          {/* Filters & Alerts Area */}
          <section className="grid gap-3 rounded-[24px] border border-[#ead9db]/85 bg-white p-4 shadow-[0_8px_30px_rgba(75,0,9,0.015)] lg:grid-cols-[1fr_280px_auto]">
            <label className="relative block">
              <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[#c2acab]" />
              <input
                className="w-full rounded-2xl border border-[#dfbfbd]/50 bg-[#fffdfd] py-3 pl-11 pr-4 text-sm outline-none transition focus:border-[#730014] focus:bg-white focus:ring-4 focus:ring-[#730014]/5"
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Tìm kiếm lớp học..."
                value={searchQuery}
              />
            </label>
            <BrandedSelect
              buttonClassName="h-full rounded-2xl border-[#dfbfbd]/50 bg-[#fffdfd]"
              onChange={(event) => setActiveTab(event.target.value)}
              options={learnerTabOptions}
              value={activeTab}
            />
            <button
              aria-label="Tải lại"
              className="inline-flex items-center justify-center gap-2 rounded-2xl border border-[#dfbfbd] bg-white px-5 py-3 text-sm font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff2f3] active:scale-95"
              onClick={loadClassrooms}
              type="button"
            >
              <RefreshCw className="h-4 w-4" /> Tải lại
            </button>
          </section>

          {/* Clean Minimalist Cards Grid */}
          <div className="space-y-6">
            <AnimatePresence mode="popLayout">
              {paginatedClassrooms.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {paginatedClassrooms.map((classroom, idx) => (
                    <motion.div
                      key={classroom.id}
                      layout
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, scale: 0.95 }}
                      transition={{ duration: 0.35, delay: Math.min(idx * 0.05, 0.3) }}
                      className="h-full"
                    >
                      <MinimalistClassroomCard
                        classroom={classroom}
                      />
                    </motion.div>
                  ))}
                </div>
              ) : (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="py-12"
                >
                  <ClassroomEmptyState
                    description="Không tìm thấy thông tin lớp nào khớp với bộ lọc hiện tại."
                    title="Trống danh sách lớp"
                  />
                </motion.div>
              )}
            </AnimatePresence>

            {totalPages > 1 && (
              <div className="flex justify-center pt-4">
                <Pagination
                  page={page}
                  onChange={setPage}
                  totalPages={totalPages}
                  totalItems={totalItems}
                  pageSize={6}
                />
              </div>
            )}
          </div>

        </div>
      )}
    </LearnerPageShell>
  );
}

// ─── Minimal counter card component ───────────────────────────────────────────
function GlassCounterCard({ label, value, dotColor, icon }) {
  return (
    <div className="relative overflow-hidden rounded-[24px] border border-gray-200/80 bg-white p-5 shadow-[0_10px_30px_rgba(0,0,0,0.02)] transition-all duration-300 hover:border-[#dfbfbd]/60 hover:shadow-[0_15px_35px_rgba(75,0,9,0.04)] hover:-translate-y-0.5 flex items-center justify-between group">
      <div className="flex items-center gap-3">
        <div className="rounded-xl p-2.5 shrink-0 bg-[#fff0f1] text-[#730014]">
          {icon}
        </div>
        <div>
          <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">{label}</span>
          <p className="font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c] mt-0.5">{value}</p>
        </div>
      </div>
      <span className={`h-2 w-2 rounded-full ${dotColor} opacity-70 group-hover:opacity-100 group-hover:scale-125 transition-all duration-300`} />
    </div>
  );
}

// ─── Minimalist Classroom card component ────────────────────────────────────────
function MinimalistClassroomCard({ classroom }) {
  const isClassActive = isActiveClass(classroom);
  const isVirtual = classroom.deliveryMode === 'VIRTUAL';

  const tuitionDue = classroom.tuitionAmountDue ?? 0;
  const tuitionPaid = classroom.tuitionAmountPaid ?? 0;
  const tuitionRemaining = Math.max(0, tuitionDue - tuitionPaid);
  const isFullyPaid = tuitionDue > 0 && tuitionRemaining === 0;

  const statusInfo = getMinimalistStatusInfo(classroom);

  return (
    <article className="relative overflow-hidden rounded-[28px] border border-gray-200/80 bg-white p-6 shadow-[0_10px_35px_rgba(0,0,0,0.02)] transition-all duration-350 hover:shadow-[0_20px_50px_rgba(115,0,20,0.07)] hover:border-[#730014]/30 hover:-translate-y-1.5 flex flex-col justify-between h-full group">
      
      <div className="space-y-4">
        {/* Card Header Row */}
        <div className="flex items-center justify-between w-full">
          {/* Status Indicator Dot */}
          <div className="flex items-center gap-2">
            <span className={`h-2 w-2 rounded-full ${statusInfo.dotColor}`} />
            <span className="text-[10px] font-extrabold uppercase tracking-wider text-gray-500">
              {statusInfo.text}
            </span>
          </div>

          {/* Delivery Mode Badge */}
          <span className="inline-flex items-center gap-1 rounded-full bg-gray-50 border border-gray-150 px-2.5 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-[#584140]">
            {isVirtual ? <Video className="h-3 w-3 text-purple-600" /> : <Building className="h-3 w-3 text-[#730014]" />}
            {isVirtual ? 'Google Meet' : 'Tại cơ sở'}
          </span>
        </div>

        {/* Title */}
        <div className="space-y-2">
          <span className="text-[9px] font-bold text-gray-400 uppercase tracking-widest block">Mã lớp: #{classroom.id}</span>
          <h3 className="font-['Manrope'] text-base font-extrabold text-[#1a1c1c] leading-snug group-hover:text-[#730014] transition-colors duration-300 line-clamp-2">
            {classroom.title}
          </h3>
        </div>

        {/* Details list */}
        <div className="space-y-2.5 text-xs text-[#584140] pt-2 border-t border-gray-50">
          <div className="flex items-center gap-2">
            <User className="h-4 w-4 text-[#730014] shrink-0" />
            <span>Giảng viên: <strong className="text-[#1a1c1c] font-semibold">{classroom.primaryTeacherName || 'Đang cập nhật'}</strong></span>
          </div>
          
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-[#730014] shrink-0" />
            <span>Thời gian: {formatClassroomDate(classroom.startDate)} – {formatClassroomDate(classroom.endDate)}</span>
          </div>

          {!isVirtual && (
            <div className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-[#730014] shrink-0" />
              <span className="truncate">{classroom.offlineAddress || 'Cơ sở Hà Nội'}</span>
            </div>
          )}
        </div>

        {/* Progress bar for active courses */}
        {isClassActive && classroom.progressPercent != null && (
          <div className="pt-2 space-y-1.5">
            <div className="flex items-center justify-between text-[10px] font-extrabold text-[#8b706e] uppercase tracking-wider">
              <span className="flex items-center gap-1"><TrendingUp className="h-3.5 w-3.5 text-[#730014]" /> Tiến độ hoàn thành</span>
              <span className="text-[#730014]">{classroom.progressPercent}%</span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-gray-150/60">
              <div
                className="h-full rounded-full bg-gradient-to-r from-[#730014] to-rose-600 shadow-[0_0_8px_rgba(115,0,20,0.2)] transition-all duration-500"
                style={{ width: `${classroom.progressPercent}%` }}
              />
            </div>
          </div>
        )}

      </div>

      {/* Tuition details & navigation */}
      <div className="space-y-4 pt-4 mt-4 border-t border-gray-50">
        
        {/* Tuition Status Row */}
        {tuitionDue > 0 && (
          <div className={`rounded-2xl border p-3 flex items-center justify-between ${
            isFullyPaid ? 'border-emerald-100/70 bg-emerald-50/15' : 'border-[#dfbfbd]/35 bg-[#fff0f1]/20'
          }`}>
            <div className="flex items-center gap-1.5">
              <DollarSign className={`h-4 w-4 shrink-0 ${isFullyPaid ? 'text-emerald-600' : 'text-[#730014]'}`} />
              <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#8b706e]">
                Học phí
              </span>
            </div>
            <div className="text-right">
              <p className={`text-xs font-extrabold ${isFullyPaid ? 'text-emerald-700' : 'text-[#730014]'}`}>
                {isFullyPaid ? 'Đã đóng đủ' : `Còn thiếu: ${formatClassroomPrice(tuitionRemaining)}`}
              </p>
              {formatTuitionSettlement(classroom.tuitionSettlementType, classroom.tuitionSettlementTypeLabel) && (
                <span className="text-[8px] font-bold text-[#8a0018] uppercase tracking-wider block mt-0.5">
                  {formatTuitionSettlement(classroom.tuitionSettlementType, classroom.tuitionSettlementTypeLabel)}
                </span>
              )}
            </div>
          </div>
        )}

        {/* Action CTAs */}
        <div className="flex gap-2 w-full">
          <Link
            className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-[#730014] to-[#4b0009] py-3 text-xs font-bold text-white shadow-sm transition hover:shadow-md active:scale-95 btn-hover"
            to={`/my-classrooms/${classroom.id}`}
          >
            Vào học
            <ChevronRight className="h-4 w-4" />
          </Link>
        </div>

      </div>
    </article>
  );
}
