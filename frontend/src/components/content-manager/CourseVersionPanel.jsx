import { useState } from 'react';
import { AlertCircle, CheckCircle2, Clock4, Eye, GitBranch, Plus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { COURSE_VERSION_STATUS_META, getCourseVersionLabel } from '../../utils/courseVersionUi';

export default function CourseVersionPanel({
  versions = [],
  busy = false,
  onCreateDraft,
  onSubmitReview,
  previewBasePath,
}) {
  const [changeNote, setChangeNote] = useState('');
  const draft = versions.find((v) => v.status === 'DRAFT');
  const pending = versions.find((v) => v.status === 'PENDING_REVIEW');
  const published = versions.find((v) => v.status === 'PUBLISHED');
  const canCreate = Boolean(published) && !draft && !pending;

  /* State-specific info */
  const info = draft ? {
    icon: AlertCircle,
    iconClass: 'text-[#730014]',
    title: `Bản nháp v${draft.versionNumber} đang chỉnh sửa`,
    sub: 'Học viên hiện tại không bị ảnh hưởng. Gửi duyệt khi đã hoàn chỉnh.',
    note: draft.reviewNote ? `Phản hồi: ${draft.reviewNote}` : null,
  } : pending ? {
    icon: Clock4,
    iconClass: 'text-amber-600',
    title: `v${pending.versionNumber} đang chờ Manager duyệt`,
    sub: 'Nội dung tạm khóa chỉnh sửa trong quá trình kiểm tra.',
    note: null,
  } : published ? {
    icon: CheckCircle2,
    iconClass: 'text-emerald-600',
    title: `${getCourseVersionLabel(published)} đang phát hành`,
    sub: 'Tạo phiên bản mới trước khi thay đổi cấu trúc hoặc nội dung.',
    note: null,
  } : {
    icon: GitBranch,
    iconClass: 'text-slate-400',
    title: 'Chưa có phiên bản',
    sub: 'Khóa học mới sẽ bắt đầu ở bản nháp v1.',
    note: null,
  };

  const InfoIcon = info.icon;

  return (
    <section className="overflow-hidden rounded-2xl border border-[#dfbfbd]/50 bg-white shadow-sm">

      {/* ── Header strip ── */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#f0e4e5] bg-[#fdf8f8] px-4 py-2.5">
        <span className="text-[11px] font-extrabold uppercase tracking-[0.15em] text-[#b09090]">
          Phiên bản nội dung
        </span>
        <div className="flex flex-wrap items-center gap-1.5">
          {versions.map((version) => {
            const meta = COURSE_VERSION_STATUS_META[version.status] || COURSE_VERSION_STATUS_META.RETIRED;
            const cls = `inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[11px] font-extrabold ${meta.className}`;
            return previewBasePath ? (
              <Link
                className={`${cls} transition hover:opacity-80`}
                key={version.id}
                title={`Xem phiên bản v${version.versionNumber}`}
                to={`${previewBasePath}?versionId=${version.id}`}
              >
                <Eye className="h-2.5 w-2.5" />
                {getCourseVersionLabel(version)}
              </Link>
            ) : (
              <span className={cls} key={version.id}>{getCourseVersionLabel(version)}</span>
            );
          })}
        </div>
      </div>

      {/* ── Info + actions ── */}
      <div className="flex flex-wrap items-center gap-x-6 gap-y-3 px-4 py-3 lg:flex-nowrap">

        {/* Info */}
        <div className="flex min-w-0 flex-1 items-start gap-2.5">
          <InfoIcon className={`mt-0.5 h-4 w-4 shrink-0 ${info.iconClass}`} />
          <div className="min-w-0">
            <p className="text-sm font-bold text-[#2b2828]">{info.title}</p>
            <p className="mt-0.5 text-xs leading-5 text-[#8b706e]">{info.sub}</p>
            {info.note ? (
              <p className="mt-1.5 rounded-lg bg-rose-50 px-2.5 py-1.5 text-xs font-semibold text-rose-700">
                {info.note}
              </p>
            ) : null}
          </div>
        </div>

        {/* Actions */}
        <div className="flex shrink-0 flex-wrap items-end gap-2">
          {canCreate ? (
            <div className="flex flex-col gap-1.5">
              <label className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#b09090]" htmlFor="cv-note">
                Tóm tắt thay đổi
              </label>
              <div className="flex gap-2">
                <input
                  className="w-48 rounded-xl border border-[#dfbfbd] bg-[#fffafa] px-3 py-2 text-sm outline-none transition focus:border-[#730014] focus:bg-white"
                  id="cv-note"
                  maxLength={700}
                  onChange={(e) => setChangeNote(e.target.value)}
                  placeholder="Mô tả ngắn…"
                  value={changeNote}
                />
                <button
                  className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014] bg-white px-3 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff2f3] disabled:opacity-60"
                  disabled={busy}
                  onClick={() => onCreateDraft?.(changeNote.trim())}
                  type="button"
                >
                  <Plus className="h-3.5 w-3.5" />
                  Tạo phiên bản
                </button>
              </div>
            </div>
          ) : null}

          {draft && onSubmitReview ? (
            <button
              className="inline-flex items-center gap-2 rounded-xl border border-[#730014] bg-white px-4 py-2.5 text-sm font-extrabold text-[#730014] transition hover:bg-[#fff2f3] disabled:opacity-60"
              disabled={busy}
              onClick={onSubmitReview}
              type="button"
            >
              Gửi duyệt v{draft.versionNumber}
            </button>
          ) : null}
        </div>

      </div>
    </section>
  );
}
