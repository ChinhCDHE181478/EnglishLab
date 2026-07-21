import {
  Archive,
  Building2,
  Check,
  ChevronLeft,
  ChevronRight,
  Copy,
  Edit3,
  Filter,
  Layers,
  RefreshCw,
  Users,
  Video,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import BrandedSelect from '../ui/BrandedSelect';
import Pagination from '../ui/Pagination';
import { Panel } from '../content-manager/ContentManagerUi';

export function ProgramPageHero({ mode, title, subtitle, stats = [], actions }) {
  return (
    <section className="space-y-8">
      {actions ? <div className="flex flex-wrap justify-end gap-2 sm:-mt-[88px] sm:mb-14">{actions}</div> : null}
      {stats.length ? (
        <div className="grid gap-6 md:grid-cols-3">
          {stats.map((item) => (
            <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-4 shadow-[0_4px_12px_rgba(75,0,9,0.05)]" key={item.label}>
              <div className="mb-1 flex items-center justify-between gap-3">
                <span className="text-xs font-bold uppercase tracking-[0.12em] text-[#4b0009]">{item.label}</span>
              </div>
              <p className="font-['Manrope'] text-3xl font-extrabold text-[#0b1c30]">{item.value}</p>
            </section>
          ))}
        </div>
      ) : null}
    </section>
  );
}

export function ProgramFilterBar({
  keyword,
  onKeywordChange,
  examFilter,
  onExamFilterChange,
  examOptions,
  levelFilter,
  onLevelFilterChange,
  statusFilter,
  onStatusFilterChange,
  statusOptions,
  usageFilter,
  onUsageFilterChange,
  usageOptions,
  platformFilter,
  onPlatformFilterChange,
  platformOptions,
  sortBy,
  onSortChange,
  sortOptions,
  showPlatform,
  onRefresh,
  loading,
  onReset,
  resultCount,
}) {
  return (
    <Panel className="rounded-xl border-[#dcc0bf]/30 bg-white p-4 shadow-sm">
      <div className="grid gap-3 xl:grid-cols-[minmax(280px,1fr)_repeat(5,minmax(130px,1fr))_44px]">
        <label className="relative block">
          <Filter className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#897270]" />
          <input
            className="h-10 w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 pl-10 pr-4 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
            onChange={(event) => onKeywordChange(event.target.value)}
            placeholder="Tìm tên, mã, cấp độ..."
            value={keyword}
          />
        </label>
        <CompactSelect onChange={onExamFilterChange} options={[{ label: 'Tất cả nhóm thi', value: 'ALL' }, ...examOptions.map((o) => ({ label: o.label, value: o.value }))]} prefix="Nhóm thi" value={examFilter} />
        <label className="block">
          <input
            className="h-10 w-full rounded-lg border border-[#dcc0bf]/50 bg-[#f8f9ff] px-3 text-sm text-[#0b1c30] outline-none transition focus:border-[#4b0009] focus:bg-white focus:ring-4 focus:ring-[#4b0009]/5"
            onChange={(event) => onLevelFilterChange(event.target.value)}
            placeholder="Cấp độ: B1, 5.0..."
            value={levelFilter}
          />
        </label>
        <CompactSelect onChange={onStatusFilterChange} options={[{ label: 'Mọi trạng thái', value: 'ALL' }, ...statusOptions.map((o) => ({ label: o.label, value: o.value }))]} prefix="Trạng thái" value={statusFilter} />
        <CompactSelect onChange={onUsageFilterChange} options={usageOptions.map((o) => ({ label: o.label, value: o.value }))} prefix="Lớp dùng" value={usageFilter} />
        {showPlatform ? (
          <CompactSelect onChange={onPlatformFilterChange} options={[{ label: 'Mọi nền tảng', value: 'ALL' }, ...platformOptions.map((o) => ({ label: o.label, value: o.value }))]} prefix="Nền tảng" value={platformFilter} />
        ) : (
          <CompactSelect onChange={onSortChange} options={sortOptions.map((o) => ({ label: o.label, value: o.value }))} prefix="Sắp xếp" value={sortBy} />
        )}
        <div className="flex gap-2">
          <button
            aria-label="Làm mới"
            className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#dcc0bf]/40 bg-white text-[#564241] transition hover:bg-[#eff4ff]"
            onClick={onRefresh}
            type="button"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>
      <div className="mt-3 flex flex-wrap items-center justify-between gap-2 border-t border-[#dcc0bf]/20 pt-3">
        <p className="text-sm text-[#564241]">
          <strong className="font-extrabold text-[#0b1c30]">{resultCount}</strong> khóa học phù hợp
        </p>
        <button className="text-xs font-bold text-[#730014] hover:underline" onClick={onReset} type="button">
          Xóa bộ lọc
        </button>
      </div>
    </Panel>
  );
}

function CompactSelect({ prefix, value, onChange, options }) {
  const normalized = options.map((option) => ({
    ...option,
    buttonLabel: `${prefix}: ${option.label}`,
  }));
  return (
    <BrandedSelect
      buttonClassName="h-10 rounded-lg border-[#dcc0bf]/50 bg-[#f8f9ff] py-2 text-sm shadow-none"
      onChange={onChange}
      options={normalized}
      value={value}
    />
  );
}

export function ProgramStatusPill({ status, label }) {
  const normalized = String(status || '').toUpperCase();
  const tone = normalized === 'PUBLISHED'
    ? 'bg-emerald-50 text-emerald-700 ring-emerald-100'
    : normalized === 'PENDING_REVIEW'
      ? 'bg-amber-50 text-amber-700 ring-amber-100'
      : normalized === 'REJECTED'
        ? 'bg-rose-50 text-rose-700 ring-rose-100'
        : normalized === 'ARCHIVED'
          ? 'bg-gray-100 text-gray-600 ring-gray-200'
          : 'bg-[#eff4ff] text-[#53627a] ring-[#dbeafe]';

  return (
    <span className={`inline-flex rounded-md px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-[0.08em] ring-1 ring-inset ${tone}`}>
      {label || status}
    </span>
  );
}

export function ProgramTable({
  programs,
  detailBasePath,
  loading,
  onEdit,
  onClone,
  onArchive,
  onPublish,
  working,
  page,
  totalPages,
  onPageChange,
  totalItems,
  pageSize,
}) {
  return (
    <Panel className="overflow-hidden rounded-xl border-[#dcc0bf]/30 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-[980px] w-full text-left">
          <thead className="bg-[#fbf3f4] text-[11px] uppercase tracking-[0.12em] text-[#8e7371]">
            <tr>
              {['Khóa học', 'Chương trình đào tạo', 'Cấp độ', 'Kế hoạch mở lớp', 'Trạng thái', 'Lớp đang dùng', 'Thao tác'].map((heading) => (
                <th className={`px-5 py-4 font-bold ${heading === 'Thao tác' ? 'text-right' : ''}`} key={heading}>{heading}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-[#dcc0bf]/15">
            {loading ? (
              Array.from({ length: 5 }).map((_, index) => (
                <tr key={index}>
                  {Array.from({ length: 7 }).map((__, cellIndex) => (
                    <td className="px-5 py-5" key={cellIndex}>
                      <div className="h-4 animate-pulse rounded bg-[#eef1f6]" />
                    </td>
                  ))}
                </tr>
              ))
            ) : programs.length ? (
              programs.map((program) => (
                <tr className="bg-white transition hover:bg-[#eff4ff]" key={program.id}>
                  <td className="px-5 py-5">
                    <div className="flex min-w-[240px] items-center">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-extrabold text-[#4b0009]">{program.title}</p>
                        <div className="mt-1 flex flex-wrap items-center gap-2">
                          <p className="text-xs text-[#564241]">{program.code}</p>
                          <span className={`rounded-md px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-wide ${program.deliveryType === 'VIRTUAL' || program.deliveryMode === 'VIRTUAL' ? 'bg-sky-50 text-sky-700' : 'bg-amber-50 text-amber-800'}`}>
                            {program.deliveryModeLabel || (program.deliveryType === 'VIRTUAL' ? 'Virtual' : 'Offline')}
                          </span>
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-5 py-5 text-sm text-[#0b1c30]">
                    <p className="max-w-[220px] truncate font-semibold text-[#0b1c30]">{program.curriculumProgramTitle || 'Chưa gắn'}</p>
                    <p className="mt-1 text-xs text-[#584140]">{program.curriculumProgramCode || program.curriculumProgramExamCategory || 'Chương trình đào tạo'}</p>
                  </td>
                  <td className="px-5 py-5 text-sm text-[#0b1c30]">{program.entryLevel || '—'}</td>
                  <td className="px-5 py-5 text-sm text-[#0b1c30]">
                    <p className="font-semibold">{program.plannedStartDate ? new Date(`${program.plannedStartDate}T00:00:00`).toLocaleDateString('vi-VN') : 'Chưa chốt ngày'}</p>
                    <p className="mt-1 max-w-[180px] truncate text-xs text-[#584140]">{program.plannedSchedule || `${program.capacity ?? program.maxCapacity ?? 30} học viên`}</p>
                  </td>
                  <td className="px-5 py-5">
                    <ProgramStatusPill label={program.statusLabel || program.status} status={program.status} />
                  </td>
                  <td className="px-5 py-5">
                    <span className={`inline-flex items-center rounded-lg px-2.5 py-1 text-xs font-bold ${program.activeClassroomCount > 0 ? 'bg-emerald-50 text-emerald-700' : 'bg-[#dce9ff] text-[#564241]'}`}>
                      {program.activeClassroomCount > 0 ? program.activeClassroomCount : (program.classroomCount || 0)}
                    </span>
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex items-center justify-end gap-1.5">
                      <button className="inline-flex items-center gap-1 rounded-lg border border-[#dcc0bf]/40 bg-white px-2.5 py-1.5 text-[11px] font-extrabold text-[#4b0009] transition hover:bg-[#eff4ff]" onClick={() => onEdit(program)} type="button">
                        <Edit3 className="h-3.5 w-3.5" />
                        Builder
                      </button>
                      <button className="inline-flex items-center justify-center rounded-lg border border-[#dcc0bf]/40 bg-white p-1.5 text-[#4b0009] hover:bg-[#eff4ff]" disabled={working} onClick={() => onClone(program)} type="button">
                        <Copy className="h-3.5 w-3.5" />
                      </button>
                      {program.status === 'DRAFT' || program.status === 'REJECTED' ? (
                        <button
                          className="inline-flex items-center gap-1 rounded-lg bg-[#4b0009] px-2.5 py-1.5 text-[11px] font-extrabold text-white hover:bg-[#730014] disabled:opacity-60"
                          disabled={working}
                          onClick={() => onPublish(program)}
                          type="button"
                        >
                          <Check className="h-3.5 w-3.5" />
                          Xuất bản
                        </button>
                      ) : null}
                      {program.status === 'PUBLISHED' ? (
                        <button
                          aria-label={`Lưu trữ ${program.title}`}
                          className="inline-flex items-center justify-center rounded-lg border border-rose-200 bg-white p-1.5 text-rose-700 hover:bg-rose-50 disabled:opacity-60"
                          disabled={working}
                          onClick={() => onArchive(program)}
                          title="Lưu trữ"
                          type="button"
                        >
                          <Archive className="h-3.5 w-3.5" />
                        </button>
                      ) : null}
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td className="px-5 py-14 text-center text-sm text-[#564241]" colSpan={7}>
                  Không có khóa học phù hợp với bộ lọc hiện tại.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="flex flex-col gap-3 border-t border-[#dcc0bf]/20 bg-[#eff4ff]/30 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
        <p className="text-sm text-[#564241]">
          Trang <span className="font-bold text-[#0b1c30]">{page}</span> / {totalPages}
          {' · '}
          <span className="font-bold text-[#0b1c30]">{totalItems}</span> khóa học
        </p>
        <Pagination
          page={page}
          totalPages={totalPages}
          onChange={onPageChange}
          totalItems={totalItems}
          pageSize={pageSize}
        />
      </div>
    </Panel>
  );
}

export function ProgramDetailHero({ program, isVirtual, listPath, actions }) {
  return (
    <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-6 shadow-sm">
      <div>
        <Link className="mb-5 inline-flex items-center gap-1.5 text-sm font-bold text-[#564241] hover:text-[#4b0009]" to={listPath}>
          <ChevronLeft className="h-4 w-4" />
          Quay lại danh sách
        </Link>
        <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <ProgramStatusPill label={program.statusLabel || program.status} status={program.status} />
              {program.activeClassroomCount > 0 ? (
                <span className="inline-flex items-center gap-1 rounded-lg bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700">
                  <Users className="h-3.5 w-3.5" />
                  {program.activeClassroomCount} lớp đang dùng
                </span>
              ) : null}
            </div>
            <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-tight text-[#0b1c30]">{program.title}</h1>
            <p className="mt-3 text-sm font-semibold text-[#584140]">{program.code} · {program.examCategory} · {program.totalSessions || 0} buổi</p>
          </div>
          {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
        </div>
      </div>
    </section>
  );
}

export function ProgramSection({ title, icon: Icon, children, action }) {
  return (
    <section className="rounded-xl border border-[#dcc0bf]/30 bg-white p-6 shadow-sm">
      <div className="mb-5 flex items-center justify-between gap-3">
        <h3 className="inline-flex items-center gap-2 font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">
          {Icon ? <Icon className="h-5 w-5 text-[#730014]" /> : null}
          {title}
        </h3>
        {action}
      </div>
      {children}
    </section>
  );
}

export function ProgramMetricGrid({ items }) {
  return (
    <dl className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {items.map((item) => (
        <div className="rounded-xl border border-[#dcc0bf]/25 bg-[#fcfbfb] px-4 py-3" key={item.label}>
          <dt className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#8b706e]">{item.label}</dt>
          <dd className="mt-1 text-sm font-extrabold text-[#0b1c30]">{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}

export function ApprovalProgramCard({ program, workingId, rejectingId, rejectReason, onRejectReasonChange, onApprove, onToggleReject, onReject, onViewDetail }) {
  const isVirtual = program.deliveryMode === 'VIRTUAL';

  return (
    <article className="overflow-hidden rounded-xl border border-[#dcc0bf]/30 bg-white shadow-[0_4px_12px_rgba(75,0,9,0.05)]">
      <div className={`h-1.5 ${isVirtual ? 'bg-gradient-to-r from-violet-500 to-purple-600' : 'bg-gradient-to-r from-[#730014] to-[#a01830]'}`} />
      <div className="p-6">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
          <div className="min-w-0 flex-1 space-y-3">
            <div className="flex flex-wrap items-center gap-2">
              <ProgramStatusPill label={program.statusLabel || program.status} status={program.status} />
              <span className="rounded-lg bg-[#dce9ff] px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wide text-[#564241]">
                {program.deliveryModeLabel || program.deliveryMode}
              </span>
            </div>
            <h3 className="font-['Manrope'] text-xl font-extrabold text-[#0b1c30]">{program.title}</h3>
            <p className="text-sm text-[#564241]">{program.code} · {program.examCategory} · {program.totalSessions || 0} buổi</p>
            {program.outcomes ? <p className="line-clamp-3 text-sm leading-7 text-[#584140]">{program.outcomes}</p> : null}
            <p className="text-xs text-[#8b706e]">
              Gửi bởi {program.submittedByName || 'Content Manager'}
              {program.submittedAt ? ` · ${new Date(program.submittedAt).toLocaleDateString('vi-VN')}` : ''}
            </p>
          </div>
          <div className="flex w-full flex-col gap-2 sm:w-[200px]">
            <button className="rounded-lg border border-[#dcc0bf]/40 bg-white px-4 py-2.5 text-xs font-extrabold text-[#4b0009] hover:bg-[#eff4ff]" onClick={() => onViewDetail(program)} type="button">
              Xem chi tiết
            </button>
            <button className="rounded-xl bg-[#4b0009] px-4 py-2.5 text-xs font-extrabold text-white hover:bg-[#730014] disabled:opacity-60" disabled={workingId === program.id} onClick={() => onApprove(program.id)} type="button">
              {workingId === program.id ? 'Đang duyệt...' : 'Duyệt & xuất bản'}
            </button>
            <button className="rounded-xl border border-rose-200 bg-white px-4 py-2.5 text-xs font-extrabold text-rose-700 hover:bg-rose-50" disabled={workingId === program.id} onClick={() => onToggleReject(program.id)} type="button">
              Từ chối
            </button>
          </div>
        </div>

        {rejectingId === program.id ? (
          <div className="mt-5 rounded-2xl border border-rose-100 bg-rose-50/40 p-4">
            <p className="mb-2 text-xs font-extrabold text-rose-800">Lý do từ chối (bắt buộc)</p>
            <textarea
              className="min-h-24 w-full rounded-xl border border-rose-200 bg-white px-4 py-3 text-sm outline-none focus:border-rose-400"
              onChange={(event) => onRejectReasonChange(event.target.value)}
              placeholder="Mô tả lý do để Content Manager chỉnh sửa..."
              value={rejectReason}
            />
            <button className="mt-3 rounded-xl bg-rose-700 px-4 py-2.5 text-xs font-extrabold text-white hover:bg-rose-800 disabled:opacity-60" disabled={workingId === program.id} onClick={() => onReject(program.id)} type="button">
              {workingId === program.id ? 'Đang gửi...' : 'Xác nhận từ chối'}
            </button>
          </div>
        ) : null}
      </div>
    </article>
  );
}
