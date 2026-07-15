import { useEffect, useState } from 'react';
import {
  Award,
  BookOpenCheck,
  Download,
  Eye,
  EyeOff,
  Loader2,
  Pencil,
  Save,
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ConfirmModal,
  StatusBadge,
} from '../classroom/ClassroomUi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatGradebookFinalResult,
  isGradebookPassed,
} from '../../utils/classroomHelpers';

const HOMEWORK_STATUS = {
  GRADED: { label: 'Đã chấm', className: 'bg-emerald-50 text-emerald-700' },
  SUBMITTED: { label: 'Chờ chấm', className: 'bg-amber-50 text-amber-700' },
  RETURNED: { label: 'Cần nộp lại', className: 'bg-blue-50 text-blue-700' },
  NOT_SUBMITTED: { label: 'Chưa nộp', className: 'bg-gray-100 text-gray-600' },
};

const formatScore = (score, maxScore = 10) => (
  score == null ? '—' : `${score} /${maxScore ?? 10}`
);

const toEditForm = (entry) => ({
  homeworkScores: Object.fromEntries(
    (entry.homeworks || []).map((homework) => [homework.id, homework.score ?? ''])
  ),
  attendancePercent: entry.attendancePercent ?? '',
  finalResult: entry.finalResult ?? '',
});

const validateForm = (form, homeworks) => {
  for (const homework of homeworks) {
    const rawScore = form.homeworkScores[homework.id];
    if (rawScore === '' || rawScore == null) continue;
    const score = Number(rawScore);
    const maxScore = Number(homework.maxScore ?? 10);
    if (!Number.isFinite(score) || score < 0 || score > maxScore) {
      return `Điểm “${homework.title}” phải nằm trong khoảng 0–${maxScore}.`;
    }
  }

  if (form.attendancePercent !== '') {
    const attendance = Number(form.attendancePercent);
    if (!Number.isFinite(attendance) || attendance < 0 || attendance > 100) {
      return 'Chuyên cần phải nằm trong khoảng 0–100%.';
    }
  }

  if (form.finalResult !== '') {
    const finalResult = Number(form.finalResult);
    if (!Number.isFinite(finalResult) || finalResult < 0 || finalResult > 10) {
      return 'Kết quả cuối phải nằm trong khoảng 0–10.';
    }
  }
  return '';
};

function HomeworkStatusBadge({ status }) {
  const config = HOMEWORK_STATUS[status] || HOMEWORK_STATUS.NOT_SUBMITTED;
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-extrabold ${config.className}`}>
      {config.label}
    </span>
  );
}

function GradebookStudentModal({ entry, loading, mode, onClose, onEdit, onSave, saving }) {
  const [form, setForm] = useState(() => toEditForm(entry));
  const [validationMessage, setValidationMessage] = useState('');
  const homeworks = entry.homeworks || [];
  const isEditing = mode === 'edit';

  useEffect(() => {
    setForm(toEditForm(entry));
    setValidationMessage('');
  }, [entry, mode]);

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !saving) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose, saving]);

  const submit = () => {
    const message = validateForm(form, homeworks);
    if (message) {
      setValidationMessage(message);
      return;
    }
    onSave(form);
  };

  return (
    <div
      aria-labelledby="gradebook-student-modal-title"
      aria-modal="true"
      className="fixed inset-0 z-[80] flex items-center justify-center bg-black/45 p-3 backdrop-blur-sm sm:p-6"
      role="dialog"
    >
      <div className="flex max-h-[92vh] w-full max-w-3xl flex-col overflow-hidden rounded-3xl border border-[#dfbfbd]/40 bg-white shadow-2xl">
        <div className="flex items-start justify-between gap-4 border-b border-gray-100 bg-[#fffafb] px-5 py-4 sm:px-7 sm:py-5">
          <div>
            <p className="text-[11px] font-extrabold uppercase tracking-[0.18em] text-[#9b1c31]">
              {isEditing ? 'Chỉnh sửa kết quả' : 'Chi tiết bài tập'}
            </p>
            <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]" id="gradebook-student-modal-title">
              {entry.studentName || `Học viên #${entry.studentId}`}
            </h3>
          </div>
          <button
            aria-label="Đóng"
            className="rounded-xl border border-gray-200 bg-white p-2 text-[#584140] transition hover:bg-gray-50"
            disabled={saving}
            onClick={onClose}
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5 sm:px-7">
          {loading ? (
            <div className="flex min-h-56 items-center justify-center gap-2 text-sm font-bold text-[#8b706e]">
              <Loader2 className="h-5 w-5 animate-spin text-[#730014]" />
              Đang tải danh sách bài tập mới nhất...
            </div>
          ) : (
            <div className="space-y-6">
              <section>
                <div className="mb-3 flex items-center justify-between gap-3">
                  <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Bài tập</h4>
                  <span className="text-xs font-bold text-[#8b706e]">{homeworks.length} bài</span>
                </div>

                {!homeworks.length ? (
                  <div className="rounded-2xl border border-dashed border-gray-200 px-4 py-8 text-center text-sm text-[#8b706e]">
                    Lớp chưa có bài tập nào.
                  </div>
                ) : (
                  <div className="divide-y divide-gray-100 overflow-hidden rounded-2xl border border-gray-100">
                    {homeworks.map((homework) => (
                      <div className="grid gap-3 px-4 py-4 sm:grid-cols-[minmax(0,1fr)_130px_110px] sm:items-center" key={homework.id}>
                        <div className="min-w-0">
                          <p className="break-words text-sm font-extrabold text-[#2b2828]">{homework.title}</p>
                          {isEditing ? (
                            <p className="mt-1 text-[11px] text-[#8b706e]">Điểm tối đa: {homework.maxScore ?? 10}</p>
                          ) : null}
                        </div>
                        {isEditing ? (
                          <div className="relative">
                            <input
                              aria-label={`Điểm ${homework.title}`}
                              className="w-full rounded-xl border border-[#dfbfbd]/60 bg-white px-3 py-2.5 pr-12 text-sm font-bold text-[#2b2828] outline-none focus:border-[#730014]"
                              max={homework.maxScore ?? 10}
                              min="0"
                              onChange={(event) => setForm((current) => ({
                                ...current,
                                homeworkScores: {
                                  ...current.homeworkScores,
                                  [homework.id]: event.target.value,
                                },
                              }))}
                              placeholder="—"
                              step="0.01"
                              type="number"
                              value={form.homeworkScores[homework.id] ?? ''}
                            />
                            <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[11px] font-bold text-[#8b706e]">
                              /{homework.maxScore ?? 10}
                            </span>
                          </div>
                        ) : (
                          <p className="text-sm font-extrabold text-[#2b2828] sm:text-right">
                            {formatScore(homework.score, homework.maxScore)}
                          </p>
                        )}
                        <div className="sm:text-right"><HomeworkStatusBadge status={homework.status} /></div>
                      </div>
                    ))}
                  </div>
                )}
              </section>

              <section className="grid gap-4 rounded-2xl border border-[#dfbfbd]/20 bg-[#fffafb] p-4 sm:grid-cols-3">
                <div>
                  <p className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">Điểm TB bài tập</p>
                  <p className="mt-1 text-lg font-extrabold text-[#730014]">{formatScore(entry.homeworkAverage)}</p>
                </div>
                <label className="block">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">Chuyên cần</span>
                  {isEditing ? (
                    <div className="relative mt-1.5">
                      <input
                        className="w-full rounded-xl border border-[#dfbfbd]/60 bg-white px-3 py-2.5 pr-8 text-sm font-bold outline-none focus:border-[#730014]"
                        max="100"
                        min="0"
                        onChange={(event) => setForm((current) => ({ ...current, attendancePercent: event.target.value }))}
                        step="0.01"
                        type="number"
                        value={form.attendancePercent}
                      />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-[#8b706e]">%</span>
                    </div>
                  ) : (
                    <p className="mt-1 text-lg font-extrabold text-[#2b2828]">
                      {entry.attendancePercent == null ? '—' : `${entry.attendancePercent}%`}
                    </p>
                  )}
                </label>
                <label className="block">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">Kết quả cuối</span>
                  {isEditing ? (
                    <div className="relative mt-1.5">
                      <input
                        className="w-full rounded-xl border border-[#dfbfbd]/60 bg-white px-3 py-2.5 pr-10 text-sm font-bold outline-none focus:border-[#730014]"
                        max="10"
                        min="0"
                        onChange={(event) => setForm((current) => ({ ...current, finalResult: event.target.value }))}
                        step="0.01"
                        type="number"
                        value={form.finalResult}
                      />
                      <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-[#8b706e]">/10</span>
                    </div>
                  ) : (
                    <p className="mt-1 text-lg font-extrabold text-[#2b2828]">{formatGradebookFinalResult(entry.finalResult)}</p>
                  )}
                </label>
              </section>

              {validationMessage ? (
                <p className="rounded-xl border border-rose-100 bg-rose-50 px-4 py-3 text-xs font-bold text-rose-700">
                  {validationMessage}
                </p>
              ) : null}
            </div>
          )}
        </div>

        {!loading ? (
          <div className="flex flex-wrap justify-end gap-2 border-t border-gray-100 bg-white px-5 py-4 sm:px-7">
            <button
              className="rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-xs font-extrabold text-[#584140] hover:bg-gray-50"
              disabled={saving}
              onClick={onClose}
              type="button"
            >
              {isEditing ? 'Hủy' : 'Đóng'}
            </button>
            {isEditing ? (
              <button
                className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-60"
                disabled={saving}
                onClick={submit}
                type="button"
              >
                {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                Lưu thay đổi
              </button>
            ) : (
              <button
                className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white hover:bg-[#730014]"
                onClick={onEdit}
                type="button"
              >
                <Pencil className="h-4 w-4" />
                Chỉnh sửa
              </button>
            )}
          </div>
        ) : null}
      </div>
    </div>
  );
}

export default function TeacherGradebookSection({
  classroomId,
  gradebook,
  onExport,
  onGradebookChange,
  onMessage,
  onPublish,
  onUnpublish,
}) {
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [modalMode, setModalMode] = useState('detail');
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [saving, setSaving] = useState(false);
  const [confirmationAction, setConfirmationAction] = useState(null);
  const hasPublishedEntries = gradebook.some((entry) => entry.status === 'PUBLISHED');
  const allEntriesPublished = gradebook.length > 0
    && gradebook.every((entry) => entry.status === 'PUBLISHED');

  const handleConfirmPublicationAction = () => {
    if (confirmationAction === 'publish') onPublish?.();
    if (confirmationAction === 'unpublish') onUnpublish?.();
    setConfirmationAction(null);
  };

  const openStudentModal = async (entry, mode) => {
    setSelectedEntry(entry);
    setModalMode(mode);
    setLoadingDetails(true);
    onMessage?.('');
    try {
      const latestGradebook = await classroomApi.getTeacherGradebook(classroomId);
      onGradebookChange?.(latestGradebook);
      const latestEntry = latestGradebook.find((item) => item.studentId === entry.studentId);
      if (latestEntry) setSelectedEntry(latestEntry);
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể tải chi tiết điểm bài tập mới nhất.'));
    } finally {
      setLoadingDetails(false);
    }
  };

  const closeModal = () => {
    if (saving) return;
    setSelectedEntry(null);
  };

  const saveEntry = async (form) => {
    if (!selectedEntry) return;
    setSaving(true);
    onMessage?.('');
    try {
      const payload = {
        studentId: selectedEntry.studentId,
        homeworkScores: (selectedEntry.homeworks || []).map((homework) => ({
          homeworkId: homework.id,
          score: form.homeworkScores[homework.id] === ''
            ? null
            : Number(form.homeworkScores[homework.id]),
        })),
      };
      if (form.attendancePercent !== '') payload.attendancePercent = Number(form.attendancePercent);
      if (form.finalResult !== '') payload.finalResult = Number(form.finalResult);

      const updated = await classroomApi.updateGradebookEntry(classroomId, payload);
      onGradebookChange?.((current) => current.map((item) => (
        item.studentId === updated.studentId ? updated : item
      )));
      onMessage?.(`Đã cập nhật bảng điểm của ${updated.studentName || selectedEntry.studentName || 'học viên'} thành công.`);
      setSelectedEntry(null);
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể cập nhật bảng điểm.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-[#dfbfbd]/15 bg-[#fffafb] p-5">
        <div>
          <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Công bố bảng điểm chính thức</h4>
          <p className="mt-1 text-xs text-[#584140]">
            Theo dõi kết quả tổng hợp; nhấn điểm trung bình để xem toàn bộ bài tập của từng học viên.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014]/20 bg-white px-4 py-2.5 text-xs font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff3f4] active:scale-95 disabled:cursor-not-allowed disabled:opacity-45"
            disabled={!gradebook.length}
            onClick={onExport}
            type="button"
          >
            <Download className="h-4 w-4" />
            Xuất CSV bảng điểm
          </button>
          <button
            className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014]/20 bg-white px-4 py-2.5 text-xs font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff3f4] active:scale-95 disabled:cursor-not-allowed disabled:opacity-45"
            disabled={!hasPublishedEntries}
            onClick={() => setConfirmationAction('unpublish')}
            type="button"
          >
            <EyeOff className="h-4 w-4" />
            Thu hồi công bố
          </button>
          <button
            className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-4 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95 disabled:cursor-not-allowed disabled:opacity-45"
            disabled={!gradebook.length || allEntriesPublished}
            onClick={() => setConfirmationAction('publish')}
            type="button"
          >
            <Award className="h-4 w-4" />
            Công bố bảng điểm
          </button>
        </div>
      </div>

      {!gradebook.length ? (
        <ClassroomEmptyState
          description="Chưa có dữ liệu bảng điểm nào được ghi nhận."
          title="Chưa có bảng điểm"
        />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-gray-100 bg-white">
          <table className="w-full min-w-[820px] divide-y divide-gray-100 text-left text-sm">
            <thead className="bg-[#fffafb] text-xs font-bold uppercase tracking-wider text-[#8b706e]">
              <tr>
                <th className="px-5 py-4">Học viên</th>
                <th className="px-5 py-4">Điểm TB bài tập</th>
                <th className="px-5 py-4">Chuyên cần</th>
                <th className="px-5 py-4">Kết quả cuối</th>
                <th className="px-5 py-4">Công bố</th>
                <th className="px-5 py-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-[#584140]">
              {gradebook.map((entry) => (
                <tr className="hover:bg-[#fffafb]/40" key={entry.studentId || entry.id}>
                  <td className="px-5 py-4 font-extrabold text-[#2b2828]">
                    {entry.studentName || `Học viên #${entry.studentId}`}
                  </td>
                  <td className="px-5 py-4">
                    <button
                      className="inline-flex items-center gap-1.5 rounded-lg px-2 py-1 font-extrabold text-[#730014] transition hover:bg-rose-50 hover:underline"
                      onClick={() => openStudentModal(entry, 'detail')}
                      type="button"
                    >
                      <BookOpenCheck className="h-4 w-4" />
                      {formatScore(entry.homeworkAverage)}
                    </button>
                  </td>
                  <td className="px-5 py-4 font-bold">
                    {entry.attendancePercent == null ? '—' : `${entry.attendancePercent}%`}
                  </td>
                  <td className="px-5 py-4">
                    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold ${
                      isGradebookPassed(entry.finalResult)
                        ? 'bg-emerald-50 text-emerald-700'
                        : 'bg-amber-50 text-amber-700'
                    }`}>
                      {formatGradebookFinalResult(entry.finalResult)}
                    </span>
                  </td>
                  <td className="px-5 py-4"><StatusBadge status={entry.status} /></td>
                  <td className="px-5 py-4 text-right">
                    <div className="inline-flex gap-2">
                      <button
                        aria-label={`Xem chi tiết ${entry.studentName || ''}`}
                        className="rounded-xl border border-gray-200 bg-white p-2 text-[#584140] transition hover:bg-gray-50"
                        onClick={() => openStudentModal(entry, 'detail')}
                        title="Xem chi tiết"
                        type="button"
                      >
                        <Eye className="h-4 w-4" />
                      </button>
                      <button
                        className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd]/50 bg-white px-3 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-rose-50"
                        onClick={() => openStudentModal(entry, 'edit')}
                        type="button"
                      >
                        <Pencil className="h-3.5 w-3.5" />
                        Chỉnh sửa
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedEntry ? (
        <GradebookStudentModal
          entry={selectedEntry}
          loading={loadingDetails}
          mode={modalMode}
          onClose={closeModal}
          onEdit={() => setModalMode('edit')}
          onSave={saveEntry}
          saving={saving}
        />
      ) : null}

      <ConfirmModal
        confirmLabel={confirmationAction === 'unpublish' ? 'Thu hồi công bố' : 'Công bố bảng điểm'}
        danger={confirmationAction === 'unpublish'}
        isOpen={confirmationAction != null}
        message={confirmationAction === 'unpublish'
          ? 'Học viên sẽ tạm thời không thể xem kết quả cho đến khi bạn công bố lại bảng điểm.'
          : 'Tất cả học viên trong lớp sẽ có thể xem điểm và kết quả đã được cập nhật.'}
        onClose={() => setConfirmationAction(null)}
        onConfirm={handleConfirmPublicationAction}
        title={confirmationAction === 'unpublish' ? 'Thu hồi bảng điểm đã công bố?' : 'Công bố bảng điểm chính thức?'}
      />
    </div>
  );
}
