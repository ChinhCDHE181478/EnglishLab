import React, { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock3,
  FileText,
  MessageSquare,
  Paperclip,
  PenTool,
  Search,
  Send,
  UserRound,
  Users,
} from 'lucide-react';
import AuthenticatedFileLink from '../classroom/AuthenticatedFileLink';
import AuthenticatedAudioPlayer from '../classroom/AuthenticatedAudioPlayer';
import { formatClassroomDateTime, getHomeworkMaxScore } from '../../utils/classroomHelpers';
import { getHomeworkActivityTypeLabel } from '../../utils/homeworkGradingConfig';
import TeacherWritingAnnotationEditor from './TeacherWritingAnnotationEditor';

const timingCopy = {
  ON_TIME: { label: 'Đúng hạn', className: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  LATE: { label: 'Nộp muộn', className: 'bg-amber-50 text-amber-700 border-amber-200' },
  NOT_SUBMITTED: { label: 'Chưa nộp', className: 'bg-gray-100 text-gray-600 border-gray-200' },
};

const safeJson = (value) => {
  if (!value || typeof value !== 'string') return null;
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
};

const collectQuestions = (value, result = []) => {
  if (Array.isArray(value)) {
    value.forEach((item) => collectQuestions(item, result));
    return result;
  }
  if (!value || typeof value !== 'object') return result;

  const prompt =
    value.prompt ||
    value.question ||
    value.questionText ||
    [value.promptBefore, value.promptAfter].filter(Boolean).join(' _____ ');
  if (prompt && (value.number != null || value.id != null || value.options)) {
    result.push(value);
  }
  Object.entries(value).forEach(([key, child]) => {
    if (!['options', 'answerKey'].includes(key)) collectQuestions(child, result);
  });
  return result;
};

const normalizeOptions = (options = []) =>
  options.map((option, index) => ({
    value: String(
      typeof option === 'object'
        ? option.value ?? option.key ?? String.fromCharCode(65 + index)
        : String.fromCharCode(65 + index)
    ),
    label: String(
      typeof option === 'object' ? option.label ?? option.text ?? option.value ?? '' : option
    ),
  }));

const readStructuredSubmission = (homework, textAnswer) => {
  const config = safeJson(homework.activityConfigJson) || {};
  const payload = safeJson(textAnswer);
  const responses =
    payload?.responses && typeof payload.responses === 'object'
      ? payload.responses
      : payload && typeof payload === 'object' && !Array.isArray(payload)
      ? payload
      : null;
  if (!responses) return null;

  const answerKey = safeJson(homework.objectiveAnswerKey) || config.answerKey || {};
  const questions = collectQuestions(config).filter(
    (question, index, items) =>
      items.findIndex(
        (candidate) => String(candidate.number ?? candidate.id) === String(question.number ?? question.id)
      ) === index
  );
  if (!questions.length) return null;

  return questions.map((question, index) => {
    const number = String(question.number ?? question.id ?? index + 1);
    const selectedValue = String(responses[number] ?? responses[question.submissionKey] ?? '');
    const options = normalizeOptions(question.options);
    const selectedOption = options.find((option) => option.value === selectedValue);
    const configuredAnswers = answerKey[number] ?? question.correctAnswer ?? '';
    const correctValues = (Array.isArray(configuredAnswers) ? configuredAnswers : [configuredAnswers])
      .map((value) => String(value).trim())
      .filter(Boolean);
    const correctLabels = correctValues.map((value) => {
      const option = options.find((item) => item.value.toLocaleLowerCase() === value.toLocaleLowerCase());
      return option ? `${option.value}. ${option.label}` : value;
    });
    return {
      number,
      prompt:
        question.prompt ||
        question.question ||
        question.questionText ||
        [question.promptBefore, question.promptAfter].filter(Boolean).join(' _____ '),
      answer: selectedOption ? `${selectedOption.value}. ${selectedOption.label}` : selectedValue || 'Không trả lời',
      correctAnswer: correctLabels.join(' hoặc '),
      correct: Boolean(
        selectedValue &&
          correctValues.some((value) => value.toLocaleLowerCase() === selectedValue.toLocaleLowerCase())
      ),
    };
  });
};

function TimingBadge({ value }) {
  const cfg = timingCopy[value] || timingCopy.NOT_SUBMITTED;
  return (
    <span
      className={`inline-block rounded-full border px-2.5 py-0.5 text-[11px] font-bold ${cfg.className}`}
    >
      {cfg.label}
    </span>
  );
}

const FEEDBACK_TEMPLATES = [
  'Bài viết tốt, bố cục 3 phần rõ ràng.',
  'Cần chú ý chia thì từ vựng & cấu trúc ngữ pháp.',
  'Từ vựng phong phú, sử dụng nhiều collocations hay.',
  'Nên mở rộng thêm luận điểm ở đoạn thân bài thứ 2.',
  'Cải thiện mạch kết nối giữa các câu văn.',
];

export default function TeacherHomeworkGradingWorkspace({
  homework,
  submissions = [],
  loading = false,
  gradingForms = {},
  onGradingFormsChange,
  gradingId = null,
  gradingNotice = null,
  onGrade,
  onSaveAnnotations,
  onBack,
}) {
  const [selectedStudentId, setSelectedStudentId] = useState(null);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [showInstruction, setShowInstruction] = useState(true);

  const isAutoGraded = ['QUIZ', 'ONLINE_TEST', 'FLASHCARD_PRACTICE'].includes(homework?.activityType);

  const counts = useMemo(
    () => ({
      ALL: submissions.length,
      ON_TIME: submissions.filter((item) => item.submissionTiming === 'ON_TIME').length,
      LATE: submissions.filter((item) => item.submissionTiming === 'LATE').length,
      NOT_SUBMITTED: submissions.filter((item) => item.submissionTiming === 'NOT_SUBMITTED').length,
      GRADED: submissions.filter((item) => item.status === 'GRADED').length,
    }),
    [submissions]
  );

  const visibleSubmissions = useMemo(
    () =>
      submissions.filter((item) => {
        const matchFilter =
          filter === 'ALL'
            ? true
            : filter === 'GRADED'
            ? item.status === 'GRADED'
            : item.submissionTiming === filter;
        const normalizedQuery = query.trim().toLocaleLowerCase();
        const matchQuery =
          !normalizedQuery ||
          (item.studentName || '').toLocaleLowerCase().includes(normalizedQuery) ||
          (item.studentEmail || '').toLocaleLowerCase().includes(normalizedQuery);
        return matchFilter && matchQuery;
      }),
    [submissions, filter, query]
  );

  useEffect(() => {
    if (!visibleSubmissions.length) {
      setSelectedStudentId(null);
      return;
    }
    if (!selectedStudentId || !visibleSubmissions.some((item) => item.studentId === selectedStudentId)) {
      setSelectedStudentId(visibleSubmissions[0].studentId);
    }
  }, [visibleSubmissions, selectedStudentId]);

  const selected = submissions.find((item) => item.studentId === selectedStudentId) || null;
  const formState = selected ? gradingForms[selected.studentId] || { score: '', teacherFeedback: '', annotations: [] } : null;
  const structuredAnswers = selected?.submitted ? readStructuredSubmission(homework, selected.textAnswer) : null;

  const updateForm = (field, value) => {
    if (!selected) return;
    onGradingFormsChange((current) => ({
      ...current,
      [selected.studentId]: {
        ...formState,
        [field]: value,
      },
    }));
  };

  const appendFeedbackTemplate = (text) => {
    const current = formState?.teacherFeedback || '';
    const updated = current ? `${current.trim()}\n- ${text}` : `- ${text}`;
    updateForm('teacherFeedback', updated);
  };

  const maxScore = getHomeworkMaxScore(homework) || 10;
  const scorePresets = useMemo(() => {
    if (maxScore === 10) return ['5.0', '6.0', '6.5', '7.0', '7.5', '8.0', '8.5', '9.0', '9.5', '10.0'];
    return Array.from({ length: 6 }, (_, i) => String(Math.round((maxScore * (i + 5)) / 10)));
  }, [maxScore]);

  const wordCount = useMemo(() => {
    if (!selected?.textAnswer) return 0;
    return selected.textAnswer.trim().split(/\s+/).filter(Boolean).length;
  }, [selected?.textAnswer]);

  return (
    <div className="space-y-5">
      {/* Header Banner */}
      <section className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-6 shadow-sm">
        <button
          className="inline-flex items-center gap-2 text-xs font-extrabold text-[#730014] transition hover:underline"
          onClick={onBack}
          type="button"
        >
          <ArrowLeft className="h-4 w-4" /> Quay lại danh sách bài tập
        </button>

        <div className="mt-4 flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <span className="rounded-full bg-[#730014]/10 px-3 py-1 text-xs font-extrabold text-[#730014]">
                {getHomeworkActivityTypeLabel(homework.activityType)}
              </span>
              {homework.skill ? (
                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-700 uppercase">
                  {homework.skill}
                </span>
              ) : null}
            </div>

            <h3 className="mt-2 break-words font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
              {homework.title}
            </h3>
            <p className="mt-1.5 text-sm text-[#584140]">
              Hạn nộp: <span className="font-bold">{formatClassroomDateTime(homework.deadline)}</span> · Điểm tối đa:{' '}
              <span className="font-bold text-[#730014]">{maxScore} điểm</span>
            </p>
          </div>

          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {[
              ['Sĩ số', counts.ALL, Users],
              ['Đúng hạn', counts.ON_TIME, CheckCircle2],
              ['Nộp muộn', counts.LATE, Clock3],
              ['Đã chấm', counts.GRADED, PenTool],
            ].map(([label, value, Icon]) => (
              <div
                className="min-w-[110px] rounded-xl border border-[#dfbfbd]/25 bg-[#fffafb] p-3 text-center"
                key={label}
              >
                <Icon className="mx-auto h-4 w-4 text-[#730014]" />
                <p className="mt-1 text-lg font-extrabold text-[#2b2828]">{value}</p>
                <p className="text-[10px] font-bold uppercase tracking-wide text-[#8b706e]">{label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Main Workspace Layout */}
      <div className="grid min-h-[680px] gap-5 xl:grid-cols-[330px_minmax(0,1fr)]">
        {/* Left Sidebar: Roster & Filters */}
        <aside className="min-w-0 rounded-2xl border border-[#dfbfbd]/30 bg-white p-4 shadow-sm space-y-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9b8583]" />
            <input
              className="w-full rounded-xl border border-gray-200 py-2.5 pl-10 pr-3 text-sm outline-none transition focus:border-[#730014]"
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm tên hoặc email..."
              value={query}
            />
          </div>

          <div className="grid grid-cols-2 gap-1.5">
            {[
              ['ALL', 'Tất cả'],
              ['ON_TIME', 'Đúng hạn'],
              ['LATE', 'Nộp muộn'],
              ['GRADED', 'Đã chấm'],
            ].map(([val, label]) => (
              <button
                key={val}
                type="button"
                onClick={() => setFilter(val)}
                className={`rounded-lg px-2.5 py-1.5 text-xs font-extrabold transition ${
                  filter === val
                    ? 'bg-[#730014] text-white shadow-xs'
                    : 'bg-[#f8f4f4] text-[#584140] hover:bg-[#f2e8e8]'
                }`}
              >
                {label} ({counts[val] || 0})
              </button>
            ))}
          </div>

          <div className="max-h-[580px] space-y-2 overflow-y-auto pr-1">
            {loading ? <p className="py-8 text-center text-sm text-[#8b706e]">Đang tải danh sách bài nộp...</p> : null}
            {!loading && !visibleSubmissions.length ? (
              <p className="py-8 text-center text-sm text-[#8b706e]">Không tìm thấy học viên nào.</p>
            ) : null}

            {!loading &&
              visibleSubmissions.map((item) => {
                const isSelected = selectedStudentId === item.studentId;
                return (
                  <button
                    key={item.studentId}
                    type="button"
                    onClick={() => setSelectedStudentId(item.studentId)}
                    className={`w-full rounded-xl border p-3 text-left transition ${
                      isSelected
                        ? 'border-[#730014] bg-[#fff0f2] shadow-xs'
                        : 'border-gray-200/80 bg-white hover:border-[#dfbfbd] hover:bg-[#fffdfc]'
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div
                        className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-extrabold ${
                          isSelected ? 'bg-[#730014] text-white' : 'bg-[#f3e7e7] text-[#730014]'
                        }`}
                      >
                        {(item.studentName || '?').trim().charAt(0).toUpperCase()}
                      </div>

                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-1">
                          <p className="truncate text-sm font-extrabold text-[#2b2828]">
                            {item.studentName || `Học viên #${item.studentId}`}
                          </p>
                          {item.score != null ? (
                            <span className="rounded-md bg-emerald-100 px-2 py-0.5 text-xs font-extrabold text-emerald-800">
                              {item.score}/{maxScore}
                            </span>
                          ) : null}
                        </div>

                        <p className="truncate text-[11px] text-[#8b706e]">{item.studentEmail}</p>

                        <div className="mt-2 flex flex-wrap items-center gap-1.5">
                          <TimingBadge value={item.submissionTiming} />
                          {item.status === 'GRADED' ? (
                            <span className="rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-extrabold text-blue-700 border border-blue-200">
                              Đã chấm
                            </span>
                          ) : null}
                        </div>
                      </div>
                    </div>
                  </button>
                );
              })}
          </div>
        </aside>

        {/* Right Main Area: Interactive Grading Canvas */}
        <main className="min-w-0 rounded-2xl border border-[#dfbfbd]/30 bg-white shadow-sm flex flex-col justify-between">
          {!selected ? (
            <div className="flex min-h-[520px] items-center justify-center p-8 text-center text-sm text-[#8b706e]">
              Chọn một học viên ở cột bên trái để tiến hành chấm bài.
            </div>
          ) : (
            <div className="divide-y divide-[#f0e8e7]">
              {/* Student Info Bar */}
              <header className="flex flex-wrap items-center justify-between gap-3 p-5 bg-[#fffafb]">
                <div className="flex items-center gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[#730014] text-base font-extrabold text-white">
                    {(selected.studentName || '?').trim().charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">
                      {selected.studentName}
                    </h4>
                    <p className="text-xs text-[#8b706e]">
                      {selected.studentEmail}
                      {selected.submitted ? (
                        <span className="ml-2 font-medium text-slate-600">
                          · Nộp lúc {formatClassroomDateTime(selected.submittedAt)}
                        </span>
                      ) : null}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <TimingBadge value={selected.submissionTiming} />
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-extrabold border ${
                      selected.status === 'GRADED'
                        ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                        : selected.submitted
                        ? 'bg-amber-50 text-amber-800 border-amber-200'
                        : 'bg-gray-50 text-gray-600 border-gray-200'
                    }`}
                  >
                    {selected.status === 'GRADED'
                      ? 'Đã hoàn tất chấm'
                      : selected.submitted
                      ? 'Chờ giáo viên chấm'
                      : 'Chưa nộp bài'}
                  </span>
                </div>
              </header>

              {/* Assignment Instruction Accordion */}
              <section className="bg-[#fffdfc] p-4">
                <button
                  type="button"
                  onClick={() => setShowInstruction(!showInstruction)}
                  className="flex w-full items-center justify-between text-xs font-extrabold uppercase tracking-wider text-[#8b706e] hover:text-[#730014] transition"
                >
                  <span className="flex items-center gap-2">
                    <FileText className="h-4 w-4 text-[#730014]" /> Đề bài &amp; Yêu cầu bài tập
                  </span>
                  {showInstruction ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                </button>

                {showInstruction ? (
                  <div className="mt-3 rounded-xl border border-gray-200/70 bg-white p-4 text-sm leading-relaxed text-slate-800 space-y-2">
                    <p className="whitespace-pre-wrap">{homework.instruction || 'Không có hướng dẫn bổ sung.'}</p>
                    {homework.attachmentUrl ? (
                      <AuthenticatedFileLink
                        className="inline-flex items-center gap-1.5 text-xs font-bold text-[#730014] hover:underline"
                        url={homework.attachmentUrl}
                      >
                        <Paperclip className="h-3.5 w-3.5" /> Xem tệp đề bài đính kèm
                      </AuthenticatedFileLink>
                    ) : null}
                  </div>
                ) : null}
              </section>

              {/* Submission Content / Writing Canvas */}
              <section className="p-6 space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <h5 className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">
                      Nội dung bài làm của học viên
                    </h5>
                    {homework.skill === 'WRITING' && selected.textAnswer ? (
                      <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-bold text-slate-700">
                        {wordCount} từ
                      </span>
                    ) : null}
                  </div>

                  {selected.attachmentUrl ? (
                    <AuthenticatedFileLink
                      className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-[#fffafb] px-3 py-1.5 text-xs font-bold text-[#730014] hover:bg-[#fff0f2] transition"
                      url={selected.attachmentUrl}
                    >
                      <Paperclip className="h-3.5 w-3.5" /> Tải tệp đính kèm bài làm
                    </AuthenticatedFileLink>
                  ) : null}
                </div>

                {homework.skill === 'SPEAKING' && selected.attachmentUrl ? (
                  <div className="mt-2">
                    <AuthenticatedAudioPlayer url={selected.attachmentUrl} />
                  </div>
                ) : null}

                {!selected.submitted ? (
                  <div className="rounded-2xl border border-dashed border-gray-300 bg-gray-50 px-5 py-12 text-center">
                    <UserRound className="mx-auto h-8 w-8 text-gray-400" />
                    <p className="mt-3 font-bold text-slate-700">Học viên chưa nộp bài làm</p>
                    <p className="mt-1 text-xs text-slate-500">
                      Không thể nhập điểm khi học viên chưa gửi bài làm trên hệ thống.
                    </p>
                  </div>
                ) : structuredAnswers ? (
                  <div className="space-y-3">
                    {structuredAnswers.map((answer) => (
                      <article className="rounded-xl border border-gray-200 bg-[#fffafb] p-4" key={answer.number}>
                        <div className="flex gap-3">
                          <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#730014] text-xs font-extrabold text-white">
                            {answer.number}
                          </span>
                          <div className="min-w-0">
                            <p className="text-sm font-bold leading-6 text-slate-800">{answer.prompt}</p>
                            <p
                              className={`mt-2 text-sm font-semibold ${
                                answer.correct ? 'text-emerald-700' : 'text-rose-700'
                              }`}
                            >
                              Học viên chọn: {answer.answer}
                            </p>
                            {!answer.correct && answer.correctAnswer ? (
                              <p className="mt-1 text-xs text-slate-600">Đáp án đúng: {answer.correctAnswer}</p>
                            ) : null}
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                ) : homework.skill === 'WRITING' && selected.textAnswer ? (
                  <TeacherWritingAnnotationEditor
                    annotations={formState?.annotations || []}
                    onChange={(annotations) => onSaveAnnotations
                      ? onSaveAnnotations(selected, annotations)
                      : updateForm('annotations', annotations)}
                    text={selected.textAnswer}
                  />
                ) : (
                  <div className="min-h-[160px] rounded-2xl border border-gray-200 bg-[#fffdfc] p-5 text-sm leading-relaxed text-slate-800 whitespace-pre-wrap">
                    {selected.textAnswer ||
                      (selected.attachmentUrl
                        ? 'Học viên nộp bài qua tệp đính kèm (vui lòng mở tệp đính kèm ở trên để xem).'
                        : 'Không có nội dung bài nộp.')}
                  </div>
                )}
              </section>

              {/* Bottom Grading & Feedback Card */}
              {selected.submitted && isAutoGraded ? (
                <section className="bg-[#fffafb] p-6">
                  <div className="rounded-2xl border border-emerald-200 bg-emerald-50/70 p-5 space-y-2">
                    <p className="text-xs font-extrabold uppercase tracking-wider text-emerald-800">
                      Điểm tự động do hệ thống chấm
                    </p>
                    <p className="text-3xl font-black text-emerald-700">
                      {selected.score ?? 0} / {maxScore}
                    </p>
                    <p className="text-xs text-slate-600">
                      {selected.teacherFeedback || 'Hệ thống tự động chấm điểm theo đáp án chuẩn đã thiết lập.'}
                    </p>
                  </div>
                </section>
              ) : selected.submitted ? (
                <section className="bg-[#fffafb] p-6 space-y-5">
                  <div className="grid gap-4 md:grid-cols-[160px_minmax(0,1fr)]">
                    <label className="block space-y-2">
                      <span className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">
                        Điểm / {maxScore}
                      </span>
                      <input
                        className="w-full rounded-xl border border-gray-300 bg-white px-4 py-2.5 text-base font-bold text-slate-800 outline-none transition focus:border-[#730014] focus:ring-2 focus:ring-[#730014]/10"
                        max={maxScore}
                        min="0"
                        onChange={(e) => updateForm('score', e.target.value)}
                        placeholder={`0 - ${maxScore}`}
                        step="0.5"
                        type="number"
                        value={formState?.score || ''}
                      />
                    </label>

                    <label className="block space-y-2">
                      <span className="text-xs font-extrabold uppercase tracking-wider text-[#730014]">
                        Nhận xét chung cho học viên
                      </span>
                      <textarea
                        className="min-h-[105px] w-full rounded-xl border border-gray-300 bg-white p-4 text-sm leading-relaxed text-slate-800 outline-none transition focus:border-[#730014] focus:ring-2 focus:ring-[#730014]/10"
                        onChange={(e) => updateForm('teacherFeedback', e.target.value)}
                        placeholder="Nêu rõ điểm làm tốt, các lỗi cần chú ý và hướng cải thiện cho học viên..."
                        value={formState?.teacherFeedback || ''}
                      />
                    </label>
                  </div>

                  {gradingNotice?.studentId === selected.studentId ? (
                    <div
                      aria-live="polite"
                      className={`flex items-start gap-2 rounded-xl border px-4 py-3 text-xs font-bold ${
                        gradingNotice.type === 'success'
                          ? 'border-emerald-200 bg-emerald-50 text-emerald-800'
                          : 'border-rose-200 bg-rose-50 text-rose-800'
                      }`}
                    >
                      {gradingNotice.type === 'success' ? (
                        <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
                      ) : (
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                      )}
                      <span>{gradingNotice.message}</span>
                    </div>
                  ) : null}

                  {/* Save Button */}
                  <div className="flex justify-end pt-2">
                    <button
                      type="button"
                      disabled={gradingId === selected.studentId}
                      onClick={() => onGrade(selected)}
                      className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-[#730014] to-[#4b0009] px-6 py-3 text-xs font-extrabold text-white shadow-md transition hover:from-[#8a0018] hover:to-[#5a000b] active:scale-95 disabled:opacity-60"
                    >
                      <Send className="h-4 w-4" />
                      {gradingId === selected.studentId
                        ? 'Đang lưu...'
                        : selected.status === 'GRADED'
                        ? 'Cập nhật kết quả chấm'
                        : 'Lưu kết quả chấm điểm'}
                    </button>
                  </div>
                </section>
              ) : null}
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
