import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, RefreshCw, XCircle } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import BrandedSelect from '../ui/BrandedSelect';

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

export default function TeacherAttendanceDisputesSection({ classroomId, onMessage }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modal, setModal] = useState(null);
  const [reviewNote, setReviewNote] = useState('');
  const [attendanceStatus, setAttendanceStatus] = useState('PRESENT');
  const [submitting, setSubmitting] = useState(false);

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      setItems(await classroomApi.listAttendanceDisputesForClass(classroomId));
    } catch (err) {
      setItems([]);
      setError(err?.response?.data?.message || 'Không tải được khiếu nại điểm danh của lớp.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadItems();
  }, [classroomId]);

  const pendingItems = useMemo(
    () => items.filter((item) => item.status === 'PENDING'),
    [items],
  );

  const openReview = (item, action) => {
    setModal({ item, action });
    setAttendanceStatus(
      action === 'approve' && item.currentAttendanceStatus !== 'ABSENT'
        ? item.currentAttendanceStatus || 'PRESENT'
        : 'PRESENT',
    );
    setReviewNote(action === 'approve' ? 'Đã đối chiếu với buổi học và cập nhật điểm danh.' : '');
    setError('');
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
      onMessage?.(modal.action === 'approve'
        ? 'Đã duyệt khiếu nại và cập nhật điểm danh.'
        : 'Đã từ chối khiếu nại điểm danh.');
      closeModal();
      await loadItems();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không xử lý được khiếu nại điểm danh.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4 rounded-2xl border border-amber-100 bg-amber-50/30 p-5">
        <div className="flex items-start gap-3">
          <span className="rounded-xl bg-amber-100 p-2 text-amber-700"><AlertTriangle className="h-5 w-5" /></span>
          <div>
            <h3 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Khiếu nại điểm danh</h3>
            <p className="mt-1 text-xs leading-5 text-[#584140]">Đối chiếu buổi học và phản hồi học viên của chính lớp bạn phụ trách.</p>
          </div>
        </div>
        <button className="inline-flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-xs font-extrabold text-[#730014]" onClick={loadItems} type="button">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Làm mới
        </button>
      </div>

      {error ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">{error}</div> : null}

      {loading ? (
        <div className="flex min-h-52 items-center justify-center text-sm font-bold text-[#8b706e]">Đang tải khiếu nại...</div>
      ) : !pendingItems.length ? (
        <div className="flex min-h-52 flex-col items-center justify-center rounded-2xl border border-dashed border-gray-200 text-center">
          <CheckCircle2 className="h-10 w-10 text-emerald-600" />
          <p className="mt-3 font-extrabold text-[#2b2828]">Không có khiếu nại chờ xử lý</p>
          <p className="mt-1 text-xs text-[#8b706e]">Các yêu cầu đã xử lý vẫn được lưu trong lịch sử điểm danh.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {pendingItems.map((item) => (
            <article className="grid gap-4 rounded-2xl border border-gray-100 bg-white p-5 lg:grid-cols-[1fr_auto] lg:items-center" key={item.id}>
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-extrabold text-[#2b2828]">{item.studentName || `Học viên #${item.studentId}`}</p>
                  <span className="rounded-full bg-[#fff0f1] px-2.5 py-1 text-[10px] font-extrabold text-[#730014]">{attendanceLabels[item.currentAttendanceStatus] || item.currentAttendanceStatus}</span>
                </div>
                <p className="mt-1 text-xs font-semibold text-[#8b706e]">{item.sessionTitle || `Buổi học #${item.sessionId}`} · Gửi {formatDateTime(item.createdAt)}</p>
                <p className="mt-3 rounded-xl bg-gray-50 px-4 py-3 text-sm leading-6 text-[#584140]">{item.reason}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <button className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs font-extrabold text-white" onClick={() => openReview(item, 'approve')} type="button"><CheckCircle2 className="h-4 w-4" />Duyệt</button>
                <button className="inline-flex items-center gap-2 rounded-xl border border-rose-200 bg-white px-4 py-2.5 text-xs font-extrabold text-rose-700" onClick={() => openReview(item, 'reject')} type="button"><XCircle className="h-4 w-4" />Từ chối</button>
              </div>
            </article>
          ))}
        </div>
      )}

      {modal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4 backdrop-blur-sm">
          <section className="w-full max-w-xl rounded-3xl border border-gray-200 bg-white p-6 shadow-2xl">
            <p className="text-xs font-extrabold uppercase tracking-widest text-[#730014]">{modal.action === 'approve' ? 'Duyệt khiếu nại' : 'Từ chối khiếu nại'}</p>
            <h3 className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{modal.item.studentName}</h3>
            {modal.action === 'approve' ? (
              <label className="mt-5 block space-y-2">
                <span className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">Trạng thái điểm danh sau xử lý</span>
                <BrandedSelect onChange={(event) => setAttendanceStatus(event.target.value)} options={attendanceOptions} value={attendanceStatus} />
              </label>
            ) : null}
            <label className="mt-5 block space-y-2">
              <span className="text-xs font-bold uppercase tracking-wider text-[#8b706e]">{modal.action === 'approve' ? 'Ghi chú xử lý' : 'Lý do từ chối'}</span>
              <textarea className="min-h-28 w-full rounded-xl border border-gray-200 bg-gray-50 px-4 py-3 text-sm outline-none focus:border-[#730014] focus:bg-white" onChange={(event) => setReviewNote(event.target.value)} value={reviewNote} />
            </label>
            <div className="mt-6 flex justify-end gap-3">
              <button className="rounded-xl border border-gray-200 px-5 py-2.5 text-sm font-bold text-[#584140]" onClick={closeModal} type="button">Hủy</button>
              <button className={`rounded-xl px-5 py-2.5 text-sm font-extrabold text-white disabled:opacity-60 ${modal.action === 'approve' ? 'bg-emerald-600' : 'bg-rose-600'}`} disabled={submitting} onClick={submitReview} type="button">{submitting ? 'Đang xử lý...' : 'Xác nhận'}</button>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '—';
}
