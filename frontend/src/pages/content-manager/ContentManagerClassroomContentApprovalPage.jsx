import { useEffect, useState } from 'react';
import { CheckCircle2, FileCheck2, RefreshCw, XCircle } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import {
  CARD_CLASS,
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

export default function ContentManagerClassroomContentApprovalPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [modal, setModal] = useState(null);
  const [reviewNote, setReviewNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadItems = async () => {
    setLoading(true);
    setError('');
    try {
      setItems(await classroomApi.getContentManagerPendingClassroomContent());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được hàng chờ duyệt học liệu lớp học.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadItems();
  }, []);

  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(items, 10);

  const submitReview = async () => {
    if (!modal) return;
    if (modal.action === 'reject' && !reviewNote.trim()) {
      setError('Vui lòng nhập lý do từ chối.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const { item, action } = modal;
      if (item.contentType === 'MATERIAL') {
        if (action === 'approve') await classroomApi.approveClassroomMaterialContent(item.id, reviewNote);
        else await classroomApi.rejectClassroomMaterialContent(item.id, reviewNote);
      } else if (action === 'approve') {
        await classroomApi.approveClassroomSyllabusContent(item.id, reviewNote);
      } else {
        await classroomApi.rejectClassroomSyllabusContent(item.id, reviewNote);
      }
      setSuccess(action === 'approve' ? 'Đã duyệt học liệu lớp học.' : 'Đã trả nội dung về để chỉnh sửa.');
      setModal(null);
      setReviewNote('');
      await loadItems();
    } catch (err) {
      setError(err?.response?.data?.message || 'Không xử lý được yêu cầu duyệt.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-slate-900">Duyệt học liệu lớp học</h2>
          <p className="mt-1 text-sm text-slate-600">Rà soát tài liệu và giáo trình do giáo viên hoặc nhân sự bổ sung riêng cho từng lớp.</p>
        </div>
        <button className={SECONDARY_BUTTON_CLASS} onClick={loadItems} type="button">
          <RefreshCw className="h-4 w-4" /> Tải lại
        </button>
      </div>

      {error ? <div className={ERROR_NOTICE_CLASS}>{error}</div> : null}
      {success ? <div className={SUCCESS_NOTICE_CLASS}>{success}</div> : null}

      {loading ? (
        <p className="text-sm font-semibold text-slate-500">Đang tải...</p>
      ) : items.length === 0 ? (
        <div className={EMPTY_STATE_CLASS}>
          <FileCheck2 className="mx-auto mb-3 h-8 w-8 text-slate-400" />
          Không có học liệu lớp học chờ duyệt.
        </div>
      ) : (
        <div className="space-y-3">
          {pageItems.map((item) => (
            <div className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`} key={`${item.contentType}-${item.id}`}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-bold text-amber-700">
                      {item.contentType === 'MATERIAL' ? 'Tài liệu' : 'Giáo trình'}
                    </span>
                    <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">{item.title}</h3>
                  </div>
                  <p className="mt-1 text-sm text-slate-600">{item.classroomTitle}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    Gửi duyệt: {item.submittedForReviewAt ? new Date(item.submittedForReviewAt).toLocaleString('vi-VN') : '—'}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    className="inline-flex items-center gap-1 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700 transition hover:bg-emerald-100"
                    onClick={() => { setModal({ item, action: 'approve' }); setReviewNote('Đã rà soát, đủ điều kiện hiển thị.'); }}
                    type="button"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" /> Duyệt
                  </button>
                  <button
                    className="inline-flex items-center gap-1 rounded-xl border border-rose-200 bg-rose-50 px-3 py-1.5 text-xs font-semibold text-rose-700 transition hover:bg-rose-100"
                    onClick={() => { setModal({ item, action: 'reject' }); setReviewNote(''); }}
                    type="button"
                  >
                    <XCircle className="h-3.5 w-3.5" /> Trả về
                  </button>
                </div>
              </div>
            </div>
          ))}
          <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={10} />
        </div>
      )}

      {modal ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <div className="w-full max-w-lg rounded-[28px] border border-slate-200 bg-white p-6 shadow-xl">
            <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">
              {modal.action === 'approve' ? 'Duyệt học liệu' : 'Yêu cầu chỉnh sửa'}
            </h3>
            <textarea
              className={`mt-4 ${TEXTAREA_CLASS}`}
              onChange={(event) => setReviewNote(event.target.value)}
              placeholder={modal.action === 'approve' ? 'Ghi chú duyệt...' : 'Nêu rõ nội dung người gửi cần chỉnh sửa...'}
              rows={4}
              value={reviewNote}
            />
            <div className="mt-4 flex justify-end gap-2">
              <button className={SECONDARY_BUTTON_CLASS} onClick={() => setModal(null)} type="button">Hủy</button>
              <button className={PRIMARY_BUTTON_CLASS} disabled={submitting} onClick={submitReview} type="button">
                {submitting ? 'Đang xử lý...' : 'Xác nhận'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
