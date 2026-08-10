import { useEffect, useMemo, useState } from 'react';
import {
  BarChart3,
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
  const [demandReport, setDemandReport] = useState([]);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [pending, approved, rejected, demand] = await Promise.all([
        enrollmentRequestApi.listManagerClassroomProposals('PENDING_APPROVAL'),
        enrollmentRequestApi.listManagerClassroomProposals('APPROVED'),
        enrollmentRequestApi.listManagerClassroomProposals('REJECTED'),
        enrollmentRequestApi.getManagerEnrollmentDemand(),
      ]);
      setProposalsByStatus({
        PENDING_APPROVAL: pending,
        APPROVED: approved,
        REJECTED: rejected,
      });
      setDemandReport(demand);
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
      setError('Vui lòng nêu rõ lý do từ chối để người đề xuất có thể chỉnh sửa và gửi lại.');
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
        ? selectedProposal.deliveryMode === 'VIRTUAL'
          ? `${updated.proposalCode} đã được duyệt và tạo lớp chính thức. Phòng Google Meet đang được tạo tự động cho từng buổi học.`
          : `${updated.proposalCode} đã được duyệt và tạo lớp chính thức.`
        : `${updated.proposalCode} đã được trả lại kèm lý do.`);
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

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
          <div>
            <div className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5 text-[#730014]" />
              <h2 className="font-['Manrope'] text-lg font-black text-[#0b1c30]">Báo cáo nhu cầu mở lớp</h2>
            </div>
            <p className="mt-1 text-xs text-slate-500">Tổng hợp trực tiếp từ hồ sơ học viên đăng ký từng khóa học.</p>
          </div>
          <span className="rounded-full bg-[#fff0f2] px-3 py-1 text-xs font-extrabold text-[#730014]">{demandReport.length} khóa học có nhu cầu</span>
        </div>
        {demandReport.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="bg-slate-50 text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
                <tr><th className="px-5 py-3">Khóa học</th><th className="px-5 py-3 text-center">Tổng đăng ký</th><th className="px-5 py-3 text-center">Chờ liên hệ</th><th className="px-5 py-3 text-center">Đã hẹn test</th><th className="px-5 py-3 text-center">Đủ điều kiện</th><th className="px-5 py-3 text-center">Đã xếp lớp</th><th className="px-5 py-3 text-center">Ước tính lớp cần mở</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {demandReport.map((item) => (
                  <tr key={item.courseOfferingId}>
                    <td className="px-5 py-4"><p className="font-extrabold text-slate-800">{item.courseOfferingTitle}</p><p className="mt-1 text-xs text-slate-400">{item.courseOfferingCode} · {deliveryLabels[item.deliveryMode] || item.deliveryMode}</p></td>
                    <td className="px-5 py-4 text-center font-black text-[#730014]">{item.totalRegistrations}</td>
                    <td className="px-5 py-4 text-center">{item.awaitingContact}</td>
                    <td className="px-5 py-4 text-center">{item.testsScheduled}</td>
                    <td className="px-5 py-4 text-center font-bold text-emerald-700">{item.qualifiedForClass}</td>
                    <td className="px-5 py-4 text-center">{item.assigned}</td>
                    <td className="px-5 py-4 text-center"><span className="rounded-lg bg-blue-50 px-3 py-1.5 font-extrabold text-blue-700">{item.suggestedClassCount} lớp</span><p className="mt-1 text-[10px] text-slate-400">{item.classCapacity} học viên/lớp</p></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="px-5 py-8 text-center text-sm text-slate-500">Chưa có hồ sơ đăng ký theo khóa học để tổng hợp.</div>
        )}
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
          <p className="mt-1 truncate">Người đề xuất: {proposal.createdByName || 'Không rõ'}</p>
        </div>
        <button className={PRIMARY_BUTTON_CLASS} onClick={onOpen} type="button">
          <Eye className="h-4 w-4" />
          Xem chi tiết
        </button>
      </div>
    </article>
  );
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
