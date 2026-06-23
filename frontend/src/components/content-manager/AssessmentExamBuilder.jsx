import { useMemo, useState } from 'react';
import { FileJson, Plus, Trash2, X } from 'lucide-react';
import BrandedSelect from '../ui/BrandedSelect';

const GROUP_TYPES = [
  { label: 'Điền câu trả lời', value: 'text' },
  { label: 'Chọn một đáp án', value: 'single_choice' },
  { label: 'Chọn nhiều đáp án', value: 'multi_select_letters' },
];

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

const createConfig = (assessment) => ({
  version: 1,
  type: String(assessment.skill || '').toUpperCase() === 'READING'
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
  const fallback = createConfig(assessment);
  const parsed = safeParse(assessment.uiConfigJson, fallback);
  const safeConfig = { ...parsed };
  delete safeConfig.answerKey;
  return {
    ...fallback,
    ...safeConfig,
    parts: Array.isArray(safeConfig.parts) && safeConfig.parts.length ? safeConfig.parts : fallback.parts,
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

export default function AssessmentExamBuilder({ assessment, onChange }) {
  const isSupported = ['LISTENING', 'READING'].includes(String(assessment.skill || '').toUpperCase());
  const [open, setOpen] = useState(false);
  const [config, setConfig] = useState(() => normalizeConfig(assessment));
  const [answerKey, setAnswerKey] = useState(() => normalizeAnswerKey(assessment.objectiveAnswerKey, assessment.uiConfigJson));
  const [rawImport, setRawImport] = useState('');
  const [error, setError] = useState('');

  const questionCount = useMemo(
    () => (config.parts || []).reduce((sum, part) =>
      sum + (part.questionGroups || []).reduce((groupSum, group) =>
        groupSum + (group.type === 'multi_select_letters'
          ? (group.questionNumbers || []).length
          : (group.questions || []).length), 0), 0),
    [config.parts],
  );

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
    if (!parsed?.parts?.length) {
      setError('JSON không hợp lệ hoặc chưa có danh sách phần thi.');
      return;
    }
    const { answerKey: importedAnswerKey, ...safeConfig } = parsed;
    setConfig({ ...createConfig(assessment), ...safeConfig });
    if (importedAnswerKey && typeof importedAnswerKey === 'object') {
      setAnswerKey(importedAnswerKey);
    }
    setError('');
  };

  const save = () => {
    if (!String(config.title || '').trim()) {
      setError('Hãy nhập tên đề thi.');
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

  return (
    <>
      <div className="mt-4 rounded-2xl border border-[#dfbfbd] bg-white p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-semibold text-[#4b0009]">Nội dung đề thi</p>
            <p className="mt-1 text-sm text-[#584140]">
              {assessment.uiConfigJson ? `${questionCount} câu đã cấu hình` : 'Chưa biên soạn cấu trúc câu hỏi.'}
            </p>
          </div>
          <button
            className="rounded-xl bg-[#4b0009] px-4 py-3 text-sm font-semibold text-white"
            onClick={openBuilder}
            type="button"
          >
            Biên soạn nội dung đề
          </button>
        </div>
      </div>

      {open ? (
        <div className="fixed inset-0 z-[90] flex items-center justify-center bg-[#1a0004]/50 p-4 backdrop-blur-sm">
          <div className="flex max-h-[94dvh] w-full max-w-6xl flex-col overflow-hidden rounded-[30px] bg-white shadow-2xl">
            <header className="flex items-start justify-between gap-4 border-b border-[#eadcdc] px-6 py-5">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">Trình biên soạn đề thi</p>
                <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#4b0009]">{assessment.title || 'Bài thi mới'}</h2>
              </div>
              <button className="rounded-xl border border-[#eadcdc] p-2 text-[#730014]" onClick={() => setOpen(false)} type="button">
                <X className="h-5 w-5" />
              </button>
            </header>

            <div className="flex-1 overflow-y-auto px-6 py-6">
              {error ? <p className="mb-4 rounded-2xl bg-[#ffdad6] px-4 py-3 text-sm font-semibold text-[#93000a]">{error}</p> : null}

              <section className="grid gap-4 rounded-2xl border border-[#eadcdc] bg-[#fffafb] p-5 md:grid-cols-2">
                <Field label="Tên hiển thị của đề" value={config.title} onChange={(value) => setConfig((current) => ({ ...current, title: value }))} />
                <Field label="Mã đề" value={config.key} onChange={(value) => setConfig((current) => ({ ...current, key: value }))} />
                <Field label="Thời gian (phút)" type="number" value={config.durationMinutes} onChange={(value) => setConfig((current) => ({ ...current, durationMinutes: Number(value) }))} />
                {config.type === 'ielts_listening_exam' ? (
                  <Field label="Liên kết audio" value={config.audioUrl || ''} onChange={(value) => setConfig((current) => ({ ...current, audioUrl: value }))} />
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
            </div>

            <footer className="flex items-center justify-between gap-3 border-t border-[#eadcdc] bg-[#fffafb] px-6 py-4">
              <p className="text-sm text-[#584140]">{questionCount} câu · {config.parts.length} phần</p>
              <div className="flex gap-3">
                <button className="rounded-xl border border-[#dfbfbd] px-4 py-3 text-sm font-semibold text-[#730014]" onClick={() => setOpen(false)} type="button">Hủy</button>
                <button className="rounded-xl bg-[#4b0009] px-5 py-3 text-sm font-semibold text-white" onClick={save} type="button">Lưu cấu trúc đề</button>
              </div>
            </footer>
          </div>
        </div>
      ) : null}
    </>
  );
}

function QuestionEditor({ answer, groupType, onAnswerChange, onChange, onNumberChange, onOptionChange, onRemove, question }) {
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
