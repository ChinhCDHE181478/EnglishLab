import { useEffect, useMemo, useState } from 'react';
import {
  Building2,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Eye,
  GraduationCap,
  Link2,
  RefreshCw,
  ShieldCheck,
  Users,
  X,
  XCircle,
} from 'lucide-react';
import enrollmentRequestApi from '../../api/enrollmentRequestApi';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  ERROR_NOTICE_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

const statusTabs = [
  { label: 'Chờ duyệt', value: 'PENDING_APPROVAL' },
  { label: 'Đã duyệt', value: 'APPROVED' },
  { label: 'Đã từ chối', value: 'REJECTED' },
];

const statusMeta = {
  PENDING_APPROVAL: { label: 'Chờ duyệt', className: 'border-amber-200 bg-amber-50 text-amber-800' },
  APPROVED: { label: 'Đã duyệt', className: 'border-emerald-200 bg-emerald-50 text-emerald-700' },
  REJECTED: { label: 'Đã từ chối', className: 'border-rose-200 bg-rose-50 text-rose-700' },
};

const deliveryLabels = {
  OFFLINE: 'Offline',
  VIRTUAL: 'Virtual',
};

export default function ManagerClassroomProposalsPage() {
  const [activeStatus, setActiveStatus] = useState('PENDING_APPROVAL');
  const [proposalsByStatus, setProposalsByStatus] = useState({
    PENDING_APPROVAL: [],
    APPROVED: [],
    REJECTED: [],
  });
  const [selectedProposal, setSelectedProposal] = useState(null);
  const [reviewAction, setReviewAction] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [pending, approved, rejected] = await Promise.all([
        enrollmentRequestApi.listManagerClassroomProposals('PENDING_APPROVAL'),
        enrollmentRequestApi.listManagerClassroomProposals('APPROVED'),
        enrollmentRequestApi.listManagerClassroomProposals('REJECTED'),
      ]);
      setProposalsByStatus({
        PENDING_APPROVAL: pending,
        APPROVED: approved,
        REJECTED: rejected,
      });
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể tải hàng chờ duyệt đề xuất lớp.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const visibleProposals = proposalsByStatus[activeStatus] || [];
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    visibleProposals,
    8,
    activeStatus,
  );

  const stats = useMemo(() => ({
    pending: proposalsByStatus.PENDING_APPROVAL.length,
    approved: proposalsByStatus.APPROVED.length,
    rejected: proposalsByStatus.REJECTED.length,
    capacity: proposalsByStatus.PENDING_APPROVAL.reduce(
      (total, proposal) => total + Number(proposal.capacity || 0),
      0,
    ),
  }), [proposalsByStatus]);

  const openDetails = (proposal) => {
    setSelectedProposal(proposal);
    setReviewAction(null);
    setRejectionReason('');
    setError('');
  };

  const closeDetails = () => {
    if (submitting) return;
    setSelectedProposal(null);
    setReviewAction(null);
    setRejectionReason('');
  };

  const beginReview = (action) => {
    setReviewAction(action);
    setRejectionReason('');
    setError('');
  };

  const submitReview = async () => {
    if (!selectedProposal || !reviewAction) return;
    if (reviewAction === 'reject' && !rejectionReason.trim()) {
      setError('Vui lòng nêu rõ lý do từ chối để Staff có thể chỉnh sửa và gửi lại.');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      const updated = reviewAction === 'approve'
        ? await enrollmentRequestApi.approveClassroomProposal(selectedProposal.id)
        : await enrollmentRequestApi.rejectClassroomProposal(selectedProposal.id, rejectionReason.trim());
      const targetStatus = updated.approvalStatus;
      setProposalsByStatus((current) => ({
        ...current,
        PENDING_APPROVAL: current.PENDING_APPROVAL.filter((item) => item.id !== updated.id),
        [targetStatus]: [
          updated,
          ...(current[targetStatus] || []).filter((item) => item.id !== updated.id),
        ],
      }));
      setSuccess(reviewAction === 'approve'
        ? `${updated.proposalCode} đã được duyệt và tạo lớp chính thức.`
        : `${updated.proposalCode} đã được trả lại Staff kèm lý do.`);
      closeDetailsAfterSubmit();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể hoàn tất thao tác duyệt đề xuất lớp.');
    } finally {
      setSubmitting(false);
    }
  };

  const closeDetailsAfterSubmit = () => {
    setSelectedProposal(null);
    setReviewAction(null);
    setRejectionReason('');
  };

  return (
    <div className="space-y-5">
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-50 text-amber-700">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <span className="font-['Manrope'] text-2xl font-black text-[#2b2828]">{stats.pending}</span>
          </div>
          <p className="mt-3 text-xs font-bold uppercase tracking-wider text-[#8b706e]">Chờ duyệt</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-700">
              <Users className="h-5 w-5" />
            </div>
            <span className="font-['Manrope'] text-2xl font-black text-[#2b2828]">{stats.capacity}</span>
          </div>
          <p className="mt-3 text-xs font-bold uppercase tracking-wider text-[#8b706e]">Tổng sức chứa</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700">
              <CheckCircle2 className="h-5 w-5" />
            </div>
            <span className="font-['Manrope'] text-2xl font-black text-[#2b2828]">{stats.approved}</span>
          </div>
          <p className="mt-3 text-xs font-bold uppercase tracking-wider text-[#8b706e]">Đã duyệt</p>
        </div>
        <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-rose-50 text-rose-700">
              <XCircle className="h-5 w-5" />
            </div>
            <span className="font-['Manrope'] text-2xl font-black text-[#2b2828]">{stats.rejected}</span>
          </div>
          <p className="mt-3 text-xs font-bold uppercase tracking-wider text-[#8b706e]">Đã từ chối</p>
        </div>
      </section>

      <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex gap-1.5 overflow-x-auto">
          {statusTabs.map((tab) => (
            <button
              className={`shrink-0 rounded-xl px-4 py-2.5 text-xs font-extrabold transition ${
                activeStatus === tab.value
                  ? 'bg-[#4b0009] text-white shadow-sm'
                  : 'bg-slate-50 text-slate-600 hover:bg-slate-100'
              }`}
              key={tab.value}
              onClick={() => setActiveStatus(tab.value)}
              type="button"
            >
              {tab.label} ({proposalsByStatus[tab.value]?.length || 0})
            </button>
          ))}
        </div>
        <button
          className="inline-flex h-11 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 text-xs font-bold text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
          disabled={loading}
          onClick={load}
          type="button"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </section>

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      {loading ? (
        <div className="grid gap-4 xl:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <div className="h-72 animate-pulse rounded-2xl bg-slate-100" key={index} />
          ))}
        </div>
      ) : null}

      {!loading && !pageItems.length ? (
        <section className="flex min-h-[380px] flex-col items-center justify-center rounded-[28px] border border-dashed border-slate-300 bg-white px-6 text-center">
          <ShieldCheck className="h-14 w-14 text-slate-300" />
          <h2 className="mt-4 font-['Manrope'] text-2xl font-black text-[#0b1c30]">
            Không có đề xuất trong tab này
          </h2>
          <p className="mt-2 max-w-xl text-sm leading-6 text-slate-500">
            Mỗi đề xuất chỉ xuất hiện ở một trạng thái. Khi duyệt hoặc từ chối, bản ghi sẽ rời
            hàng chờ và được lưu đúng tab lịch sử tương ứng.
          </p>
        </section>
      ) : null}

      {!loading && pageItems.length ? (
        <div className="space-y-4">
          <div className="grid gap-4 xl:grid-cols-2">
            {pageItems.map((proposal) => (
              <ProposalCard key={proposal.id} onOpen={() => openDetails(proposal)} proposal={proposal} />
            ))}
          </div>
          <div className="rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
            <Pagination onChange={setPage} page={page} pageSize={8} totalItems={totalItems} totalPages={totalPages} />
          </div>
        </div>
      ) : null}

      {selectedProposal ? (
        <ProposalDetailsModal
          action={reviewAction}
          error={error}
          onBeginReview={beginReview}
          onClose={closeDetails}
          onReasonChange={setRejectionReason}
          onSubmit={submitReview}
          proposal={selectedProposal}
          reason={rejectionReason}
          submitting={submitting}
        />
      ) : null}
    </div>
  );
}

function ProposalCard({ proposal, onOpen }) {
  return (
    <article className="group rounded-[24px] border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-[#d8aab2] hover:shadow-md">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={proposal.approvalStatus} />
            <span className="text-xs font-bold text-slate-400">{proposal.proposalCode}</span>
          </div>
          <h2 className="mt-3 truncate font-['Manrope'] text-xl font-black text-[#0b1c30]">
            {proposal.title}
          </h2>
          <p className="mt-1 truncate text-sm font-semibold text-slate-500">{proposal.courseOfferingTitle}</p>
        </div>
        <span className="shrink-0 rounded-xl bg-[#fff4f5] px-3 py-1.5 text-xs font-extrabold text-[#730014]">
          {deliveryLabels[proposal.deliveryType] || proposal.deliveryType}
        </span>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3">
        <Metric icon={Users} label="Sức chứa" value={`${proposal.capacity} học viên`} />
        <Metric icon={GraduationCap} label="Giáo viên" value={proposal.primaryTeacherName || 'Chưa chọn'} />
        <Metric icon={CalendarDays} label="Khoảng học" value={`${formatDate(proposal.plannedStartDate)} – ${formatDate(proposal.plannedEndDate)}`} />
        <Metric icon={Clock3} label="Số buổi" value={`${proposal.plannedSessionCount || 0} buổi`} />
      </div>

      <div className="mt-4 flex items-center justify-between gap-4 border-t border-slate-100 pt-4">
        <div className="min-w-0 text-xs text-slate-500">
          <p className="truncate font-bold text-slate-700">{proposal.primaryTeacherName || 'Chưa chọn giáo viên'}</p>
          <p className="mt-1 truncate">Staff: {proposal.createdByName || 'Không rõ'}</p>
        </div>
        <button className={PRIMARY_BUTTON_CLASS} onClick={onOpen} type="button">
          <Eye className="h-4 w-4" />
          Xem chi tiết
        </button>
      </div>
    </article>
  );
}

function ProposalDetailsModal({ action, error, onBeginReview, onClose, onReasonChange, onSubmit, proposal, reason, submitting }) {
  const canReview = proposal.approvalStatus === 'PENDING_APPROVAL';
  const isVirtual = proposal.deliveryType === 'VIRTUAL';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm">
      <section aria-modal="true" className="max-h-[94vh] w-full max-w-5xl overflow-y-auto rounded-[28px] bg-white shadow-2xl" role="dialog">
        <header className="sticky top-0 z-10 flex items-start justify-between gap-4 border-b border-slate-100 bg-white/95 px-6 py-5 backdrop-blur">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge status={proposal.approvalStatus} />
              <span className="text-xs font-bold text-slate-400">{proposal.proposalCode}</span>
            </div>
            <h2 className="mt-2 font-['Manrope'] text-2xl font-black text-[#0b1c30]">{proposal.title}</h2>
            <p className="mt-1 text-sm text-slate-500">{proposal.courseOfferingTitle}</p>
          </div>
          <button className="rounded-xl p-2 text-slate-400 transition hover:bg-slate-100" disabled={submitting} onClick={onClose} type="button">
            <X className="h-5 w-5" />
          </button>
        </header>

        {error ? <div className={`mx-6 mt-6 ${ERROR_NOTICE_CLASS}`}>{error}</div> : null}

        <div className="grid gap-6 p-6 lg:grid-cols-[1.05fr_0.95fr]">
          <div className="space-y-5">
            <Section title="Kế hoạch lớp học">
              <div className="grid gap-3 sm:grid-cols-2">
                <Detail icon={Users} label="Sức chứa" value={`${proposal.capacity} học viên`} />
                <Detail icon={Clock3} label="Số buổi dự kiến" value={`${proposal.plannedSessionCount || 0} buổi`} />
                <Detail icon={CalendarDays} label="Ngày học" value={`${formatDate(proposal.plannedStartDate)} – ${formatDate(proposal.plannedEndDate)}`} />
                <Detail icon={Clock3} label="Lịch lặp" value={`${formatWeekdays(proposal.weekdays)} · ${formatTime(proposal.sessionStartTime)}–${formatTime(proposal.sessionEndTime)}`} />
                <Detail icon={GraduationCap} label="Giáo viên" value={proposal.primaryTeacherName || 'Chưa chọn'} />
                <Detail
                  icon={isVirtual ? Link2 : Building2}
                  label={isVirtual ? 'Phòng học Virtual' : 'Phòng học Offline'}
                  value={isVirtual
                    ? proposal.virtualMeetingUrl || 'Chưa có link'
                    : [proposal.roomName, proposal.offlineAddress].filter(Boolean).join(' · ') || 'Chưa chọn phòng'}
                />
              </div>
              <div className="mt-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm">
                <span className="font-extrabold text-[#0b1c30]">Tổng số buổi dự kiến:</span>{' '}
                <span className="font-bold text-[#730014]">{proposal.plannedSessionCount || 0}</span>
              </div>
            </Section>

            <Section title="Phạm vi phê duyệt">
              <p className="rounded-2xl border border-sky-200 bg-sky-50 p-4 text-sm leading-6 text-sky-900">Đề xuất này chỉ xin duyệt việc mở lớp và nguồn lực. Học viên chưa được gắn vào proposal; sau tư vấn/test bên ngoài, Staff sẽ xếp từng học viên vào lớp phù hợp.</p>
            </Section>
          </div>

          <div className="space-y-5">
            <Section title="Thông tin trình duyệt">
              <div className="space-y-3 text-sm">
                <ReviewRow label="Người tạo" value={proposal.createdByName || 'Không rõ'} />
                <ReviewRow label="Gửi duyệt lúc" value={formatDateTime(proposal.submittedAt)} />
                <ReviewRow label="Ghi chú Staff" value={proposal.staffNote || 'Không có ghi chú'} />
                {proposal.reviewedAt ? <ReviewRow label="Người duyệt" value={proposal.reviewedByName || 'Không rõ'} /> : null}
                {proposal.reviewedAt ? <ReviewRow label="Duyệt lúc" value={formatDateTime(proposal.reviewedAt)} /> : null}
                {proposal.reviewNote ? <ReviewRow label="Phản hồi" value={proposal.reviewNote} /> : null}
                {proposal.approvedClassroomId ? <ReviewRow label="Lớp đã tạo" value={`#${proposal.approvedClassroomId}`} /> : null}
              </div>
            </Section>

            {canReview ? (
              <Section title="Quyết định phê duyệt">
                {!action ? (
                  <div className="space-y-3">
                    <p className="text-sm leading-6 text-slate-500">
                      Khi duyệt, hệ thống sẽ tạo lớp và toàn bộ buổi học trong một transaction. Nếu phát hiện
                      trùng lịch giáo viên hoặc phòng, thao tác sẽ dừng và không tạo dữ liệu dở dang.
                    </p>
                    <button className="flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-4 py-3 text-sm font-extrabold text-white transition hover:bg-emerald-700" onClick={() => onBeginReview('approve')} type="button">
                      <CheckCircle2 className="h-4 w-4" />
                      Duyệt và tạo lớp chính thức
                    </button>
                    <button className="flex w-full items-center justify-center gap-2 rounded-2xl border border-rose-200 bg-white px-4 py-3 text-sm font-extrabold text-rose-700 transition hover:bg-rose-50" onClick={() => onBeginReview('reject')} type="button">
                      <XCircle className="h-4 w-4" />
                      Từ chối đề xuất
                    </button>
                  </div>
                ) : (
                  <div>
                    <div className={`rounded-2xl border p-4 text-sm font-bold ${action === 'approve' ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-rose-200 bg-rose-50 text-rose-800'}`}>
                      {action === 'approve'
                        ? 'Xác nhận tạo lớp chính thức và lịch học; lớp sẽ xuất hiện trên Lịch khai giảng.'
                        : 'Đề xuất sẽ quay lại Staff để chỉnh sửa và gửi duyệt lại.'}
                    </div>
                    {action === 'reject' ? (
                      <label className="mt-4 block">
                        <span className="mb-2 block text-xs font-extrabold uppercase tracking-[0.12em] text-slate-500">Lý do từ chối</span>
                        <textarea
                          autoFocus
                          className={TEXTAREA_CLASS}
                          onChange={(event) => onReasonChange(event.target.value)}
                          placeholder="Nêu rõ lịch, nguồn lực hoặc nhóm học viên cần điều chỉnh..."
                          rows={5}
                          value={reason}
                        />
                      </label>
                    ) : null}
                    <div className="mt-4 flex justify-end gap-2">
                      <button className={SECONDARY_BUTTON_CLASS} disabled={submitting} onClick={() => onBeginReview(null)} type="button">Quay lại</button>
                      <button
                        className={`inline-flex items-center gap-2 rounded-2xl px-4 py-2.5 text-sm font-extrabold text-white transition disabled:opacity-60 ${action === 'approve' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'}`}
                        disabled={submitting}
                        onClick={onSubmit}
                        type="button"
                      >
                        {submitting ? 'Đang xử lý...' : action === 'approve' ? 'Xác nhận duyệt' : 'Xác nhận từ chối'}
                      </button>
                    </div>
                  </div>
                )}
              </Section>
            ) : null}
          </div>
        </div>
      </section>
    </div>
  );
}

function HeroStat({ label, value }) {
  return <div className="rounded-2xl border border-white/10 bg-white/10 p-4 text-center"><p className="font-['Manrope'] text-3xl font-black">{value}</p><p className="mt-1 text-[10px] font-bold uppercase tracking-[0.14em] text-white/60">{label}</p></div>;
}

function StatusBadge({ status }) {
  const meta = statusMeta[status] || { label: status || 'Không rõ', className: 'border-slate-200 bg-slate-50 text-slate-600' };
  return <span className={`rounded-full border px-3 py-1 text-xs font-extrabold ${meta.className}`}>{meta.label}</span>;
}

function Metric({ icon: Icon, label, value }) {
  return <div className="rounded-2xl bg-slate-50 p-3"><div className="flex items-center gap-2 text-slate-400"><Icon className="h-4 w-4" /><span className="text-[10px] font-extrabold uppercase tracking-[0.1em]">{label}</span></div><p className="mt-2 text-sm font-extrabold text-slate-800">{value}</p></div>;
}

function Section({ children, title }) {
  return <section className="rounded-[22px] border border-slate-200 bg-white p-4 shadow-sm"><h3 className="font-['Manrope'] text-base font-black text-[#0b1c30]">{title}</h3><div className="mt-4">{children}</div></section>;
}

function Detail({ icon: Icon, label, value }) {
  return <div className="rounded-2xl bg-slate-50 p-3"><div className="flex items-center gap-2 text-slate-400"><Icon className="h-4 w-4" /><span className="text-[10px] font-extrabold uppercase tracking-[0.1em]">{label}</span></div><p className="mt-2 break-words text-sm font-bold leading-5 text-slate-700">{value}</p></div>;
}

function ReviewRow({ label, value }) {
  return <div className="grid gap-1 border-b border-slate-100 pb-3 last:border-0 last:pb-0 sm:grid-cols-[130px_1fr]"><span className="font-semibold text-slate-400">{label}</span><span className="break-words font-bold text-slate-700">{value}</span></div>;
}

function formatDate(value) {
  if (!value) return 'Chưa có';
  return new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatDateTime(value) {
  if (!value) return 'Chưa có';
  return new Date(value).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : '--:--';
}

function formatWeekdays(values = []) {
  const labels = { MONDAY: 'T2', TUESDAY: 'T3', WEDNESDAY: 'T4', THURSDAY: 'T5', FRIDAY: 'T6', SATURDAY: 'T7', SUNDAY: 'CN' };
  return values.map((value) => labels[value] || value).join(', ') || 'Chưa có lịch';
}
