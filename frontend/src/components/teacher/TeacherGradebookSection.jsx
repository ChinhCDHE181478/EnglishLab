import { useState } from 'react';
import {
  Award,
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

const SCORE_FIELDS = [
  { key: 'homeworkScore', label: 'Bài tập', max: 10 },
  { key: 'quizScore', label: 'Quiz', max: 10 },
  { key: 'attendancePercent', label: 'Chuyên cần (%)', max: 100 },
  { key: 'participationScore', label: 'Phát biểu', max: 10 },
  { key: 'finalResult', label: 'Kết quả', max: 10 },
];

const toForm = (entry) => ({
  homeworkScore: entry.homeworkScore ?? '',
  quizScore: entry.quizScore ?? '',
  attendancePercent: entry.attendancePercent ?? '',
  participationScore: entry.participationScore ?? '',
  finalResult: entry.finalResult ?? '',
  teacherComment: entry.teacherComment ?? '',
});

const validateForm = (form) => {
  for (const field of SCORE_FIELDS) {
    if (form[field.key] === '') continue;
    const value = Number(form[field.key]);
    if (!Number.isFinite(value) || value < 0 || value > field.max) {
      return `${field.label} phải nằm trong khoảng 0–${field.max}.`;
    }
  }
  if (form.teacherComment.length > 2000) {
    return 'Nhận xét của giáo viên không được vượt quá 2000 ký tự.';
  }
  return '';
};

export default function TeacherGradebookSection({
  classroomId,
  gradebook,
  onGradebookChange,
  onMessage,
  onPublish,
  onUnpublish,
}) {
  const [editingStudentId, setEditingStudentId] = useState(null);
  const [form, setForm] = useState(null);
  const [saving, setSaving] = useState(false);
  const [confirmationAction, setConfirmationAction] = useState(null);
  const hasPublishedEntries = gradebook.some((entry) => entry.status === 'PUBLISHED');
  const allEntriesPublished = gradebook.length > 0
    && gradebook.every((entry) => entry.status === 'PUBLISHED');

  const handleConfirmPublicationAction = () => {
    if (confirmationAction === 'publish') {
      onPublish?.();
    } else if (confirmationAction === 'unpublish') {
      onUnpublish?.();
    }
    setConfirmationAction(null);
  };

  const beginEditing = (entry) => {
    setEditingStudentId(entry.studentId);
    setForm(toForm(entry));
    onMessage?.('');
  };

  const cancelEditing = () => {
    setEditingStudentId(null);
    setForm(null);
  };

  const saveEntry = async (entry) => {
    const validationMessage = validateForm(form);
    if (validationMessage) {
      onMessage?.(validationMessage);
      return;
    }

    setSaving(true);
    onMessage?.('');
    try {
      const payload = {
        studentId: entry.studentId,
        teacherComment: form.teacherComment.trim(),
      };
      SCORE_FIELDS.forEach(({ key }) => {
        if (form[key] !== '') payload[key] = Number(form[key]);
      });

      const updated = await classroomApi.updateGradebookEntry(classroomId, payload);
      onGradebookChange?.((current) => current.map((item) => (
        item.studentId === updated.studentId ? updated : item
      )));
      onMessage?.(`Đã cập nhật bảng điểm của ${updated.studentName || entry.studentName || 'học viên'} thành công.`);
      cancelEditing();
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
            Chỉnh sửa điểm thủ công khi cần, sau đó công bố để học viên xem kết quả đánh giá.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014]/20 bg-white px-5 py-3 text-xs font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff3f4] active:scale-95 disabled:cursor-not-allowed disabled:opacity-45"
            disabled={!hasPublishedEntries}
            onClick={() => setConfirmationAction('unpublish')}
            type="button"
          >
            <EyeOff className="h-4 w-4" />
            Thu hồi công bố
          </button>
          <button
            className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95 disabled:cursor-not-allowed disabled:opacity-45"
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
          <table className="min-w-[1020px] w-full divide-y divide-gray-100 text-left text-sm">
            <thead className="bg-[#fffafb] text-xs font-bold uppercase tracking-wider text-[#8b706e]">
              <tr>
                <th className="px-5 py-4">Học viên</th>
                <th className="px-5 py-4">Bài tập</th>
                <th className="px-5 py-4">Quiz</th>
                <th className="px-5 py-4">Chuyên cần</th>
                <th className="px-5 py-4">Phát biểu</th>
                <th className="px-5 py-4">Kết quả</th>
                <th className="px-5 py-4">Trạng thái</th>
                <th className="px-5 py-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-[#584140]">
              {gradebook.map((entry) => {
                const isEditing = editingStudentId === entry.studentId;
                return (
                  <tr className="align-top hover:bg-[#fffafb]/30" key={entry.studentId || entry.id}>
                    {isEditing ? (
                      <td className="px-5 py-4" colSpan={8}>
                        <div className="space-y-4 rounded-2xl border border-[#dfbfbd]/30 bg-[#fffafb]/50 p-4">
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <div>
                              <p className="font-extrabold text-[#2b2828]">
                                {entry.studentName || `Học viên #${entry.studentId}`}
                              </p>
                              <p className="mt-0.5 text-xs text-[#8b706e]">Chỉnh sửa điểm thủ công</p>
                            </div>
                            <StatusBadge status={entry.status} />
                          </div>

                          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
                            {SCORE_FIELDS.map((field) => (
                              <label className="space-y-1.5" key={field.key}>
                                <span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">
                                  {field.label}
                                </span>
                                <input
                                  className="w-full rounded-xl border border-[#dfbfbd]/60 bg-white px-3 py-2.5 text-sm font-semibold text-[#2b2828] outline-none focus:border-[#730014]"
                                  max={field.max}
                                  min="0"
                                  onChange={(event) => setForm((current) => ({
                                    ...current,
                                    [field.key]: event.target.value,
                                  }))}
                                  step="0.01"
                                  type="number"
                                  value={form[field.key]}
                                />
                              </label>
                            ))}
                          </div>

                          <label className="block space-y-1.5">
                            <span className="text-[11px] font-bold uppercase tracking-wider text-[#8b706e]">
                              Nhận xét của giáo viên
                            </span>
                            <textarea
                              className="min-h-[90px] w-full rounded-xl border border-[#dfbfbd]/60 bg-white px-3 py-2.5 text-sm text-[#2b2828] outline-none focus:border-[#730014]"
                              maxLength={2000}
                              onChange={(event) => setForm((current) => ({
                                ...current,
                                teacherComment: event.target.value,
                              }))}
                              placeholder="Nhận xét về kết quả học tập..."
                              value={form.teacherComment}
                            />
                          </label>

                          <div className="flex justify-end gap-2">
                            <button
                              className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-xs font-extrabold text-[#584140] hover:bg-gray-50"
                              disabled={saving}
                              onClick={cancelEditing}
                              type="button"
                            >
                              <X className="h-4 w-4" />
                              Hủy
                            </button>
                            <button
                              className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-4 py-2.5 text-xs font-extrabold text-white hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-60"
                              disabled={saving}
                              onClick={() => saveEntry(entry)}
                              type="button"
                            >
                              {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                              Lưu thay đổi
                            </button>
                          </div>
                        </div>
                      </td>
                    ) : (
                      <>
                        <td className="px-5 py-4 font-extrabold text-[#2b2828]">
                          {entry.studentName || `Học viên #${entry.studentId}`}
                        </td>
                        <td className="px-5 py-4 font-bold">
                          {entry.homeworkScore ?? '—'}
                        </td>
                        <td className="px-5 py-4 font-bold">{entry.quizScore ?? '—'}</td>
                        <td className="px-5 py-4 font-bold">
                          {entry.attendancePercent != null ? `${entry.attendancePercent}%` : '—'}
                        </td>
                        <td className="px-5 py-4 font-bold">{entry.participationScore ?? '—'}</td>
                        <td className="px-5 py-4">
                          <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold ${
                            isGradebookPassed(entry.finalResult)
                              ? 'bg-emerald-50 text-emerald-700'
                              : 'bg-amber-50 text-amber-700'
                          }`}>
                            {formatGradebookFinalResult(entry.finalResult)}
                          </span>
                        </td>
                        <td className="px-5 py-4"><StatusBadge status={entry.status} /></td>
                        <td className="px-5 py-4 text-right">
                          <button
                            className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd]/50 bg-white px-3 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-rose-50"
                            onClick={() => beginEditing(entry)}
                            type="button"
                          >
                            <Pencil className="h-3.5 w-3.5" />
                            Chỉnh sửa
                          </button>
                        </td>
                      </>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      <ConfirmModal
        confirmLabel={confirmationAction === 'unpublish' ? 'Thu hồi công bố' : 'Công bố bảng điểm'}
        danger={confirmationAction === 'unpublish'}
        isOpen={confirmationAction != null}
        message={confirmationAction === 'unpublish'
          ? 'Học viên sẽ tạm thời không thể xem kết quả cho đến khi bạn công bố lại bảng điểm.'
          : 'Tất cả học viên trong lớp sẽ có thể xem điểm và nhận xét đã được cập nhật.'}
        onClose={() => setConfirmationAction(null)}
        onConfirm={handleConfirmPublicationAction}
        title={confirmationAction === 'unpublish' ? 'Thu hồi bảng điểm đã công bố?' : 'Công bố bảng điểm chính thức?'}
      />
    </div>
  );
}
