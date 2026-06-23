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
  getHomeworkSkillLabel,
  HOMEWORK_GRADING_MODES,
  HOMEWORK_SKILLS,
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
  gradingMode: 'TEACHER',
  skill: 'SPEAKING',
  rubricId: '',
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
}) {
  const [formOpen, setFormOpen] = useState(false);
  const [editingHomework, setEditingHomework] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [attachmentFile, setAttachmentFile] = useState(null);

  const [gradingHomework, setGradingHomework] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [submissionsLoading, setSubmissionsLoading] = useState(false);
  const [gradingForms, setGradingForms] = useState({});
  const [gradingId, setGradingId] = useState(null);
  const [rubrics, setRubrics] = useState([]);
  const [rubricsLoading, setRubricsLoading] = useState(false);

  const gradingModeOptions = useMemo(
    () => HOMEWORK_GRADING_MODES.map((item) => ({ label: item.label, value: item.value })),
    [],
  );

  const skillOptions = useMemo(
    () => HOMEWORK_SKILLS.map((item) => ({ label: item.label, value: item.value })),
    [],
  );

  const rubricOptions = useMemo(
    () => rubrics.map((rubric) => ({
      label: rubric.name,
      value: String(rubric.id),
    })),
    [rubrics],
  );

  const selectedRubric = useMemo(
    () => rubrics.find((rubric) => String(rubric.id) === String(form.rubricId)) || null,
    [rubrics, form.rubricId],
  );

  const selectedGradingMode = HOMEWORK_GRADING_MODES.find((item) => item.value === form.gradingMode);
  const selectedSkillMeta = HOMEWORK_SKILLS.find((item) => item.value === form.skill);

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
    if (!formOpen || form.gradingMode !== 'AI' || !form.skill) {
      setRubrics([]);
      return undefined;
    }

    let active = true;
    setRubricsLoading(true);
    classroomApi.getHomeworkRubrics(form.skill)
      .then((data) => {
        if (!active) return;
        setRubrics(data);
      })
      .catch(() => {
        if (active) setRubrics([]);
      })
      .finally(() => {
        if (active) setRubricsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [formOpen, form.gradingMode, form.skill]);

  useEffect(() => {
    if (form.gradingMode !== 'AI' || !rubrics.length) return;
    const hasCurrent = rubrics.some((rubric) => String(rubric.id) === String(form.rubricId));
    if (!hasCurrent) {
      setForm((current) => ({ ...current, rubricId: String(rubrics[0].id) }));
    }
  }, [form.gradingMode, form.rubricId, rubrics]);

  const resetForm = () => {
    setForm(emptyForm);
    setEditingHomework(null);
    setFormOpen(false);
    setAttachmentFile(null);
  };

  const openCreateForm = () => {
    setEditingHomework(null);
    setForm(emptyForm);
    setAttachmentFile(null);
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
      gradingMode: item.gradingMode || 'TEACHER',
      skill: item.skill || 'SPEAKING',
      rubricId: item.rubricId ? String(item.rubricId) : '',
    });
    setAttachmentFile(null);
    setFormOpen(true);
  };

  const buildPayload = (attachmentUrl) => ({
    title: form.title.trim(),
    instruction: form.instruction.trim(),
    deadline: fromDateTimeLocalValue(form.deadline),
    maxScore: Number(form.maxScore) || 10,
    allowResubmission: Boolean(form.allowResubmission),
    status: form.status,
    sessionId: form.sessionId ? Number(form.sessionId) : null,
    attachmentUrl,
    gradingMode: form.gradingMode,
    skill: form.gradingMode === 'AI' ? form.skill : null,
    rubricId: form.gradingMode === 'AI' && form.rubricId ? Number(form.rubricId) : null,
  });

  const handleSaveHomework = async () => {
    if (!form.title.trim()) {
      onMessage?.('Vui lòng nhập tiêu đề bài tập.');
      return;
    }
    if (form.gradingMode === 'AI' && !form.rubricId) {
      onMessage?.('Vui lòng chọn bộ tiêu chí chấm AI.');
      return;
    }

    setSaving(true);
    onMessage?.('');
    try {
      let attachmentUrl = editingHomework?.attachmentUrl || null;
      if (attachmentFile) {
        const uploaded = await classroomApi.uploadHomeworkAttachment(attachmentFile);
        attachmentUrl = uploaded.url;
      }
      const payload = buildPayload(attachmentUrl);
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

            <label className="block space-y-2 md:col-span-2">
              <span className="text-xs font-bold text-[#8b706e]">Tệp đính kèm (tối đa 20 MB)</span>
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

            <label className="block space-y-2 md:col-span-2">
              <span className="text-xs font-bold text-[#8b706e]">Cách chấm bài</span>
              <BrandedSelect
                onChange={(event) => setForm((current) => ({
                  ...current,
                  gradingMode: event.target.value,
                  rubricId: '',
                }))}
                options={gradingModeOptions}
                value={form.gradingMode}
              />
              {selectedGradingMode?.description ? (
                <p className="text-xs leading-5 text-[#8b706e]">{selectedGradingMode.description}</p>
              ) : null}
            </label>

            {form.gradingMode === 'AI' ? (
              <>
                <label className="block space-y-2">
                  <span className="text-xs font-bold text-[#8b706e]">Kỹ năng bài tập *</span>
                  <BrandedSelect
                    onChange={(event) => setForm((current) => ({
                      ...current,
                      skill: event.target.value,
                      rubricId: '',
                    }))}
                    options={skillOptions}
                    value={form.skill}
                  />
                  {selectedSkillMeta?.description ? (
                    <p className="text-xs leading-5 text-[#8b706e]">{selectedSkillMeta.description}</p>
                  ) : null}
                </label>

                <label className="block space-y-2">
                  <span className="text-xs font-bold text-[#8b706e]">Bộ tiêu chí chấm AI *</span>
                  {rubricsLoading ? (
                    <p className="text-xs text-[#8b706e]">Đang tải rubric...</p>
                  ) : (
                    <BrandedSelect
                      onChange={(event) => setForm((current) => ({ ...current, rubricId: event.target.value }))}
                      options={rubricOptions}
                      placeholder="Chọn rubric"
                      value={form.rubricId}
                    />
                  )}
                </label>

                {selectedRubric ? (
                  <div className="md:col-span-2 rounded-2xl border border-[#dfbfbd]/30 bg-[#fffafb] p-4 space-y-3">
                    <div>
                      <p className="text-xs font-bold uppercase tracking-wider text-[#730014]">Tiêu chí chấm</p>
                      <p className="mt-1 text-sm font-extrabold text-[#2b2828]">{selectedRubric.name}</p>
                      <p className="mt-1 text-xs leading-5 text-[#8b706e]">{selectedRubric.description}</p>
                      <p className="mt-1 text-[11px] font-semibold text-[#8b706e]">Thang điểm: {selectedRubric.scoringScale}</p>
                    </div>
                    <ul className="space-y-2">
                      {(selectedRubric.criteria || []).map((criterion) => (
                        <li className="rounded-xl border border-white bg-white px-3 py-2 text-xs text-[#584140]" key={criterion.id || criterion.name}>
                          <span className="font-extrabold text-[#2b2828]">{criterion.name}</span>
                          {criterion.weight != null ? ` · ${criterion.weight}%` : ''}
                          {criterion.description ? ` — ${criterion.description}` : ''}
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : null}
              </>
            ) : null}

            <label className="flex items-center gap-3 md:col-span-2 rounded-xl border border-[#e5e7eb] px-4 py-3">
              <input
                checked={form.allowResubmission}
                className="h-4 w-4 accent-[#730014]"
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
