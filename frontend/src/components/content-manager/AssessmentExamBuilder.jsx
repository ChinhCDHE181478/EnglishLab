import { useEffect, useMemo, useRef, useState } from 'react';
import { ChevronDown, FileJson, Loader2, Plus, Trash2, UploadCloud, X } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { stripRichTextToPlain } from '../../utils/lessonRichText';
import BrandedSelect from '../ui/BrandedSelect';
import RichTextEditor from './RichTextEditor';

const GROUP_TYPES = [
  { label: 'Điền câu trả lời', value: 'text' },
  { label: 'Chọn một đáp án', value: 'single_choice' },
  { label: 'Chọn nhiều đáp án', value: 'multi_select_letters' },
];

const OBJECTIVE_SKILLS = ['LISTENING', 'READING'];
const SUPPORTED_SKILLS = [...OBJECTIVE_SKILLS, 'WRITING', 'SPEAKING'];

const getAssessmentSkill = (assessment) => String(assessment?.skill || '').toUpperCase();
const isQuizAssessment = (assessment) => String(assessment?.type || '').toUpperCase() === 'QUIZ';

const createQuestion = (number = 1) => ({
  number,
  prompt: '',
  promptBefore: '',
  promptAfter: '',
  imageUrl: '',
  audioUrl: '',
  options: [
    { value: 'A', label: '' },
    { value: 'B', label: '' },
    { value: 'C', label: '' },
    { value: 'D', label: '' },
  ],
});

const createGroup = (number = 1) => ({
  // "Nhóm câu N" on purpose, not "Câu N" — a group can (and usually does) hold several questions,
  // so a name that reads like a single question is misleading the moment a teacher adds a second one.
  title: `Nhóm câu ${number}`,
  instructions: '',
  descriptionHtml: '',
  passageHtml: '',
  type: 'text',
  hideOptionText: false,
  perQuestionAudio: false,
  audioUrl: '',
  questions: [createQuestion(number)],
  questionNumbers: [],
  maxSelections: 2,
  options: [
    { value: 'A', label: '' },
    { value: 'B', label: '' },
    { value: 'C', label: '' },
    { value: 'D', label: '' },
  ],
});

const createPart = (index = 0, firstQuestionNumber = 1) => ({
  key: `part_${index + 1}`,
  partNumber: index + 1,
  title: `Phần ${index + 1}`,
  summary: '',
  passage: {
    title: '',
    paragraphs: [],
  },
  questionGroups: [createGroup(firstQuestionNumber)],
});

const createWritingTask = (index = 0) => {
  const taskNumber = index + 1;
  const isTaskTwo = taskNumber === 2;
  return {
    key: `task_${taskNumber}`,
    title: `Task ${taskNumber}`,
    heading: `Writing Task ${taskNumber}`,
    summary: isTaskTwo ? 'Viết một bài luận hoàn chỉnh.' : 'Viết bài mô tả, thư hoặc báo cáo theo đề.',
    question: '',
    promptHtml: '',
    promptParagraphs: [''],
    imageUrl: '',
    minimumWords: isTaskTwo ? 250 : 150,
    recommendedMinutes: isTaskTwo ? 40 : 20,
  };
};

const createWritingConfig = (assessment) => ({
  version: 1,
  type: 'ielts_writing_exam',
  key: `englishlab_${String(assessment.title || 'writing').toLowerCase().replace(/[^a-z0-9]+/g, '_')}`,
  title: assessment.title || 'Bài viết mới',
  durationMinutes: Number(assessment.timeLimitMinutes || 60),
  tasks: [createWritingTask(0), createWritingTask(1)],
});

const createSpeakingPart = (index = 0) => {
  const partNumber = index + 1;
  const partDefaults = [
    {
      caption: 'Làm quen và trả lời ngắn',
      prepSeconds: 0,
      answerSeconds: 300,
      prompts: ['Where do you live?', 'Do you work or study?', 'What do you usually do in your free time?'],
    },
    {
      caption: 'Thẻ gợi ý',
      prepSeconds: 60,
      answerSeconds: 120,
      cueCardTitle: 'Describe a person, place or experience related to this lesson.',
      cueCardBullets: ['What it is', 'When it happened', 'Why it matters to you'],
      prompts: ['You should say as much as you can about this topic.'],
    },
    {
      caption: 'Thảo luận chủ đề',
      prepSeconds: 0,
      answerSeconds: 300,
      prompts: ['Why is this topic important?', 'How has it changed in recent years?', 'What might happen in the future?'],
    },
  ][index] || {
    caption: `Phần ${partNumber}`,
    prepSeconds: 0,
    answerSeconds: 180,
    prompts: [''],
  };

  return {
    key: `part_${partNumber}`,
    label: `Part ${partNumber}`,
    ...partDefaults,
    prompts: (partDefaults.prompts || ['']).map((text) => ({ text, videoUrl: '', audioUrl: '' })),
  };
};

const createSpeakingVariant = (index = 0) => ({
  key: `test_${index + 1}`,
  label: `Đề ${index + 1}`,
  parts: [createSpeakingPart(0), createSpeakingPart(1), createSpeakingPart(2)],
});

const createSpeakingConfig = (assessment) => ({
  version: 1,
  type: 'speaking_mock_test',
  key: `englishlab_${String(assessment.title || 'speaking').toLowerCase().replace(/[^a-z0-9]+/g, '_')}`,
  title: assessment.title || 'Bài nói mới',
  durationMinutes: Number(assessment.timeLimitMinutes || 15),
  briefing: {
    title: 'Hướng dẫn làm bài Speaking',
    summary: 'Kiểm tra micro, đọc đề theo từng phần rồi ghi âm câu trả lời như khi thi thật.',
  },
  flow: ['mic_check', 'briefing', 'mock_test', 'recording', 'submit'],
  variants: [createSpeakingVariant(0)],
});

const createConfig = (assessment) => {
  const skill = getAssessmentSkill(assessment);
  const examType = isQuizAssessment(assessment)
    ? 'GENERAL'
    : resolveObjectiveExamType(assessment, safeParse(assessment?.uiConfigJson, {}));
  return {
    version: 1,
    examType,
    type: examType === 'GENERAL' ? 'lesson_quiz' : resolveObjectiveExamTypeLabel(examType, skill),
    key: `englishlab_${String(assessment.title || 'assessment').toLowerCase().replace(/[^a-z0-9]+/g, '_')}`,
    title: assessment.title || 'Bài thi mới',
    durationMinutes: Number(assessment.timeLimitMinutes || 40),
    audioLabel: 'Bản nghe',
    audioUrl: '',
    rules: [],
    parts: [createPart(0, 1)],
  };
};

const getAssessmentExamType = (assessment) => resolveObjectiveExamType(assessment, safeParse(assessment?.uiConfigJson, {}));

const safeParse = (value, fallback) => {
  try {
    const parsed = JSON.parse(String(value || ''));
    return parsed && typeof parsed === 'object' ? parsed : fallback;
  } catch {
    return fallback;
  }
};

const resolveObjectiveExamType = (assessment, parsed = {}) => {
  const fromProp = String(assessment?.examType || '').toUpperCase();
  if (['TOEIC', 'IELTS', 'GENERAL'].includes(fromProp)) return fromProp;
  const fromConfig = String(parsed?.examType || '').toUpperCase();
  if (['TOEIC', 'IELTS', 'GENERAL'].includes(fromConfig)) return fromConfig;
  const type = String(parsed?.type || '').toLowerCase();
  if (type === 'lesson_quiz') return 'GENERAL';
  if (type.startsWith('toeic_')) return 'TOEIC';
  return 'IELTS';
};

const resolveObjectiveExamTypeLabel = (examType, skill) => {
  if (examType === 'GENERAL') return 'lesson_quiz';
  if (examType === 'TOEIC') {
    return skill === 'READING' ? 'toeic_reading_exam' : 'toeic_listening_exam';
  }
  return skill === 'READING' ? 'ielts_reading_exam' : 'ielts_listening_exam';
};

const nextQuestionNumber = (parts) => {
  const numbers = (parts || []).flatMap((part) =>
    (part.questionGroups || []).flatMap((group) => [
      ...(group.questions || []).map((question) => Number(question.number || 0)),
      ...(group.questionNumbers || []).map(Number),
    ]),
  );
  return Math.max(0, ...numbers) + 1;
};

const escapeHtml = (value = '') => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#039;');

const plainTextToRichHtml = (value = '') => String(value || '')
  .split(/\n+/)
  .map((line) => line.trim())
  .filter(Boolean)
  .map((line) => `<p>${escapeHtml(line)}</p>`)
  .join('');

const getWritingPromptHtml = (task = {}) => {
  if (String(task.promptHtml || '').trim()) return task.promptHtml;
  const legacyPrompt = Array.isArray(task.promptParagraphs) && task.promptParagraphs.length
    ? task.promptParagraphs.join('\n')
    : task.question || task.prompt || '';
  return plainTextToRichHtml(legacyPrompt);
};

const getPassageHtml = (passage = {}) => (passage.paragraphs || [])
  .map((paragraph) => paragraph?.html || plainTextToRichHtml([
    paragraph?.label ? `${paragraph.label}.` : '',
    paragraph?.text || '',
  ].filter(Boolean).join(' ')))
  .filter(Boolean)
  .join('');

const normalizeConfig = (assessment) => {
  const skill = getAssessmentSkill(assessment);
  const fallback = skill === 'WRITING'
    ? createWritingConfig(assessment)
    : skill === 'SPEAKING'
      ? createSpeakingConfig(assessment)
      : createConfig(assessment);
  const parsed = safeParse(assessment.uiConfigJson, fallback);
  const safeConfig = { ...parsed };
  delete safeConfig.answerKey;

  if (skill === 'WRITING') {
    const tasks = Array.isArray(safeConfig.tasks) && safeConfig.tasks.length
      ? safeConfig.tasks
      : fallback.tasks;
    return {
      ...fallback,
      ...safeConfig,
      type: 'ielts_writing_exam',
      tasks: tasks.map((task, index) => ({
        ...createWritingTask(index),
        ...task,
        key: task.key || `task_${index + 1}`,
        promptHtml: getWritingPromptHtml(task),
        promptParagraphs: Array.isArray(task.promptParagraphs)
          ? task.promptParagraphs
          : String(task.question || task.prompt || '').split('\n').filter(Boolean),
      })),
    };
  }

  if (skill === 'SPEAKING') {
    const variants = Array.isArray(safeConfig.variants) && safeConfig.variants.length
      ? safeConfig.variants
      : Array.isArray(safeConfig.parts) && safeConfig.parts.length
        ? [{ key: 'test_1', label: 'Đề 1', parts: safeConfig.parts }]
        : fallback.variants;
    return {
      ...fallback,
      ...safeConfig,
      type: 'speaking_mock_test',
      variants: variants.map((variant, variantIndex) => ({
        ...createSpeakingVariant(variantIndex),
        ...variant,
        key: variant.key || `test_${variantIndex + 1}`,
        label: variant.label || `Đề ${variantIndex + 1}`,
        parts: (Array.isArray(variant.parts) && variant.parts.length ? variant.parts : createSpeakingVariant(variantIndex).parts)
          .map((part, partIndex) => ({
            ...createSpeakingPart(partIndex),
            ...part,
            key: part.key || `part_${partIndex + 1}`,
            label: part.label || `Part ${partIndex + 1}`,
            prompts: (Array.isArray(part.prompts) && part.prompts.length ? part.prompts : ['']).map((prompt) => (
              typeof prompt === 'string'
                ? { text: prompt, videoUrl: '', audioUrl: '' }
                : {
                  text: String(prompt?.text || ''),
                  videoUrl: String(prompt?.videoUrl || ''),
                  audioUrl: String(prompt?.audioUrl || ''),
                }
            )),
          })),
      })),
    };
  }

  const legacyQuestions = Array.isArray(safeConfig.questions) ? safeConfig.questions : [];
  const normalizedParts = Array.isArray(safeConfig.parts) && safeConfig.parts.length
    ? safeConfig.parts
    : legacyQuestions.length
      ? [{
        ...createPart(0, 1),
        title: safeConfig.title || fallback.title,
        questionGroups: legacyQuestions.map((question, index) => {
          const number = Number(question.number || index + 1);
          const options = Array.isArray(question.options) ? question.options : [];
          return {
            ...createGroup(number),
            title: `Câu ${number}`,
            type: options.length ? 'single_choice' : 'text',
            questions: [{
              ...createQuestion(number),
              ...question,
              number,
              options: options.length
                ? options.map((option, optionIndex) => (
                  typeof option === 'object'
                    ? {
                      value: String(option.value || String.fromCharCode(65 + optionIndex)),
                      label: String(option.label || option.text || ''),
                    }
                    : { value: String.fromCharCode(65 + optionIndex), label: String(option) }
                ))
                : [],
            }],
          };
        }),
      }]
      : fallback.parts;
  const normalizedObjectiveParts = normalizedParts.map((part, partIndex) => ({
    ...part,
    passage: skill === 'READING' && !(part.passage?.paragraphs || []).length && partIndex === 0 && safeConfig.passage
      ? {
        title: safeConfig.passageTitle || part.title || '',
        paragraphs: [{ html: plainTextToRichHtml(safeConfig.passage) }],
      }
      : part.passage,
    questionGroups: (part.questionGroups || []).map((group) => {
      const { description: legacyDescription, ...normalizedGroup } = group;
      return {
        ...normalizedGroup,
        descriptionHtml: group.descriptionHtml ?? legacyDescription ?? '',
      };
    }),
  }));

  return {
    ...fallback,
    ...safeConfig,
    examType: isQuizAssessment(assessment) ? 'GENERAL' : resolveObjectiveExamType(assessment, safeConfig),
    type: isQuizAssessment(assessment)
      ? 'lesson_quiz'
      : resolveObjectiveExamTypeLabel(resolveObjectiveExamType(assessment, safeConfig), skill),
    parts: normalizedObjectiveParts,
  };
};

const normalizeAnswerKey = (value, uiConfigJson = '') => {
  const explicit = safeParse(value, null);
  if (explicit && Object.keys(explicit).length) return explicit;
  return safeParse(uiConfigJson, {})?.answerKey || {};
};

const answerToEditorText = (value) => Array.isArray(value) ? value.join(' | ') : String(value || '');

const parseAcceptedAnswers = (value) => {
  const answers = String(value || '').split('|').map((item) => item.trim()).filter(Boolean);
  return answers.length <= 1 ? (answers[0] || '') : answers;
};

const getBuilderLabels = (skill) => ({
  LISTENING: {
    summaryTitle: 'Nội dung bài nghe',
    openButton: 'Biên soạn bài nghe',
    modalEyebrow: 'Listening editor',
    saveButton: 'Lưu cấu trúc bài nghe',
    emptySummary: 'Chưa biên soạn audio, transcript và câu hỏi.',
    titleRequired: 'Hãy nhập tên bài nghe.',
  },
  READING: {
    summaryTitle: 'Nội dung bài đọc',
    openButton: 'Biên soạn bài đọc',
    modalEyebrow: 'Reading editor',
    saveButton: 'Lưu cấu trúc bài đọc',
    emptySummary: 'Chưa biên soạn passage và câu hỏi.',
    titleRequired: 'Hãy nhập tên bài đọc.',
  },
  WRITING: {
    summaryTitle: 'Nội dung đề viết',
    openButton: 'Biên soạn đề viết',
    modalEyebrow: 'Writing editor',
    saveButton: 'Lưu cấu trúc đề viết',
    emptySummary: 'Chưa biên soạn task Writing.',
    titleRequired: 'Hãy nhập tên đề viết.',
  },
  SPEAKING: {
    summaryTitle: 'Nội dung đề nói',
    openButton: 'Biên soạn đề nói',
    modalEyebrow: 'Speaking editor',
    saveButton: 'Lưu cấu trúc đề nói',
    emptySummary: 'Chưa biên soạn Part 1, Part 2 và Part 3.',
    titleRequired: 'Hãy nhập tên đề nói.',
  },
}[skill] || {
  summaryTitle: 'Nội dung đề',
  openButton: 'Biên soạn nội dung',
  modalEyebrow: 'Trình biên soạn',
  saveButton: 'Lưu cấu trúc',
  emptySummary: 'Chưa biên soạn cấu trúc câu hỏi.',
  titleRequired: 'Hãy nhập tên nội dung.',
});

export default function AssessmentExamBuilder({ assessment, inline = false, onChange }) {
  const skill = getAssessmentSkill(assessment);
  const isObjectiveSkill = OBJECTIVE_SKILLS.includes(skill) || isQuizAssessment(assessment);
  const isWritingSkill = skill === 'WRITING';
  const isSpeakingSkill = skill === 'SPEAKING';
  const isSupported = SUPPORTED_SKILLS.includes(skill) || isQuizAssessment(assessment);
  const builderLabels = getBuilderLabels(skill);
  const [open, setOpen] = useState(inline);
  const [config, setConfig] = useState(() => normalizeConfig(assessment));
  const [answerKey, setAnswerKey] = useState(() => normalizeAnswerKey(assessment.objectiveAnswerKey, assessment.uiConfigJson));
  const [rawImport, setRawImport] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open || inline) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [inline, open]);

  const questionCount = useMemo(
    () => (config.parts || []).reduce((sum, part) =>
      sum + (part.questionGroups || []).reduce((groupSum, group) =>
        groupSum + (group.type === 'multi_select_letters'
          ? (group.questionNumbers || []).length
          : (group.questions || []).length), 0), 0),
    [config.parts],
  );

  const contentCount = useMemo(() => {
    if (isWritingSkill) return (config.tasks || []).length;
    if (isSpeakingSkill) {
      return (config.variants || []).reduce((sum, variant) =>
        sum + (variant.parts || []).reduce((partSum, part) => partSum + (part.prompts || []).length, 0), 0);
    }
    return questionCount;
  }, [config, isSpeakingSkill, isWritingSkill, questionCount]);

  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    if (!inline) return;
    onChangeRef.current('uiConfigJson', JSON.stringify(config, null, 2));
    if (isObjectiveSkill) {
      onChangeRef.current('objectiveAnswerKey', JSON.stringify(answerKey, null, 2));
      onChangeRef.current('maxScore', String(questionCount));
    }
    onChangeRef.current('timeLimitMinutes', String(config.durationMinutes || assessment.timeLimitMinutes || (isSpeakingSkill ? 15 : (isWritingSkill ? 60 : 40))));
  }, [config, answerKey, inline, isObjectiveSkill, isSpeakingSkill, isWritingSkill, questionCount, assessment.timeLimitMinutes]);

  if (!isSupported) return null;

  const openBuilder = () => {
    setConfig(normalizeConfig(assessment));
    setAnswerKey(normalizeAnswerKey(assessment.objectiveAnswerKey, assessment.uiConfigJson));
    setRawImport('');
    setError('');
    setOpen(true);
  };

  const updatePart = (partIndex, patch) => {
    setConfig((current) => ({
      ...current,
      parts: current.parts.map((part, index) => index === partIndex ? { ...part, ...patch } : part),
    }));
  };

  const updateGroup = (partIndex, groupIndex, patch) => {
    setConfig((current) => ({
      ...current,
      parts: current.parts.map((part, index) => index === partIndex ? {
        ...part,
        questionGroups: part.questionGroups.map((group, innerIndex) =>
          innerIndex === groupIndex ? { ...group, ...patch } : group),
      } : part),
    }));
  };

  const updateQuestion = (partIndex, groupIndex, questionIndex, patch) => {
    const part = config.parts[partIndex];
    const group = part.questionGroups[groupIndex];
    updateGroup(partIndex, groupIndex, {
      questions: group.questions.map((question, index) =>
        index === questionIndex ? { ...question, ...patch } : question),
    });
  };

  const moveAnswer = (oldNumber, newNumber) => {
    const oldKey = String(oldNumber);
    const newKey = String(newNumber);
    if (oldKey === newKey) return;
    setAnswerKey((current) => {
      const next = { ...current };
      if (Object.prototype.hasOwnProperty.call(next, oldKey)) {
        next[newKey] = next[oldKey];
        delete next[oldKey];
      }
      return next;
    });
  };

  const removeAnswers = (numbers) => {
    setAnswerKey((current) => {
      const next = { ...current };
      numbers.forEach((number) => delete next[String(number)]);
      return next;
    });
  };

  const updateOption = (partIndex, groupIndex, questionIndex, optionIndex, value, shared = false) => {
    const group = config.parts[partIndex].questionGroups[groupIndex];
    const options = shared ? group.options : group.questions[questionIndex].options;
    const nextOptions = options.map((option, index) => index === optionIndex ? { ...option, label: value } : option);
    if (shared) {
      updateGroup(partIndex, groupIndex, { options: nextOptions });
    } else {
      updateQuestion(partIndex, groupIndex, questionIndex, { options: nextOptions });
    }
  };

  const addPart = () => {
    setConfig((current) => ({
      ...current,
      parts: [...current.parts, createPart(current.parts.length, nextQuestionNumber(current.parts))],
    }));
  };

  const removePart = (partIndex) => {
    const removedNumbers = collectQuestionNumbers([config.parts[partIndex]]);
    removeAnswers(removedNumbers);
    setConfig((current) => ({
      ...current,
      parts: current.parts
        .filter((_, index) => index !== partIndex)
        .map((part, index) => ({ ...part, key: `part_${index + 1}`, partNumber: index + 1 })),
    }));
  };

  const addGroup = (partIndex) => {
    const number = nextQuestionNumber(config.parts);
    updatePart(partIndex, {
      questionGroups: [...config.parts[partIndex].questionGroups, createGroup(number)],
    });
  };

  const removeGroup = (partIndex, groupIndex) => {
    const removedGroup = config.parts[partIndex].questionGroups[groupIndex];
    removeAnswers(collectQuestionNumbers([{ questionGroups: [removedGroup] }]));
    updatePart(partIndex, {
      questionGroups: config.parts[partIndex].questionGroups.filter((_, index) => index !== groupIndex),
    });
  };

  const addQuestion = (partIndex, groupIndex) => {
    const group = config.parts[partIndex].questionGroups[groupIndex];
    updateGroup(partIndex, groupIndex, {
      questions: [...(group.questions || []), createQuestion(nextQuestionNumber(config.parts))],
    });
  };

  const removeQuestion = (partIndex, groupIndex, questionIndex) => {
    const group = config.parts[partIndex].questionGroups[groupIndex];
    removeAnswers([group.questions[questionIndex]?.number]);
    updateGroup(partIndex, groupIndex, {
      questions: group.questions.filter((_, index) => index !== questionIndex),
    });
  };

  const importJson = () => {
    const parsed = safeParse(rawImport, null);
    if (isObjectiveSkill && !parsed?.parts?.length) {
      setError('JSON không hợp lệ hoặc chưa có danh sách phần thi.');
      return;
    }
    if (isWritingSkill && !parsed?.tasks?.length) {
      setError('JSON không hợp lệ hoặc chưa có danh sách task Writing.');
      return;
    }
    if (isSpeakingSkill && !parsed?.variants?.length) {
      setError('JSON không hợp lệ hoặc chưa có danh sách đề Speaking.');
      return;
    }
    const { answerKey: importedAnswerKey, ...safeConfig } = parsed;
    setConfig(normalizeConfig({ ...assessment, uiConfigJson: JSON.stringify(safeConfig) }));
    if (importedAnswerKey && typeof importedAnswerKey === 'object') {
      setAnswerKey(importedAnswerKey);
    }
    setError('');
  };

  const save = () => {
    if (!String(config.title || '').trim()) {
      setError(builderLabels.titleRequired);
      return;
    }
    if (isWritingSkill) {
      const tasks = config.tasks || [];
      if (!tasks.length) {
        setError('Đề Writing cần ít nhất một task.');
        return;
      }
      if (tasks.some((task) => !String(task.title || '').trim())) {
        setError('Mỗi task Writing cần có tên hiển thị.');
        return;
      }
      if (tasks.some((task) => !stripRichTextToPlain(task.promptHtml || '').trim()
        && !String(task.question || task.prompt || '').trim()
        && !(task.promptParagraphs || []).some((paragraph) => String(paragraph || '').trim()))) {
        setError('Mỗi task Writing cần có nội dung đề bài.');
        return;
      }
      onChange('uiConfigJson', JSON.stringify(config, null, 2));
      onChange('timeLimitMinutes', String(config.durationMinutes || assessment.timeLimitMinutes || 60));
      if (!inline) setOpen(false);
      return;
    }
    if (isSpeakingSkill) {
      const variants = config.variants || [];
      if (!variants.length) {
        setError('Đề Speaking cần ít nhất một phiên bản đề.');
        return;
      }
      const hasEmptyPrompt = variants.some((variant) => !(variant.parts || []).length || (variant.parts || []).some((part) =>
        !(part.prompts || []).some((prompt) => String(prompt?.text || prompt || '').trim())
          && !String(part.cueCardTitle || '').trim(),
      ));
      if (hasEmptyPrompt) {
        setError('Mỗi phần Speaking cần có câu hỏi hoặc thẻ gợi ý.');
        return;
      }
      onChange('uiConfigJson', JSON.stringify(config, null, 2));
      onChange('timeLimitMinutes', String(config.durationMinutes || assessment.timeLimitMinutes || 15));
      if (!inline) setOpen(false);
      return;
    }
    if (!config.parts?.length || questionCount === 0) {
      setError('Đề thi cần ít nhất một phần và một câu hỏi.');
      return;
    }
    if (!Number.isFinite(Number(config.durationMinutes)) || Number(config.durationMinutes) <= 0) {
      setError('Thời gian làm bài phải lớn hơn 0 phút.');
      return;
    }
    if (config.parts.some((part) => !(part.questionGroups || []).length)) {
      setError('Mỗi phần thi cần có ít nhất một nhóm câu hỏi.');
      return;
    }

    const numbers = [];
    config.parts.forEach((part) => (part.questionGroups || []).forEach((group) => {
      if (group.type === 'multi_select_letters') {
        numbers.push(...(group.questionNumbers || []).map(Number));
      } else {
        numbers.push(...(group.questions || []).map((question) => Number(question.number)));
      }
    }));
    if (numbers.some((number) => !Number.isInteger(number) || number <= 0)) {
      setError('Số thứ tự câu hỏi phải là số nguyên lớn hơn 0.');
      return;
    }
    if (new Set(numbers).size !== numbers.length) {
      setError('Số thứ tự câu hỏi không được trùng nhau.');
      return;
    }
    const unansweredNumber = numbers.find((number) => {
      const answer = answerKey[String(number)];
      return Array.isArray(answer) ? answer.length === 0 : !String(answer ?? '').trim();
    });
    if (unansweredNumber) {
      setError(`Câu ${unansweredNumber} chưa có đáp án.`);
      return;
    }

    onChange('uiConfigJson', JSON.stringify(config, null, 2));
    onChange('objectiveAnswerKey', JSON.stringify(answerKey, null, 2));
    onChange('maxScore', String(questionCount));
    onChange('timeLimitMinutes', String(config.durationMinutes || assessment.timeLimitMinutes || 40));
    if (!inline) setOpen(false);
  };

  const configuredSummary = assessment.uiConfigJson
    ? isWritingSkill
      ? `${contentCount} task đã cấu hình`
      : isSpeakingSkill
        ? `${contentCount} câu hỏi/chủ đề đã cấu hình`
        : `${questionCount} câu đã cấu hình`
    : builderLabels.emptySummary;

  return (
    <>
      {!inline ? <div className="mt-4 rounded-2xl border border-[#dfbfbd] bg-white p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-semibold text-[#4b0009]">{builderLabels.summaryTitle}</p>
            <p className="mt-1 text-sm text-[#584140]">
              {configuredSummary}
            </p>
          </div>
          <button
            className="rounded-xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white"
            onClick={openBuilder}
            type="button"
          >
            {builderLabels.openButton}
          </button>
        </div>
      </div> : null}

      {open ? (
        <div className={inline ? 'space-y-6' : 'fixed inset-0 z-[90] flex items-center justify-center overflow-hidden p-4'}>
          {!inline ? <button
            aria-label="Đóng modal"
            className="absolute -inset-10 bg-[#1a0004]/50 backdrop-blur-sm"
            onClick={() => setOpen(false)}
            type="button"
          /> : null}
          <div className={inline ? 'w-full' : 'relative z-10 flex max-h-[94dvh] w-full max-w-6xl flex-col overflow-hidden rounded-[30px] bg-white shadow-2xl'}>
            {!inline ? <header className="flex items-start justify-between gap-4 border-b border-[#eadcdc] px-6 py-5">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{builderLabels.modalEyebrow}</p>
                <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#4b0009]">{assessment.title || builderLabels.summaryTitle}</h2>
              </div>
              <button className="rounded-xl border border-[#eadcdc] p-2 text-[#730014]" onClick={() => setOpen(false)} type="button">
                <X className="h-5 w-5" />
              </button>
            </header> : null}

            <div className={inline ? '' : 'flex-1 overflow-y-auto px-6 py-6'}>
              {error ? <p className="mb-4 rounded-2xl bg-[#ffdad6] px-4 py-3 text-sm font-semibold text-[#93000a]">{error}</p> : null}

              {isWritingSkill ? (
                <WritingConfigEditor compact={inline} config={config} onChange={setConfig} />
              ) : null}

              {isSpeakingSkill ? (
                <SpeakingConfigEditor compact={inline} config={config} onChange={setConfig} />
              ) : null}

              {isObjectiveSkill ? (
                <>
              <section className="grid gap-4 rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-5 md:grid-cols-2">
                {!inline ? (
                  <>
                    <Field label="Tên hiển thị của đề" value={config.title} onChange={(value) => setConfig((current) => ({ ...current, title: value }))} />
                    <Field label="Mã đề" value={config.key} onChange={(value) => setConfig((current) => ({ ...current, key: value }))} />
                    <Field label="Thời gian (phút)" type="number" value={config.durationMinutes} onChange={(value) => setConfig((current) => ({ ...current, durationMinutes: Number(value) }))} />
                  </>
                ) : null}
                {skill === 'LISTENING' ? (
                  <div className="md:col-span-2">
                    <MediaUploadField
                      accept=".mp3,.m4a,.wav,.webm"
                      hint="MP3, M4A, WAV hoặc WEBM, tối đa 20 MB"
                      label="Audio của toàn bài nghe"
                      onChange={(audioUrl) => setConfig((current) => ({ ...current, audioUrl }))}
                      value={config.audioUrl || ''}
                    />
                  </div>
                ) : null}
                <div className="md:col-span-2">
                  <OptionalFieldsSection
                    defaultExpanded={Boolean((config.rules || []).length)}
                    label="Thông tin bổ sung của đề"
                  >
                    <div className="grid gap-3 md:grid-cols-2">
                      <div className="md:col-span-2">
                        <TextAreaField
                          label="Quy định trong lúc làm bài"
                          value={(config.rules || []).join('\n')}
                          onChange={(value) => setConfig((current) => ({
                            ...current,
                            rules: value.split('\n').map((rule) => rule.trim()).filter(Boolean),
                          }))}
                        />
                        <p className="mt-2 text-xs leading-5 text-[#584140]">Mỗi dòng là một hướng dẫn hiển thị cho học viên trước khi làm bài.</p>
                      </div>
                    </div>
                  </OptionalFieldsSection>
                </div>
              </section>

              <div className="mt-6 space-y-5">
                {config.parts.map((part, partIndex) => (
                  <section key={part.key} className="rounded-3xl border border-[#dfbfbd] bg-white p-5">
                    <div className="flex items-start justify-between gap-3">
                      <div className="grid flex-1 gap-3">
                        <Field label={`Tên phần ${partIndex + 1}`} value={part.title || ''} onChange={(value) => updatePart(partIndex, { title: value })} />
                      </div>
                      <IconButton label="Xóa phần" onClick={() => removePart(partIndex)}><Trash2 className="h-4 w-4" /></IconButton>
                    </div>

                    {skill === 'READING' && config.examType !== 'TOEIC' ? (
                      <div className="mt-4 rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-4">
                        <Field
                          label="Tiêu đề bài đọc"
                          value={part.passage?.title || ''}
                          onChange={(title) => updatePart(partIndex, {
                            passage: { ...(part.passage || {}), title },
                          })}
                        />
                        <div className="mt-4">
                          <RichTextEditor
                            helperText="Đây là đoạn văn học viên sẽ đọc ở cột bên trái khi làm bài."
                            label="Nội dung bài đọc"
                            onChange={(html) => updatePart(partIndex, {
                              passage: {
                                ...(part.passage || {}),
                                paragraphs: html ? [{ html }] : [],
                              },
                            })}
                            placeholder="Nhập toàn bộ đoạn văn Reading..."
                            size="form"
                            value={getPassageHtml(part.passage)}
                          />
                        </div>
                      </div>
                    ) : null}

                    <div className="mt-5 space-y-4">
                      {(part.questionGroups || []).map((group, groupIndex) => (
                        <div key={`${part.key}-${groupIndex}`} className="rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-4">
                          <div className="grid gap-3 md:grid-cols-[1fr_240px_auto]">
                            <Field label="Tên nhóm câu hỏi (có thể gồm nhiều câu)" value={group.title || ''} onChange={(value) => updateGroup(partIndex, groupIndex, { title: value })} />
                            <SelectField label="Dạng câu hỏi" value={group.type} options={GROUP_TYPES} onChange={(value) => {
                              // "Chọn nhiều đáp án" stores its number in questionNumbers instead of
                              // questions, so switching to/from it used to look up a brand-new "next"
                              // number instead of keeping the one this group already had — bumping
                              // "Số câu" by 1 for no reason. Reuse the group's own current number instead.
                              const preservedNumber = group.questions?.[0]?.number
                                ?? group.questionNumbers?.[0]
                                ?? nextQuestionNumber(config.parts);
                              updateGroup(partIndex, groupIndex, {
                                type: value,
                                questions: value === 'multi_select_letters' ? [] : (group.questions?.length ? group.questions : [createQuestion(preservedNumber)]),
                                questionNumbers: value === 'multi_select_letters' ? (group.questionNumbers?.length ? group.questionNumbers : [preservedNumber]) : [],
                              });
                            }} />
                            <IconButton label="Xóa nhóm" onClick={() => removeGroup(partIndex, groupIndex)}><Trash2 className="h-4 w-4" /></IconButton>
                          </div>
                          <div className="mt-3">
                            <TextAreaField label="Hướng dẫn (có thể viết cả form/biểu mẫu mẫu nhiều dòng)" value={group.instructions || ''} onChange={(value) => updateGroup(partIndex, groupIndex, { instructions: value })} />
                          </div>
                          {skill === 'READING' || config.examType === 'TOEIC' ? (
                          <div className="mt-3">
                            <OptionalFieldsSection
                              defaultExpanded={Boolean(
                                (config.examType === 'TOEIC' && group.audioUrl)
                                || ((skill === 'READING' || (skill === 'LISTENING' && config.examType === 'TOEIC')) && group.hideOptionText)
                                || (config.examType === 'TOEIC' && group.perQuestionAudio)
                                || group.passageHtml
                              )}
                              label="Thông tin bổ sung của nhóm"
                            >
                              <div className="grid gap-3 md:grid-cols-2">
                                {skill === 'LISTENING' && config.examType === 'TOEIC' ? (
                                  <div className="md:col-span-2">
                                    <MediaUploadField
                                      accept=".mp3,.m4a,.wav,.webm"
                                      hint="Chỉ dùng khi Part TOEIC này có audio riêng."
                                      label="Audio của nhóm câu hỏi"
                                      onChange={(audioUrl) => updateGroup(partIndex, groupIndex, { audioUrl })}
                                      value={group.audioUrl || ''}
                                    />
                                  </div>
                                ) : null}
                                {skill === 'READING' || config.examType === 'TOEIC' ? (
                                <div className="flex flex-wrap items-end gap-4 pb-1">
                                  {(skill === 'READING' || (skill === 'LISTENING' && config.examType === 'TOEIC')) ? (
                                    <label className="inline-flex items-center gap-2 text-sm font-semibold text-[#584140]">
                                      <input
                                        checked={Boolean(group.hideOptionText)}
                                        className="h-4 w-4 accent-[#4b0009]"
                                        onChange={(event) => updateGroup(partIndex, groupIndex, { hideOptionText: event.target.checked })}
                                        type="checkbox"
                                      />
                                      Ẩn chữ lựa chọn (A/B/C/D)
                                    </label>
                                  ) : null}
                                  {skill === 'LISTENING' && config.examType === 'TOEIC' ? (
                                    <label className="inline-flex items-center gap-2 text-sm font-semibold text-[#584140]">
                                      <input
                                        checked={Boolean(group.perQuestionAudio)}
                                        className="h-4 w-4 accent-[#4b0009]"
                                        onChange={(event) => updateGroup(partIndex, groupIndex, { perQuestionAudio: event.target.checked })}
                                        type="checkbox"
                                      />
                                      Audio từng câu
                                    </label>
                                  ) : null}
                                </div>
                                ) : null}
                                {skill === 'READING' && config.examType === 'TOEIC' ? (
                                  <div className="md:col-span-2">
                                    <RichTextEditor
                                      helperText="Đoạn văn dùng riêng cho nhóm câu hỏi TOEIC này."
                                      label="Đoạn văn của nhóm"
                                      value={group.passageHtml || ''}
                                      onChange={(passageHtml) => updateGroup(partIndex, groupIndex, { passageHtml })}
                                      placeholder="Nhập email, thông báo hoặc đoạn văn..."
                                      size="compact"
                                    />
                                  </div>
                                ) : null}
                              </div>
                            </OptionalFieldsSection>
                          </div>
                          ) : null}

                          {group.type === 'multi_select_letters' ? (
                            <MultiSelectEditor
                              answerKey={answerKey}
                              group={group}
                              onAnswerNumberChange={moveAnswer}
                              onAnswerKeyChange={setAnswerKey}
                              onChange={(patch) => updateGroup(partIndex, groupIndex, patch)}
                              onOptionChange={(optionIndex, value) => updateOption(partIndex, groupIndex, 0, optionIndex, value, true)}
                            />
                          ) : (
                            <div className="mt-4 space-y-3">
                              {(group.questions || []).map((question, questionIndex) => (
                                <QuestionEditor
                                  answer={answerKey[String(question.number)]}
                                  groupType={group.type}
                                  key={`${question.number}-${questionIndex}`}
                                  onAnswerChange={(value) => setAnswerKey((current) => ({
                                    ...current,
                                    [String(question.number)]: parseAcceptedAnswers(value),
                                  }))}
                                  onNumberChange={(value) => {
                                    moveAnswer(question.number, value);
                                    updateQuestion(partIndex, groupIndex, questionIndex, { number: Number(value) });
                                  }}
                                  onChange={(patch) => updateQuestion(partIndex, groupIndex, questionIndex, patch)}
                                  onOptionChange={(optionIndex, value) => updateOption(partIndex, groupIndex, questionIndex, optionIndex, value)}
                                  onRemove={() => removeQuestion(partIndex, groupIndex, questionIndex)}
                                  question={question}
                                  examType={config.examType}
                                  skill={skill}
                                />
                              ))}
                              <button className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" onClick={() => addQuestion(partIndex, groupIndex)} type="button">
                                <Plus className="h-4 w-4" /> Thêm câu hỏi
                              </button>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                    <button className="mt-4 inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" onClick={() => addGroup(partIndex)} type="button">
                      <Plus className="h-4 w-4" /> Thêm nhóm câu hỏi
                    </button>
                  </section>
                ))}
              </div>

              <button className="mt-5 inline-flex items-center gap-2 rounded-xl bg-[#730014] px-4 py-3 text-sm font-semibold text-white" onClick={addPart} type="button">
                <Plus className="h-4 w-4" /> Thêm phần
              </button>

              <details className="mt-6 rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-4">
                <summary className="cursor-pointer font-semibold text-[#730014]">Nhập nhanh từ JSON có sẵn</summary>
                <textarea className="mt-4 min-h-40 w-full rounded-xl border border-[#dfbfbd] p-3 font-mono text-xs" onChange={(event) => setRawImport(event.target.value)} value={rawImport} />
                <button className="mt-3 inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]" onClick={importJson} type="button">
                  <FileJson className="h-4 w-4" /> Đọc cấu trúc JSON
                </button>
              </details>
                </>
              ) : null}
            </div>

            <footer className={inline ? 'mt-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#eadcdc] bg-[#fffafb] px-5 py-4' : 'flex items-center justify-between gap-3 border-t border-[#eadcdc] bg-[#fffafb] px-6 py-4'}>
              <p className="text-sm text-[#584140]">{buildFooterSummary(config, skill, questionCount)}</p>
              <div className="flex gap-3">
                {!inline ? <button className="rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-semibold text-[#730014]" onClick={() => setOpen(false)} type="button">Hủy</button> : null}
                <button className="rounded-xl bg-[#4b0009] px-5 py-3 text-sm font-semibold text-white" onClick={save} type="button">{inline ? 'Áp dụng nội dung' : builderLabels.saveButton}</button>
              </div>
            </footer>
          </div>
        </div>
      ) : null}
    </>
  );
}

function WritingConfigEditor({ compact = false, config, onChange }) {
  const tasks = config.tasks || [];
  const updateConfig = (patch) => onChange((current) => ({ ...current, ...patch }));
  const updateTask = (taskIndex, patch) => onChange((current) => ({
    ...current,
    tasks: (current.tasks || []).map((task, index) => (index === taskIndex ? { ...task, ...patch } : task)),
  }));

  return (
    <div className="space-y-5">
      {!compact ? <section className="grid gap-4 rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-5 md:grid-cols-2">
        <Field label="Tên hiển thị của đề" value={config.title} onChange={(value) => updateConfig({ title: value })} />
        <Field label="Mã đề" value={config.key} onChange={(value) => updateConfig({ key: value })} />
        <Field label="Thời gian (phút)" type="number" value={config.durationMinutes} onChange={(value) => updateConfig({ durationMinutes: Number(value) })} />
      </section> : null}

      {tasks.map((task, taskIndex) => (
        <section key={task.key || taskIndex} className="rounded-3xl border border-[#dfbfbd] bg-white p-5">
          <div className="flex items-start justify-between gap-3">
            <div className="grid flex-1 gap-3 md:grid-cols-2">
              <Field label="Tên task" value={task.title || ''} onChange={(value) => updateTask(taskIndex, { title: value })} />
              <Field label="Tiêu đề hiển thị" value={task.heading || ''} onChange={(value) => updateTask(taskIndex, { heading: value })} />
              <Field label="Số từ tối thiểu" type="number" value={task.minimumWords || task.minWords || 0} onChange={(value) => updateTask(taskIndex, { minimumWords: Number(value) })} />
              <Field label="Thời gian gợi ý (phút)" type="number" value={task.recommendedMinutes || task.durationMinutes || 0} onChange={(value) => updateTask(taskIndex, { recommendedMinutes: Number(value) })} />
            </div>
            <IconButton
              label="Xóa task"
              onClick={() => onChange((current) => ({
                ...current,
                tasks: (current.tasks || []).filter((_, index) => index !== taskIndex),
              }))}
            >
              <Trash2 className="h-4 w-4" />
            </IconButton>
          </div>

          <div className="mt-4">
            <Field label="Tóm tắt yêu cầu" value={task.summary || ''} onChange={(value) => updateTask(taskIndex, { summary: value })} />
          </div>
          <div className="mt-4">
            <RichTextEditor
              helperText="Có thể dùng tiêu đề, in đậm, danh sách và liên kết. Nội dung sẽ hiển thị đúng định dạng cho học viên."
              label="Đề bài"
              value={task.promptHtml || getWritingPromptHtml(task)}
              onChange={(promptHtml) => updateTask(taskIndex, {
                promptHtml,
                promptParagraphs: [],
                question: stripRichTextToPlain(promptHtml),
              })}
              placeholder="Soạn đầy đủ đề bài Writing..."
              size="form"
            />
          </div>
          <div className="mt-4">
            <MediaUploadField
              accept=".jpg,.jpeg,.png"
              label="Ảnh đề bài / biểu đồ / hình minh họa"
              onChange={(imageUrl) => updateTask(taskIndex, { imageUrl })}
              value={task.imageUrl || ''}
            />
          </div>
        </section>
      ))}

      <button
        className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]"
        onClick={() => onChange((current) => ({
          ...current,
          tasks: [...(current.tasks || []), createWritingTask((current.tasks || []).length)],
        }))}
        type="button"
      >
        <Plus className="h-4 w-4" /> Thêm task Writing
      </button>
    </div>
  );
}

function SpeakingConfigEditor({ compact = false, config, onChange }) {
  const variants = config.variants || [];
  const updateConfig = (patch) => onChange((current) => ({ ...current, ...patch }));
  const updateBriefing = (patch) => onChange((current) => ({
    ...current,
    briefing: { ...(current.briefing || {}), ...patch },
  }));
  const updateVariant = (variantIndex, patch) => onChange((current) => ({
    ...current,
    variants: (current.variants || []).map((variant, index) => (index === variantIndex ? { ...variant, ...patch } : variant)),
  }));
  const updatePart = (variantIndex, partIndex, patch) => onChange((current) => ({
    ...current,
    variants: (current.variants || []).map((variant, index) => {
      if (index !== variantIndex) return variant;
      return {
        ...variant,
        parts: (variant.parts || []).map((part, innerIndex) => (innerIndex === partIndex ? { ...part, ...patch } : part)),
      };
    }),
  }));

  return (
    <div className="space-y-5">
      <section className="grid gap-4 rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-5 md:grid-cols-2">
        {!compact ? (
          <>
            <Field label="Tên hiển thị của đề" value={config.title} onChange={(value) => updateConfig({ title: value })} />
            <Field label="Mã đề" value={config.key} onChange={(value) => updateConfig({ key: value })} />
            <Field label="Thời gian (phút)" type="number" value={config.durationMinutes} onChange={(value) => updateConfig({ durationMinutes: Number(value) })} />
          </>
        ) : null}
        <Field label="Tiêu đề hướng dẫn" value={config.briefing?.title || ''} onChange={(value) => updateBriefing({ title: value })} />
        <div className="md:col-span-2">
          <TextAreaField label="Tóm tắt hướng dẫn" value={config.briefing?.summary || ''} onChange={(value) => updateBriefing({ summary: value })} />
        </div>
      </section>

      {variants.map((variant, variantIndex) => (
        <section key={variant.key || variantIndex} className="rounded-3xl border border-[#dfbfbd] bg-white p-5">
          <div className="flex items-start justify-between gap-3">
            <div className="grid flex-1 gap-3 md:grid-cols-2">
              <Field label="Mã đề Speaking" value={variant.key || ''} onChange={(value) => updateVariant(variantIndex, { key: value })} />
              <Field label="Tên đề Speaking" value={variant.label || ''} onChange={(value) => updateVariant(variantIndex, { label: value })} />
            </div>
            <IconButton
              label="Xóa đề"
              onClick={() => onChange((current) => ({
                ...current,
                variants: (current.variants || []).filter((_, index) => index !== variantIndex),
              }))}
            >
              <Trash2 className="h-4 w-4" />
            </IconButton>
          </div>

          <div className="mt-5 space-y-4">
            {(variant.parts || []).map((part, partIndex) => (
              <div key={part.key || partIndex} className="rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-4">
                <div className="grid gap-3 md:grid-cols-[1fr_1fr_120px_120px_auto]">
                  <Field label="Nhãn phần" value={part.label || ''} onChange={(value) => updatePart(variantIndex, partIndex, { label: value })} />
                  <Field label="Mô tả phần" value={part.caption || ''} onChange={(value) => updatePart(variantIndex, partIndex, { caption: value })} />
                  <Field label="Thời gian chuẩn bị" type="number" value={part.prepSeconds || 0} onChange={(value) => updatePart(variantIndex, partIndex, { prepSeconds: Number(value) })} />
                  <Field label="Thời gian trả lời" type="number" value={part.answerSeconds || 0} onChange={(value) => updatePart(variantIndex, partIndex, { answerSeconds: Number(value) })} />
                  <IconButton
                    label="Xóa phần"
                    onClick={() => updateVariant(variantIndex, {
                      parts: (variant.parts || []).filter((_, index) => index !== partIndex),
                    })}
                  >
                    <Trash2 className="h-4 w-4" />
                  </IconButton>
                </div>
                <div className="mt-4 space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">Câu hỏi và media</p>
                    <button
                      className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-bold text-[#730014] transition hover:bg-[#fff4f5]"
                      onClick={() => updatePart(variantIndex, partIndex, {
                        prompts: [...(part.prompts || []), { text: '', videoUrl: '', audioUrl: '' }],
                      })}
                      type="button"
                    >
                      <Plus className="h-3.5 w-3.5" /> Thêm câu hỏi
                    </button>
                  </div>
                  {(part.prompts || []).map((prompt, promptIndex) => (
                    <div
                      className="grid gap-3 rounded-2xl border border-[#eadcdc] bg-white p-4 md:grid-cols-[1fr_1fr_auto]"
                      key={`${part.key || partIndex}-prompt-${promptIndex}`}
                    >
                      <div className="md:col-span-2">
                        <TextAreaField
                          label={`Câu hỏi ${promptIndex + 1}`}
                          value={prompt.text || ''}
                          onChange={(value) => updatePart(variantIndex, partIndex, {
                            prompts: (part.prompts || []).map((item, index) => (
                              index === promptIndex ? { ...item, text: value } : item
                            )),
                          })}
                        />
                      </div>
                      <IconButton
                        label="Xóa câu hỏi"
                        onClick={() => updatePart(variantIndex, partIndex, {
                          prompts: (part.prompts || []).filter((_, index) => index !== promptIndex),
                        })}
                      >
                        <Trash2 className="h-4 w-4" />
                      </IconButton>
                      <div className="md:col-span-3">
                        <OptionalFieldsSection
                          defaultExpanded={Boolean(prompt.videoUrl || prompt.audioUrl)}
                          label="Media câu hỏi"
                        >
                          <div className="grid gap-3 md:grid-cols-2">
                            <MediaUploadField
                              accept=".mp4,.webm"
                              hint="MP4 hoặc WEBM, tối đa 20 MB"
                              label="Video minh họa"
                              value={prompt.videoUrl || ''}
                              onChange={(videoUrl) => updatePart(variantIndex, partIndex, {
                                prompts: (part.prompts || []).map((item, index) => (
                                  index === promptIndex ? { ...item, videoUrl } : item
                                )),
                              })}
                            />
                            <MediaUploadField
                              accept=".mp3,.m4a,.wav,.webm"
                              hint="MP3, M4A, WAV hoặc WEBM, tối đa 20 MB"
                              label="Audio câu hỏi"
                              value={prompt.audioUrl || ''}
                              onChange={(audioUrl) => updatePart(variantIndex, partIndex, {
                                prompts: (part.prompts || []).map((item, index) => (
                                  index === promptIndex ? { ...item, audioUrl } : item
                                )),
                              })}
                            />
                          </div>
                        </OptionalFieldsSection>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="mt-3">
                  <OptionalFieldsSection
                    defaultExpanded={Boolean(part.cueCardTitle || (part.cueCardBullets || []).length)}
                    label="Thẻ gợi ý (cue card)"
                  >
                    <div className="grid gap-3 md:grid-cols-2">
                      <Field label="Tiêu đề cue card" value={part.cueCardTitle || ''} onChange={(value) => updatePart(variantIndex, partIndex, { cueCardTitle: value })} />
                      <TextAreaField
                        label="Các ý gợi ý trên cue card"
                        value={(part.cueCardBullets || []).join('\n')}
                        onChange={(value) => updatePart(variantIndex, partIndex, {
                          cueCardBullets: value.split('\n').map((line) => line.trim()).filter(Boolean),
                        })}
                      />
                    </div>
                  </OptionalFieldsSection>
                </div>
              </div>
            ))}
          </div>

          <button
            className="mt-4 inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]"
            onClick={() => updateVariant(variantIndex, {
              parts: [...(variant.parts || []), createSpeakingPart((variant.parts || []).length)],
            })}
            type="button"
          >
            <Plus className="h-4 w-4" /> Thêm phần Speaking
          </button>
        </section>
      ))}

      <button
        className="inline-flex items-center gap-2 rounded-xl border border-[#dfbfbd] px-3 py-2 text-sm font-semibold text-[#730014]"
        onClick={() => onChange((current) => ({
          ...current,
          variants: [...(current.variants || []), createSpeakingVariant((current.variants || []).length)],
        }))}
        type="button"
      >
        <Plus className="h-4 w-4" /> Thêm đề Speaking
      </button>
    </div>
  );
}

function buildFooterSummary(config, skill, questionCount) {
  if (skill === 'WRITING') {
    return `${config.tasks?.length || 0} task Writing`;
  }
  if (skill === 'SPEAKING') {
    const partCount = (config.variants || []).reduce((sum, variant) => sum + (variant.parts || []).length, 0);
    return `${config.variants?.length || 0} đề · ${partCount} phần Speaking`;
  }
  return `${questionCount} câu · ${config.parts?.length || 0} phần`;
}

function QuestionEditor({ answer, examType, groupType, onAnswerChange, onChange, onNumberChange, onOptionChange, onRemove, question, skill }) {
  const supportsQuestionImage = skill === 'READING' || (skill === 'LISTENING' && examType === 'TOEIC');
  const supportsQuestionAudio = skill === 'LISTENING' && examType === 'TOEIC';
  const showOptionalSection = groupType === 'text' || supportsQuestionImage || supportsQuestionAudio;
  const hasOptionalContent = Boolean(
    question.promptAfter
    || question.imageUrl
    || question.audioUrl
  );
  return (
    <div className="rounded-xl border border-[#eadcdc] bg-white p-4">
      <div className="grid gap-3 md:grid-cols-[110px_1fr_auto]">
        <Field label="Số câu" type="number" value={question.number} onChange={onNumberChange} />
        <Field
          label="Nội dung câu hỏi"
          value={groupType === 'single_choice' ? question.prompt || '' : question.promptBefore || ''}
          onChange={(value) => onChange(groupType === 'single_choice' ? { prompt: value } : { promptBefore: value })}
        />
        <IconButton label="Xóa câu" onClick={onRemove}><Trash2 className="h-4 w-4" /></IconButton>
      </div>
      {groupType === 'text' ? (
        <div className="mt-3">
          <div className="md:w-1/2 md:pr-1.5">
            <Field label="Đáp án chấp nhận" value={answerToEditorText(answer)} onChange={onAnswerChange} />
            <p className="mt-1 text-xs leading-5 text-[#584140]">Nếu có nhiều cách trả lời đúng, ngăn cách từng cách bằng dấu <strong>|</strong>.</p>
          </div>
        </div>
      ) : (
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          {(question.options || []).map((option, index) => (
            <Field key={option.value} label={`Lựa chọn ${option.value}`} value={option.label || ''} onChange={(value) => onOptionChange(index, value)} />
          ))}
          <SelectField label="Đáp án đúng" value={Array.isArray(answer) ? answer[0] || '' : answer || ''} options={(question.options || []).map((option) => ({ label: option.value, value: option.value }))} onChange={onAnswerChange} />
        </div>
      )}
      {showOptionalSection ? (
        <div className="mt-3">
          <OptionalFieldsSection defaultExpanded={hasOptionalContent} label="Thông tin bổ sung của câu hỏi">
            <div className="grid gap-3 md:grid-cols-2">
              {groupType === 'text' ? (
                <Field label="Nội dung sau ô trả lời" value={question.promptAfter || ''} onChange={(value) => onChange({ promptAfter: value })} />
              ) : null}
              {supportsQuestionImage ? (
                <MediaUploadField
                  accept=".jpg,.jpeg,.png"
                  hint="JPG hoặc PNG, tối đa 20 MB"
                  label="Ảnh minh họa câu hỏi"
                  onChange={(imageUrl) => onChange({ imageUrl })}
                  value={question.imageUrl || ''}
                />
              ) : null}
              {supportsQuestionAudio ? (
                <MediaUploadField
                  accept=".mp3,.m4a,.wav,.webm"
                  hint="Chỉ dùng khi câu TOEIC này có audio riêng."
                  label="Audio của câu hỏi"
                  onChange={(audioUrl) => onChange({ audioUrl })}
                  value={question.audioUrl || ''}
                />
              ) : null}
            </div>
          </OptionalFieldsSection>
        </div>
      ) : null}
    </div>
  );
}

function MultiSelectEditor({ answerKey, group, onAnswerKeyChange, onAnswerNumberChange, onChange, onOptionChange }) {
  const number = Number(group.questionNumbers?.[0] || 1);
  const answer = Array.isArray(answerKey[String(number)]) ? answerKey[String(number)] : [];
  return (
    <div className="mt-4 rounded-xl border border-[#eadcdc] bg-white p-4">
      <div className="grid gap-3 md:grid-cols-2">
        <Field
          label="Số câu"
          type="number"
          value={number}
          onChange={(value) => {
            onAnswerNumberChange(number, value);
            onChange({ questionNumbers: [Number(value)] });
          }}
        />
        <Field label="Số lựa chọn cần chọn" type="number" value={group.maxSelections || 2} onChange={(value) => onChange({ maxSelections: Number(value) })} />
        {(group.options || []).map((option, index) => (
          <Field key={option.value} label={`Lựa chọn ${option.value}`} value={option.label || ''} onChange={(value) => onOptionChange(index, value)} />
        ))}
      </div>
      <div className="mt-3">
        <Field
          label="Đáp án đúng, cách nhau bằng dấu phẩy"
          value={answer.join(', ')}
          onChange={(value) => onAnswerKeyChange((current) => ({
            ...current,
            [String(number)]: value.split(',').map((item) => item.trim().toUpperCase()).filter(Boolean),
          }))}
        />
      </div>
    </div>
  );
}

function MediaUploadField({ accept, hint, label, onChange, value }) {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const fileInputRef = useRef(null);

  const upload = async (file) => {
    if (!file) return;
    setUploading(true);
    setUploadError('');
    try {
      const uploaded = await classroomApi.uploadContentManagerMaterialLibraryFile(file);
      if (!uploaded?.url) throw new Error('Upload response is missing a URL.');
      onChange(uploaded.url);
    } catch (error) {
      setUploadError(error?.response?.data?.message || 'Không thể tải tệp lên. Vui lòng thử lại.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.14em] text-[#8b706e]">{label}</span>
      <input
        accept={accept}
        className="hidden"
        disabled={uploading}
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = '';
          if (file) void upload(file);
        }}
        ref={fileInputRef}
        type="file"
      />
      <div className="flex flex-col gap-2 sm:flex-row">
        <input
          className="min-w-0 flex-1 rounded-xl border border-[#dfbfbd] bg-white px-3 py-2.5 text-sm outline-none focus:border-[#730014]"
          onChange={(event) => onChange(event.target.value)}
          placeholder="Dán URL hoặc tải tệp từ máy tính"
          type="url"
          value={value || ''}
        />
        <button
          className="inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-xl border border-[#dfbfbd] bg-white px-4 text-sm font-bold text-[#730014] transition hover:bg-[#fff4f5] disabled:opacity-60"
          disabled={uploading}
          onClick={() => fileInputRef.current?.click()}
          type="button"
        >
          {uploading ? <Loader2 className="h-4 w-4 animate-spin" /> : <UploadCloud className="h-4 w-4" />}
          {uploading ? 'Đang tải...' : 'Tải tệp'}
        </button>
      </div>
      {hint ? <div className="mt-1.5 text-xs text-slate-500">{hint}</div> : null}
      {uploadError ? <div className="mt-2 text-xs font-semibold text-rose-700" role="alert">{uploadError}</div> : null}
    </div>
  );
}

function OptionalFieldsSection({ children, defaultExpanded = false, label }) {
  const [expanded, setExpanded] = useState(defaultExpanded);

  useEffect(() => {
    if (defaultExpanded) setExpanded(true);
  }, [defaultExpanded]);

  return (
    <div className="overflow-hidden rounded-xl border border-[#eadcdc] bg-[#fffafb]">
      <button
        aria-expanded={expanded}
        className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm font-bold text-[#730014] transition hover:bg-[#fff4f5]"
        onClick={() => setExpanded((current) => !current)}
        type="button"
      >
        <span>{label}</span>
        <ChevronDown className={`h-4 w-4 shrink-0 transition-transform ${expanded ? 'rotate-180' : ''}`} />
      </button>
      {expanded ? <div className="border-t border-[#eadcdc] bg-white p-4">{children}</div> : null}
    </div>
  );
}

function Field({ label, onChange, type = 'text', value }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.14em] text-[#8b706e]">{label}</span>
      <input
        className="w-full rounded-xl border border-[#dfbfbd] bg-white px-3 py-2.5 text-sm outline-none focus:border-[#730014]"
        onChange={(event) => onChange(event.target.value)}
        type={type}
        value={value ?? ''}
      />
    </label>
  );
}

function TextAreaField({ label, onChange, value }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.14em] text-[#8b706e]">{label}</span>
      <textarea
        className="min-h-24 w-full rounded-xl border border-[#dfbfbd] bg-white px-3 py-2.5 text-sm outline-none focus:border-[#730014]"
        onChange={(event) => onChange(event.target.value)}
        value={value ?? ''}
      />
    </label>
  );
}

function SelectField({ label, onChange, options, value }) {
  return (
    <label className="block">
      <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.14em] text-[#8b706e]">{label}</span>
      <BrandedSelect options={options} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function IconButton({ children, label, onClick }) {
  return (
    <button aria-label={label} className="mt-6 inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[#f0c7c7] text-[#93000a]" onClick={onClick} title={label} type="button">
      {children}
    </button>
  );
}

function collectQuestionNumbers(parts) {
  return (parts || []).flatMap((part) =>
    (part?.questionGroups || []).flatMap((group) =>
      group.type === 'multi_select_letters'
        ? (group.questionNumbers || [])
        : (group.questions || []).map((question) => question.number),
    ),
  ).filter((number) => number != null);
}
