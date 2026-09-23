import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import {
  Award,
  CheckCircle2,
  Clock,
  Edit3,
  Eye,
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
import { sanitizeLessonHtml } from '../../utils/lessonRichText';
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
import { formatCriteriaSetName } from '../../utils/assessmentRubricLabels';
import TeacherHomeworkGradingWorkspace from './TeacherHomeworkGradingWorkspace';
import TeacherHomeworkContentBuilder, {
  buildHomeworkActivityConfig,
  createEmptyFlashcard,
  createEmptyQuestion,
  createEmptySpeakingPart,
  createEmptyWritingTask,
  parseHomeworkBuilderDrafts,
} from './TeacherHomeworkContentBuilder';

// sessionStorage drafts saved before multi-select answers shipped only have a single
// `correctAnswer` letter — normalize those back into the `correctAnswers` array shape.
const normalizeQuestionDraft = (question) => (
  Array.isArray(question?.correctAnswers)
    ? question
    : { ...question, correctAnswers: question?.correctAnswer ? [question.correctAnswer] : [] }
);

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

// "Bài thực hành" (TEXT_RESPONSE) was merged into "Bài luyện tập" (SKILL_PRACTICE) as one
// visible option covering all 5 skills. TEXT_RESPONSE still exists as a backend value — the
// form keeps activityType='SKILL_PRACTICE' for display, and buildPayload()/openEditForm()
// translate to/from TEXT_RESPONSE for Speaking/Writing, since the backend still rejects
// SKILL_PRACTICE for those two skills (Speaking/Writing need a real submission to grade, not
// an auto-graded quiz).
const SKILL_PRACTICE_TEXT_RESPONSE_SKILLS = ['SPEAKING', 'WRITING'];

const SKILLS_BY_ACTIVITY_TYPE = {
  FILE_RESPONSE: ['SPEAKING', 'WRITING', 'LISTENING', 'READING'],
  // Vocabulary has no dedicated content builder outside SKILL_PRACTICE/FLASHCARD_REVIEW — it
  // falls back to the generic essay-style prompt editor for other activity types, which doesn't
  // fit a vocabulary task.
  SKILL_PRACTICE: ['SPEAKING', 'WRITING', 'LISTENING', 'READING', 'VOCABULARY'],
  FLASHCARD_REVIEW: ['VOCABULARY'],
  MIXED: ['SPEAKING', 'WRITING', 'LISTENING', 'READING'],
};

const CONTENT_SOURCE_ACTIVITY_TYPES = ['TEXT_RESPONSE', 'FILE_RESPONSE', 'SKILL_PRACTICE', 'MIXED'];
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

const normalizePreviewAnswerKey = (objectiveAnswerKey, config) => {
  const explicit = safeParseActivityConfig(objectiveAnswerKey);
  if (explicit && Object.keys(explicit).length) return explicit;
  return config?.answerKey || {};
};

const isLetterCorrect = (answer, letter) => (
  Array.isArray(answer) ? answer.includes(letter) : String(answer ?? '').trim().toUpperCase() === letter
);

const formatAcceptedAnswers = (answer) => (
  Array.isArray(answer) ? answer.join(' hoặc ') : String(answer ?? '').trim()
);

const PreviewAudio = ({ label, src }) => {
  if (!src) return null;
  return (
    <div className="mt-2">
      {label ? <p className="mb-1 text-xs font-bold text-[#8b706e]">{label}</p> : null}
      <audio className="w-full" controls src={src} />
    </div>
  );
};

const PreviewOptionsList = ({ answer, options }) => (
  <ul className="mt-2 grid gap-1.5 sm:grid-cols-2">
    {(options || []).map((option) => {
      const correct = isLetterCorrect(answer, option.value);
      return (
        <li
          className={`flex items-center gap-2 rounded-lg border px-3 py-1.5 text-sm ${
            correct ? 'border-emerald-300 bg-emerald-50 font-bold text-emerald-700' : 'border-[#eadcdc] text-[#584140]'
          }`}
          key={option.value}
        >
          <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full border border-current text-xs">
            {option.value}
          </span>
          <span>{option.label || <span className="italic text-[#8b706e]">(chưa nhập nội dung)</span>}</span>
          {correct ? <CheckCircle2 className="ml-auto h-4 w-4 shrink-0" /> : null}
        </li>
      );
    })}
  </ul>
);

const AssessmentContentPreview = ({ answerKey, config, skill }) => {
  if (skill === 'WRITING') {
    // Some older bank items were authored as a bare { prompt, responseType } payload instead of
    // the builder's { tasks: [...] } shape. Fall back to showing that prompt as a single task so
    // the preview still shows something instead of a blank panel.
    const tasks = Array.isArray(config.tasks) && config.tasks.length
      ? config.tasks
      : (config.prompt || config.question
        ? [{ key: 'legacy_prompt', heading: 'Đề bài', question: config.prompt || config.question }]
        : []);
    return (
      <div className="space-y-4">
        {tasks.map((task, index) => (
          <div className="rounded-2xl border border-[#dfbfbd] bg-white p-4" key={task.key || index}>
            <p className="text-sm font-extrabold text-[#730014]">{task.heading || task.title || `Task ${index + 1}`}</p>
            {task.summary ? <p className="mt-1 text-xs text-[#8b706e]">{task.summary}</p> : null}
            {task.promptHtml ? (
              <div
                className="mt-3 text-sm leading-6 text-[#3a2a29]"
                dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(task.promptHtml) }}
              />
            ) : (
              <div className="mt-3 space-y-2 text-sm leading-6 text-[#3a2a29]">
                {(task.promptParagraphs?.length ? task.promptParagraphs : [task.question]).filter(Boolean).map((paragraph, paragraphIndex) => (
                  <p key={paragraphIndex}>{paragraph}</p>
                ))}
              </div>
            )}
            {task.imageUrl ? <img alt="" className="mt-3 max-h-64 rounded-lg border border-[#eadcdc] object-contain" src={task.imageUrl} /> : null}
            <p className="mt-3 text-xs font-semibold text-[#8b706e]">
              Tối thiểu {task.minimumWords || 150} từ · Gợi ý {task.recommendedMinutes || 20} phút
            </p>
          </div>
        ))}
      </div>
    );
  }

  if (skill === 'SPEAKING') {
    // Same legacy-payload fallback as Writing above: an older bank item with just
    // { prompt, responseType } and no parts/variants structure.
    const legacyParts = !config.variants?.length && !config.parts?.length && (config.prompt || config.question)
      ? [{ key: 'legacy_prompt', label: 'Đề bài', prompts: [{ text: config.prompt || config.question }] }]
      : (config.parts || []);
    const variants = Array.isArray(config.variants) && config.variants.length
      ? config.variants
      : [{ key: 'default', label: 'Đề', parts: legacyParts }];
    return (
      <div className="space-y-5">
        {variants.map((variant, variantIndex) => (
          <div key={variant.key || variantIndex}>
            {variants.length > 1 ? <p className="mb-2 text-sm font-extrabold text-[#730014]">{variant.label || `Đề ${variantIndex + 1}`}</p> : null}
            <div className="space-y-3">
              {(variant.parts || []).map((part, partIndex) => (
                <div className="rounded-2xl border border-[#dfbfbd] bg-white p-4" key={part.key || partIndex}>
                  <p className="text-sm font-extrabold text-[#730014]">
                    {part.label || `Part ${partIndex + 1}`}{part.caption ? ` — ${part.caption}` : ''}
                  </p>
                  {part.prepSeconds || part.answerSeconds ? (
                    <p className="mt-1 text-xs text-[#8b706e]">
                      Chuẩn bị {part.prepSeconds || 0}s · Trả lời {part.answerSeconds || 0}s
                    </p>
                  ) : null}
                  {part.cueCardTitle ? (
                    <div className="mt-3 rounded-xl border border-[#eadcdc] bg-[#fffafb] p-3">
                      <p className="text-sm font-bold text-[#3a2a29]">{part.cueCardTitle}</p>
                      {(part.cueCardBullets || []).length ? (
                        <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-[#584140]">
                          {part.cueCardBullets.map((bullet, bulletIndex) => <li key={bulletIndex}>{bullet}</li>)}
                        </ul>
                      ) : null}
                    </div>
                  ) : null}
                  <div className="mt-3 space-y-2">
                    {(part.prompts || []).map((prompt, promptIndex) => {
                      const text = typeof prompt === 'string' ? prompt : prompt?.text;
                      const audioUrl = typeof prompt === 'object' ? prompt?.audioUrl : '';
                      const videoUrl = typeof prompt === 'object' ? prompt?.videoUrl : '';
                      return (
                        <div className="rounded-lg bg-[#fffafb] px-3 py-2 text-sm text-[#3a2a29]" key={promptIndex}>
                          {text}
                          <PreviewAudio src={audioUrl} />
                          {videoUrl ? <video className="mt-2 w-full max-w-md rounded-lg" controls src={videoUrl} /> : null}
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    );
  }

  // LISTENING / READING: rendered as a flat answer-key style list rather than the student's
  // page-by-page exam flow, so a teacher can scan every question and its correct answer at once.
  const parts = Array.isArray(config.parts) ? config.parts : [];
  return (
    <div className="space-y-5">
      <PreviewAudio label="Audio toàn bài" src={config.audioUrl} />
      {parts.map((part, partIndex) => (
        <div className="rounded-2xl border border-[#dfbfbd] bg-white p-4" key={part.key || partIndex}>
          <p className="text-sm font-extrabold text-[#730014]">{part.title || `Phần ${partIndex + 1}`}</p>
          {part.summary ? <p className="mt-1 text-xs text-[#8b706e]">{part.summary}</p> : null}
          {(part.passage?.paragraphs || []).length ? (
            <div className="mt-3 rounded-xl border border-[#eadcdc] bg-[#fffafb] p-3">
              {part.passage.title ? <p className="mb-2 text-sm font-bold text-[#3a2a29]">{part.passage.title}</p> : null}
              {part.passage.paragraphs.map((paragraph, paragraphIndex) => (
                <div
                  className="text-sm leading-6 text-[#3a2a29]"
                  dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(paragraph.html || paragraph) }}
                  key={paragraphIndex}
                />
              ))}
            </div>
          ) : null}

          <div className="mt-3 space-y-3">
            {(part.questionGroups || []).map((group, groupIndex) => (
              <div className="rounded-xl border border-[#eadcdc] bg-[#fffafb] p-3" key={`${part.key || partIndex}-${groupIndex}`}>
                {group.title ? <p className="text-sm font-bold text-[#3a2a29]">{group.title}</p> : null}
                {group.instructions ? <p className="mt-1 whitespace-pre-wrap text-xs text-[#8b706e]">{group.instructions}</p> : null}
                {group.passageHtml ? (
                  <div
                    className="mt-2 text-sm leading-6 text-[#3a2a29]"
                    dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(group.passageHtml) }}
                  />
                ) : null}
                <PreviewAudio label="Audio nhóm câu hỏi" src={group.audioUrl} />

                {group.type === 'multi_select_letters' ? (
                  (() => {
                    const number = Number(group.questionNumbers?.[0] || 0);
                    const answer = answerKey[String(number)];
                    return (
                      <div className="mt-3">
                        <p className="text-sm font-bold text-[#3a2a29]">
                          Câu {number} · Chọn {group.maxSelections || 2} đáp án đúng
                        </p>
                        <PreviewOptionsList answer={answer} options={group.options} />
                      </div>
                    );
                  })()
                ) : (
                  <div className="mt-3 space-y-3">
                    {(group.questions || []).map((question, questionIndex) => {
                      const answer = answerKey[String(question.number)];
                      return (
                        <div key={question.number ?? questionIndex}>
                          <p className="text-sm font-bold text-[#3a2a29]">
                            Câu {question.number}
                            {group.type === 'single_choice'
                              ? `. ${question.prompt || ''}`
                              : ` . ${question.promptBefore || ''} ___ ${question.promptAfter || ''}`}
                          </p>
                          {question.imageUrl ? <img alt="" className="mt-2 max-h-56 rounded-lg border border-[#eadcdc] object-contain" src={question.imageUrl} /> : null}
                          <PreviewAudio src={question.audioUrl} />
                          {group.type === 'single_choice' ? (
                            <PreviewOptionsList answer={answer} options={question.options} />
                          ) : (
                            <p className="mt-1 text-sm text-emerald-700">
                              Đáp án đúng: <span className="font-bold">{formatAcceptedAnswers(answer) || '(chưa nhập)'}</span>
                            </p>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

const homeworkStatusOptions = [
  { label: 'Mở nộp bài (OPEN)', value: 'OPEN' },
  { label: 'Lưu nháp (DRAFT)', value: 'DRAFT' },
  { label: 'Đóng bài (CLOSED)', value: 'CLOSED' },
];

const readHomeworkDraft = (key) => {
  try {
    const storedDraft = window.sessionStorage.getItem(key);
    return storedDraft ? JSON.parse(storedDraft) : null;
  } catch {
    return null;
  }
};

const writeHomeworkDraft = (key, draft) => {
  try {
    window.sessionStorage.setItem(key, JSON.stringify(draft));
  } catch {
    // Browser storage may be unavailable; the in-memory draft still remains usable.
  }
};

const clearHomeworkDraft = (key) => {
  try {
    window.sessionStorage.removeItem(key);
  } catch {
    // Resetting the in-memory form is still sufficient when storage is unavailable.
  }
};

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
  const homeworkDraftStorageKey = `englishlab.teacher.classroom.${classroomId}.homework-draft`;

  const [gradingHomework, setGradingHomework] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [submissionsLoading, setSubmissionsLoading] = useState(false);
  const [gradingForms, setGradingForms] = useState({});
  const [gradingId, setGradingId] = useState(null);
  const [gradingNotice, setGradingNotice] = useState(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const closingHomeworkIdRef = useRef(null);
  const {
    page: homeworkPage,
    setPage: setHomeworkPage,
    totalPages: homeworkTotalPages,
    pageItems: paginatedHomework,
    totalItems: homeworkTotalItems,
  } = usePagination(homework, 6, `homework-${classroomId}`);

  const activityTypeOptions = useMemo(
    () => HOMEWORK_ACTIVITY_TYPES.filter((item) => !item.hidden).map((item) => ({ label: item.label, value: item.value })),
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
      label: formatCriteriaSetName(rubric.name),
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
  // AI grading only depends on the homework's skill (Writing/Speaking) and having a rubric —
  // the backend never requires the content to come from the assessment bank (ClassroomHomeworkAiGradingServiceImpl
  // only checks gradingMode == AI and rubric != null). So this must be keyed on form.skill, not on
  // selectedAiAssessment, otherwise a teacher who writes their own Writing/Speaking task can never enable AI.
  const skillSupportsAi = AI_SUPPORTED_SKILLS.includes(form.skill);

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

  // The teacher can only see title + skill + max score in the "Nguồn nội dung" dropdown before
  // picking a bank assessment — this renders the actual selected content read-only, reusing the
  // exact same components students see, so there's no risk of the preview drifting from reality.
  const previewConfig = useMemo(() => {
    if (!selectedAiAssessment) return null;
    return safeParseActivityConfig(selectedAiAssessment.uiConfigJson);
  }, [selectedAiAssessment]);

  const previewAnswerKey = useMemo(() => {
    if (!selectedAiAssessment) return {};
    return normalizePreviewAnswerKey(selectedAiAssessment.objectiveAnswerKey, previewConfig);
  }, [previewConfig, selectedAiAssessment]);

  const canEnableAi = Boolean(
    skillSupportsAi
    && selectedRubric
    && selectedRubric.skill === form.skill,
  );

  // The AI toggle is disabled whenever canEnableAi is false, so the only way aiReviewEnabled can
  // still be true here is a restored draft from an earlier, now-invalid selection (e.g. the skill
  // or rubric changed since it was saved). Turn it back off so the form doesn't silently carry a
  // stale AI setting the teacher can no longer see or change through the (disabled) switch.
  useEffect(() => {
    if (form.aiReviewEnabled && !canEnableAi) {
      setForm((current) => ({ ...current, aiReviewEnabled: false }));
    }
  }, [canEnableAi, form.aiReviewEnabled]);

  const linkedSession = useMemo(
    () => (sessions || []).find((session) => String(session.id) === String(form.sessionId)) || null,
    [sessions, form.sessionId],
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
    if (!formOpen || editingHomework) return;
    writeHomeworkDraft(homeworkDraftStorageKey, {
      form,
      questionDrafts,
      writingTaskDrafts,
      speakingPartDrafts,
      flashcardDrafts,
    });
  }, [editingHomework, flashcardDrafts, form, formOpen, homeworkDraftStorageKey, questionDrafts, speakingPartDrafts, writingTaskDrafts]);

  useEffect(() => {
    if (!formOpen || !skillSupportsAi) {
      setRubrics([]);
      setRubricsLoading(false);
      return undefined;
    }

    let active = true;
    setRubricsLoading(true);
    const loadRubrics = async () => {
      try {
        const data = await classroomApi.getHomeworkRubrics(form.skill);
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
  }, [formOpen, form.skill, skillSupportsAi]);

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

  // A session already belongs to a curriculum unit through its course lesson
  // (ClassroomSessionResponse.courseUnitId). When the homework is attached to such a session,
  // keep the unit field in sync with it instead of letting the teacher pick a possibly different one.
  useEffect(() => {
    if (!linkedSession?.courseUnitId) return;
    const inferredUnitId = String(linkedSession.courseUnitId);
    if (form.curriculumUnitId === inferredUnitId) return;
    setForm((current) => ({ ...current, curriculumUnitId: inferredUnitId }));
  }, [form.curriculumUnitId, linkedSession]);

  const resetForm = useCallback(() => {
    if (!editingHomework) {
      clearHomeworkDraft(homeworkDraftStorageKey);
    }
    setForm(emptyForm);
    setFormError('');
    setEditingHomework(null);
    setFormOpen(false);
    setAttachmentFile(null);
    setQuestionDrafts([createEmptyQuestion()]);
    setWritingTaskDrafts([createEmptyWritingTask()]);
    setSpeakingPartDrafts([createEmptySpeakingPart()]);
    setFlashcardDrafts([createEmptyFlashcard()]);
    setPreviewOpen(false);
  }, [editingHomework, homeworkDraftStorageKey]);

  const dismissForm = useCallback(() => {
    if (!editingHomework) {
      writeHomeworkDraft(homeworkDraftStorageKey, {
        form,
        questionDrafts,
        writingTaskDrafts,
        speakingPartDrafts,
        flashcardDrafts,
      });
    } else {
      setAttachmentFile(null);
    }
    setEditingHomework(null);
    setFormError('');
    setFormOpen(false);
    setPreviewOpen(false);
  }, [editingHomework, flashcardDrafts, form, homeworkDraftStorageKey, questionDrafts, speakingPartDrafts, writingTaskDrafts]);

  useEffect(() => {
    if (!formOpen) return undefined;
    const previousOverflow = document.body.style.overflow;
    const handleKeyDown = (event) => {
      // While the preview overlay is on top, Escape should only close that, not cascade into
      // closing the whole "Tạo bài tập mới" form underneath it.
      if (event.key === 'Escape' && previewOpen) {
        setPreviewOpen(false);
        return;
      }
      if (event.key === 'Escape' && !saving) dismissForm();
    };
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [dismissForm, formOpen, previewOpen, saving]);

  const openCreateForm = () => {
    const draft = readHomeworkDraft(homeworkDraftStorageKey);
    setEditingHomework(null);
    setForm(draft?.form && typeof draft.form === 'object' ? { ...emptyForm, ...draft.form } : emptyForm);
    if (!draft) setAttachmentFile(null);
    setQuestionDrafts(Array.isArray(draft?.questionDrafts) && draft.questionDrafts.length
      ? draft.questionDrafts.map(normalizeQuestionDraft)
      : [createEmptyQuestion()]);
    setWritingTaskDrafts(Array.isArray(draft?.writingTaskDrafts) && draft.writingTaskDrafts.length ? draft.writingTaskDrafts : [createEmptyWritingTask()]);
    setSpeakingPartDrafts(Array.isArray(draft?.speakingPartDrafts) && draft.speakingPartDrafts.length ? draft.speakingPartDrafts : [createEmptySpeakingPart()]);
    setFlashcardDrafts(Array.isArray(draft?.flashcardDrafts) && draft.flashcardDrafts.length ? draft.flashcardDrafts : [createEmptyFlashcard()]);
    setFormError('');
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
    // TEXT_RESPONSE is no longer a selectable "Hình thức bài tập" — existing homework saved with
    // it (always Speaking/Writing, see SKILL_PRACTICE_TEXT_RESPONSE_SKILLS) now displays and
    // re-saves as SKILL_PRACTICE, matching the merged "Bài luyện tập" option.
    const activityType = item.activityType === 'TEXT_RESPONSE' ? 'SKILL_PRACTICE' : (item.activityType || 'SKILL_PRACTICE');
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

  const buildPayload = (attachmentUrl, activityConfigJson = form.activityConfigJson, statusOverride) => {
    const effectiveSkill = selectedAiAssessment?.skill || form.skill || null;
    // The merged "Bài luyện tập" option is SKILL_PRACTICE in the UI, but the backend still
    // rejects SKILL_PRACTICE for Speaking/Writing — send TEXT_RESPONSE for those, exactly as
    // "Bài thực hành" used to before the two were merged into one visible option.
    const activityType = form.activityType === 'SKILL_PRACTICE' && SKILL_PRACTICE_TEXT_RESPONSE_SKILLS.includes(effectiveSkill)
      ? 'TEXT_RESPONSE'
      : form.activityType;
    return {
      title: form.title.trim(),
      instruction: form.instruction.trim(),
      deadline: fromDateTimeLocalValue(form.deadline),
      maxScore: Number(form.maxScore) || 10,
      allowResubmission: Boolean(form.allowResubmission),
      status: statusOverride || form.status,
      sessionId: form.sessionId ? Number(form.sessionId) : null,
      curriculumUnitId: form.curriculumUnitId ? Number(form.curriculumUnitId) : null,
      activityType,
      activityConfigJson: activityConfigJson?.trim() || '',
      aiReviewEnabled: Boolean(form.aiReviewEnabled),
      attachmentUrl,
      gradingMode: form.aiReviewEnabled ? 'AI' : 'TEACHER',
      skill: effectiveSkill,
      rubricId: selectedRubric?.id || null,
      assessmentBankItemId: form.assessmentBankItemId ? Number(form.assessmentBankItemId) : null,
    };
  };

  const handleSaveHomework = async (statusOverride) => {
    if (!form.title.trim()) {
      setFormError('Vui lòng nhập tiêu đề bài tập.');
      return;
    }
    if (form.aiReviewEnabled && !canEnableAi) {
      setFormError('Muốn dùng AI, bài tập phải là Writing hoặc Speaking và đã chọn Bộ tiêu chí chấm AI phù hợp.');
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
        || !(question.correctAnswers || []).length
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
      const payload = buildPayload(attachmentUrl, activityConfigJson, statusOverride);
      const savedHomework = editingHomework?.id
        ? await classroomApi.updateHomework(editingHomework.id, payload)
        : await classroomApi.createHomework(classroomId, payload);
      onHomeworkChange?.(editingHomework?.id
        ? homework.map((item) => (item.id === savedHomework.id ? savedHomework : item))
        : [savedHomework, ...homework]);
      resetForm();
      onMessage?.(editingHomework?.id ? 'Đã cập nhật bài tập.' : 'Đã giao bài tập mới.');

      try {
        const refreshed = await classroomApi.getTeacherHomework(classroomId);
        onHomeworkChange?.(refreshed);
      } catch {
        // The save succeeded; keep the server response instead of reporting a false save error.
      }
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

  const renderAssessmentPreview = () => {
    if (!previewOpen || !selectedAiAssessment || !previewConfig) return null;
    const closePreview = () => setPreviewOpen(false);
    return createPortal(
      <div className="fixed inset-0 z-[150] flex items-start justify-center overflow-y-auto bg-black/50 px-4 py-8">
        <div className="w-full max-w-3xl rounded-3xl bg-white shadow-2xl">
          <div className="flex items-start justify-between gap-3 rounded-t-3xl border-b border-[#eadcdc] bg-[#fffafb] px-6 py-4">
            <div>
              <p className="text-xs font-bold uppercase tracking-wide text-[#8b706e]">Xem trước nội dung</p>
              <p className="text-lg font-extrabold text-[#730014]">{selectedAiAssessment.title}</p>
              <p className="mt-1 text-xs text-[#8b706e]">{getHomeworkSkillLabel(selectedAiAssessment.skill)}</p>
            </div>
            <button
              className="rounded-lg border border-[#dfbfbd] p-1.5 text-[#730014] transition hover:bg-[#f7e9e9]"
              onClick={closePreview}
              type="button"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="max-h-[75vh] overflow-y-auto px-6 py-5">
            <AssessmentContentPreview answerKey={previewAnswerKey} config={previewConfig} skill={selectedAiAssessment.skill} />
          </div>
        </div>
      </div>,
      document.body,
    );
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

      {formOpen && typeof document !== 'undefined' ? createPortal(
        <div
          aria-labelledby="homework-form-modal-title"
          aria-modal="true"
          className="fixed inset-0 z-[80] flex items-center justify-center p-3 sm:p-6"
          role="dialog"
        >
          <div aria-hidden="true" className="absolute inset-0 bg-black/45 backdrop-blur-sm" />
          <section className="relative z-10 flex max-h-[94vh] w-full max-w-5xl flex-col overflow-hidden rounded-3xl border border-[#dfbfbd]/40 bg-white shadow-2xl">
          <header className="flex items-center justify-between gap-3 border-b border-gray-100 bg-[#fffafb] px-5 py-4 sm:px-7 sm:py-5">
            <h5 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]" id="homework-form-modal-title">
              {editingHomework ? 'Chỉnh sửa bài tập' : 'Tạo bài tập mới'}
            </h5>
            <button
              aria-label="Đóng và giữ bản nháp"
              className="rounded-xl border border-gray-200 bg-white p-2 text-[#8b706e] transition hover:bg-gray-50"
              disabled={saving}
              onClick={dismissForm}
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
                    // Ôn flashcard has no deadline field in the UI (nothing is graded or submitted),
                    // so clear any date already typed before switching to it instead of silently keeping it.
                    deadline: activityType === 'FLASHCARD_REVIEW' ? '' : current.deadline,
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
                  <div className="flex items-center justify-between gap-3 rounded-xl border border-[#dfbfbd]/30 bg-white px-4 py-3">
                    <div>
                      <p className="text-xs font-extrabold text-[#730014]">{selectedAiAssessment.title}</p>
                      <p className="mt-1 text-xs text-[#8b706e]">
                        {getHomeworkSkillLabel(selectedAiAssessment.skill)} · {form.maxScore} điểm
                      </p>
                    </div>
                    <button
                      className="inline-flex shrink-0 items-center gap-1.5 rounded-lg border border-[#dfbfbd] px-3 py-2 text-xs font-bold text-[#730014] transition hover:bg-[#fff4f5]"
                      onClick={() => setPreviewOpen(true)}
                      type="button"
                    >
                      <Eye className="h-3.5 w-3.5" />
                      Xem trước
                    </button>
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
              <span className="text-xs font-bold text-[#8b706e]">
                {richBuilderEnabled ? 'Mô tả ngắn cho học viên (không bắt buộc)' : 'Hướng dẫn / đề bài'}
              </span>
              <textarea
                className="min-h-[120px] w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                onChange={(event) => setForm((current) => ({ ...current, instruction: event.target.value }))}
                value={form.instruction}
              />
              {richBuilderEnabled ? (
                <p className="text-xs leading-5 text-[#8b706e]">
                  Chỉ hiện ở danh sách bài tập và màn hình xác nhận trước khi làm bài — không phải đề bài học viên sẽ làm.
                  Đề bài thật sự cần biên soạn ở khung &quot;Nội dung làm bài&quot; bên dưới.
                </p>
              ) : null}
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

            {form.activityType !== 'FLASHCARD_REVIEW' ? (
              <label className="block space-y-2">
                <span className="text-xs font-bold text-[#8b706e]">Hạn nộp</span>
                <VietnameseDateTimeInput
                  className="w-full rounded-xl border border-[#e5e7eb] px-4 py-3 text-sm outline-none focus:border-[#730014]"
                  onChange={(value) => setForm((current) => ({ ...current, deadline: value }))}
                  value={form.deadline}
                />
              </label>
            ) : null}

            {form.activityType !== 'FLASHCARD_REVIEW' ? (
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
            ) : null}

            {editingHomework ? (
              <label className="block space-y-2">
                <span className="text-xs font-bold text-[#8b706e]">Trạng thái</span>
                <BrandedSelect
                  onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
                  options={homeworkStatusOptions}
                  value={form.status}
                />
              </label>
            ) : null}

            <label className="block space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Gắn với buổi học</span>
              <BrandedSelect
                onChange={(event) => setForm((current) => ({ ...current, sessionId: event.target.value }))}
                options={sessionOptions}
                value={form.sessionId}
              />
            </label>

            {skillSupportsAi ? (
              <>
                <label className="block space-y-2 md:col-span-2">
                  <span className="text-xs font-bold text-[#8b706e]">Bộ tiêu chí chấm AI *</span>
                  {rubricsLoading ? (
                    <p className="text-xs text-[#8b706e]">Đang tải bộ tiêu chí...</p>
                  ) : (
                    <BrandedSelect
                      onChange={(event) => setForm((current) => ({ ...current, rubricId: event.target.value }))}
                      options={rubricOptions}
                      placeholder="Chọn bộ tiêu chí"
                      value={form.rubricId}
                    />
                  )}
                </label>

                {selectedRubric ? (
                  <div className="md:col-span-2 rounded-2xl border border-[#dfbfbd]/30 bg-[#fffafb] p-4 space-y-3">
                    <div>
                      <p className="text-xs font-bold uppercase tracking-wider text-[#730014]">Tiêu chí chấm</p>
                      <p className="mt-1 text-sm font-extrabold text-[#2b2828]">{formatCriteriaSetName(selectedRubric.name)}</p>
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

            {skillSupportsAi ? <div className={`md:col-span-2 rounded-2xl border p-4 ${canEnableAi ? 'border-[#dfbfbd] bg-[#fffafb]' : 'border-gray-200 bg-gray-50'}`}>
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-extrabold text-[#2b2828]">Sử dụng AI hỗ trợ chấm điểm</p>
                  <p className="mt-1 text-xs leading-5 text-[#8b706e]">
                    Chỉ bật được với bài Writing/Speaking đã chọn bộ tiêu chí chấm ở trên. Giáo viên có thể xem lại và sửa điểm AI.
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
                <span className="font-extrabold text-[#730014]">{getHomeworkSkillLabel(selectedAiAssessment?.skill || form.skill)}</span>
                {' · '}{selectedAiAssessment ? selectedAiAssessment.title : (form.title || 'Đề tự soạn')}
                {selectedRubric?.name ? ` · Bộ tiêu chí: ${formatCriteriaSetName(selectedRubric.name)}` : ' · Chưa có bộ tiêu chí'}
              </div>
            </div> : null}

            {form.activityType !== 'FLASHCARD_REVIEW' ? (
              <label className="flex items-center gap-3 md:col-span-2 rounded-xl border border-[#e5e7eb] px-4 py-3">
                <input
                  checked={form.allowResubmission}
                  className="h-4 w-4 accent-[#4b0009]"
                  onChange={(event) => setForm((current) => ({ ...current, allowResubmission: event.target.checked }))}
                  type="checkbox"
                />
                <span className="text-sm text-[#584140]">Cho phép học viên nộp lại sau khi đã chấm điểm</span>
              </label>
            ) : null}
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
            {editingHomework ? (
              <button
                className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white disabled:opacity-60"
                disabled={saving}
                onClick={() => handleSaveHomework()}
                type="button"
              >
                {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>
            ) : (
              <>
                <button
                  className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd] px-5 py-3 text-xs font-extrabold text-[#730014] transition hover:bg-[#fff4f5] disabled:opacity-60"
                  disabled={saving}
                  onClick={() => handleSaveHomework('DRAFT')}
                  type="button"
                >
                  {saving ? 'Đang lưu...' : 'Lưu nháp'}
                </button>
                <button
                  className="inline-flex items-center gap-1.5 rounded-xl bg-[#4b0009] px-5 py-3 text-xs font-extrabold text-white disabled:opacity-60"
                  disabled={saving}
                  onClick={() => handleSaveHomework('OPEN')}
                  type="button"
                >
                  {saving ? 'Đang lưu...' : 'Giao bài tập ngay'}
                </button>
              </>
            )}
          </footer>
          </section>
        </div>,
        document.body,
      ) : null}

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
                  {item.activityType !== 'FLASHCARD_REVIEW' ? (
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
                  ) : null}
                </div>
                <h3 className="mt-4 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{item.title}</h3>
                {item.rubricName ? (
                  <p className="mt-1 text-xs font-semibold text-purple-700">Bộ tiêu chí: {formatCriteriaSetName(item.rubricName)}</p>
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
      {renderAssessmentPreview()}
    </div>
  );
}
