import { useEffect, useMemo, useState } from 'react';
import {
  Award,
  CheckCircle2,
  Clock,
  Edit3,
  Paperclip,
  FileText,
  Plus,
  Send,
  Trash2,
  Users,
  X,
} from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { ClassroomEmptyState } from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import {
  formatClassroomDateTime,
  formatHomeworkStatus,
  fromDateTimeLocalValue,
  getHomeworkMaxScore,
  getSubmissionFeedback,
  toDateTimeLocalValue,
} from '../../utils/classroomHelpers';
import {
  getHomeworkGradingModeLabel,
  getHomeworkActivityTypeLabel,
  getHomeworkSkillLabel,
  HOMEWORK_ACTIVITY_TYPES,
  isAiGradedHomework,
} from '../../utils/homeworkGradingConfig';

const emptyForm = {
  title: '',
  instruction: '',
  deadline: '',
  maxScore: '10',
  allowResubmission: false,
  status: 'OPEN',
  sessionId: '',
  curriculumUnitId: '',
  activityType: 'SKILL_PRACTICE',
  activityConfigJson: '',
  aiReviewEnabled: false,
  gradingMode: 'TEACHER',
  skill: 'SPEAKING',
  rubricId: '',
  assessmentBankItemId: '',
};

const createEmptyQuestion = () => ({
  prompt: '',
  options: ['', '', '', ''],
  correctAnswer: 'A',
});

const parseQuestionBuilderConfig = (value) => {
  try {
    const config = JSON.parse(value || '{}');
    return (config.questions || []).map((question, index) => ({
      prompt: question.prompt || '',
      options: (question.options || []).map((option) => (
        typeof option === 'object' ? String(option.label || option.value || '') : String(option)
      )).concat(['', '', '', '']).slice(0, 4),
      correctAnswer: config.answerKey?.[String(question.number || index + 1)] || question.correctAnswer || 'A',
    }));
  } catch {
    return [];
  }
};

const homeworkStatusOptions = [
  { label: 'Mở nộp bài (OPEN)', value: 'OPEN' },
  { label: 'Lưu nháp (DRAFT)', value: 'DRAFT' },
  { label: 'Đóng bài (CLOSED)', value: 'CLOSED' },
];

const statusTone = (status) => {
  if (status === 'OPEN') return 'bg-emerald-50 text-emerald-700';
  if (status === 'DRAFT') return 'bg-gray-100 text-gray-600';
  return 'bg-amber-50 text-amber-700';
};

export default function TeacherHomeworkSection({
  classroomId,
  homework,
  sessions,
  onHomeworkChange,
  onGradebookChange,
  onMessage,
  initialOpenCreate = false,
  onCreateFormOpened,
  curriculumUnits = [],
}) {
  const [formOpen, setFormOpen] = useState(false);
  const [editingHomework, setEditingHomework] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [attachmentFile, setAttachmentFile] = useState(null);
  const [questionDrafts, setQuestionDrafts] = useState([createEmptyQuestion()]);

  const [gradingHomework, setGradingHomework] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [submissionsLoading, setSubmissionsLoading] = useState(false);
  const [gradingForms, setGradingForms] = useState({});
  const [gradingId, setGradingId] = useState(null);
  const [aiAssessmentOptions, setAiAssessmentOptions] = useState([]);
  const [aiAssessmentOptionsLoading, setAiAssessmentOptionsLoading] = useState(false);

  const activityTypeOptions = useMemo(
    () => HOMEWORK_ACTIVITY_TYPES.map((item) => ({ label: item.label, value: item.value })),
    [],
  );

  const curriculumUnitOptions = useMemo(
    () => [
      { label: 'Không gắn unit cụ thể', value: '' },
      ...curriculumUnits.map((unit) => ({
        label: `${unit.displayOrder ?? 0}. ${unit.title}`,
        value: String(unit.id),
      })),
    ],
    [curriculumUnits],
  );

  const aiAssessmentSelectOptions = useMemo(
    () => aiAssessmentOptions.map((item) => ({
      label: `${item.title} · ${item.skill === 'WRITING' ? 'Writing' : 'Speaking'}`,
      value: String(item.id),
    })),
    [aiAssessmentOptions],
  );

  const selectedAiAssessment = useMemo(
    () => aiAssessmentOptions.find((item) => String(item.id) === String(form.assessmentBankItemId)) || null,
    [aiAssessmentOptions, form.assessmentBankItemId],
  );
  const canEnableAi = Boolean(selectedAiAssessment?.rubricId);

  const sessionOptions = useMemo(
    () => [
      { label: 'Không gắn buổi học cụ thể', value: '' },
      ...(sessions || []).map((session) => ({
        label: `${session.title || `Buổi #${session.id}`} · ${formatClassroomDateTime(session.startTime)}`,
        value: String(session.id),
      })),
    ],
    [sessions],
  );

  useEffect(() => {
    if (!formOpen) {
      return undefined;
    }

    let active = true;
    setAiAssessmentOptionsLoading(true);
    const loadAiAssessmentOptions = async () => {
      try {
        const data = await classroomApi.getHomeworkAiAssessmentOptions();
        if (!active) return;
        setAiAssessmentOptions(data);
      } catch {
        if (active) setAiAssessmentOptions([]);
      } finally {
        if (active) setAiAssessmentOptionsLoading(false);
      }
    };

    loadAiAssessmentOptions();

    return () => {
      active = false;
    };
  }, [formOpen]);

  const resetForm = () => {
    setForm(emptyForm);
    setEditingHomework(null);
    setFormOpen(false);
    setAttachmentFile(null);
    setQuestionDrafts([createEmptyQuestion()]);
  };

  const openCreateForm = () => {
    setEditingHomework(null);
    setForm(emptyForm);
    setAttachmentFile(null);
    setQuestionDrafts([createEmptyQuestion()]);
    setFormOpen(true);
  };

  useEffect(() => {
    if (!initialOpenCreate) {
      return;
    }
    openCreateForm();
    onCreateFormOpened?.();
  }, [initialOpenCreate]);

  const openEditForm = (item) => {
    setEditingHomework(item);
    setForm({
      title: item.title || '',
      instruction: item.instruction || '',
      deadline: toDateTimeLocalValue(item.deadline),
      maxScore: String(getHomeworkMaxScore(item)),
      allowResubmission: Boolean(item.allowResubmission),
      status: item.status || 'OPEN',
      sessionId: item.sessionId ? String(item.sessionId) : '',
      curriculumUnitId: item.curriculumUnitId ? String(item.curriculumUnitId) : '',
      activityType: item.activityType || 'TEXT_RESPONSE',
      activityConfigJson: item.activityConfigJson || '',
      aiReviewEnabled: Boolean(item.aiReviewEnabled || item.gradingMode === 'AI'),
      gradingMode: item.gradingMode || 'TEACHER',
      skill: item.skill || 'SPEAKING',
      rubricId: item.rubricId ? String(item.rubricId) : '',
      assessmentBankItemId: item.assessmentBankItemId ? String(item.assessmentBankItemId) : '',
    });
    setAttachmentFile(null);
    const parsedQuestions = parseQuestionBuilderConfig(item.activityConfigJson);
    setQuestionDrafts(parsedQuestions.length ? parsedQuestions : [createEmptyQuestion()]);
    setFormOpen(true);
  };

  const buildPayload = (attachmentUrl, activityConfigJson = form.activityConfigJson) => ({
    title: form.title.trim(),
    instruction: form.instruction.trim(),
    deadline: fromDateTimeLocalValue(form.deadline),
    maxScore: Number(form.maxScore) || 10,
    allowResubmission: Boolean(form.allowResubmission),
    status: form.status,
    sessionId: form.sessionId ? Number(form.sessionId) : null,
    curriculumUnitId: form.curriculumUnitId ? Number(form.curriculumUnitId) : null,
    activityType: form.activityType,
    activityConfigJson: activityConfigJson?.trim() || '',
    aiReviewEnabled: Boolean(form.aiReviewEnabled),
    attachmentUrl,
    gradingMode: form.aiReviewEnabled ? 'AI' : 'TEACHER',
    skill: selectedAiAssessment?.skill || null,
    rubricId: selectedAiAssessment?.rubricId || null,
    assessmentBankItemId: form.assessmentBankItemId ? Number(form.assessmentBankItemId) : null,
  });

  const handleSaveHomework = async () => {
    if (!form.title.trim()) {
      onMessage?.('Vui lòng nhập tiêu đề bài tập.');
      return;
    }
    if (form.aiReviewEnabled && !canEnableAi) {
      onMessage?.('Muốn dùng AI, vui lòng chọn một MODULE_TEST Writing hoặc Speaking của hệ thống.');
      return;
    }
    if (form.activityType === 'FILE_RESPONSE' && !attachmentFile && !editingHomework?.attachmentUrl) {
      onMessage?.('Vui lòng tải tệp đề bài khi chọn hình thức giao bài bằng file.');
      return;
    }
    if (form.activityType === 'SKILL_PRACTICE') {
      const invalidQuestion = questionDrafts.find((question) => (
        !question.prompt.trim()
        || question.options.some((option) => !option.trim())
        || !question.correctAnswer
      ));
      if (invalidQuestion) {
        onMessage?.('Vui lòng nhập đủ câu hỏi, 4 lựa chọn và đáp án đúng cho bài soạn trên hệ thống.');
        return;
      }
    }

    setSaving(true);
    onMessage?.('');
    try {
      let attachmentUrl = editingHomework?.attachmentUrl || null;
      if (attachmentFile) {
        const uploaded = await classroomApi.uploadHomeworkAttachment(attachmentFile);
        attachmentUrl = uploaded.url;
      }
      const activityConfigJson = form.activityType === 'SKILL_PRACTICE'
        ? JSON.stringify({
          questions: questionDrafts.map((question, index) => ({
            number: index + 1,
            prompt: question.prompt.trim(),
            options: question.options.map((option, optionIndex) => ({
              value: String.fromCharCode(65 + optionIndex),
              label: option.trim(),
            })),
          })),
          answerKey: Object.fromEntries(questionDrafts.map((question, index) => [String(index + 1), question.correctAnswer])),
        })
        : form.activityConfigJson;
      const payload = buildPayload(attachmentUrl, activityConfigJson);
      if (editingHomework?.id) {
        await classroomApi.updateHomework(editingHomework.id, payload);
        onMessage?.('Đã cập nhật bài tập.');
      } else {
        await classroomApi.createHomework(classroomId, payload);
        onMessage?.('Đã giao bài tập mới.');
      }
      const refreshed = await classroomApi.getTeacherHomework(classroomId);
      onHomeworkChange?.(refreshed);
      resetForm();
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể lưu bài tập.'));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteHomework = async (item) => {
    if (!window.confirm(`Xóa bài tập "${item.title}"? Hành động này không thể hoàn tác.`)) {
      return;
    }
    onMessage?.('');
    try {
      await classroomApi.deleteHomework(item.id);
      const refreshed = await classroomApi.getTeacherHomework(classroomId);
      onHomeworkChange?.(refreshed);
      onMessage?.('Đã xóa bài tập.');
      if (gradingHomework?.id === item.id) {
        setGradingHomework(null);
        setSubmissions([]);
      }
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể xóa bài tập.'));
    }
  };

  const handlePublishHomework = async (item) => {
    onMessage?.('');
    try {
      await classroomApi.updateHomework(item.id, {
        title: item.title,
        instruction: item.instruction,
        deadline: item.deadline,
        maxScore: getHomeworkMaxScore(item),
        allowResubmission: item.allowResubmission,
        status: 'OPEN',
        sessionId: item.sessionId,
        attachmentUrl: item.attachmentUrl,
        gradingMode: item.gradingMode || 'TEACHER',
        skill: item.skill || null,
        rubricId: item.rubricId || null,
        curriculumUnitId: item.curriculumUnitId || null,
        activityType: item.activityType || 'TEXT_RESPONSE',
        activityConfigJson: item.activityConfigJson || '',
        aiReviewEnabled: Boolean(item.aiReviewEnabled),
        assessmentBankItemId: item.assessmentBankItemId || null,
      });
      const refreshed = await classroomApi.getTeacherHomework(classroomId);
      onHomeworkChange?.(refreshed);
      onMessage?.('Đã mở bài tập cho học viên nộp bài.');
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể mở bài tập.'));
    }
  };

  const openGradingPanel = async (item) => {
    setGradingHomework(item);
    setSubmissions([]);
    setGradingForms({});
    setSubmissionsLoading(true);
    onMessage?.('');
    try {
      const data = await classroomApi.getHomeworkSubmissions(item.id);
      setSubmissions(data);
      const initialForms = {};
      data.forEach((submission) => {
        initialForms[submission.studentId] = {
          score: submission.score != null ? String(submission.score) : '',
          teacherFeedback: getSubmissionFeedback(submission),
        };
      });
      setGradingForms(initialForms);
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể tải bài nộp.'));
    } finally {
      setSubmissionsLoading(false);
    }
  };

  const handleGradeSubmission = async (submission) => {
    if (!gradingHomework) return;
    const formState = gradingForms[submission.studentId] || {};
    const score = Number(formState.score);
    if (!Number.isFinite(score)) {
      onMessage?.('Vui lòng nhập điểm hợp lệ.');
      return;
    }

    setGradingId(submission.studentId);
    onMessage?.('');
    try {
      await classroomApi.gradeHomework(gradingHomework.id, submission.studentId, {
        score,
        teacherFeedback: formState.teacherFeedback?.trim() || '',
      });
      const [refreshedHomework, refreshedSubmissions, refreshedGradebook] = await Promise.all([
        classroomApi.getTeacherHomework(classroomId),
        classroomApi.getHomeworkSubmissions(gradingHomework.id),
        classroomApi.getTeacherGradebook(classroomId),
      ]);
      onHomeworkChange?.(refreshedHomework);
      onGradebookChange?.(refreshedGradebook);
      setSubmissions(refreshedSubmissions);
      onMessage?.(`Đã chấm điểm cho ${submission.studentName || 'học viên'}.`);
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể chấm điểm.'));
    } finally {
      setGradingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-[#dfbfbd]/20 bg-[#fffafb] p-5">
        <div>
          <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Giao bài tập & chấm điểm</h4>
          <p className="mt-1 text-xs leading-5 text-[#584140]">
            Tạo bài tập, theo dõi bài nộp và chấm điểm. Điểm homework sẽ được cập nhật vào bảng điểm lớp.
          </p>
        </div>
        <button
          className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
          onClick={openCreateForm}
          type="button"
        >
          <Plus className="h-4 w-4" />
          Giao bài tập mới
        </button>
      </div>

      {formOpen && (
        <div className="rounded-2xl border border-[#dfbfbd]/25 bg-white p-6 shadow-sm space-y-5">
          <div className="flex items-center justify-between gap-3">
            <h5 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">
              {editingHomework ? 'Chỉnh sửa bài tập' : 'Tạo bài tập mới'}
            </h5>
            <button className="rounded-lg p-2 text-[#8b706e] hover:bg-gray-100" onClick={resetForm} type="button">
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <label className="block space-y-2 md:col-span-2">
              <span className="text-xs font-bold text-[#8b706e]">Tiêu đề bài tập *</span>
              <input
                className="w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                value={form.title}
              />
            </label>

            <div className="md:col-span-2 space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Hình thức giao bài</span>
              <div className="grid gap-2 rounded-xl border border-[#e5e7eb] bg-gray-50 p-1.5 sm:grid-cols-2">
                <button
                  className={`flex items-center justify-center gap-2 rounded-lg px-4 py-3 text-xs font-extrabold transition ${form.activityType !== 'FILE_RESPONSE' ? 'bg-white text-[#730014] shadow-sm' : 'text-[#584140] hover:bg-white/70'}`}
                  onClick={() => setForm((current) => ({ ...current, activityType: 'SKILL_PRACTICE' }))}
                  type="button"
                >
                  <FileText className="h-4 w-4" />
                  Soạn trên hệ thống
                </button>
                <button
                  className={`flex items-center justify-center gap-2 rounded-lg px-4 py-3 text-xs font-extrabold transition ${form.activityType === 'FILE_RESPONSE' ? 'bg-white text-[#730014] shadow-sm' : 'text-[#584140] hover:bg-white/70'}`}
                  onClick={() => setForm((current) => ({ ...current, activityType: 'FILE_RESPONSE' }))}
                  type="button"
                >
                  <Paperclip className="h-4 w-4" />
                  Giao bằng tệp
                </button>
              </div>
            </div>

            <label className="block space-y-2 md:col-span-2">
              <span className="text-xs font-bold text-[#8b706e]">
                Tệp đính kèm {form.activityType === 'FILE_RESPONSE' ? '*' : '(không bắt buộc)'} · tối đa 20 MB
              </span>
              <input
                accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png"
                className="block w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm text-[#584140] file:mr-4 file:rounded-lg file:border-0 file:bg-[#fff0f1] file:px-3 file:py-2 file:text-xs file:font-bold file:text-[#730014] hover:file:bg-[#ffe2e6]"
                onChange={(event) => setAttachmentFile(event.target.files?.[0] || null)}
                type="file"
              />
              <p className="text-xs text-[#8b706e]">
                {attachmentFile ? `Sẽ tải lên: ${attachmentFile.name}` : editingHomework?.attachmentUrl ? 'Đang giữ tệp đính kèm hiện tại.' : 'Hỗ trợ PDF, Office, TXT, ZIP/RAR và ảnh JPG/PNG.'}
              </p>
            </label>

            <label className="block space-y-2 md:col-span-2">
              <span className="text-xs font-bold text-[#8b706e]">Hướng dẫn / đề bài</span>
              <textarea
                className="min-h-[120px] w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                onChange={(event) => setForm((current) => ({ ...current, instruction: event.target.value }))}
                value={form.instruction}
              />
            </label>

            <label className="block space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Hạn nộp</span>
              <input
                className="w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                onChange={(event) => setForm((current) => ({ ...current, deadline: event.target.value }))}
                type="datetime-local"
                value={form.deadline}
              />
            </label>

            <label className="block space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Điểm tối đa</span>
              <input
                className="w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                min="0"
                onChange={(event) => setForm((current) => ({ ...current, maxScore: event.target.value }))}
                step="0.5"
                type="number"
                value={form.maxScore}
              />
            </label>

            <label className="block space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Trạng thái</span>
              <BrandedSelect
                onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
                options={homeworkStatusOptions}
                value={form.status}
              />
            </label>

            <label className="block space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Gắn với buổi học</span>
              <BrandedSelect
                onChange={(event) => setForm((current) => ({ ...current, sessionId: event.target.value }))}
                options={sessionOptions}
                value={form.sessionId}
              />
            </label>

            <label className="block space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Unit trong chương trình học</span>
              <BrandedSelect
                onChange={(event) => setForm((current) => ({ ...current, curriculumUnitId: event.target.value }))}
                options={curriculumUnitOptions}
                value={form.curriculumUnitId}
                searchable={true}
              />
            </label>

            <label className="block space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Loại bài học viên sẽ làm</span>
              <BrandedSelect
                onChange={(event) => setForm((current) => ({ ...current, activityType: event.target.value }))}
                options={activityTypeOptions}
                value={form.activityType}
              />
              <p className="text-xs leading-5 text-[#8b706e]">
                {HOMEWORK_ACTIVITY_TYPES.find((item) => item.value === form.activityType)?.description}
              </p>
            </label>

            {form.activityType === 'SKILL_PRACTICE' ? (
              <div className="space-y-4 md:col-span-2">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <span className="text-xs font-bold text-[#8b706e]">Câu hỏi làm trực tiếp</span>
                    <p className="mt-1 text-xs text-[#8b706e]">Học viên chọn đáp án ngay trên website; đáp án đúng được lưu cùng đề.</p>
                  </div>
                  <button
                    className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-extrabold text-[#730014]"
                    onClick={() => setQuestionDrafts((current) => [...current, createEmptyQuestion()])}
                    type="button"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    Thêm câu
                  </button>
                </div>

                {questionDrafts.map((question, questionIndex) => (
                  <div className="space-y-3 rounded-xl border border-[#e5e7eb] bg-[#fffafb] p-4" key={`question-${questionIndex}`}>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-xs font-extrabold text-[#730014]">Câu {questionIndex + 1}</span>
                      {questionDrafts.length > 1 ? (
                        <button
                          aria-label={`Xóa câu ${questionIndex + 1}`}
                          className="rounded-lg p-2 text-red-600 hover:bg-red-50"
                          onClick={() => setQuestionDrafts((current) => current.filter((_, index) => index !== questionIndex))}
                          title={`Xóa câu ${questionIndex + 1}`}
                          type="button"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      ) : null}
                    </div>
                    <input
                      className="w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]"
                      onChange={(event) => setQuestionDrafts((current) => current.map((item, index) => (
                        index === questionIndex ? { ...item, prompt: event.target.value } : item
                      )))}
                      placeholder="Nhập nội dung câu hỏi"
                      value={question.prompt}
                    />
                    <div className="grid gap-3 sm:grid-cols-2">
                      {question.options.map((option, optionIndex) => (
                        <label className="flex items-center gap-2" key={`option-${questionIndex}-${optionIndex}`}>
                          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[#fff0f1] text-xs font-extrabold text-[#730014]">
                            {String.fromCharCode(65 + optionIndex)}
                          </span>
                          <input
                            className="w-full rounded-xl border border-[#e5e7eb] bg-white px-3 py-2.5 text-xs outline-none focus:border-[#730014]"
                            onChange={(event) => setQuestionDrafts((current) => current.map((item, index) => {
                              if (index !== questionIndex) return item;
                              const options = [...item.options];
                              options[optionIndex] = event.target.value;
                              return { ...item, options };
                            }))}
                            placeholder={`Lựa chọn ${String.fromCharCode(65 + optionIndex)}`}
                            value={option}
                          />
                        </label>
                      ))}
                    </div>
                    <label className="block max-w-xs space-y-2">
                      <span className="text-xs font-bold text-[#8b706e]">Đáp án đúng</span>
                      <BrandedSelect
                        onChange={(event) => setQuestionDrafts((current) => current.map((item, index) => (
                          index === questionIndex ? { ...item, correctAnswer: event.target.value } : item
                        )))}
                        options={['A', 'B', 'C', 'D'].map((value) => ({ label: `Đáp án ${value}`, value }))}
                        value={question.correctAnswer}
                      />
                    </label>
                  </div>
                ))}
              </div>
            ) : form.activityType === 'FLASHCARD_REVIEW' || form.activityType === 'MIXED' ? (
              <label className="block space-y-2 md:col-span-2">
                <span className="text-xs font-bold text-[#8b706e]">Cấu hình hoạt động bổ sung</span>
                <textarea
                  className="min-h-[96px] w-full rounded-xl border border-[#e5e7eb] px-4 py-3 font-mono text-xs outline-none focus:border-[#730014]"
                  onChange={(event) => setForm((current) => ({ ...current, activityConfigJson: event.target.value }))}
                  placeholder='{"flashcardSetIds":[1]}'
                  value={form.activityConfigJson}
                />
              </label>
            ) : null}

            <label className="block space-y-2 md:col-span-2">
              <span className="text-xs font-bold text-[#8b706e]">Đề MODULE_TEST của hệ thống (không bắt buộc)</span>
              {aiAssessmentOptionsLoading ? (
                <p className="text-xs text-[#8b706e]">Đang tải đề Writing/Speaking...</p>
              ) : (
                <BrandedSelect
                  onChange={(event) => {
                    const assessment = aiAssessmentOptions.find((item) => String(item.id) === event.target.value);
                    setForm((current) => ({
                      ...current,
                      assessmentBankItemId: event.target.value,
                      aiReviewEnabled: event.target.value ? current.aiReviewEnabled : false,
                      skill: assessment?.skill || '',
                      rubricId: assessment?.rubricId ? String(assessment.rubricId) : '',
                      activityType: assessment ? 'TEXT_RESPONSE' : current.activityType,
                      instruction: assessment?.instructions || current.instruction,
                      maxScore: assessment?.maxScore ? String(assessment.maxScore) : current.maxScore,
                    }));
                  }}
                  options={aiAssessmentSelectOptions}
                  placeholder="Không dùng đề hệ thống"
                  value={form.assessmentBankItemId}
                  searchable={true}
                />
              )}
              <p className="text-xs leading-5 text-[#8b706e]">
                Chọn đề nếu muốn giao MODULE_TEST có sẵn. Giáo viên vẫn là người chịu trách nhiệm điểm cuối cùng.
              </p>
            </label>

            <div className={`md:col-span-2 rounded-2xl border p-4 ${canEnableAi ? 'border-[#dfbfbd] bg-[#fffafb]' : 'border-gray-200 bg-gray-50'}`}>
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-extrabold text-[#2b2828]">Sử dụng AI hỗ trợ chấm điểm</p>
                  <p className="mt-1 text-xs leading-5 text-[#8b706e]">
                    Chỉ bật được với MODULE_TEST Writing/Speaking của hệ thống. Giáo viên có thể xem lại và sửa điểm AI.
                  </p>
                </div>
                <button
                  aria-checked={form.aiReviewEnabled}
                  aria-label="Sử dụng AI hỗ trợ chấm điểm"
                  className={`relative h-7 w-12 shrink-0 rounded-full transition ${form.aiReviewEnabled ? 'bg-[#730014]' : 'bg-gray-300'} ${canEnableAi ? '' : 'cursor-not-allowed opacity-50'}`}
                  disabled={!canEnableAi}
                  onClick={() => setForm((current) => ({ ...current, aiReviewEnabled: !current.aiReviewEnabled }))}
                  role="switch"
                  type="button"
                >
                  <span className={`absolute top-1 h-5 w-5 rounded-full bg-white shadow transition-all ${form.aiReviewEnabled ? 'left-6' : 'left-1'}`} />
                </button>
              </div>
              {selectedAiAssessment ? (
                <div className="mt-3 rounded-xl border border-white bg-white px-3 py-2 text-xs text-[#584140]">
                  <span className="font-extrabold text-[#730014]">{selectedAiAssessment.skill === 'WRITING' ? 'Writing' : 'Speaking'}</span>
                  {' · '}{selectedAiAssessment.title}
                  {selectedAiAssessment.rubricName ? ` · Rubric: ${selectedAiAssessment.rubricName}` : ' · Chưa có rubric'}
                </div>
              ) : null}
            </div>

            <label className="flex items-center gap-3 md:col-span-2 rounded-xl border border-[#e5e7eb] px-4 py-3">
              <input
                checked={form.allowResubmission}
                className="h-4 w-4 accent-[#4b0009]"
                onChange={(event) => setForm((current) => ({ ...current, allowResubmission: event.target.checked }))}
                type="checkbox"
              />
              <span className="text-sm text-[#584140]">Cho phép học viên nộp lại sau khi đã chấm điểm</span>
            </label>
          </div>

          <div className="flex flex-wrap justify-end gap-3">
            <button className="rounded-xl border border-gray-200 px-5 py-3 text-xs font-extrabold text-[#584140]" onClick={resetForm} type="button">
              Hủy
            </button>
            <button
              className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white disabled:opacity-60"
              disabled={saving}
              onClick={handleSaveHomework}
              type="button"
            >
              {saving ? 'Đang lưu...' : editingHomework ? 'Lưu thay đổi' : 'Giao bài tập'}
            </button>
          </div>
        </div>
      )}

      {gradingHomework && (
        <div className="rounded-2xl border border-[#dfbfbd]/25 bg-white p-6 shadow-sm space-y-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-bold uppercase tracking-wider text-[#730014]">Chấm điểm bài tập</p>
              <h5 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{gradingHomework.title}</h5>
              <p className="mt-1 text-xs text-[#8b706e]">
                Hạn nộp: {formatClassroomDateTime(gradingHomework.deadline)} · Điểm tối đa: {getHomeworkMaxScore(gradingHomework)}
                {isAiGradedHomework(gradingHomework) ? ` · AI chấm (${getHomeworkSkillLabel(gradingHomework.skill)})` : ''}
              </p>
            </div>
            <button
              className="rounded-xl border border-gray-200 px-4 py-2 text-xs font-extrabold text-[#584140]"
              onClick={() => {
                setGradingHomework(null);
                setSubmissions([]);
              }}
              type="button"
            >
              Đóng
            </button>
          </div>

          {submissionsLoading ? (
            <p className="text-sm text-[#8b706e]">Đang tải bài nộp...</p>
          ) : !submissions.length ? (
            <ClassroomEmptyState description="Chưa có học viên nào nộp bài cho bài tập này." title="Chưa có bài nộp" />
          ) : (
            <div className="space-y-4">
              {submissions.map((submission) => {
                const formState = gradingForms[submission.studentId] || { score: '', teacherFeedback: '' };
                const isGraded = submission.status === 'GRADED';
                return (
                  <article className="rounded-2xl border border-gray-100 bg-[#fffafb]/40 p-5 space-y-4" key={submission.id}>
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="font-extrabold text-[#2b2828]">{submission.studentName || `Học viên #${submission.studentId}`}</p>
                        <p className="text-xs text-[#8b706e]">
                          Nộp lúc: {formatClassroomDateTime(submission.submittedAt)}
                          {isGraded ? ` · Đã chấm ${formatClassroomDateTime(submission.gradedAt)}` : ' · Chờ chấm'}
                        </p>
                      </div>
                      <span className={`rounded-full px-3 py-1 text-xs font-bold ${isGraded ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
                        {isGraded ? 'Đã chấm' : 'Chờ chấm'}
                      </span>
                    </div>

                    <div className="rounded-xl border border-gray-100 bg-white p-4 text-sm text-[#584140] whitespace-pre-wrap min-h-[80px]">
                      {submission.textAnswer || 'Không có nội dung văn bản.'}
                    </div>

                    <div className="grid gap-4 md:grid-cols-[160px_1fr_auto]">
                      <label className="block space-y-2">
                        <span className="text-xs font-bold text-[#8b706e]">Điểm / {getHomeworkMaxScore(gradingHomework)}</span>
                        <input
                          className="w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                          max={getHomeworkMaxScore(gradingHomework)}
                          min="0"
                          onChange={(event) => setGradingForms((current) => ({
                            ...current,
                            [submission.studentId]: {
                              ...formState,
                              score: event.target.value,
                            },
                          }))}
                          step="0.5"
                          type="number"
                          value={formState.score}
                        />
                      </label>

                      <label className="block space-y-2">
                        <span className="text-xs font-bold text-[#8b706e]">Nhận xét</span>
                        <textarea
                          className="min-h-[48px] w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                          onChange={(event) => setGradingForms((current) => ({
                            ...current,
                            [submission.studentId]: {
                              ...formState,
                              teacherFeedback: event.target.value,
                            },
                          }))}
                          placeholder="Ghi nhận xét cho học viên..."
                          value={formState.teacherFeedback}
                        />
                      </label>

                      <div className="flex items-end">
                        <button
                          className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white disabled:opacity-60"
                          disabled={gradingId === submission.studentId}
                          onClick={() => handleGradeSubmission(submission)}
                          type="button"
                        >
                          <Award className="h-4 w-4" />
                          {gradingId === submission.studentId ? 'Đang lưu...' : isGraded ? 'Cập nhật điểm' : 'Chấm điểm'}
                        </button>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>
      )}

      {!homework.length ? (
        <ClassroomEmptyState
          actionLabel="Giao bài tập đầu tiên"
          description="Chưa có bài tập nào được giao cho lớp học này."
          onAction={openCreateForm}
          title="Chưa có bài tập"
        />
      ) : (
        <div className="grid gap-6 md:grid-cols-2">
          {homework.map((item) => (
            <article
              className="flex flex-col justify-between rounded-xl border border-[#e5e7eb] bg-white p-5 transition hover:border-[#d0c4c3]"
              key={item.id}
            >
              <div>
                <div className="flex items-center justify-between gap-3">
                  <span className={`rounded-full px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider ${statusTone(item.status)}`}>
                    {formatHomeworkStatus(item.status, item.overdue)}
                  </span>
                  <div className="flex flex-wrap items-center gap-2">
                    {isAiGradedHomework(item) ? (
                      <span className="rounded-full bg-purple-50 px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider text-purple-700">
                        AI · {getHomeworkSkillLabel(item.skill)}
                      </span>
                    ) : (
                      <span className="rounded-full bg-gray-100 px-2.5 py-1 text-[10px] font-extrabold uppercase tracking-wider text-gray-600">
                        {getHomeworkGradingModeLabel(item.gradingMode)}
                      </span>
                    )}
                    <span className="text-xs font-bold text-[#8b706e]">/{getHomeworkMaxScore(item)} điểm</span>
                  </div>
                </div>
                <h3 className="mt-4 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{item.title}</h3>
                {item.rubricName ? (
                  <p className="mt-1 text-xs font-semibold text-purple-700">Rubric: {item.rubricName}</p>
                ) : null}
                <div className="mt-2 flex flex-wrap gap-2 text-[11px] font-bold text-[#8b706e]">
                  {item.curriculumUnitTitle ? (
                    <span className="rounded-full bg-[#fff0f1] px-2.5 py-1 text-[#730014]">
                      Unit: {item.curriculumUnitTitle}
                    </span>
                  ) : null}
                  <span className="rounded-full bg-gray-100 px-2.5 py-1 text-gray-700">
                    {getHomeworkActivityTypeLabel(item.activityType)}
                  </span>
                </div>
                <p className="mt-2 line-clamp-3 text-sm text-[#584140]">{item.instruction || 'Không có hướng dẫn chi tiết.'}</p>

                <div className="mt-4 flex flex-wrap gap-3 text-xs text-[#8b706e]">
                  <span className="inline-flex items-center gap-1">
                    <Clock className="h-3.5 w-3.5 text-[#730014]" />
                    {formatClassroomDateTime(item.deadline)}
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <Users className="h-3.5 w-3.5 text-[#730014]" />
                    {item.submissionCount ?? 0} bài nộp
                  </span>
                  {(item.pendingGradingCount ?? 0) > 0 && (
                    <span className="inline-flex items-center gap-1 font-bold text-amber-700">
                      <FileText className="h-3.5 w-3.5" />
                      {item.pendingGradingCount} chờ chấm
                    </span>
                  )}
                  {(item.gradedCount ?? 0) > 0 && (
                    <span className="inline-flex items-center gap-1 font-bold text-emerald-700">
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      {item.gradedCount} đã chấm
                    </span>
                  )}
                </div>
              </div>

              <div className="mt-6 flex flex-wrap gap-2 border-t border-gray-50 pt-4">
                <button
                  className="inline-flex items-center gap-1 rounded-xl bg-[#4b0009] px-4 py-2.5 text-xs font-extrabold text-white"
                  onClick={() => openGradingPanel(item)}
                  type="button"
                >
                  <Award className="h-3.5 w-3.5" />
                  Chấm điểm
                </button>
                <button
                  className="inline-flex items-center gap-1 rounded-xl border border-gray-200 px-4 py-2.5 text-xs font-extrabold text-[#584140]"
                  onClick={() => openEditForm(item)}
                  type="button"
                >
                  <Edit3 className="h-3.5 w-3.5" />
                  Sửa
                </button>
                {item.status === 'DRAFT' && (
                  <button
                    className="inline-flex items-center gap-1 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2.5 text-xs font-extrabold text-emerald-700"
                    onClick={() => handlePublishHomework(item)}
                    type="button"
                  >
                    <Send className="h-3.5 w-3.5" />
                    Mở bài
                  </button>
                )}
                <button
                  className="inline-flex items-center gap-1 rounded-xl border border-red-100 px-4 py-2.5 text-xs font-extrabold text-red-700"
                  onClick={() => handleDeleteHomework(item)}
                  type="button"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  Xóa
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
