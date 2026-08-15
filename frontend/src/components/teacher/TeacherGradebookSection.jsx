import { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Award,
  BookOpenCheck,
  CheckCircle2,
  ChevronRight,
  CircleDashed,
  Clock3,
  Download,
  Eye,
  EyeOff,
  Layers3,
  Loader2,
  LockKeyhole,
  Pencil,
  Save,
  Search,
  Users,
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import {
  ClassroomEmptyState,
  ConfirmModal,
  StatusBadge,
} from '../classroom/ClassroomUi';
import Pagination, { usePagination } from '../ui/Pagination';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatGradebookFinalResult,
  isGradebookPassed,
} from '../../utils/classroomHelpers';
import {
  buildGradebookLessons,
  getStudentLessonProgress,
  LESSON_GRADING_STATUS,
  LESSON_POSITION_STATUS,
  orderHomeworkGradingChoices,
} from '../../utils/teacherGradebookProgress';

const HOMEWORK_STATUS = {
  GRADED: { label: 'Đã chấm', className: 'bg-emerald-50 text-emerald-700' },
  SUBMITTED: { label: 'Chờ chấm', className: 'bg-amber-50 text-amber-700' },
  RETURNED: { label: 'Cần nộp lại', className: 'bg-blue-50 text-blue-700' },
  NOT_SUBMITTED: { label: 'Chưa nộp', className: 'bg-gray-100 text-gray-600' },
};

const LESSON_FILTERS = [
  { id: 'ALL', label: 'Tất cả' },
  { id: 'GRADING_PENDING', label: 'Cần chấm', gradingStatus: LESSON_GRADING_STATUS.PENDING },
  { id: 'GRADING_GRADED', label: 'Đã chấm', gradingStatus: LESSON_GRADING_STATUS.GRADED },
  { id: 'POSITION_NOT_REACHED', label: 'Chưa học tới', positionStatus: LESSON_POSITION_STATUS.NOT_REACHED },
];

const STUDENTS_PER_PAGE = 8;

const formatScore = (score, maxScore = 10) => (
  score == null ? '—' : `${score} /${maxScore ?? 10}`
);

const toEditForm = (entry, homeworks) => ({
  homeworkScores: Object.fromEntries(
    homeworks.map((homework) => [homework.id, homework.score ?? ''])
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
    <span className={`inline-flex whitespace-nowrap rounded-full px-2.5 py-1 text-[11px] font-extrabold ${config.className}`}>
      {config.label}
    </span>
  );
}

function LessonGradingBadge({ status }) {
  if (status === LESSON_GRADING_STATUS.GRADED) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-extrabold text-emerald-700">
        <CheckCircle2 className="h-3.5 w-3.5" /> Đã chấm
      </span>
    );
  }
  if (status === LESSON_GRADING_STATUS.PENDING) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-1 text-[11px] font-extrabold text-amber-700">
        <AlertTriangle className="h-3.5 w-3.5" /> Cần chấm
      </span>
    );
  }
  if (status === LESSON_GRADING_STATUS.INCOMPLETE) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2.5 py-1 text-[11px] font-extrabold text-blue-700">
        <CircleDashed className="h-3.5 w-3.5" /> Chưa hoàn tất
      </span>
    );
  }
  return null;
}

function LessonPositionBadge({ status }) {
  if (status === LESSON_POSITION_STATUS.CURRENT) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-1 text-[11px] font-extrabold text-[#9b1c31]">
        <Clock3 className="h-3.5 w-3.5" /> Đang học
      </span>
    );
  }
  if (status === LESSON_POSITION_STATUS.NOT_REACHED) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-extrabold text-slate-500">
        <LockKeyhole className="h-3.5 w-3.5" /> Chưa học tới
      </span>
    );
  }
  if (status === LESSON_POSITION_STATUS.UNASSIGNED) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-violet-50 px-2.5 py-1 text-[11px] font-extrabold text-violet-700">
        <Layers3 className="h-3.5 w-3.5" /> Chưa xếp bài
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2.5 py-1 text-[11px] font-extrabold text-blue-700">
      <BookOpenCheck className="h-3.5 w-3.5" /> Đã học
    </span>
  );
}

function AggregateGradebookTable({ gradebook, onOpenStudent }) {
  const { page, setPage, totalPages, pageItems, totalItems } = usePagination(
    gradebook,
    STUDENTS_PER_PAGE,
    gradebook.length,
  );

  return (
    <section className="overflow-hidden rounded-3xl border border-gray-100 bg-white">
      <div className="flex items-center gap-3 border-b border-gray-100 bg-[#fffafb] px-5 py-4">
        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-50 text-[#730014]">
          <Users className="h-4.5 w-4.5" />
        </span>
        <div>
          <h5 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Bảng điểm tổng hợp</h5>
          <p className="mt-1 text-xs text-[#8b706e]">Điểm bài tập, chuyên cần và kết quả của từng học viên.</p>
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[820px] divide-y divide-gray-100 text-left text-sm">
          <thead className="bg-white text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
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
            {pageItems.map((entry) => (
              <tr className="hover:bg-[#fffafb]/40" key={entry.studentId || entry.id}>
                <td className="px-5 py-4 text-sm font-bold text-[#0b1c30]">
                  {entry.studentName || `Học viên #${entry.studentId}`}
                </td>
                <td className="px-5 py-4 font-extrabold text-[#730014]">{formatScore(entry.homeworkAverage)}</td>
                <td className="px-5 py-4 font-bold">{entry.attendancePercent == null ? '—' : `${entry.attendancePercent}%`}</td>
                <td className="px-5 py-4">
                  <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${
                    isGradebookPassed(entry.finalResult) ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'
                  }`}>
                    {formatGradebookFinalResult(entry.finalResult)}
                  </span>
                </td>
                <td className="px-5 py-4"><StatusBadge status={entry.status} /></td>
                <td className="px-5 py-4 text-right">
                  <button
                    className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd]/50 bg-white px-3 py-2 text-xs font-extrabold text-[#730014] transition hover:bg-rose-50"
                    onClick={() => onOpenStudent(entry)}
                    type="button"
                  >
                    <Eye className="h-3.5 w-3.5" /> Xem tổng hợp
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {totalPages > 1 ? (
        <div className="border-t border-gray-100 bg-[#fffafb]/50 px-5 py-4">
          <Pagination
            onChange={setPage}
            page={page}
            pageSize={STUDENTS_PER_PAGE}
            totalItems={totalItems}
            totalPages={totalPages}
          />
        </div>
      ) : null}
    </section>
  );
}

function HomeworkGradingChoiceModal({ choices, onClose, onSelect, studentName }) {
  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div
      aria-labelledby="homework-choice-title"
      aria-modal="true"
      className="fixed inset-0 z-[90] flex items-center justify-center bg-black/45 p-3 backdrop-blur-sm sm:p-6"
      role="dialog"
    >
      <section className="flex max-h-[88vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-[#dfbfbd]/40 bg-white shadow-2xl">
        <header className="flex items-start justify-between gap-4 border-b border-gray-100 bg-[#fffafb] px-5 py-4 sm:px-6">
          <div>
            <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Chọn bài tập</p>
            <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]" id="homework-choice-title">
              {studentName}
            </h3>
          </div>
          <button
            aria-label="Đóng danh sách bài tập"
            className="rounded-xl border border-gray-200 bg-white p-2 text-[#584140] transition hover:bg-gray-50"
            onClick={onClose}
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
        </header>

        <div className="min-h-0 flex-1 space-y-2 overflow-y-auto p-4 sm:p-5">
          {choices.map((homework) => (
            <article
              className="grid gap-3 rounded-2xl border border-gray-100 px-4 py-4 sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:items-center"
              key={homework.id}
            >
              <div className="min-w-0">
                <h4 className="break-words text-sm font-extrabold text-[#2b2828]">{homework.title}</h4>
                <p className="mt-1 text-xs text-[#8b706e]">{formatScore(homework.score, homework.maxScore)}</p>
              </div>
              <HomeworkStatusBadge status={homework.status} />
              <button
                className={`rounded-xl px-3.5 py-2 text-xs font-extrabold transition ${
                  homework.status === 'SUBMITTED'
                    ? 'bg-amber-500 text-white hover:bg-amber-600'
                    : 'border border-[#dfbfbd]/50 bg-white text-[#730014] hover:bg-rose-50'
                }`}
                onClick={() => onSelect(homework)}
                type="button"
              >
                {homework.status === 'SUBMITTED'
                  ? 'Chấm bài'
                  : homework.status === 'GRADED'
                    ? 'Xem / sửa'
                    : 'Xem chi tiết'}
              </button>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

function GradebookStudentModal({
  entry,
  homeworkIds,
  lessonTitle,
  loading,
  mode,
  onClose,
  onEdit,
  onOpenHomework,
  onSave,
  saving,
}) {
  const homeworks = useMemo(() => {
    const allowedIds = new Set(homeworkIds);
    return (entry.homeworks || []).filter((homework) => allowedIds.has(homework.id));
  }, [entry.homeworks, homeworkIds]);
  const [form, setForm] = useState(() => toEditForm(entry, homeworks));
  const [validationMessage, setValidationMessage] = useState('');
  const isEditing = mode === 'edit';

  useEffect(() => {
    setForm(toEditForm(entry, homeworks));
    setValidationMessage('');
  }, [entry, homeworks, mode]);

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
      <div className="flex max-h-[92vh] w-full max-w-4xl flex-col overflow-hidden rounded-3xl border border-[#dfbfbd]/40 bg-white shadow-2xl">
        <div className="flex items-start justify-between gap-4 border-b border-gray-100 bg-[#fffafb] px-5 py-4 sm:px-7 sm:py-5">
          <div>
            <p className="text-[11px] font-extrabold uppercase tracking-[0.18em] text-[#9b1c31]">
              {isEditing ? 'Cập nhật tổng kết học viên' : 'Chi tiết điểm bài học'}
            </p>
            <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]" id="gradebook-student-modal-title">
              {entry.studentName || `Học viên #${entry.studentId}`}
            </h3>
            <p className="mt-1 text-xs font-bold text-[#8b706e]">{lessonTitle}</p>
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
              Đang tải điểm mới nhất...
            </div>
          ) : (
            <div className="space-y-6">
              <section>
                <div className="mb-3 flex items-center justify-between gap-3">
                  <h4 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Bài tập cần xử lý</h4>
                  <span className="text-xs font-bold text-[#8b706e]">{homeworks.length} bài tập</span>
                </div>

                {!homeworks.length ? (
                  <div className="rounded-2xl border border-dashed border-gray-200 px-4 py-8 text-center text-sm text-[#8b706e]">
                    Bài học này chưa có bài tập để chấm.
                  </div>
                ) : (
                  <div className="divide-y divide-gray-100 overflow-hidden rounded-2xl border border-gray-100">
                    {homeworks.map((homework) => (
                      <div className="grid gap-3 px-4 py-4 sm:grid-cols-[minmax(0,1fr)_90px_110px_108px] sm:items-center" key={homework.id}>
                        <div className="min-w-0">
                          <p className="break-words text-sm font-extrabold text-[#2b2828]">{homework.title}</p>
                          <p className="mt-1 text-[11px] text-[#8b706e]">Điểm tối đa: {homework.maxScore ?? 10}</p>
                        </div>
                        <p className="text-sm font-extrabold text-[#2b2828] sm:text-right">
                          {formatScore(homework.score, homework.maxScore)}
                        </p>
                        <div className="sm:text-center"><HomeworkStatusBadge status={homework.status} /></div>
                        <button
                          className="rounded-lg border border-[#dfbfbd]/50 bg-white px-3 py-2 text-[11px] font-extrabold text-[#730014] transition hover:bg-rose-50"
                          onClick={() => onOpenHomework(homework.id, entry.studentId)}
                          type="button"
                        >
                          Mở bài chấm
                        </button>
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
                disabled={saving || !homeworks.length}
                onClick={submit}
                type="button"
              >
                {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                Lưu tổng kết
              </button>
            ) : (
              <button
                className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-extrabold text-white hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-50"
                disabled={!homeworks.length}
                onClick={onEdit}
                type="button"
              >
                <Pencil className="h-4 w-4" />
                Sửa tổng kết
              </button>
            )}
          </div>
        ) : null}
      </div>
    </div>
  );
}

function LessonNavigationItem({ lesson, onSelect, selected }) {
  const isFuture = lesson.positionStatus === LESSON_POSITION_STATUS.NOT_REACHED;
  return (
    <button
      className={`group w-full rounded-2xl border p-3.5 text-left transition ${
        selected
          ? 'border-[#730014]/35 bg-[#fff3f4] shadow-sm'
          : isFuture
            ? 'border-slate-100 bg-slate-50/70 text-slate-500 hover:border-slate-200'
            : 'border-gray-100 bg-white hover:border-[#dfbfbd]/50 hover:bg-[#fffafb]'
      }`}
      onClick={onSelect}
      type="button"
    >
      <div className="flex items-start gap-3">
        <span className={`flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-xl text-xs font-extrabold ${
          selected ? 'bg-[#730014] text-white' : isFuture ? 'bg-slate-200 text-slate-500' : 'bg-rose-50 text-[#730014]'
        }`}>
          {lesson.displayOrder ?? <Layers3 className="h-4 w-4" />}
        </span>
        <div className="min-w-0 flex-1">
          <p className={`truncate text-sm font-extrabold ${selected ? 'text-[#4b0009]' : 'text-[#2b2828]'}`}>
            {lesson.title}
          </p>
          <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
            <LessonPositionBadge status={lesson.positionStatus} />
            <LessonGradingBadge status={lesson.gradingStatus} />
            {lesson.stats.pendingCount > 0 ? (
              <span className="text-[10px] font-extrabold text-amber-700">{lesson.stats.pendingCount} bài chờ</span>
            ) : null}
          </div>
        </div>
        <ChevronRight className={`mt-1 h-4 w-4 flex-shrink-0 transition ${selected ? 'text-[#730014]' : 'text-gray-300 group-hover:text-[#730014]'}`} />
      </div>
    </button>
  );
}

export default function TeacherGradebookSection({
  classroomId,
  curriculumUnits = [],
  gradebook,
  homework = [],
  onExport,
  onGradebookChange,
  onMessage,
  onOpenHomeworkGrading,
  onPublish,
  onUnpublish,
  sessions = [],
}) {
  const [selectedEntry, setSelectedEntry] = useState(null);
  const [modalLesson, setModalLesson] = useState(null);
  const [modalMode, setModalMode] = useState('detail');
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [saving, setSaving] = useState(false);
  const [confirmationAction, setConfirmationAction] = useState(null);
  const [lessonFilter, setLessonFilter] = useState('ALL');
  const [lessonSearch, setLessonSearch] = useState('');
  const [selectedLessonId, setSelectedLessonId] = useState(null);
  const [gradingSelection, setGradingSelection] = useState(null);

  const lessons = useMemo(() => buildGradebookLessons({
    curriculumUnits,
    gradebook,
    homeworks: homework,
    sessions,
  }), [curriculumUnits, gradebook, homework, sessions]);

  const gradingLessons = useMemo(
    () => lessons.filter((lesson) => lesson.assignments.length > 0),
    [lessons],
  );
  const pendingLesson = gradingLessons.find((lesson) => lesson.gradingStatus === LESSON_GRADING_STATUS.PENDING);
  const currentLesson = gradingLessons.find((lesson) => lesson.positionStatus === LESSON_POSITION_STATUS.CURRENT);
  const selectedLesson = gradingLessons.find((lesson) => lesson.id === selectedLessonId)
    || pendingLesson
    || currentLesson
    || gradingLessons[0]
    || null;
  const hasPublishedEntries = gradebook.some((entry) => entry.status === 'PUBLISHED');
  const allEntriesPublished = gradebook.length > 0
    && gradebook.every((entry) => entry.status === 'PUBLISHED');
  const pendingSubmissionCount = homework.reduce((total, item) => total + Number(item.pendingGradingCount || 0), 0);
  const publishedStudentCount = gradebook.filter((entry) => entry.status === 'PUBLISHED').length;

  const visibleLessons = gradingLessons.filter((lesson) => {
    const activeFilter = LESSON_FILTERS.find((filter) => filter.id === lessonFilter);
    const matchesFilter = lessonFilter === 'ALL'
      || (activeFilter?.gradingStatus && lesson.gradingStatus === activeFilter.gradingStatus)
      || (activeFilter?.positionStatus && lesson.positionStatus === activeFilter.positionStatus);
    const query = lessonSearch.trim().toLocaleLowerCase('vi');
    const matchesSearch = !query || `${lesson.title} ${lesson.displayOrder || ''}`.toLocaleLowerCase('vi').includes(query);
    return matchesFilter && matchesSearch;
  });
  const {
    page: gradingPage,
    setPage: setGradingPage,
    totalPages: gradingTotalPages,
    pageItems: gradingPageItems,
    totalItems: gradingTotalItems,
  } = usePagination(
    gradebook,
    STUDENTS_PER_PAGE,
    `${selectedLesson?.id || 'none'}|${gradebook.length}`,
  );

  const openHomeworkGrading = (entry, results) => {
    const choices = orderHomeworkGradingChoices(results);
    if (choices.length === 1) {
      onOpenHomeworkGrading?.(choices[0].id, entry.studentId);
      return;
    }
    setGradingSelection({
      choices,
      studentId: entry.studentId,
      studentName: entry.studentName || `Học viên #${entry.studentId}`,
    });
  };

  const handleConfirmPublicationAction = () => {
    if (confirmationAction === 'publish') onPublish?.();
    if (confirmationAction === 'unpublish') onUnpublish?.();
    setConfirmationAction(null);
  };

  const openStudentModal = async (entry, mode, lesson) => {
    setSelectedEntry(entry);
    setModalLesson(lesson);
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
    setModalLesson(null);
  };

  const saveEntry = async (form) => {
    if (!selectedEntry) return;
    setSaving(true);
    onMessage?.('');
    try {
      const payload = {
        studentId: selectedEntry.studentId,
        homeworkScores: [],
      };
      if (form.attendancePercent !== '') payload.attendancePercent = Number(form.attendancePercent);
      if (form.finalResult !== '') payload.finalResult = Number(form.finalResult);

      const updated = await classroomApi.updateGradebookEntry(classroomId, payload);
      onGradebookChange?.((current) => current.map((item) => (
        item.studentId === updated.studentId ? updated : item
      )));
      onMessage?.(`Đã cập nhật điểm ${modalLesson?.title || 'bài học'} của ${updated.studentName || selectedEntry.studentName || 'học viên'}.`);
      closeModal();
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể cập nhật bảng điểm.'));
    } finally {
      setSaving(false);
    }
  };

  const allHomeworkLesson = {
    id: 'all-homeworks',
    title: 'Toàn bộ bài tập',
    assignments: homework,
    positionStatus: LESSON_POSITION_STATUS.PASSED,
    gradingStatus: LESSON_GRADING_STATUS.INCOMPLETE,
  };

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-3xl border border-[#dfbfbd]/25 bg-gradient-to-br from-[#fffafb] via-white to-rose-50/40">
        <div className="flex flex-col gap-5 p-5 lg:flex-row lg:items-start lg:justify-between lg:p-6">
          <div className="max-w-2xl">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-[#730014] px-3 py-1 text-[10px] font-extrabold uppercase tracking-[0.16em] text-white">
                Bảng điểm lớp học
              </span>
            </div>
            <h4 className="mt-3 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">
              Theo dõi kết quả của {gradebook.length} học viên
            </h4>
            <p className="mt-1.5 text-sm leading-6 text-[#584140]">
              Xem kết quả toàn lớp, xử lý bài chờ chấm và công bố điểm tại cùng một nơi.
            </p>
          </div>

          <div className="flex flex-wrap gap-2 lg:max-w-md lg:justify-end">
            <button
              className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014]/20 bg-white px-4 py-2.5 text-xs font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff3f4] disabled:cursor-not-allowed disabled:opacity-45"
              disabled={!gradebook.length}
              onClick={onExport}
              type="button"
            >
              <Download className="h-4 w-4" /> Xuất CSV
            </button>
            <button
              className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014]/20 bg-white px-4 py-2.5 text-xs font-extrabold text-[#730014] shadow-sm transition hover:bg-[#fff3f4] disabled:cursor-not-allowed disabled:opacity-45"
              disabled={!hasPublishedEntries}
              onClick={() => setConfirmationAction('unpublish')}
              type="button"
            >
              <EyeOff className="h-4 w-4" /> Thu hồi
            </button>
            <button
              className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-4 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-45"
              disabled={!gradebook.length || allEntriesPublished}
              onClick={() => setConfirmationAction('publish')}
              type="button"
            >
              <Award className="h-4 w-4" /> Công bố bảng điểm
            </button>
          </div>
        </div>

        <div className="grid border-t border-[#dfbfbd]/20 bg-white/75 sm:grid-cols-2 lg:grid-cols-4">
          {[
            { label: 'Học viên', value: gradebook.length, icon: Users, className: 'text-[#9b1c31]' },
            { label: 'Bài tập', value: homework.length, icon: BookOpenCheck, className: 'text-blue-600' },
            { label: 'Lượt bài chờ chấm', value: pendingSubmissionCount, icon: AlertTriangle, className: 'text-amber-600' },
            { label: 'Học viên đã công bố', value: publishedStudentCount, icon: CheckCircle2, className: 'text-emerald-600' },
          ].map((item) => (
            <div className="flex items-center gap-3 border-b border-[#dfbfbd]/15 px-5 py-4 last:border-b-0 sm:odd:border-r lg:border-b-0 lg:border-r lg:last:border-r-0" key={item.label}>
              <item.icon className={`h-5 w-5 flex-shrink-0 ${item.className}`} />
              <div>
                <p className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">{item.value}</p>
                <p className="text-[11px] font-bold text-[#8b706e]">{item.label}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {gradebook.length ? (
        <AggregateGradebookTable
          gradebook={gradebook}
          onOpenStudent={(entry) => openStudentModal(entry, 'detail', allHomeworkLesson)}
        />
      ) : null}

      {!gradebook.length ? (
        <ClassroomEmptyState
          description="Chưa có học viên hoặc dữ liệu bảng điểm để tổ chức theo bài học."
          title="Chưa có dữ liệu chấm điểm"
        />
      ) : !gradingLessons.length ? (
        <section className="rounded-3xl border border-dashed border-[#dfbfbd]/60 bg-[#fffafb] px-6 py-10 text-center">
          <CircleDashed className="mx-auto h-9 w-9 text-[#c9adab]" />
          <h5 className="mt-3 font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">Chưa có bài tập để chấm</h5>
          <p className="mt-1 text-sm text-[#8b706e]">Thêm bài tập trong tab Bài tập để bắt đầu nhận và chấm bài.</p>
        </section>
      ) : (
        <section className="grid min-h-[620px] gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
          <aside className="flex min-h-0 flex-col rounded-3xl border border-gray-100 bg-[#fffafb]/70 p-4">
            <div>
              <h5 className="font-['Manrope'] text-base font-extrabold text-[#2b2828]">Danh sách bài học</h5>
              <p className="mt-1 text-xs text-[#8b706e]">Chọn một bài để xem tình trạng chấm của cả lớp.</p>
            </div>
            <div className="relative mt-4">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#8b706e]" />
              <input
                aria-label="Tìm bài học"
                className="w-full rounded-xl border border-[#dfbfbd]/40 bg-white py-2.5 pl-9 pr-3 text-xs font-bold text-[#2b2828] outline-none placeholder:font-normal focus:border-[#730014]"
                onChange={(event) => setLessonSearch(event.target.value)}
                placeholder="Tìm theo tên hoặc số bài..."
                type="search"
                value={lessonSearch}
              />
            </div>
            <div className="mt-3 flex flex-wrap gap-1.5" role="group" aria-label="Lọc trạng thái bài học">
              {LESSON_FILTERS.map((filter) => (
                <button
                  className={`rounded-full px-2.5 py-1.5 text-[10px] font-extrabold transition ${
                    lessonFilter === filter.id
                      ? 'bg-[#730014] text-white'
                      : 'border border-gray-200 bg-white text-[#584140] hover:border-[#dfbfbd]'
                  }`}
                  key={filter.id}
                  onClick={() => setLessonFilter(filter.id)}
                  type="button"
                >
                  {filter.label}
                </button>
              ))}
            </div>
            <div className="mt-4 min-h-0 flex-1 space-y-2 overflow-y-auto pr-1 lg:max-h-[660px]">
              {visibleLessons.length ? visibleLessons.map((lesson) => (
                <LessonNavigationItem
                  key={lesson.id}
                  lesson={lesson}
                  onSelect={() => setSelectedLessonId(lesson.id)}
                  selected={selectedLesson?.id === lesson.id}
                />
              )) : (
                <div className="rounded-2xl border border-dashed border-gray-200 px-4 py-8 text-center text-xs text-[#8b706e]">
                  Không có bài học phù hợp bộ lọc.
                </div>
              )}
            </div>
          </aside>

          <div className="min-w-0 overflow-hidden rounded-3xl border border-gray-100 bg-white">
            <div className="border-b border-gray-100 bg-[#fffafb] px-5 py-5 sm:px-6">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    {selectedLesson?.displayOrder != null ? (
                      <span className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#9b1c31]">Bài {selectedLesson.displayOrder}</span>
                    ) : null}
                    <LessonPositionBadge status={selectedLesson.positionStatus} />
                    <LessonGradingBadge status={selectedLesson.gradingStatus} />
                  </div>
                  <h5 className="mt-2 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{selectedLesson.title}</h5>
                  {selectedLesson.description ? (
                    <p className="mt-1 line-clamp-2 text-xs leading-5 text-[#8b706e]">{selectedLesson.description}</p>
                  ) : null}
                </div>
                <div className="flex flex-shrink-0 items-center gap-4 text-center">
                  <div>
                    <p className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">{selectedLesson.assignments.length}</p>
                    <p className="text-[10px] font-bold text-[#8b706e]">Bài tập</p>
                  </div>
                  <div className="h-8 w-px bg-gray-200" />
                  <div>
                    <p className="font-['Manrope'] text-lg font-extrabold text-emerald-700">{selectedLesson.stats.gradedCount}</p>
                    <p className="text-[10px] font-bold text-[#8b706e]">Đã chấm</p>
                  </div>
                  <div className="h-8 w-px bg-gray-200" />
                  <div>
                    <p className="font-['Manrope'] text-lg font-extrabold text-amber-700">{selectedLesson.stats.pendingCount}</p>
                    <p className="text-[10px] font-bold text-[#8b706e]">Chờ chấm</p>
                  </div>
                </div>
              </div>
              {selectedLesson.stats.expectedSubmissionCount > 0 ? (
                <div className="mt-4">
                  <div className="mb-1.5 flex justify-between text-[10px] font-extrabold text-[#8b706e]">
                    <span>Tiến độ chấm của bài học</span>
                    <span>{selectedLesson.stats.completionPercent}%</span>
                  </div>
                  <div className="h-1.5 overflow-hidden rounded-full bg-gray-100">
                    <div className="h-full rounded-full bg-emerald-500" style={{ width: `${selectedLesson.stats.completionPercent}%` }} />
                  </div>
                </div>
              ) : null}
            </div>

            {selectedLesson.positionStatus === LESSON_POSITION_STATUS.NOT_REACHED ? (
              <div className="m-5 flex min-h-72 flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-slate-50/70 px-6 text-center sm:m-6">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-200 text-slate-500">
                  <LockKeyhole className="h-6 w-6" />
                </div>
                <h6 className="mt-4 font-['Manrope'] text-lg font-extrabold text-slate-700">Bài này chưa học tới</h6>
                <p className="mt-1 max-w-md text-sm leading-6 text-slate-500">
                  Bạn vẫn có thể xem bài trong danh sách tiến độ, nhưng thao tác chấm được khóa để tránh nhập điểm nhầm trước khi lớp học tới bài này.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] divide-y divide-gray-100 text-left text-sm">
                  <thead className="bg-white text-[10px] font-bold uppercase tracking-wider text-[#8b706e]">
                    <tr>
                      <th className="px-5 py-3.5 sm:px-6">Học viên</th>
                      {selectedLesson.assignments.map((assignment) => (
                        <th className="max-w-44 px-4 py-3.5" key={assignment.id}>{assignment.title}</th>
                      ))}
                      <th className="px-4 py-3.5">Tiến độ</th>
                      <th className="px-5 py-3.5 text-right sm:px-6">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 text-[#584140]">
                    {gradingPageItems.map((entry) => {
                      const progress = getStudentLessonProgress(entry, selectedLesson.assignments);
                      return (
                        <tr className="hover:bg-[#fffafb]/50" key={entry.studentId || entry.id}>
                          <td className="px-5 py-4 sm:px-6">
                            <div className="flex items-center gap-2.5">
                              <span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-rose-50 text-xs font-extrabold text-[#730014]">
                                {(entry.studentName || 'H').charAt(0).toUpperCase()}
                              </span>
                              <p className="max-w-44 truncate font-extrabold text-[#2b2828]">{entry.studentName || `Học viên #${entry.studentId}`}</p>
                            </div>
                          </td>
                          {progress.results.map((result) => (
                            <td className="px-4 py-4" key={result.id}>
                              <div className="space-y-1.5">
                                <p className="whitespace-nowrap text-xs font-extrabold text-[#2b2828]">{formatScore(result.score, result.maxScore)}</p>
                                <HomeworkStatusBadge status={result.status} />
                              </div>
                            </td>
                          ))}
                          <td className="px-4 py-4">
                            <div className="flex items-center gap-2 whitespace-nowrap">
                              <div className="h-1.5 w-16 overflow-hidden rounded-full bg-gray-100">
                                <div
                                  className={`h-full rounded-full ${progress.isComplete ? 'bg-emerald-500' : 'bg-amber-400'}`}
                                  style={{ width: `${selectedLesson.assignments.length ? (progress.gradedCount / selectedLesson.assignments.length) * 100 : 0}%` }}
                                />
                              </div>
                              <span className="text-[10px] font-extrabold text-[#8b706e]">{progress.gradedCount}/{selectedLesson.assignments.length}</span>
                            </div>
                          </td>
                          <td className="px-5 py-4 text-right sm:px-6">
                            <button
                              className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-extrabold transition ${
                                progress.pendingCount > 0
                                  ? 'bg-amber-500 text-white hover:bg-amber-600'
                                  : 'border border-[#dfbfbd]/50 bg-white text-[#730014] hover:bg-rose-50'
                              }`}
                              onClick={() => openHomeworkGrading(entry, progress.results)}
                              type="button"
                            >
                              <Pencil className="h-3.5 w-3.5" />
                              {progress.results.length > 1
                                ? 'Chọn bài'
                                : progress.isComplete ? 'Sửa điểm' : 'Chấm điểm'}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
                {gradingTotalPages > 1 ? (
                  <div className="border-t border-gray-100 bg-[#fffafb]/50 px-5 py-4 sm:px-6">
                    <Pagination
                      onChange={setGradingPage}
                      page={gradingPage}
                      pageSize={STUDENTS_PER_PAGE}
                      totalItems={gradingTotalItems}
                      totalPages={gradingTotalPages}
                    />
                  </div>
                ) : null}
              </div>
            )}
          </div>
        </section>
      )}

      {selectedEntry && modalLesson ? (
        <GradebookStudentModal
          entry={selectedEntry}
          homeworkIds={modalLesson.assignments.map((item) => item.id)}
          lessonTitle={modalLesson.title}
          loading={loadingDetails}
          mode={modalMode}
          onClose={closeModal}
          onEdit={() => setModalMode('edit')}
          onOpenHomework={(homeworkId, studentId) => {
            closeModal();
            onOpenHomeworkGrading?.(homeworkId, studentId);
          }}
          onSave={saveEntry}
          saving={saving}
        />
      ) : null}

      {gradingSelection ? (
        <HomeworkGradingChoiceModal
          choices={gradingSelection.choices}
          onClose={() => setGradingSelection(null)}
          onSelect={(homeworkItem) => {
            const studentId = gradingSelection.studentId;
            setGradingSelection(null);
            onOpenHomeworkGrading?.(homeworkItem.id, studentId);
          }}
          studentName={gradingSelection.studentName}
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
