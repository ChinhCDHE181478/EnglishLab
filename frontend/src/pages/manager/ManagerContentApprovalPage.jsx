import { useEffect, useState } from 'react';
import { CheckCircle2, FileCheck2, GitBranch, RefreshCw, XCircle } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import courseApi from '../../api/courseApi';
import Pagination, { usePagination } from '../../components/ui/Pagination';
import { getCourseVersionLabel } from '../../utils/courseVersionUi';
import {
  CARD_CLASS,
  EMPTY_STATE_CLASS,
  ERROR_NOTICE_CLASS,
  PRIMARY_BUTTON_CLASS,
  SECONDARY_BUTTON_CLASS,
  SUCCESS_NOTICE_CLASS,
  TEXTAREA_CLASS,
} from '../../utils/formStyles';

export default function ManagerContentApprovalPage() {
  const [items, setItems] = useState([]);
  const [pendingVersions, setPendingVersions] = useState([]);
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
      const [contentItems, versionItems] = await Promise.all([
        classroomApi.getManagerPendingContentApprovals(),
        courseApi.getPendingOnlineCourseVersions(),
      ]);
      setItems(contentItems);
      setPendingVersions(versionItems);
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được hàng chờ duyệt nội dung.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadItems(); }, []);

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
      if (modal.version) {
        if (action === 'approve') {
          await courseApi.publishOnlineCourseVersion(modal.version.courseId, modal.version.id);
        } else {
          await courseApi.rejectOnlineCourseVersion(modal.version.courseId, modal.version.id, reviewNote.trim());
        }
        setSuccess(action === 'approve'
          ? `Đã xuất bản phiên bản v${modal.version.versionNumber}. Học viên cũ vẫn giữ phiên bản đã đăng ký.`
          : `Đã trả phiên bản v${modal.version.versionNumber} về bản nháp kèm phản hồi.`);
      } else if (item.contentType === 'MATERIAL') {
        if (action === 'approve') await classroomApi.approveManagerMaterial(item.id, reviewNote);
        else await classroomApi.rejectManagerMaterial(item.id, reviewNote);
      } else {
        if (action === 'approve') await classroomApi.approveManagerSyllabus(item.id, reviewNote);
        else await classroomApi.rejectManagerSyllabus(item.id, reviewNote);
      }
      if (!modal.version) {
        setSuccess(action === 'approve' ? 'Đã duyệt nội dung.' : 'Đã từ chối nội dung.');
      }
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
          <h2 className="font-['Manrope'] text-2xl font-extrabold text-slate-900">Duyệt nội dung</h2>
          <p className="mt-1 text-sm text-slate-600">Rà soát phiên bản khóa học, tài liệu và giáo trình do Content Manager gửi duyệt.</p>
        </div>
        <button type="button" onClick={loadItems} className={SECONDARY_BUTTON_CLASS}>
          <RefreshCw className="h-4 w-4" /> Tải lại
        </button>
      </div>

      {error && <div className={ERROR_NOTICE_CLASS}>{error}</div>}
      {success && <div className={SUCCESS_NOTICE_CLASS}>{success}</div>}

      <section className="overflow-hidden rounded-2xl border border-amber-200 bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-amber-100 bg-amber-50 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
              <GitBranch className="h-5 w-5" />
            </span>
            <div>
              <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">Phiên bản khóa học chờ duyệt</h3>
              <p className="text-xs font-semibold text-slate-500">Phiên bản được duyệt chỉ áp dụng cho lượt ghi danh mới.</p>
            </div>
          </div>
          <span className="w-fit rounded-full bg-amber-600 px-3 py-1 text-xs font-extrabold text-white">
            {pendingVersions.length} phiên bản
          </span>
        </div>
        {loading ? (
          <div className="px-5 py-8 text-center text-sm font-semibold text-slate-500">Đang tải phiên bản chờ duyệt...</div>
        ) : pendingVersions.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm font-semibold text-slate-500">Không có phiên bản khóa học đang chờ duyệt.</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {pendingVersions.map((version) => (
              <article className="grid gap-4 p-5 lg:grid-cols-[1fr_auto] lg:items-center" key={version.id}>
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-extrabold text-amber-700">
                      {getCourseVersionLabel(version)}
                    </span>
                    <span className="text-xs font-semibold text-slate-400">Khóa học #{version.courseId}</span>
                  </div>
                  <h4 className="mt-2 font-['Manrope'] text-xl font-extrabold text-slate-900">
                    {version.content?.title || 'Khóa học chưa có tiêu đề'}
                  </h4>
                  <p className="mt-1 text-sm text-slate-500">{version.changeNote || 'Content Manager chưa ghi chú thay đổi.'}</p>
                  <p className="mt-2 text-xs font-bold text-slate-400">
                    {version.totalRequiredLessons || 0} bài bắt buộc · {version.totalRequiredAssessments || 0} bài đánh giá
                  </p>
                </div>
                <div className="flex flex-wrap gap-2 lg:justify-end">
                  <button
                    className="inline-flex items-center gap-2 rounded-2xl bg-emerald-600 px-4 py-3 text-sm font-extrabold text-white transition hover:bg-emerald-700"
                    onClick={() => {
                      setModal({ version, action: 'approve' });
                      setReviewNote('Đã rà soát nội dung phiên bản, đủ điều kiện áp dụng cho lượt ghi danh mới.');
                    }}
                    type="button"
                  >
                    <CheckCircle2 className="h-4 w-4" /> Duyệt và xuất bản
                  </button>
                  <button
                    className="inline-flex items-center gap-2 rounded-2xl border border-rose-200 px-4 py-3 text-sm font-extrabold text-rose-700 transition hover:bg-rose-50"
                    onClick={() => { setModal({ version, action: 'reject' }); setReviewNote(''); }}
                    type="button"
                  >
                    <XCircle className="h-4 w-4" /> Yêu cầu chỉnh sửa
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-3">
        <div>
          <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">Tài liệu và giáo trình lớp học</h3>
          <p className="mt-1 text-xs font-semibold text-slate-500">Nội dung phục vụ từng lớp học trực tiếp.</p>
        </div>

      {loading ? (
        <p className="text-sm font-semibold text-slate-500">Đang tải...</p>
      ) : items.length === 0 ? (
        <div className={EMPTY_STATE_CLASS}>
          <FileCheck2 className="mx-auto mb-3 h-8 w-8 text-slate-400" />
          Không có nội dung chờ duyệt.
        </div>
      ) : (
        <div className="space-y-3">
          {pageItems.map((item) => (
            <div key={`${item.contentType}-${item.id}`} className={`${CARD_CLASS} transition hover:border-[#dfbfbd]`}>
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
                    type="button"
                    onClick={() => { setModal({ item, action: 'approve' }); setReviewNote('Đã rà soát, đủ điều kiện hiển thị.'); }}
                    className="inline-flex items-center gap-1 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700 transition hover:bg-emerald-100"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" /> Duyệt
                  </button>
                  <button
                    type="button"
                    onClick={() => { setModal({ item, action: 'reject' }); setReviewNote(''); }}
                    className="inline-flex items-center gap-1 rounded-xl border border-rose-200 bg-rose-50 px-3 py-1.5 text-xs font-semibold text-rose-700 transition hover:bg-rose-100"
                  >
                    <XCircle className="h-3.5 w-3.5" /> Từ chối
                  </button>
                </div>
              </div>
            </div>
          ))}
          <Pagination page={page} totalPages={totalPages} onChange={setPage} totalItems={totalItems} pageSize={10} />
        </div>
      )}
      </section>

      {modal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <div className="w-full max-w-lg rounded-[28px] border border-slate-200 bg-white p-6 shadow-xl">
            <h3 className="font-['Manrope'] text-lg font-extrabold text-slate-900">
              {modal.version
                ? `${modal.action === 'approve' ? 'Duyệt và xuất bản' : 'Yêu cầu chỉnh sửa'} phiên bản v${modal.version.versionNumber}`
                : modal.action === 'approve' ? 'Duyệt nội dung' : 'Từ chối nội dung'}
            </h3>
            {modal.version ? (
              <div className="mt-4 max-h-64 overflow-y-auto rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="font-['Manrope'] text-base font-extrabold text-slate-900">
                  {modal.version.content?.title || 'Khóa học chưa có tiêu đề'}
                </p>
                <p className="mt-1 text-sm text-slate-600">{modal.version.changeNote || 'Không có ghi chú thay đổi.'}</p>
                <div className="mt-3 space-y-2">
                  {(modal.version.content?.modules || []).map((module, index) => (
                    <div className="rounded-xl border border-slate-200 bg-white px-3 py-2" key={module.id || module.title || index}>
                      <p className="text-sm font-extrabold text-slate-800">Mô-đun {index + 1}: {module.title}</p>
                      <p className="mt-1 text-xs text-slate-500">{module.lessons?.length || 0} bài học</p>
                    </div>
                  ))}
                </div>
              </div>
            ) : null}
            <textarea
              value={reviewNote}
              onChange={(e) => setReviewNote(e.target.value)}
              rows={4}
              placeholder={modal.action === 'approve' ? 'Ghi chú duyệt...' : 'Nêu rõ nội dung Content Manager cần chỉnh sửa...'}
              className={`mt-4 ${TEXTAREA_CLASS}`}
            />
            <div className="mt-4 flex justify-end gap-2">
              <button type="button" onClick={() => setModal(null)} className={SECONDARY_BUTTON_CLASS}>Hủy</button>
              <button type="button" disabled={submitting} onClick={submitReview} className={PRIMARY_BUTTON_CLASS}>
                {submitting ? 'Đang xử lý...' : 'Xác nhận'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
