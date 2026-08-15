import { useEffect, useMemo, useState } from 'react';
import { FileJson, Plus, Trash2, X } from 'lucide-react';
import BrandedSelect from '../ui/BrandedSelect';

const GROUP_TYPES = [
  { label: 'Điền câu trả lời', value: 'text' },
  { label: 'Chọn một đáp án', value: 'single_choice' },
  { label: 'Chọn nhiều đáp án', value: 'multi_select_letters' },
];

const OBJECTIVE_SKILLS = ['LISTENING', 'READING'];
const SUPPORTED_SKILLS = [...OBJECTIVE_SKILLS, 'WRITING', 'SPEAKING'];

const getAssessmentSkill = (assessment) => String(assessment?.skill || '').toUpperCase();

const createQuestion = (number = 1) => ({
  number,
  prompt: '',
  promptBefore: '',
  promptAfter: '',
  options: [
    { value: 'A', label: '' },
    { value: 'B', label: '' },
    { value: 'C', label: '' },
    { value: 'D', label: '' },
  ],
});

const createGroup = (number = 1) => ({
  title: `Câu ${number}`,
  instructions: '',
  descriptionHtml: '',
  type: 'text',
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

const createConfig = (assessment) => ({
  version: 1,
  type: getAssessmentSkill(assessment) === 'READING'
    ? 'ielts_reading_exam'
    : 'ielts_listening_exam',
  key: `englishlab_${String(assessment.title || 'assessment').toLowerCase().replace(/[^a-z0-9]+/g, '_')}`,
  title: assessment.title || 'Bài thi mới',
  durationMinutes: Number(assessment.timeLimitMinutes || 40),
  audioLabel: 'Bản nghe',
  audioUrl: '',
  rules: [],
  parts: [createPart(0, 1)],
});

const safeParse = (value, fallback) => {
  try {
    const parsed = JSON.parse(String(value || ''));
    return parsed && typeof parsed === 'object' ? parsed : fallback;
  } catch {
    return fallback;
  }
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

  return {
    ...fallback,
    ...safeConfig,
    type: fallback.type,
    parts: normalizedParts,
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

export default function AssessmentExamBuilder({ assessment, onChange }) {
  const skill = getAssessmentSkill(assessment);
  const isObjectiveSkill = OBJECTIVE_SKILLS.includes(skill);
  const isWritingSkill = skill === 'WRITING';
  const isSpeakingSkill = skill === 'SPEAKING';
  const isSupported = SUPPORTED_SKILLS.includes(skill);
  const builderLabels = getBuilderLabels(skill);
  const [open, setOpen] = useState(false);
  const [config, setConfig] = useState(() => normalizeConfig(assessment));
  const [answerKey, setAnswerKey] = useState(() => normalizeAnswerKey(assessment.objectiveAnswerKey, assessment.uiConfigJson));
  const [rawImport, setRawImport] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

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
      if (tasks.some((task) => !String(task.question || task.prompt || '').trim() && !(task.promptParagraphs || []).some((paragraph) => String(paragraph || '').trim()))) {
        setError('Mỗi task Writing cần có nội dung đề bài.');
        return;
      }
      onChange('uiConfigJson', JSON.stringify(config, null, 2));
      onChange('timeLimitMinutes', String(config.durationMinutes || assessment.timeLimitMinutes || 60));
      setOpen(false);
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
      setOpen(false);
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
    setOpen(false);
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
      <div className="mt-4 rounded-2xl border border-[#dfbfbd] bg-white p-4">
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
      </div>

      {open ? (
        <div className="fixed inset-0 z-[90] flex items-center justify-center overflow-hidden p-4">
          <button
            aria-label="Đóng modal"
            className="absolute -inset-10 bg-[#1a0004]/50 backdrop-blur-sm"
            onClick={() => setOpen(false)}
            type="button"
          />
          <div className="relative z-10 flex max-h-[94dvh] w-full max-w-6xl flex-col overflow-hidden rounded-[30px] bg-white shadow-2xl">
            <header className="flex items-start justify-between gap-4 border-b border-[#eadcdc] px-6 py-5">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">{builderLabels.modalEyebrow}</p>
                <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#4b0009]">{assessment.title || builderLabels.summaryTitle}</h2>
              </div>
              <button className="rounded-xl border border-[#eadcdc] p-2 text-[#730014]" onClick={() => setOpen(false)} type="button">
                <X className="h-5 w-5" />
              </button>
            </header>

            <div className="flex-1 overflow-y-auto px-6 py-6">
              {error ? <p className="mb-4 rounded-2xl bg-[#ffdad6] px-4 py-3 text-sm font-semibold text-[#93000a]">{error}</p> : null}

              {isWritingSkill ? (
                <WritingConfigEditor config={config} onChange={setConfig} />
              ) : null}

              {isSpeakingSkill ? (
                <SpeakingConfigEditor config={config} onChange={setConfig} />
              ) : null}

              {isObjectiveSkill ? (
                <>
              <section className="grid gap-4 rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-5 md:grid-cols-2">
                <Field label="Tên hiển thị của đề" value={config.title} onChange={(value) => setConfig((current) => ({ ...current, title: value }))} />
                <Field label="Mã đề" value={config.key} onChange={(value) => setConfig((current) => ({ ...current, key: value }))} />
                <Field label="Thời gian (phút)" type="number" value={config.durationMinutes} onChange={(value) => setConfig((current) => ({ ...current, durationMinutes: Number(value) }))} />
                {skill === 'LISTENING' ? (
                  <>
                    <Field label="Audio" value={config.audioUrl || ''} onChange={(value) => setConfig((current) => ({ ...current, audioUrl: value }))} />
                    <div className="md:col-span-2">
                      <TextAreaField label="Transcript" value={config.transcript || ''} onChange={(value) => setConfig((current) => ({ ...current, transcript: value }))} />
                    </div>
                  </>
                ) : null}
                {skill === 'READING' ? (
                  <>
                    <Field label="Tiêu đề passage" value={config.passageTitle || ''} onChange={(value) => setConfig((current) => ({ ...current, passageTitle: value }))} />
                    <div className="md:col-span-2">
                      <TextAreaField label="Passage" value={config.passage || ''} onChange={(value) => setConfig((current) => ({ ...current, passage: value }))} />
                    </div>
                    <label className="md:col-span-2 flex items-center gap-3 rounded-xl border border-[#dfbfbd] bg-white px-3 py-2.5 text-sm font-semibold text-[#4b0009]">
                      <input
                        checked={Boolean(config.paragraphNumbering)}
                        className="h-4 w-4 accent-[#4b0009]"
                        onChange={(event) => setConfig((current) => ({ ...current, paragraphNumbering: event.target.checked }))}
                        type="checkbox"
                      />
                      Đánh số đoạn văn trong preview passage
                    </label>
                  </>
                ) : null}
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
              </section>

              <div className="mt-6 space-y-5">
                {config.parts.map((part, partIndex) => (
                  <section key={part.key} className="rounded-3xl border border-[#dfbfbd] bg-white p-5">
                    <div className="flex items-start justify-between gap-3">
                      <div className="grid flex-1 gap-3 md:grid-cols-2">
                        <Field label={`Tên phần ${partIndex + 1}`} value={part.title || ''} onChange={(value) => updatePart(partIndex, { title: value })} />
                        <Field label="Mô tả ngắn" value={part.summary || ''} onChange={(value) => updatePart(partIndex, { summary: value })} />
                      </div>
                      <IconButton label="Xóa phần" onClick={() => removePart(partIndex)}><Trash2 className="h-4 w-4" /></IconButton>
                    </div>

                    <div className="mt-5 space-y-4">
                      {(part.questionGroups || []).map((group, groupIndex) => (
                        <div key={`${part.key}-${groupIndex}`} className="rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-4">
                          <div className="grid gap-3 md:grid-cols-[1fr_240px_auto]">
                            <Field label="Tên nhóm câu hỏi" value={group.title || ''} onChange={(value) => updateGroup(partIndex, groupIndex, { title: value })} />
                            <SelectField label="Dạng câu hỏi" value={group.type} options={GROUP_TYPES} onChange={(value) => updateGroup(partIndex, groupIndex, {
                              type: value,
                              questions: value === 'multi_select_letters' ? [] : (group.questions?.length ? group.questions : [createQuestion(nextQuestionNumber(config.parts))]),
                              questionNumbers: value === 'multi_select_letters' ? (group.questionNumbers?.length ? group.questionNumbers : [nextQuestionNumber(config.parts)]) : [],
                            })} />
                            <IconButton label="Xóa nhóm" onClick={() => removeGroup(partIndex, groupIndex)}><Trash2 className="h-4 w-4" /></IconButton>
                          </div>
                          <div className="mt-3">
                            <Field label="Hướng dẫn" value={group.instructions || ''} onChange={(value) => updateGroup(partIndex, groupIndex, { instructions: value })} />
                          </div>
                          <div className="mt-3">
                            <TextAreaField
                              label="Nội dung dẫn nhập hoặc biểu mẫu"
                              value={group.descriptionHtml || ''}
                              onChange={(value) => updateGroup(partIndex, groupIndex, { descriptionHtml: value })}
                            />
                          </div>

                          {group.type === 'multi_select_letters' ? (
                            <MultiSelectEditor
                              answerKey={answerKey}
                              group={group}
                              onAnswerNumberChange={moveAnswer}
                              onAnswerKeyChange={setAnswerKey}
                              onChange={(patch) => updateGroup(partIndex, groupIndex, patch)}
                              onOptionChange={(optionIndex, value) => updateOption(partIndex, groupIndex, 0, optionIndex, value, true)}
                              skill={skill}
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

            <footer className="flex items-center justify-between gap-3 border-t border-[#eadcdc] bg-[#fffafb] px-6 py-4">
              <p className="text-sm text-[#584140]">{buildFooterSummary(config, skill, questionCount)}</p>
              <div className="flex gap-3">
                <button className="rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-semibold text-[#730014]" onClick={() => setOpen(false)} type="button">Hủy</button>
                <button className="rounded-xl bg-[#4b0009] px-5 py-3 text-sm font-semibold text-white" onClick={save} type="button">{builderLabels.saveButton}</button>
              </div>
            </footer>
          </div>
        </div>
      ) : null}
    </>
  );
}

function WritingConfigEditor({ config, onChange }) {
  const tasks = config.tasks || [];
  const updateConfig = (patch) => onChange((current) => ({ ...current, ...patch }));
  const updateTask = (taskIndex, patch) => onChange((current) => ({
    ...current,
    tasks: (current.tasks || []).map((task, index) => (index === taskIndex ? { ...task, ...patch } : task)),
  }));

  return (
    <div className="space-y-5">
      <section className="grid gap-4 rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-5 md:grid-cols-2">
        <Field label="Tên hiển thị của đề" value={config.title} onChange={(value) => updateConfig({ title: value })} />
        <Field label="Mã đề" value={config.key} onChange={(value) => updateConfig({ key: value })} />
        <Field label="Thời gian (phút)" type="number" value={config.durationMinutes} onChange={(value) => updateConfig({ durationMinutes: Number(value) })} />
      </section>

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

          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <Field label="Ảnh minh họa hoặc biểu đồ" value={task.imageUrl || ''} onChange={(value) => updateTask(taskIndex, { imageUrl: value })} />
            <Field label="Tóm tắt yêu cầu" value={task.summary || ''} onChange={(value) => updateTask(taskIndex, { summary: value })} />
          </div>
          <div className="mt-4">
            <TextAreaField
              label="Prompt"
              value={(task.promptParagraphs || []).join('\n')}
              onChange={(value) => updateTask(taskIndex, {
                promptParagraphs: value.split('\n').map((line) => line.trim()).filter(Boolean),
                question: value.split('\n').map((line) => line.trim()).filter(Boolean).join('\n'),
              })}
            />
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <TextAreaField label="Rubric" value={task.rubric || ''} onChange={(value) => updateTask(taskIndex, { rubric: value })} />
            <TextAreaField label="Sample answer" value={task.sampleAnswer || ''} onChange={(value) => updateTask(taskIndex, { sampleAnswer: value })} />
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

function SpeakingConfigEditor({ config, onChange }) {
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
        <Field label="Tên hiển thị của đề" value={config.title} onChange={(value) => updateConfig({ title: value })} />
        <Field label="Mã đề" value={config.key} onChange={(value) => updateConfig({ key: value })} />
        <Field label="Thời gian (phút)" type="number" value={config.durationMinutes} onChange={(value) => updateConfig({ durationMinutes: Number(value) })} />
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
                <div className="mt-3 grid gap-3 md:grid-cols-2">
                  <Field label="Tiêu đề cue card" value={part.cueCardTitle || ''} onChange={(value) => updatePart(variantIndex, partIndex, { cueCardTitle: value })} />
                  <TextAreaField
                    label="Cue card bullets"
                    value={(part.cueCardBullets || []).join('\n')}
                    onChange={(value) => updatePart(variantIndex, partIndex, {
                      cueCardBullets: value.split('\n').map((line) => line.trim()).filter(Boolean),
                    })}
                  />
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
                      <Field
                        label="Liên kết video minh họa"
                        value={prompt.videoUrl || ''}
                        onChange={(value) => updatePart(variantIndex, partIndex, {
                          prompts: (part.prompts || []).map((item, index) => (
                            index === promptIndex ? { ...item, videoUrl: value } : item
                          )),
                        })}
                      />
                      <Field
                        label="Liên kết audio câu hỏi"
                        value={prompt.audioUrl || ''}
                        onChange={(value) => updatePart(variantIndex, partIndex, {
                          prompts: (part.prompts || []).map((item, index) => (
                            index === promptIndex ? { ...item, audioUrl: value } : item
                          )),
                        })}
                      />
                    </div>
                  ))}
                </div>
                <div className="mt-3">
                  <TextAreaField label="Rubric" value={part.rubric || ''} onChange={(value) => updatePart(variantIndex, partIndex, { rubric: value })} />
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

function QuestionEditor({ answer, groupType, onAnswerChange, onChange, onNumberChange, onOptionChange, onRemove, question, skill }) {
  const evidenceLabel = skill === 'READING' ? 'Evidence đoạn/dòng' : 'Mốc audio/transcript';
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
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <Field label="Nội dung sau ô trả lời" value={question.promptAfter || ''} onChange={(value) => onChange({ promptAfter: value })} />
          <div>
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
      <div className="mt-3 grid gap-3 md:grid-cols-2">
        <Field label={evidenceLabel} value={question.evidence || ''} onChange={(value) => onChange({ evidence: value })} />
        <TextAreaField label="Giải thích" value={question.explanation || ''} onChange={(value) => onChange({ explanation: value })} />
      </div>
    </div>
  );
}

function MultiSelectEditor({ answerKey, group, onAnswerKeyChange, onAnswerNumberChange, onChange, onOptionChange, skill }) {
  const number = Number(group.questionNumbers?.[0] || 1);
  const answer = Array.isArray(answerKey[String(number)]) ? answerKey[String(number)] : [];
  const evidenceLabel = skill === 'READING' ? 'Evidence đoạn/dòng' : 'Mốc audio/transcript';
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
      <div className="mt-3 grid gap-3 md:grid-cols-2">
        <Field label={evidenceLabel} value={group.evidence || ''} onChange={(value) => onChange({ evidence: value })} />
        <TextAreaField label="Giải thích" value={group.explanation || ''} onChange={(value) => onChange({ explanation: value })} />
      </div>
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
