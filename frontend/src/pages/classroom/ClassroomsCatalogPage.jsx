import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  DollarSign,
  Search,
  ArrowRight,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import {
  ClassroomEmptyState,
  ClassroomErrorState,
  LoadingSkeleton,
  ClassroomTypeBadge,
  StatusBadge,
} from '../../components/classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDate, formatClassroomPrice, formatOfferingStatus } from '../../utils/classroomHelpers';

const MODES = [
  { id: 'ALL', label: 'Tất cả lớp học' },
  { id: 'OFFLINE', label: 'Tại trung tâm' },
  { id: 'VIRTUAL', label: 'Trực tuyến' },
];

const isUpcomingOffering = (offering) => {
  if (offering?.classroomStatus !== 'UPCOMING' || !offering?.startDate) return false;
  const startDate = new Date(`${offering.startDate}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return !Number.isNaN(startDate.getTime()) && startDate > today;
};

const capacityPercent = (enrolled, max) => {
  if (!max || max <= 0) return 0;
  return Math.min(100, Math.round((enrolled / max) * 100));
};

const capacityColor = (pct) => {
  if (pct >= 90) return 'bg-rose-500';
  if (pct >= 70) return 'bg-amber-400';
  return 'bg-emerald-500';
};

export default function ClassroomsCatalogPage() {
  const [activeMode, setActiveMode] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [offerings, setOfferings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadOfferings = async () => {
    setLoading(true);
    setError('');
    try {
      const params = { page: 0, size: 100 };
      if (activeMode !== 'ALL') params.mode = activeMode;
      const page = await classroomApi.getClassroomOfferings(params);
      setOfferings((page.content || []).filter(isUpcomingOffering));
    } catch (err) {
      setOfferings([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách lớp học.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOfferings();
  }, [activeMode]);

  const filteredOfferings = useMemo(() => {
    return offerings.filter((o) => {
      const matchesSearch = !searchQuery
        || o.title?.toLowerCase().includes(searchQuery.toLowerCase())
        || o.primaryTeacherName?.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesSearch;
    });
  }, [offerings, searchQuery]);

  const totalStats = useMemo(() => ({
    offline: offerings.filter((o) => o.deliveryMode === 'OFFLINE').length,
    virtual: offerings.filter((o) => o.deliveryMode === 'VIRTUAL').length,
    open: offerings.length,
  }), [offerings]);

  return (
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />

      <main className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[80px] pt-8 md:px-10 space-y-8">

        {/* Page Header */}
        <motion.section
          className="border-b border-[#ebebeb] bg-white pb-6 pt-2"
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, ease: 'easeOut' }}
        >
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <div className="mb-2 flex items-center gap-3">
                <span className="h-7 w-1 rounded-full bg-[#8a0018]" />
                <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#1a1c1c] md:text-3xl">
                  Lịch khai giảng
                </h1>
              </div>
              <p className="pl-4 text-sm leading-6 text-[#6a5553]">
                Khám phá các lớp IELTS / TOEIC tại trung tâm hoặc trực tuyến với lịch cố định và giảng viên đồng hành.
              </p>
            </div>
            <div className="flex gap-8 pl-4 md:pl-0">
              <div>
                <p className="font-['Manrope'] text-2xl font-extrabold text-[#8a0018]">{totalStats.offline}</p>
                <p className="text-xs font-semibold text-[#9a8b8a]">Tại trung tâm</p>
              </div>
              <div>
                <p className="font-['Manrope'] text-2xl font-extrabold text-[#8a0018]">{totalStats.virtual}</p>
                <p className="text-xs font-semibold text-[#9a8b8a]">Trực tuyến</p>
              </div>
              <div>
                <p className="font-['Manrope'] text-2xl font-extrabold text-[#8a0018]">{totalStats.open}</p>
                <p className="text-xs font-semibold text-[#9a8b8a]">Đang mở đăng ký</p>
              </div>
            </div>
          </div>
        </motion.section>

        {/* Filter Bar */}
        <section className="rounded-xl border border-[#ebebeb] bg-white p-4 shadow-sm space-y-4">
          {/* Mode tabs */}
          <div className="flex flex-wrap gap-2">
            {MODES.map((m) => (
              <button
                key={m.id}
                type="button"
                className={`rounded-2xl px-5 py-2.5 text-sm font-extrabold transition-all duration-200 ${
                  activeMode === m.id
                    ? 'bg-[#4b0009] text-white shadow-md shadow-[#4b0009]/10'
                    : 'border border-gray-100 bg-white text-[#584140] hover:bg-[#fff3f4] hover:text-[#730014]'
                }`}
                onClick={() => setActiveMode(m.id)}
              >
                {m.label}
              </button>
            ))}
          </div>

          {/* Search row */}
          <div className="relative">
            <input
              type="text"
              placeholder="Tìm theo tên lớp hoặc giảng viên..."
              className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/60 py-3 pl-10 pr-4 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <Search className="absolute left-3.5 top-3.5 h-4 w-4 text-[#8b706e]" />
          </div>
        </section>

        {/* Results count */}
        {!loading && !error && (
          <p className="text-xs font-bold text-[#8b706e] px-1">
            Hiển thị <strong className="text-[#2b2828]">{filteredOfferings.length}</strong> lớp học
          </p>
        )}

        {/* Content */}
        {loading ? (
          <LoadingSkeleton count={6} type="card" />
        ) : error ? (
          <ClassroomErrorState message={error} onRetry={loadOfferings} />
        ) : !filteredOfferings.length ? (
          <ClassroomEmptyState
            icon={BookOpen}
            title="Chưa có lớp phù hợp"
            description="Hiện chưa có lớp khai giảng nào phù hợp. Vui lòng quay lại sau hoặc thử từ khóa khác."
          />
        ) : (
          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
            {filteredOfferings.map((offering, idx) => {
              const pct = capacityPercent(offering.enrolledCount ?? 0, offering.maxCapacity);
              const isVirtual = offering.deliveryMode === 'VIRTUAL';
              const isFull = offering.classroomStatus === 'FULL' || pct >= 100;
              const isOpen = ['OPEN', 'UPCOMING'].includes(offering.classroomStatus);

              return (
                <motion.article
                  key={offering.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.35, delay: Math.min(idx * 0.06, 0.4), ease: 'easeOut' }}
                  className="group flex flex-col overflow-hidden rounded-xl border border-[#e5e7eb] bg-white shadow-sm transition-shadow duration-200 hover:shadow-md"
                >
                  {/* Card image/banner */}
                  <div className="relative h-36 overflow-hidden bg-[#f9f9f9]">
                    {offering.thumbnailUrl ? (
                      <img
                        alt={offering.title}
                        className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                        src={offering.thumbnailUrl}
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center">
                        <div className="flex flex-col items-center gap-2 opacity-30">
                          {isVirtual
                            ? <Video className="h-12 w-12 text-[#730014]" />
                            : <MapPin className="h-12 w-12 text-[#730014]" />}
                        </div>
                      </div>
                    )}

                    {/* Overlay badges */}
                    <div className="absolute left-4 top-4 flex flex-wrap gap-1.5">
                      <ClassroomTypeBadge mode={offering.deliveryMode} />
                      {isFull && (
                        <span className="inline-flex items-center gap-1 rounded-full border border-rose-100 bg-rose-50 px-2.5 py-0.5 text-[10px] font-extrabold text-rose-700">
                          Hết chỗ
                        </span>
                      )}
                      {isOpen && !isFull && (
                        <span className="inline-flex items-center gap-1 rounded-full border border-emerald-100 bg-emerald-50 px-2.5 py-0.5 text-[10px] font-extrabold text-emerald-700">
                          Đang mở đăng ký
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Card body */}
                  <div className="flex flex-1 flex-col p-6 space-y-4">
                    {/* Status badge row */}
                    <div className="flex items-center gap-2">
                      <StatusBadge status={offering.classroomStatus} />
                    </div>

                    {/* Title */}
                    <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828] leading-tight line-clamp-2 group-hover:text-[#730014] transition-colors">
                      {offering.title}
                    </h2>

                    {/* Info grid */}
                    <div className="grid grid-cols-2 gap-x-4 gap-y-2.5 text-xs text-[#584140]">
                      <div className="flex items-center gap-1.5 col-span-2">
                        <div className="flex h-5 w-5 items-center justify-center rounded-md bg-rose-50 text-[#730014]">
                          <Users className="h-3 w-3" />
                        </div>
                        <span className="font-bold">Giảng viên:</span>
                        <span className="text-[#2b2828] font-extrabold truncate">
                          {offering.primaryTeacherName || 'Đang cập nhật'}
                        </span>
                      </div>

                      <div className="flex items-center gap-1.5">
                        <div className="flex h-5 w-5 items-center justify-center rounded-md bg-rose-50 text-[#730014]">
                          <Calendar className="h-3 w-3" />
                        </div>
                        <div>
                          <p className="text-[10px] text-[#8b706e]">Khai giảng</p>
                          <p className="font-extrabold text-[#2b2828]">{formatClassroomDate(offering.startDate)}</p>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5">
                        <div className="flex h-5 w-5 items-center justify-center rounded-md bg-rose-50 text-[#730014]">
                          <DollarSign className="h-3 w-3" />
                        </div>
                        <div>
                          <p className="text-[10px] text-[#8b706e]">Học phí</p>
                          <p className="font-extrabold text-[#2b2828]">{formatClassroomPrice(offering.price)}</p>
                        </div>
                      </div>

                      {/* Location / Lark */}
                      {isVirtual ? (
                        <div className="col-span-2 flex items-center gap-1.5">
                          <div className="flex h-5 w-5 items-center justify-center rounded-md bg-purple-50 text-purple-700">
                            <Video className="h-3 w-3" />
                          </div>
                          <span className="font-bold text-purple-700">Học trực tuyến</span>
                        </div>
                      ) : (
                        <div className="col-span-2 flex items-center gap-1.5">
                          <div className="flex h-5 w-5 items-center justify-center rounded-md bg-rose-50 text-[#730014]">
                            <MapPin className="h-3 w-3" />
                          </div>
                          <span className="font-extrabold text-[#2b2828] truncate">
                            {offering.offlineAddress || 'Cơ sở Hà Nội'}
                          </span>
                        </div>
                      )}
                    </div>

                    {/* Capacity progress bar */}
                    {offering.maxCapacity > 0 && (
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between text-[10px] font-bold">
                          <span className="text-[#8b706e] flex items-center gap-1">
                            <Users className="h-3 w-3" />
                            Sĩ số
                          </span>
                          <span className={`font-extrabold ${pct >= 90 ? 'text-rose-600' : pct >= 70 ? 'text-amber-600' : 'text-emerald-700'}`}>
                            {offering.enrolledCount ?? 0} / {offering.maxCapacity} học viên
                          </span>
                        </div>
                        <div className="h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
                          <div
                            className={`h-full rounded-full transition-all duration-500 ${capacityColor(pct)}`}
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Card footer CTA */}
                  <div className="border-t border-[#f0f0f0] px-5 py-3">
                    <Link
                      className="flex items-center justify-center gap-2 rounded-lg bg-[#8a0018] px-5 py-2.5 text-xs font-bold text-white transition hover:bg-[#6b0013] active:scale-95"
                      to={`/opening-schedule/${offering.slug || offering.id}`}
                    >
                      Xem chi tiết & Đăng ký
                      <ArrowRight className="h-3.5 w-3.5" />
                    </Link>
                  </div>
                </motion.article>
              );
            })}
          </div>
        )}
      </main>

      <CourseFooter />
    </div>
  );
}
