import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, RefreshCw, XCircle } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import BrandedSelect from '../../components/ui/BrandedSelect';

const attendanceOptions = [
  { label: 'Có mặt', value: 'PRESENT' },
  { label: 'Đi muộn', value: 'LATE' },
  { label: 'Vắng có phép', value: 'EXCUSED' },
  { label: 'Vắng', value: 'ABSENT' },
];

const attendanceLabels = {
  PRESENT: 'Có mặt',
  LATE: 'Đi muộn',
  EXCUSED: 'Vắng có phép',
  ABSENT: 'Vắng',
};

export default function TrainingManagerAttendanceDisputesPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [modal, setModal] = useState(null);
  const [reviewNote, setReviewNote] = useState('');
  const [attendanceStatus, setAttendanceStatus] = useState('PRESENT');
  const [submitting, setSubmitting] = useState(false);

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      setItems(await classroomApi.listPendingAttendanceDisputes());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được danh sách khiếu nại điểm danh.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadItems();
  }, []);

  const stats = useMemo(() => ({
    pending: items.length,
    absent: items.filter((item) => item.currentAttendanceStatus === 'ABSENT').length,
    late: items.filter((item) => item.currentAttendanceStatus === 'LATE').length,
  }), [items]);

  const openReview = (item, action) => {
    setModal({ item, action });
    setAttendanceStatus(action === 'approve' ? (item.currentAttendanceStatus === 'ABSENT' ? 'PRESENT' : item.currentAttendanceStatus || 'PRESENT') : 'ABSENT');
    setReviewNote(action === 'approve' ? 'Đã đối chiếu và cập nhật điểm danh.' : '');
    setError('');
    setSuccess('');
  };

  const closeModal = () => {
    setModal(null);
    setReviewNote('');
  };

  const submitReview = async () => {
    if (!modal) return;
    if (modal.action === 'reject' && !reviewNote.trim()) {
      setError('Vui lòng nhập lý do từ chối khiếu nại.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await classroomApi.reviewAttendanceDispute(modal.item.id, {
        status: modal.action === 'approve' ? 'APPROVED' : 'REJECTED',
        reviewNote: reviewNote.trim(),
        attendanceStatus: modal.action === 'approve' ? attendanceStatus : undefined,
      });
      setSuccess(modal.action === 'approve' ? 'Đã duyệt và cập nhật điểm danh.' : 'Đã từ chối khiếu nại.');
      closeModal();
      await loadItems();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không xử lý được khiếu nại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-[28px] border border-slate-200 bg-gradient-to-br from-amber-50 via-white to-rose-50 p-6 shadow-sm">
        <div className="grid gap-6 lg:grid-cols-[1.5fr_1fr] lg:items-end">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-amber-200 bg-white px-3 py-1 text-xs font-extrabold uppercase tracking-[0.16em] text-amber-700">
              <AlertTriangle className="h-4 w-4" />
              Attendance dispute desk
            </div>
            <h2 className="mt-5 font-['Manrope'] text-3xl font-black text-slate-900">Xử lý khiếu nại điểm danh</h2>
            <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600">
              Mỗi khiếu nại cần được duyệt có ghi chú, cập nhật điểm danh có audit trail để tránh sửa tay không dấu vết.
            </p>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Stat label="Chờ xử lý" value={stats.pending} />
            <Stat label="Đang vắng" value={stats.absent} />
            <Stat label="Đang muộn" value={stats.late} />
          </div>
        </div>
      </section>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          {error ? <Notice tone="error">{error}</Notice> : null}
          {success ? <Notice tone="success">{success}</Notice> : null}
        </div>
        <button
          className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-extrabold text-[#730014] transition hover:bg-[#fff4f5]"
          onClick={loadItems}
          type="button"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </div>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {loading ? (
          <div className="flex min-h-[360px] items-center justify-center text-sm font-bold text-slate-500">
            Đang tải khiếu nại điểm danh...
          </div>
        ) : !items.length ? (
          <div className="flex min-h-[360px] flex-col items-center justify-center px-6 text-center">
            <CheckCircle2 className="h-14 w-14 text-emerald-600" />
            <h3 className="mt-4 font-['Manrope'] text-2xl font-extrabold text-slate-900">Không có khiếu nại chờ xử lý</h3>
            <p className="mt-2 max-w-xl text-sm leading-7 text-slate-500">Hàng đợi sạch. Các khiếu nại mới từ học viên sẽ xuất hiện tại đây.</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {items.map((item) => (
              <article className="grid gap-4 p-5 lg:grid-cols-[1.2fr_1fr_auto] lg:items-center" key={item.id}>
                <div>
                  <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014]">
                    {item.studentName || `Học viên #${item.studentId}`}
                  </p>
                  <h3 className="mt-2 font-['Manrope'] text-xl font-extrabold text-slate-900">
                    {item.sessionTitle || `Buổi học #${item.sessionId}`}
                  </h3>
                  <p className="mt-2 rounded-2xl bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-600">{item.reason}</p>
                </div>
                <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-1">
                  <Info label="Trạng thái hiện tại" value={attendanceLabels[item.currentAttendanceStatus] || item.currentAttendanceStatus || '—'} />
                  <Info label="Gửi lúc" value={formatDateTime(item.createdAt)} />
                </div>
                <div className="flex flex-wrap justify-end gap-2">
                  <button
                    className="inline-flex items-center gap-2 rounded-2xl bg-emerald-600 px-4 py-3 text-sm font-extrabold text-white transition hover:bg-emerald-700"
                    onClick={() => openReview(item, 'approve')}
                    type="button"
                  >
                    <CheckCircle2 className="h-4 w-4" />
                    Duyệt
                  </button>
                  <button
                    className="inline-flex items-center gap-2 rounded-2xl border border-rose-200 bg-white px-4 py-3 text-sm font-extrabold text-rose-700 transition hover:bg-rose-50"
                    onClick={() => openReview(item, 'reject')}
                    type="button"
                  >
                    <XCircle className="h-4 w-4" />
                    Từ chối
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {modal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 px-4 py-8 backdrop-blur-sm">
          <section className="w-full max-w-xl rounded-[28px] border border-slate-200 bg-white p-6 shadow-2xl">
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#730014]">
              {modal.action === 'approve' ? 'Duyệt khiếu nại' : 'Từ chối khiếu nại'}
            </p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-slate-900">{modal.item.studentName}</h3>
            {modal.action === 'approve' ? (
              <label className="mt-5 block">
                <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">Cập nhật trạng thái điểm danh</span>
                <BrandedSelect
                  onChange={(event) => setAttendanceStatus(event.target.value)}
                  options={attendanceOptions}
                  value={attendanceStatus}
                />
              </label>
            ) : null}
            <label className="mt-5 block">
              <span className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-slate-500">
                {modal.action === 'approve' ? 'Ghi chú xử lý' : 'Lý do từ chối'}
              </span>
              <textarea
                className="min-h-28 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-[#730014] focus:bg-white"
                onChange={(event) => setReviewNote(event.target.value)}
                value={reviewNote}
              />
            </label>
            <div className="mt-6 flex justify-end gap-3">
              <button className="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-extrabold text-slate-600" onClick={closeModal} type="button">
                Hủy
              </button>
              <button
                className={`rounded-2xl px-5 py-3 text-sm font-extrabold text-white disabled:opacity-60 ${modal.action === 'approve' ? 'bg-emerald-600' : 'bg-rose-600'}`}
                disabled={submitting}
                onClick={submitReview}
                type="button"
              >
                {submitting ? 'Đang xử lý...' : 'Xác nhận'}
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}

function Stat({ label, value }) {
  return (
    <div className="rounded-2xl border border-white bg-white/80 p-4 text-center shadow-sm">
      <p className="font-['Manrope'] text-3xl font-black text-[#4b0009]">{value}</p>
      <p className="mt-1 text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{label}</p>
    </div>
  );
}

function Info({ label, value }) {
  return (
    <div className="rounded-2xl bg-slate-50 px-4 py-3">
      <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-slate-400">{label}</p>
      <p className="mt-1 text-sm font-bold text-slate-800">{value}</p>
    </div>
  );
}

function Notice({ children, tone }) {
  const className = tone === 'error'
    ? 'border-rose-200 bg-rose-50 text-rose-700'
    : 'border-emerald-200 bg-emerald-50 text-emerald-700';
  return <div className={`rounded-2xl border px-5 py-3 text-sm font-bold ${className}`}>{children}</div>;
}

function formatDateTime(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString('vi-VN');
}
