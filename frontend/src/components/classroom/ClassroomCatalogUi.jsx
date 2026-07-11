import { Link } from 'react-router-dom';
import {
  ArrowRight,
  BookOpen,
  Calendar,
  Clock,
  DollarSign,
  Filter,
  MapPin,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Users,
  Video,
} from 'lucide-react';
import BrandedSelect from '../ui/BrandedSelect';
import { ClassroomTypeBadge, StatusBadge } from './ClassroomUi';
import { formatClassroomDate, formatClassroomPrice } from '../../utils/classroomHelpers';

export function CatalogHero({ stats, title = 'Lịch khai giảng', subtitle }) {
  return (
    <section className="relative overflow-hidden rounded-[28px] border border-[#dfc4c2]/50 bg-gradient-to-br from-[#3d0008] via-[#730014] to-[#9a1830] px-6 py-8 text-white shadow-[0_24px_60px_rgba(75,0,9,0.22)] md:px-10 md:py-10">
      <div className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-white/10 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-20 left-1/3 h-48 w-48 rounded-full bg-[#ffb4c0]/15 blur-3xl" />
      <div className="relative flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
        <div className="max-w-2xl">
          <p className="mb-3 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-[11px] font-bold uppercase tracking-[0.18em] text-white/90">
            <BookOpen className="h-3.5 w-3.5" />
            Lớp học tại trung tâm & trực tuyến
          </p>
          <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-tight md:text-4xl">{title}</h1>
          <p className="mt-3 text-sm leading-7 text-white/80 md:text-base">
            {subtitle || 'Khám phá các lớp IELTS / TOEIC với lịch cố định, giảng viên đồng hành và lộ trình rõ ràng từ đầu vào đến mục tiêu đầu ra.'}
          </p>
        </div>
        <div className="grid grid-cols-3 gap-3 sm:gap-4">
          {[
            { label: 'Tại trung tâm', value: stats.offline },
            { label: 'Trực tuyến', value: stats.virtual },
            { label: 'Đang mở', value: stats.open },
          ].map((item) => (
            <div
              className="rounded-2xl border border-white/15 bg-white/10 px-4 py-3 backdrop-blur-sm"
              key={item.label}
            >
              <p className="font-['Manrope'] text-2xl font-extrabold">{item.value}</p>
              <p className="mt-0.5 text-[11px] font-semibold uppercase tracking-wide text-white/70">{item.label}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export function CatalogModeTabs({ modes, activeMode, onChange }) {
  return (
    <div className="flex flex-wrap gap-2">
      {modes.map((mode) => (
        <button
          className={`rounded-xl px-4 py-2 text-sm font-extrabold transition-all ${
            activeMode === mode.id
              ? 'bg-[#4b0009] text-white shadow-md shadow-[#4b0009]/20'
              : 'border border-[#ecdedd] bg-white text-[#584140] hover:border-[#730014]/30 hover:bg-[#fff7f7] hover:text-[#730014]'
          }`}
          key={mode.id}
          onClick={() => onChange(mode.id)}
          type="button"
        >
          {mode.label}
        </button>
      ))}
    </div>
  );
}

function FilterGroup({ title, children }) {
  return (
    <div className="space-y-2.5">
      <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8b706e]">{title}</p>
      {children}
    </div>
  );
}

export function CatalogFilterSidebar({
  searchQuery,
  onSearchChange,
  filters,
  onFilterChange,
  filterOptions,
  sortBy,
  onSortChange,
  sortOptions,
  priceRanges,
  seatFilters,
  startWindows,
  dayOptions,
  timeSlotOptions,
  hasActiveFilters,
  onResetFilters,
  resultCount,
}) {
  return (
    <aside className="lg:w-[292px] lg:shrink-0">
      <div className="space-y-4 lg:sticky lg:top-24">
        <div className="rounded-[24px] border border-[#ecdedd]/80 bg-white p-5 shadow-[0_12px_40px_rgba(75,0,9,0.05)]">
          <div className="mb-5 flex items-center justify-between gap-2">
            <p className="inline-flex items-center gap-2 font-['Manrope'] text-sm font-extrabold text-[#2b2828]">
              <SlidersHorizontal className="h-4 w-4 text-[#730014]" />
              Bộ lọc
            </p>
            {hasActiveFilters ? (
              <button
                className="inline-flex items-center gap-1 text-[11px] font-bold text-[#730014] hover:underline"
                onClick={onResetFilters}
                type="button"
              >
                <RotateCcw className="h-3 w-3" />
                Xóa lọc
              </button>
            ) : null}
          </div>

          <div className="relative mb-5">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#c2acab]" />
            <input
              className="h-11 w-full rounded-xl border border-[#ecdedd] bg-[#fffafb] py-2 pl-10 pr-3 text-sm text-[#2b2828] outline-none transition focus:border-[#730014] focus:bg-white"
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder="Tên lớp, giảng viên..."
              type="text"
              value={searchQuery}
            />
          </div>

          <div className="space-y-5">
            <FilterGroup title="Kỳ thi & trình độ">
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('examCategory')}
                options={[{ value: 'ALL', label: 'Mọi kỳ thi' }, ...filterOptions.examCategories.map((v) => ({ value: v, label: v }))]}
                value={filters.examCategory}
              />
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('entryLevel')}
                options={[{ value: 'ALL', label: 'Mọi trình độ' }, ...filterOptions.entryLevels.map((v) => ({ value: v, label: v }))]}
                value={filters.entryLevel}
              />
            </FilterGroup>

            <FilterGroup title="Cơ sở & giảng viên">
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('campus')}
                options={[{ value: 'ALL', label: 'Mọi cơ sở' }, ...filterOptions.campuses.map((v) => ({ value: v, label: v }))]}
                value={filters.campus}
              />
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('teacher')}
                options={[{ value: 'ALL', label: 'Mọi giảng viên' }, ...filterOptions.teachers.map((v) => ({ value: v, label: v }))]}
                value={filters.teacher}
              />
            </FilterGroup>

            <FilterGroup title="Học phí & chỗ">
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('priceRange')}
                options={priceRanges.map(({ value, label }) => ({ value, label }))}
                value={filters.priceRange}
              />
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('seatStatus')}
                options={seatFilters}
                value={filters.seatStatus}
              />
            </FilterGroup>

            <FilterGroup title="Lịch học">
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('startWindow')}
                options={startWindows.map(({ value, label }) => ({ value, label }))}
                value={filters.startWindow}
              />
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('dayOfWeek')}
                options={dayOptions}
                value={filters.dayOfWeek}
              />
              <BrandedSelect
                buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-white text-sm shadow-none"
                onChange={onFilterChange('timeSlot')}
                options={timeSlotOptions}
                value={filters.timeSlot}
              />
            </FilterGroup>
          </div>
        </div>

        <div className="rounded-[20px] border border-[#ecdedd]/70 bg-[#fffafb] px-4 py-3 text-xs text-[#584140]">
          <p className="inline-flex items-center gap-1.5 font-bold text-[#730014]">
            <Filter className="h-3.5 w-3.5" />
            {resultCount} lớp phù hợp
          </p>
        </div>
      </div>
    </aside>
  );
}

export function CatalogResultsToolbar({ sortBy, onSortChange, sortOptions, resultCount }) {
  return (
    <div className="flex flex-col gap-3 rounded-[20px] border border-[#ecdedd]/70 bg-white px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-[#584140]">
        Hiển thị <strong className="font-extrabold text-[#2b2828]">{resultCount}</strong> lớp học
      </p>
      <div className="w-full sm:w-[240px]">
        <BrandedSelect
          buttonClassName="h-11 rounded-xl border-[#ecdedd] bg-[#fffafb] text-sm shadow-none"
          onChange={(event) => onSortChange(event?.target?.value ?? 'START_DATE')}
          options={sortOptions}
          value={sortBy}
        />
      </div>
    </div>
  );
}

export function CatalogOfferingCard({ offering, capacityPercent, capacityColor, index = 0 }) {
  const pct = capacityPercent(offering.enrolledCount ?? 0, offering.maxCapacity);
  const isVirtual = offering.deliveryMode === 'VIRTUAL';
  const isFull = offering.classroomStatus === 'FULL' || pct >= 100;
  const isOpen = ['OPEN', 'UPCOMING'].includes(offering.classroomStatus);
  const seatsLeft = Math.max(0, (offering.maxCapacity ?? 0) - (offering.enrolledCount ?? 0));

  return (
    <article
      className="group flex h-full flex-col overflow-hidden rounded-[24px] border border-[#ecdedd]/80 bg-white shadow-[0_10px_30px_rgba(75,0,9,0.04)] transition-all duration-300 hover:-translate-y-0.5 hover:border-[#dfc4c2] hover:shadow-[0_18px_45px_rgba(75,0,9,0.10)]"
      style={{ animationDelay: `${Math.min(index * 60, 360)}ms` }}
    >
      <div className={`relative h-32 overflow-hidden ${isVirtual ? 'bg-gradient-to-br from-[#2e1065] via-[#5b21b6] to-[#7c3aed]' : 'bg-gradient-to-br from-[#4b0009] via-[#730014] to-[#a01830]'}`}>
        {offering.thumbnailUrl ? (
          <img
            alt={offering.title}
            className="absolute inset-0 h-full w-full object-cover opacity-35 mix-blend-overlay"
            src={offering.thumbnailUrl}
          />
        ) : null}
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,255,255,0.18),transparent_55%)]" />
        <div className="relative flex h-full flex-col justify-between p-4">
          <div className="flex flex-wrap gap-1.5">
            <ClassroomTypeBadge mode={offering.deliveryMode} />
            {isFull ? (
              <span className="rounded-full border border-white/20 bg-white/15 px-2.5 py-0.5 text-[10px] font-extrabold text-white backdrop-blur-sm">
                Hết chỗ
              </span>
            ) : isOpen ? (
              <span className="rounded-full border border-emerald-200/40 bg-emerald-500/20 px-2.5 py-0.5 text-[10px] font-extrabold text-emerald-50 backdrop-blur-sm">
                Đang mở đăng ký
              </span>
            ) : null}
          </div>
          <p className="font-['Manrope'] text-lg font-extrabold leading-snug text-white line-clamp-2 drop-shadow-sm">
            {offering.title}
          </p>
        </div>
      </div>

      <div className="flex flex-1 flex-col gap-4 p-5">
        <div className="flex items-center gap-2">
          <StatusBadge status={offering.classroomStatus} />
        </div>

        {(offering.curriculumProgramExamCategory || offering.entryLevel || offering.targetOutcome) ? (
          <div className="flex flex-wrap gap-1.5">
            {offering.curriculumProgramExamCategory ? (
              <span className="rounded-lg bg-[#fff1f3] px-2.5 py-1 text-[10px] font-extrabold text-[#730014]">
                {offering.curriculumProgramExamCategory}
              </span>
            ) : null}
            {offering.entryLevel ? (
              <span className="rounded-lg bg-[#f4f4f5] px-2.5 py-1 text-[10px] font-bold text-[#584140]">
                Đầu vào {offering.entryLevel}
              </span>
            ) : null}
            {offering.targetOutcome ? (
              <span className="rounded-lg bg-emerald-50 px-2.5 py-1 text-[10px] font-bold text-emerald-700">
                Mục tiêu {offering.targetOutcome}
              </span>
            ) : null}
          </div>
        ) : null}

        {offering.scheduleSummary ? (
          <div className="flex items-start gap-2 rounded-xl bg-[#fffafb] px-3 py-2.5 text-xs text-[#584140]">
            <Clock className="mt-0.5 h-3.5 w-3.5 flex-shrink-0 text-[#730014]" />
            <span className="font-semibold leading-5">{offering.scheduleSummary}</span>
          </div>
        ) : null}

        <div className="grid grid-cols-2 gap-3 text-xs">
          <MetaItem icon={<Users className="h-3.5 w-3.5" />} label="Giảng viên" value={offering.primaryTeacherName || 'Đang cập nhật'} />
          <MetaItem icon={<Calendar className="h-3.5 w-3.5" />} label="Khai giảng" value={formatClassroomDate(offering.startDate)} />
          <MetaItem icon={<DollarSign className="h-3.5 w-3.5" />} label="Học phí" value={formatClassroomPrice(offering.price)} />
          {isVirtual ? (
            <MetaItem accent="purple" icon={<Video className="h-3.5 w-3.5" />} label="Hình thức" value="Trực tuyến" />
          ) : (
            <MetaItem icon={<MapPin className="h-3.5 w-3.5" />} label="Cơ sở" value={offering.offlineAddress || 'Hà Nội'} />
          )}
        </div>

        {offering.maxCapacity > 0 ? (
          <div className="mt-auto space-y-1.5 rounded-xl border border-[#f0e4e3] bg-[#fffbfb] px-3 py-2.5">
            <div className="flex items-center justify-between text-[10px] font-bold">
              <span className="text-[#8b706e]">Sĩ số</span>
              <span className={pct >= 90 ? 'text-rose-600' : pct >= 70 ? 'text-amber-600' : 'text-emerald-700'}>
                {offering.enrolledCount ?? 0}/{offering.maxCapacity}
                {!isFull ? ` · còn ${seatsLeft} chỗ` : ''}
              </span>
            </div>
            <div className="h-1.5 overflow-hidden rounded-full bg-[#f3eceb]">
              <div className={`h-full rounded-full transition-all ${capacityColor(pct)}`} style={{ width: `${pct}%` }} />
            </div>
          </div>
        ) : null}
      </div>

      <div className="border-t border-[#f3eceb] p-4 pt-3">
        <Link
          className="flex items-center justify-center gap-2 rounded-xl bg-[#4b0009] px-4 py-3 text-xs font-extrabold text-white transition hover:bg-[#730014] active:scale-[0.99]"
          to={`/opening-schedule/${offering.slug || offering.id}`}
        >
          {isFull ? 'Xem chi tiết & Danh sách chờ' : 'Xem chi tiết & Đăng ký'}
          <ArrowRight className="h-3.5 w-3.5" />
        </Link>
      </div>
    </article>
  );
}

function MetaItem({ icon, label, value, accent }) {
  const tone = accent === 'purple' ? 'text-purple-700' : 'text-[#730014]';
  return (
    <div className="min-w-0">
      <p className={`mb-1 inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-wide text-[#8b706e] ${tone}`}>
        {icon}
        {label}
      </p>
      <p className="truncate text-sm font-extrabold text-[#2b2828]">{value}</p>
    </div>
  );
}
