import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BookOpen,
  Calendar,
  Clock,
  MapPin,
  Video,
  Users,
  DollarSign,
  Search,
  SlidersHorizontal,
  ArrowRight,
  CheckCircle2,
  Zap,
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

const STATUS_FILTERS = [
  { value: '', label: 'Mọi trạng thái' },
  { value: 'OPEN', label: 'Đang mở đăng ký' },
  { value: 'UPCOMING', label: 'Sắp khai giảng' },
  { value: 'ACTIVE', label: 'Đang học' },
  { value: 'FULL', label: 'Đã đủ chỗ' },
];

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
  const [statusFilter, setStatusFilter] = useState('');
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
      setOfferings(page.content || []);
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
      const matchesStatus = !statusFilter || o.classroomStatus === statusFilter;
      const matchesSearch = !searchQuery
        || o.title?.toLowerCase().includes(searchQuery.toLowerCase())
        || o.primaryTeacherName?.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesStatus && matchesSearch;
    });
  }, [offerings, statusFilter, searchQuery]);

  const totalStats = useMemo(() => ({
    offline: offerings.filter((o) => o.deliveryMode === 'OFFLINE').length,
    virtual: offerings.filter((o) => o.deliveryMode === 'VIRTUAL').length,
    open: offerings.filter((o) => ['OPEN', 'UPCOMING'].includes(o.classroomStatus)).length,
  }), [offerings]);

  return (
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />

      <main className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[80px] pt-8 md:px-10 space-y-8">

        {/* Hero Section */}
        <section className="relative overflow-hidden rounded-[32px] border border-[#dfbfbd]/15 bg-gradient-to-br from-[#4b0009] via-[#730014] to-[#9b1a29] p-8 shadow-lg md:p-12">
          {/* Decorative blobs */}
          <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/5 blur-3xl" />
          <div className="absolute bottom-0 left-1/4 h-48 w-48 rounded-full bg-white/5 blur-3xl" />

          <div className="relative flex flex-col gap-8 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-2xl">
              <span className="inline-flex rounded-full border border-white/20 bg-white/10 px-3 py-1 text-[10px] font-extrabold uppercase tracking-widest text-white/80">
                Lớp học EnglishLab
              </span>
              <h1 className="mt-4 font-['Manrope'] text-4xl font-extrabold tracking-tight text-white md:text-5xl">
                Danh mục lớp học
              </h1>
              <p className="mt-3 text-base leading-8 text-white/75">
                Khám phá các lớp IELTS / TOEIC tại trung tâm hoặc học trực tuyến qua Lark với lịch cố định và giảng viên đồng hành.
              </p>
            </div>

            {/* Quick Stats */}
            <div className="flex flex-wrap gap-4 lg:flex-col lg:items-end">
              <div className="rounded-2xl border border-white/15 bg-white/10 px-5 py-3 text-center backdrop-blur-sm">
                <p className="font-['Manrope'] text-2xl font-extrabold text-white">{totalStats.offline}</p>
                <p className="text-xs font-bold text-white/70 uppercase tracking-wider mt-0.5">Tại trung tâm</p>
              </div>
              <div className="rounded-2xl border border-white/15 bg-white/10 px-5 py-3 text-center backdrop-blur-sm">
                <p className="font-['Manrope'] text-2xl font-extrabold text-white">{totalStats.virtual}</p>
                <p className="text-xs font-bold text-white/70 uppercase tracking-wider mt-0.5">Trực tuyến</p>
              </div>
              <div className="rounded-2xl border border-white/15 bg-white/10 px-5 py-3 text-center backdrop-blur-sm">
                <p className="font-['Manrope'] text-2xl font-extrabold text-white">{totalStats.open}</p>
                <p className="text-xs font-bold text-white/70 uppercase tracking-wider mt-0.5">Đang mở Đăng ký</p>
              </div>
            </div>
          </div>
        </section>

        {/* Filter Bar */}
        <section className="rounded-[28px] border border-[#dfbfbd]/15 bg-white p-5 shadow-sm space-y-4">
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

          {/* Search & status filter row */}
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="Tìm theo tên lớp hoặc giảng viên..."
                className="w-full rounded-2xl border border-[#dfbfbd]/60 bg-[#fffafb]/60 py-3 pl-10 pr-4 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <Search className="absolute left-3.5 top-3.5 h-4 w-4 text-[#8b706e]" />
            </div>

            <div className="flex items-center gap-2">
              <SlidersHorizontal className="h-4 w-4 text-[#8b706e] flex-shrink-0" />
              <select
                className="rounded-xl border border-gray-200 bg-white px-4 py-3 text-xs font-bold text-[#584140] outline-none focus:border-[#730014]"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                {STATUS_FILTERS.map((f) => (
                  <option key={f.value} value={f.value}>{f.label}</option>
                ))}
              </select>
            </div>
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
            description={`Không tìm thấy lớp học nào với bộ lọc hiện tại. Hãy thử thay đổi tiêu chí tìm kiếm.`}
            actionLabel="Xem tất cả lớp"
            onAction={() => { setActiveMode('ALL'); setStatusFilter(''); setSearchQuery(''); }}
          />
        ) : (
          <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
            {filteredOfferings.map((offering) => {
              const pct = capacityPercent(offering.enrolledCount ?? 0, offering.maxCapacity);
              const isVirtual = offering.deliveryMode === 'VIRTUAL';
              const isFull = offering.classroomStatus === 'FULL' || pct >= 100;
              const isOpen = ['OPEN', 'UPCOMING'].includes(offering.classroomStatus);

              return (
                <article
                  key={offering.id}
                  className="group flex flex-col overflow-hidden rounded-[28px] border border-[#dfbfbd]/15 bg-white shadow-sm transition-all duration-300 hover:-translate-y-1 hover:border-[#dfbfbd]/30 hover:shadow-md"
                >
                  {/* Card image/banner */}
                  <div className="relative h-40 overflow-hidden bg-gradient-to-br from-[#fff3f4] to-[#ffe8ea]">
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
                          <Zap className="h-2.5 w-2.5" />
                          Đang mở Đăng ký
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
                            {offering.campusName || offering.offlineAddress || 'Cơ sở Hà Nội'}
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
                  <div className="border-t border-gray-50 px-6 py-4 bg-gray-50/30">
                    <Link
                      className="flex items-center justify-center gap-2 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition-all hover:bg-[#730014] hover:shadow active:scale-95"
                      to={`/classrooms/${offering.slug || offering.id}`}
                    >
                      Xem chi tiết & Đăng ký
                      <ArrowRight className="h-3.5 w-3.5" />
                    </Link>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </main>

      <CourseFooter />
    </div>
  );
}
