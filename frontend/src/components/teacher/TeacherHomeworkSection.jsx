import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import {
  Award,
  CheckCircle2,
  Clock,
  Edit3,
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
import Pagination, { usePagination } from '../../components/ui/Pagination';
import VietnameseDateTimeInput from '../../components/ui/VietnameseDateTimeInput';
import AssessmentExamBuilder from '../content-manager/AssessmentExamBuilder';
import { useAppDialog } from '../ui/AppDialog';
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
  HOMEWORK_SKILLS,
  isAiGradedHomework,
  isAutoGradedHomework,
} from '../../utils/homeworkGradingConfig';
import TeacherHomeworkGradingWorkspace from './TeacherHomeworkGradingWorkspace';
import TeacherHomeworkContentBuilder, {
  buildHomeworkActivityConfig,
  createEmptyFlashcard,
  createEmptyQuestion,
  createEmptySpeakingPart,
  createEmptyWritingTask,
  parseHomeworkBuilderDrafts,
} from './TeacherHomeworkContentBuilder';

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
  skill: 'READING',
  rubricId: '',
  assessmentBankItemId: '',
};

const SKILLS_BY_ACTIVITY_TYPE = {
  TEXT_RESPONSE: ['SPEAKING', 'WRITING', 'LISTENING', 'READING', 'VOCABULARY'],
  FILE_RESPONSE: ['SPEAKING', 'WRITING', 'LISTENING', 'READING', 'VOCABULARY'],
  SKILL_PRACTICE: ['LISTENING', 'READING', 'VOCABULARY'],
  FLASHCARD_REVIEW: ['VOCABULARY'],
  MIXED: ['SPEAKING', 'WRITING', 'LISTENING', 'READING', 'VOCABULARY'],
};

const CONTENT_SOURCE_ACTIVITY_TYPES = ['TEXT_RESPONSE', 'SKILL_PRACTICE', 'MIXED'];
const ASSESSMENT_BANK_SKILLS = ['LISTENING', 'READING', 'WRITING', 'SPEAKING'];
const AI_SUPPORTED_SKILLS = ['WRITING', 'SPEAKING'];

const safeParseActivityConfig = (value) => {
  try {
    const parsed = JSON.parse(String(value || '{}'));
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
};

const usesAssessmentBuilder = (activityType, skill) => (
  ['TEXT_RESPONSE', 'SKILL_PRACTICE', 'MIXED'].includes(activityType)
  && ['LISTENING', 'READING', 'WRITING', 'SPEAKING'].includes(skill)
);

const collectAssessmentQuestionNumbers = (config) => (config.parts || []).flatMap((part) => (
  (part.questionGroups || []).flatMap((group) => (
    group.type === 'multi_select_letters'
      ? (group.questionNumbers || []).map(Number)
      : (group.questions || []).map((question) => Number(question.number))
  ))
));

const validateAssessmentBuilderConfig = (skill, rawConfig) => {
  const config = safeParseActivityConfig(rawConfig);
  if (['LISTENING', 'READING'].includes(skill)) {
    const questionNumbers = collectAssessmentQuestionNumbers(config)
      .filter((number) => Number.isInteger(number) && number > 0);
    if (!questionNumbers.length) return 'Vui lòng biên soạn ít nhất một câu hỏi cho bài tập.';
    const answerKey = config.answerKey || {};
    const missingAnswer = questionNumbers.find((number) => {
      const answer = answerKey[String(number)];
      return Array.isArray(answer) ? !answer.length : !String(answer ?? '').trim();
    });
    if (missingAnswer) return `Câu ${missingAnswer} chưa có đáp án đúng.`;
  }
  if (skill === 'WRITING') {
    const tasks = Array.isArray(config.tasks) ? config.tasks : [];
    const invalidTask = !tasks.length || tasks.some((task) => (
      !String(task.question || task.prompt || '').trim()
      && !(task.promptParagraphs || []).some((paragraph) => String(paragraph || '').trim())
    ));
    if (invalidTask) return 'Vui lòng biên soạn đầy đủ nội dung đề Writing.';
  }
  if (skill === 'SPEAKING') {
    const variants = Array.isArray(config.variants) && config.variants.length
      ? config.variants
      : [{ parts: config.parts || [] }];
    const invalidPart = !variants.length || variants.some((variant) => (
      !(variant.parts || []).length || (variant.parts || []).some((part) => (
        !(part.prompts || []).some((prompt) => String(prompt?.text || prompt || '').trim())
        && !String(part.cueCardTitle || '').trim()
      ))
    ));
    if (invalidPart) return 'Vui lòng biên soạn đầy đủ câu hỏi hoặc thẻ gợi ý cho từng phần Speaking.';
  }
  return '';
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
  selectedHomeworkId = null,
  selectedStudentId = null,
  onSelectedHomeworkChange,
}) {
  const { confirm: confirmDialog } = useAppDialog();
  const [formOpen, setFormOpen] = useState(false);
  const [editingHomework, setEditingHomework] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [attachmentFile, setAttachmentFile] = useState(null);
  const [questionDrafts, setQuestionDrafts] = useState([createEmptyQuestion()]);
  const [writingTaskDrafts, setWritingTaskDrafts] = useState([createEmptyWritingTask()]);
  const [speakingPartDrafts, setSpeakingPartDrafts] = useState([createEmptySpeakingPart()]);
  const [flashcardDrafts, setFlashcardDrafts] = useState([createEmptyFlashcard()]);
  const [aiAssessmentOptions, setAiAssessmentOptions] = useState([]);
  const [aiAssessmentOptionsLoading, setAiAssessmentOptionsLoading] = useState(false);
  const [rubrics, setRubrics] = useState([]);
  const [rubricsLoading, setRubricsLoading] = useState(false);

  const [gradingHomework, setGradingHomework] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [submissionsLoading, setSubmissionsLoading] = useState(false);
  const [gradingForms, setGradingForms] = useState({});
  const [gradingId, setGradingId] = useState(null);
  const [gradingNotice, setGradingNotice] = useState(null);
  const closingHomeworkIdRef = useRef(null);
  const {
    page: homeworkPage,
    setPage: setHomeworkPage,
    totalPages: homeworkTotalPages,
    pageItems: paginatedHomework,
    totalItems: homeworkTotalItems,
  } = usePagination(homework, 6, `homework-${classroomId}`);

  const activityTypeOptions = useMemo(
    () => HOMEWORK_ACTIVITY_TYPES.map((item) => ({ label: item.label, value: item.value })),
    [],
  );

  const skillOptions = useMemo(
    () => HOMEWORK_SKILLS
      .filter((item) => (SKILLS_BY_ACTIVITY_TYPE[form.activityType] || []).includes(item.value))
      .map((item) => ({ label: item.label, value: item.value })),
    [form.activityType],
  );

  const compatibleAssessmentOptions = useMemo(
    () => aiAssessmentOptions.filter((item) => item.skill === form.skill),
    [aiAssessmentOptions, form.skill],
  );

  const aiAssessmentSelectOptions = useMemo(
    () => [
      { label: 'Tự soạn nội dung bài tập', value: '' },
      ...compatibleAssessmentOptions.map((item) => ({
        label: `${item.title} · ${getHomeworkSkillLabel(item.skill)}`,
        value: String(item.id),
      })),
    ],
    [compatibleAssessmentOptions],
  );

  const selectedAiAssessment = useMemo(
    () => aiAssessmentOptions.find((item) => String(item.id) === String(form.assessmentBankItemId)) || null,
    [aiAssessmentOptions, form.assessmentBankItemId],
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

  const contentSourceVisible = CONTENT_SOURCE_ACTIVITY_TYPES.includes(form.activityType)
    && ASSESSMENT_BANK_SKILLS.includes(form.skill);
  const selectedAssessmentSupportsAi = AI_SUPPORTED_SKILLS.includes(selectedAiAssessment?.skill);

  const richBuilderEnabled = !selectedAiAssessment
    && usesAssessmentBuilder(form.activityType, form.skill);

  const richBuilderAssessment = useMemo(() => {
    const config = safeParseActivityConfig(form.activityConfigJson);
    return {
      title: form.title || 'Bài tập mới',
      skill: form.skill,
      uiConfigJson: form.activityConfigJson,
      objectiveAnswerKey: config.answerKey ? JSON.stringify(config.answerKey) : '',
      maxScore: Number(form.maxScore) || 10,
      timeLimitMinutes: Number(config.durationMinutes || config.timeLimitMinutes || 0),
    };
  }, [form.activityConfigJson, form.maxScore, form.skill, form.title]);

  const canEnableAi = Boolean(
    selectedAiAssessment
    && selectedAssessmentSupportsAi
    && selectedRubric
    && selectedRubric.skill === selectedAiAssessment.skill,
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

  useEffect(() => {
    if (!formOpen || !selectedAssessmentSupportsAi) {
      setRubrics([]);
      setRubricsLoading(false);
      return undefined;
    }

    let active = true;
    setRubricsLoading(true);
    const loadRubrics = async () => {
      try {
        const data = await classroomApi.getHomeworkRubrics(selectedAiAssessment.skill);
        if (!active) return;
        setRubrics(data);
      } catch {
        if (active) setRubrics([]);
      } finally {
        if (active) setRubricsLoading(false);
      }
    };

    loadRubrics();

    return () => {
      active = false;
    };
  }, [formOpen, selectedAiAssessment?.skill, selectedAssessmentSupportsAi]);

  useEffect(() => {
    if (!selectedAiAssessment || !rubrics.length) return;
    const hasCurrentRubric = rubrics.some((rubric) => String(rubric.id) === String(form.rubricId));
    if (hasCurrentRubric) return;

    const assessmentRubric = rubrics.find(
      (rubric) => String(rubric.id) === String(selectedAiAssessment.rubricId),
    );
    setForm((current) => ({
      ...current,
      rubricId: String((assessmentRubric || rubrics[0]).id),
    }));
  }, [form.rubricId, rubrics, selectedAiAssessment]);

  const resetForm = useCallback(() => {
    setForm(emptyForm);
    setFormError('');
    setEditingHomework(null);
    setFormOpen(false);
    setAttachmentFile(null);
    setQuestionDrafts([createEmptyQuestion()]);
    setWritingTaskDrafts([createEmptyWritingTask()]);
    setSpeakingPartDrafts([createEmptySpeakingPart()]);
    setFlashcardDrafts([createEmptyFlashcard()]);
  }, []);

  useEffect(() => {
    if (!formOpen) return undefined;
    const previousOverflow = document.body.style.overflow;
    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !saving) resetForm();
    };
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [formOpen, resetForm, saving]);

  const openCreateForm = () => {
    setEditingHomework(null);
    setForm(emptyForm);
    setFormError('');
    setAttachmentFile(null);
    setQuestionDrafts([createEmptyQuestion()]);
    setWritingTaskDrafts([createEmptyWritingTask()]);
    setSpeakingPartDrafts([createEmptySpeakingPart()]);
    setFlashcardDrafts([createEmptyFlashcard()]);
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
    const activityType = item.activityType || 'TEXT_RESPONSE';
    const allowedSkills = SKILLS_BY_ACTIVITY_TYPE[activityType] || [];
    const skill = allowedSkills.includes(item.skill) ? item.skill : allowedSkills[0] || 'READING';
    setEditingHomework(item);
    setFormError('');
    setForm({
      title: item.title || '',
      instruction: item.instruction || '',
      deadline: toDateTimeLocalValue(item.deadline),
      maxScore: String(getHomeworkMaxScore(item)),
      allowResubmission: Boolean(item.allowResubmission),
      status: item.status || 'OPEN',
      sessionId: item.sessionId ? String(item.sessionId) : '',
      curriculumUnitId: item.curriculumUnitId ? String(item.curriculumUnitId) : '',
      activityType,
      activityConfigJson: item.activityConfigJson || '',
      aiReviewEnabled: Boolean(item.aiReviewEnabled || item.gradingMode === 'AI'),
      gradingMode: item.gradingMode || 'TEACHER',
      skill,
      rubricId: item.rubricId ? String(item.rubricId) : '',
      assessmentBankItemId: item.assessmentBankItemId ? String(item.assessmentBankItemId) : '',
    });
    setAttachmentFile(null);
    const drafts = parseHomeworkBuilderDrafts(item.activityConfigJson);
    setQuestionDrafts(drafts.questions);
    setWritingTaskDrafts(drafts.writingTasks);
    setSpeakingPartDrafts(drafts.speakingParts);
    setFlashcardDrafts(drafts.flashcards);
    setFormOpen(true);
  };

  const handleAssessmentBuilderChange = (field, value) => {
    setForm((current) => {
      if (field === 'maxScore') {
        return { ...current, maxScore: String(value || current.maxScore) };
      }
      if (field === 'timeLimitMinutes') {
        return current;
      }

      const currentConfig = safeParseActivityConfig(current.activityConfigJson);
      if (field === 'uiConfigJson') {
        const nextConfig = safeParseActivityConfig(value);
        if (currentConfig.answerKey && !nextConfig.answerKey) {
          nextConfig.answerKey = currentConfig.answerKey;
        }
        return { ...current, activityConfigJson: JSON.stringify(nextConfig, null, 2) };
      }
      if (field === 'objectiveAnswerKey') {
        return {
          ...current,
          activityConfigJson: JSON.stringify({
            ...currentConfig,
            answerKey: safeParseActivityConfig(value),
          }, null, 2),
        };
      }
      return current;
    });
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
    skill: selectedAiAssessment?.skill || form.skill || null,
    rubricId: selectedRubric?.id || null,
    assessmentBankItemId: form.assessmentBankItemId ? Number(form.assessmentBankItemId) : null,
  });

  const handleSaveHomework = async () => {
    if (!form.title.trim()) {
      setFormError('Vui lòng nhập tiêu đề bài tập.');
      return;
    }
    if (form.aiReviewEnabled && !canEnableAi) {
      setFormError('Muốn dùng AI, vui lòng chọn một MODULE_TEST Writing hoặc Speaking của hệ thống.');
      return;
    }
    if (!selectedAiAssessment && form.activityType === 'FILE_RESPONSE' && !attachmentFile && !editingHomework?.attachmentUrl) {
      setFormError('Vui lòng tải tệp đề bài khi chọn hình thức giao bài bằng file.');
      return;
    }
    if (richBuilderEnabled) {
      const builderError = validateAssessmentBuilderConfig(
        form.skill,
        form.activityConfigJson,
      );
      if (builderError) {
        setFormError(builderError);
        return;
      }
    }
    if (!selectedAiAssessment && !richBuilderEnabled && form.activityType === 'SKILL_PRACTICE') {
      const invalidQuestion = questionDrafts.find((question) => (
        !question.prompt.trim()
        || question.options.some((option) => !option.trim())
        || !question.correctAnswer
      ));
      if (invalidQuestion) {
        setFormError('Vui lòng nhập đủ câu hỏi, 4 lựa chọn và đáp án đúng cho bài soạn trên hệ thống.');
        return;
      }
    }
    if (!selectedAiAssessment && !richBuilderEnabled && (form.activityType === 'TEXT_RESPONSE' || form.activityType === 'MIXED') && form.skill === 'SPEAKING') {
      if (speakingPartDrafts.some((part) => !part.prompts.length || part.prompts.some((prompt) => !prompt.trim()))) {
        setFormError('Vui lòng nhập đầy đủ câu hỏi cho từng phần Speaking.');
        return;
      }
    }
    if (!selectedAiAssessment && !richBuilderEnabled && (form.activityType === 'TEXT_RESPONSE' || form.activityType === 'MIXED') && form.skill !== 'SPEAKING') {
      if (writingTaskDrafts.some((task) => !task.question.trim())) {
        setFormError('Vui lòng nhập đầy đủ nội dung đề bài trực tiếp.');
        return;
      }
    }
    if (!selectedAiAssessment && form.activityType === 'FLASHCARD_REVIEW' && flashcardDrafts.some((card) => !card.term.trim() || !card.meaning.trim())) {
      setFormError('Mỗi flashcard cần có thuật ngữ và định nghĩa.');
      return;
    }

    setSaving(true);
    setFormError('');
    onMessage?.('');
    try {
      const supportsTeacherAttachment = ['FILE_RESPONSE', 'MIXED'].includes(form.activityType);
      let attachmentUrl = supportsTeacherAttachment
        ? editingHomework?.attachmentUrl || null
        : null;
      if (supportsTeacherAttachment && attachmentFile) {
        const uploaded = await classroomApi.uploadHomeworkAttachment(classroomId, attachmentFile);
        attachmentUrl = uploaded.url;
      }
      const activityConfigJson = selectedAiAssessment
        ? selectedAiAssessment.uiConfigJson || form.activityConfigJson || ''
        : richBuilderEnabled ? form.activityConfigJson : buildHomeworkActivityConfig({
          activityType: form.activityType,
          skill: form.skill,
          questions: questionDrafts,
          writingTasks: writingTaskDrafts,
          speakingParts: speakingPartDrafts,
          flashcards: flashcardDrafts,
        });
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
      setFormError(getClassroomErrorMessage(err, 'Không thể lưu bài tập.'));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteHomework = async (item) => {
    if (!await confirmDialog(`Xóa bài tập “${item.title}”? Hành động này không thể hoàn tác.`, {
      title: 'Xóa bài tập',
      confirmLabel: 'Xóa bài tập',
      tone: 'danger',
    })) {
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
          annotations: Array.isArray(submission.annotations) ? submission.annotations : [],
        };
      });
      setGradingForms(initialForms);
    } catch (err) {
      onMessage?.(getClassroomErrorMessage(err, 'Không thể tải bài nộp.'));
    } finally {
      setSubmissionsLoading(false);
    }
  };

  useEffect(() => {
    if (!selectedHomeworkId) {
      closingHomeworkIdRef.current = null;
      if (gradingHomework) {
        setGradingHomework(null);
        setSubmissions([]);
        setGradingForms({});
      }
      return;
    }
    if (String(closingHomeworkIdRef.current) === String(selectedHomeworkId)) return;
    if (submissionsLoading || gradingHomework?.id === Number(selectedHomeworkId)) return;
    const selectedHomework = homework.find((item) => item.id === Number(selectedHomeworkId));
    if (selectedHomework) openGradingPanel(selectedHomework);
  }, [gradingHomework?.id, homework, selectedHomeworkId, submissionsLoading]);

  const handleOpenGrading = (item) => {
    closingHomeworkIdRef.current = null;
    onSelectedHomeworkChange?.(item.id);
    openGradingPanel(item);
  };

  const handleCloseGrading = () => {
    closingHomeworkIdRef.current = gradingHomework?.id || selectedHomeworkId;
    setGradingHomework(null);
    setSubmissions([]);
    setGradingForms({});
    setGradingNotice(null);
    onSelectedHomeworkChange?.(null);
  };

  const handleGradeSubmission = async (submission) => {
    if (!gradingHomework) return;
    const formState = gradingForms[submission.studentId] || {};
    const score = Number(formState.score);
    if (!Number.isFinite(score)) {
      const message = 'Vui lòng nhập điểm hợp lệ.';
      setGradingNotice({ studentId: submission.studentId, type: 'error', message });
      onMessage?.(message);
      return;
    }

    setGradingId(submission.studentId);
    setGradingNotice(null);
    onMessage?.('');
    try {
      await classroomApi.gradeHomework(gradingHomework.id, submission.studentId, {
        score,
        teacherFeedback: formState.teacherFeedback?.trim() || '',
        annotations: formState.annotations || [],
      });
      const [refreshedHomework, refreshedSubmissions, refreshedGradebook] = await Promise.all([
        classroomApi.getTeacherHomework(classroomId),
        classroomApi.getHomeworkSubmissions(gradingHomework.id),
        classroomApi.getTeacherGradebook(classroomId),
      ]);
      onHomeworkChange?.(refreshedHomework);
      onGradebookChange?.(refreshedGradebook);
      setSubmissions(refreshedSubmissions);
      const message = `Đã cập nhật kết quả chấm cho ${submission.studentName || 'học viên'}.`;
      setGradingNotice({ studentId: submission.studentId, type: 'success', message });
      onMessage?.(message);
    } catch (err) {
      const message = getClassroomErrorMessage(err, 'Không thể cập nhật kết quả chấm.');
      setGradingNotice({ studentId: submission.studentId, type: 'error', message });
      onMessage?.(message);
    } finally {
      setGradingId(null);
    }
  };

  const handleSaveAnnotations = async (submission, annotations) => {
    if (!gradingHomework || !submission?.studentId) {
      throw new Error('Không xác định được bài nộp cần lưu nhận xét.');
    }
    try {
      const saved = await classroomApi.saveHomeworkAnnotations(
        gradingHomework.id,
        submission.studentId,
        annotations,
      );
      const savedAnnotations = Array.isArray(saved?.annotations) ? saved.annotations : [];
      setSubmissions((current) => current.map((item) => (
        item.studentId === submission.studentId ? { ...item, ...saved } : item
      )));
      setGradingForms((current) => ({
        ...current,
        [submission.studentId]: {
          ...(current[submission.studentId] || {}),
          annotations: savedAnnotations,
        },
      }));
      return savedAnnotations;
    } catch (err) {
      throw new Error(getClassroomErrorMessage(err, 'Không thể lưu nhận xét. Vui lòng thử lại.'));
    }
  };

  if (gradingHomework) {
    return (
      <TeacherHomeworkGradingWorkspace
        gradingForms={gradingForms}
        gradingId={gradingId}
        gradingNotice={gradingNotice}
        homework={gradingHomework}
        loading={submissionsLoading}
        onBack={handleCloseGrading}
        onGrade={handleGradeSubmission}
        onGradingFormsChange={setGradingForms}
        onSaveAnnotations={handleSaveAnnotations}
        submissions={submissions}
        initialStudentId={selectedStudentId}
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-[#dfbfbd]/20 bg-[#fffafb] p-5">
        <div>
          <h4 className="font-['Manrope'] text-lg font-extrabold text-[#2b2828]">Bài tập của lớp</h4>
          <p className="mt-1 text-xs leading-5 text-[#584140]">
            Trắc nghiệm được hệ thống tự chấm theo đáp án; bài viết và bài nói được giáo viên xem, nhận xét và chấm trực tiếp.
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

      {formOpen && createPortal(
        <div
          aria-labelledby="homework-form-modal-title"
          aria-modal="true"
          className="fixed inset-0 z-[80] flex items-center justify-center p-3 sm:p-6"
          role="dialog"
        >
          <button
            aria-label="Đóng biểu mẫu bài tập"
            className="absolute inset-0 bg-black/45 backdrop-blur-sm"
            disabled={saving}
            onClick={resetForm}
            type="button"
          />
          <section className="relative z-10 flex max-h-[94vh] w-full max-w-5xl flex-col overflow-hidden rounded-3xl border border-[#dfbfbd]/40 bg-white shadow-2xl">
          <header className="flex items-center justify-between gap-3 border-b border-gray-100 bg-[#fffafb] px-5 py-4 sm:px-7 sm:py-5">
            <h5 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]" id="homework-form-modal-title">
              {editingHomework ? 'Chỉnh sửa bài tập' : 'Tạo bài tập mới'}
            </h5>
            <button
              aria-label="Đóng"
              className="rounded-xl border border-gray-200 bg-white p-2 text-[#8b706e] transition hover:bg-gray-50"
              disabled={saving}
              onClick={resetForm}
              type="button"
            >
              <X className="h-4 w-4" />
            </button>
          </header>

          {formError ? (
            <p className="mx-5 mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700 sm:mx-7">
              {formError}
            </p>
          ) : null}

          <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5 sm:px-7">
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
              <span className="text-xs font-bold text-[#8b706e]">Hình thức bài tập</span>
              <BrandedSelect
                onChange={(event) => {
                  const activityType = event.target.value;
                  if (!['FILE_RESPONSE', 'MIXED'].includes(activityType)) setAttachmentFile(null);
                  setForm((current) => ({
                    ...current,
                    activityType,
                    skill: (SKILLS_BY_ACTIVITY_TYPE[activityType] || []).includes(current.skill)
                      ? current.skill
                      : SKILLS_BY_ACTIVITY_TYPE[activityType]?.[0] || current.skill,
                    assessmentBankItemId: '',
                    activityConfigJson: '',
                    aiReviewEnabled: false,
                    rubricId: '',
                    title: current.title === selectedAiAssessment?.title ? '' : current.title,
                    instruction: current.instruction === selectedAiAssessment?.instructions ? '' : current.instruction,
                    maxScore: String(current.maxScore) === String(selectedAiAssessment?.maxScore) ? '10' : current.maxScore,
                  }));
                }}
                options={activityTypeOptions}
                value={form.activityType}
              />
              <p className="text-xs leading-5 text-[#8b706e]">
                {HOMEWORK_ACTIVITY_TYPES.find((item) => item.value === form.activityType)?.description}
              </p>
            </label>

            <label className="block space-y-2 md:col-span-2">
              <span className="text-xs font-bold text-[#8b706e]">Kỹ năng</span>
              <BrandedSelect
                onChange={(event) => setForm((current) => ({
                  ...current,
                  skill: event.target.value,
                  assessmentBankItemId: '',
                  activityConfigJson: '',
                  aiReviewEnabled: false,
                  rubricId: '',
                  title: current.title === selectedAiAssessment?.title ? '' : current.title,
                  instruction: current.instruction === selectedAiAssessment?.instructions ? '' : current.instruction,
                  maxScore: String(current.maxScore) === String(selectedAiAssessment?.maxScore) ? '10' : current.maxScore,
                }))}
                options={skillOptions}
                value={form.skill}
              />
            </label>

            {contentSourceVisible ? (
              <div className="space-y-3 rounded-2xl border border-[#dfbfbd]/30 bg-[#fffafb] p-4 md:col-span-2">
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Nguồn nội dung</p>
                {aiAssessmentOptionsLoading ? (
                  <p className="text-xs text-[#8b706e]">Đang tải ngân hàng đề...</p>
                ) : (
                  <BrandedSelect
                    onChange={(event) => {
                      const assessment = compatibleAssessmentOptions.find(
                        (item) => String(item.id) === event.target.value,
                      );
                      setForm((current) => ({
                        ...current,
                        assessmentBankItemId: event.target.value,
                        activityConfigJson: assessment ? current.activityConfigJson : '',
                        aiReviewEnabled: assessment && AI_SUPPORTED_SKILLS.includes(assessment.skill)
                          ? current.aiReviewEnabled
                          : false,
                        title: assessment
                          ? current.title || assessment.title || ''
                          : current.title === selectedAiAssessment?.title ? '' : current.title,
                        rubricId: assessment?.rubricId ? String(assessment.rubricId) : '',
                        instruction: assessment?.instructions
                          || (current.instruction === selectedAiAssessment?.instructions ? '' : current.instruction),
                        maxScore: assessment?.maxScore
                          ? String(assessment.maxScore)
                          : String(current.maxScore) === String(selectedAiAssessment?.maxScore) ? '10' : current.maxScore,
                      }));
                    }}
                    options={aiAssessmentSelectOptions}
                    placeholder="Tự soạn nội dung bài tập"
                    searchable
                    value={form.assessmentBankItemId}
                  />
                )}
                {selectedAiAssessment ? (
                  <div className="rounded-xl border border-[#dfbfbd]/30 bg-white px-4 py-3">
                    <p className="text-xs font-extrabold text-[#730014]">{selectedAiAssessment.title}</p>
                    <p className="mt-1 text-xs text-[#8b706e]">
                      {getHomeworkSkillLabel(selectedAiAssessment.skill)} · {form.maxScore} điểm
                    </p>
                  </div>
                ) : null}
              </div>
            ) : null}

            {!selectedAiAssessment && ['FILE_RESPONSE', 'MIXED'].includes(form.activityType) ? (
              <label className="block space-y-2 md:col-span-2">
                <span className="text-xs font-bold text-[#8b706e]">
                  {form.activityType === 'FILE_RESPONSE' ? 'Tệp giao bài *' : 'Tệp đề / tài liệu kèm (không bắt buộc)'} · tối đa 20 MB
                </span>
                <input
                  accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png"
                  className="block w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm text-[#584140] file:mr-4 file:rounded-lg file:border-0 file:bg-[#fff0f1] file:px-3 file:py-2 file:text-xs file:font-bold file:text-[#730014] hover:file:bg-[#ffe2e6]"
                  onChange={(event) => setAttachmentFile(event.target.files?.[0] || null)}
                  type="file"
                />
                <p className="text-xs text-[#8b706e]">
                  {attachmentFile ? `Sẽ tải lên: ${attachmentFile.name}` : editingHomework?.attachmentUrl ? 'Đang giữ tệp hiện tại.' : 'Hỗ trợ PDF, Office, TXT, ZIP/RAR và ảnh JPG/PNG.'}
                </p>
              </label>
            ) : null}

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
              <VietnameseDateTimeInput
                className="w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                onChange={(value) => setForm((current) => ({ ...current, deadline: value }))}
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
              <span className="text-xs font-bold text-[#8b706e]">Unit trong chương trình học</span>
              <BrandedSelect
                onChange={(event) => setForm((current) => ({ ...current, curriculumUnitId: event.target.value }))}
                options={curriculumUnitOptions}
                value={form.curriculumUnitId}
                searchable={true}
              />
            </label>

            {richBuilderEnabled ? (
              <div className="md:col-span-2 rounded-2xl border border-[#ead9db] bg-[#fffdfd] p-5">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Nội dung làm bài</p>
                  <h6 className="mt-1 font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">
                    Biên soạn bài {form.skill === 'SPEAKING' ? 'Speaking' : form.skill === 'WRITING' ? 'Writing' : form.skill === 'LISTENING' ? 'Listening' : 'Reading'}
                  </h6>
                </div>
                <AssessmentExamBuilder
                  assessment={richBuilderAssessment}
                  onChange={handleAssessmentBuilderChange}
                />
              </div>
            ) : !selectedAiAssessment ? (
              <TeacherHomeworkContentBuilder
                activityType={form.activityType}
                flashcards={flashcardDrafts}
                questions={questionDrafts}
                setFlashcards={setFlashcardDrafts}
                setQuestions={setQuestionDrafts}
                setSpeakingParts={setSpeakingPartDrafts}
                setWritingTasks={setWritingTaskDrafts}
                skill={form.skill}
                speakingParts={speakingPartDrafts}
                writingTasks={writingTaskDrafts}
              />
            ) : null}

            {selectedAssessmentSupportsAi ? (
              <>
                <label className="block space-y-2 md:col-span-2">
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

            {selectedAssessmentSupportsAi ? <div className={`md:col-span-2 rounded-2xl border p-4 ${canEnableAi ? 'border-[#dfbfbd] bg-[#fffafb]' : 'border-gray-200 bg-gray-50'}`}>
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-extrabold text-[#2b2828]">Sử dụng AI hỗ trợ chấm điểm</p>
                  <p className="mt-1 text-xs leading-5 text-[#8b706e]">
                    Chỉ bật được với Writing/Speaking của hệ thống. Giáo viên có thể xem lại và sửa điểm AI.
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
              <div className="mt-3 rounded-xl border border-white bg-white px-3 py-2 text-xs text-[#584140]">
                <span className="font-extrabold text-[#730014]">{getHomeworkSkillLabel(selectedAiAssessment.skill)}</span>
                {' · '}{selectedAiAssessment.title}
                {selectedRubric?.name ? ` · Rubric: ${selectedRubric.name}` : ' · Chưa có rubric'}
              </div>
            </div> : null}

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

          </div>

          <footer className="flex flex-wrap justify-end gap-3 border-t border-gray-100 bg-white px-5 py-4 sm:px-7">
            <button
              className="rounded-xl border border-gray-200 px-5 py-3 text-xs font-extrabold text-[#584140] transition hover:bg-gray-50 disabled:opacity-60"
              disabled={saving}
              onClick={resetForm}
              type="button"
            >
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
          </footer>
          </section>
        </div>,
        document.body,
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
          {paginatedHomework.map((item) => (
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
                  onClick={() => handleOpenGrading(item)}
                  type="button"
                >
                  <Award className="h-3.5 w-3.5" />
                  {isAutoGradedHomework(item) ? 'Xem kết quả' : 'Chấm bài'}
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
      {homework.length ? (
        <div className="flex justify-center pt-1">
          <Pagination
            alwaysVisible={true}
            onChange={setHomeworkPage}
            page={homeworkPage}
            pageSize={6}
            totalItems={homeworkTotalItems}
            totalPages={homeworkTotalPages}
          />
        </div>
      ) : null}
    </div>
  );
}
