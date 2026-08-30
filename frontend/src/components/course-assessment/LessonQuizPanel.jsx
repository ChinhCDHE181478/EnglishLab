import { useEffect, useMemo, useState } from 'react';
import { CheckCircle2, ChevronLeft, ChevronRight } from 'lucide-react';
import RichTextHtml from '../content-manager/RichTextHtml';

const safeParse = (value, fallback = {}) => {
  try {
    const parsed = JSON.parse(String(value || ''));
    return parsed && typeof parsed === 'object' ? parsed : fallback;
  } catch {
    return fallback;
  }
};

const flattenQuestions = (assessment) => {
  const config = safeParse(assessment?.uiConfigJson);
  return (config.parts || []).flatMap((part, partIndex) => (
    (part.questionGroups || []).flatMap((group, groupIndex) => {
      if (group.type === 'multi_select_letters') {
        return (group.questionNumbers || []).map((number) => ({
          key: `${part.key || partIndex}-${groupIndex}-${number}`,
          number: String(number),
          part: part.key || `part_${partIndex + 1}`,
          partTitle: part.title || `Phần ${partIndex + 1}`,
          prompt: group.instructions || group.title || `Câu ${number}`,
          descriptionHtml: group.descriptionHtml || group.passageHtml || '',
          answerType: 'multi_select_letters',
          maxSelections: Number(group.maxSelections || 2),
          options: group.options || [],
        }));
      }
      return (group.questions || []).map((question, questionIndex) => ({
        key: `${part.key || partIndex}-${groupIndex}-${question.number || questionIndex + 1}`,
        number: String(question.number || questionIndex + 1),
        part: part.key || `part_${partIndex + 1}`,
        partTitle: part.title || `Phần ${partIndex + 1}`,
        prompt: question.prompt || question.promptBefore || group.instructions || group.title || `Câu ${question.number || questionIndex + 1}`,
        promptAfter: question.promptAfter || '',
        descriptionHtml: group.descriptionHtml || group.passageHtml || '',
        imageUrl: question.imageUrl || '',
        audioUrl: question.audioUrl || group.audioUrl || '',
        answerType: group.type || 'text',
        options: question.options?.length ? question.options : group.options || [],
      }));
    })
  ));
};

const readInitialAnswers = (assessment) => {
  const parsed = safeParse(assessment?.latestSubmission?.objectiveAnswersJson);
  return (parsed.responses || []).reduce((result, response) => ({
    ...result,
    [String(response.questionNumber)]: String(response.answer || ''),
  }), {});
};

export default function LessonQuizPanel({ assessment, isLocked = false, onMoveStep, onSubmit }) {
  const questions = useMemo(() => flattenQuestions(assessment), [assessment]);
  const [answers, setAnswers] = useState(() => readInitialAnswers(assessment));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(assessment?.latestSubmission || null);

  useEffect(() => {
    setAnswers(readInitialAnswers(assessment));
    setResult(assessment?.latestSubmission || null);
    setError('');
  }, [assessment?.id, assessment?.latestSubmission?.id]);

  const updateAnswer = (questionNumber, value) => {
    setAnswers((current) => ({ ...current, [String(questionNumber)]: value }));
  };

  const toggleMultiAnswer = (question, optionValue) => {
    const selected = String(answers[question.number] || '').split(',').map((item) => item.trim()).filter(Boolean);
    const next = selected.includes(optionValue)
      ? selected.filter((item) => item !== optionValue)
      : selected.length < question.maxSelections ? [...selected, optionValue] : selected;
    updateAnswer(question.number, next.join(','));
  };

  const submitQuiz = async () => {
    const missing = questions.find((question) => !String(answers[question.number] || '').trim());
    if (missing) {
      setError(`Hãy trả lời câu ${missing.number} trước khi nộp bài.`);
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const response = await onSubmit(assessment.id, {
        objectiveAnswersJson: JSON.stringify({
          responses: questions.map((question) => ({
            questionNumber: question.number,
            part: question.part,
            answerType: question.answerType,
            answer: answers[question.number] || '',
          })),
        }),
      });
      setResult(response);
    } catch (submissionError) {
      setError(submissionError?.message || 'Không thể nộp bài trắc nghiệm. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  const feedback = safeParse(result?.aiFeedbackJson);
  const completed = Boolean(result);

  return (
    <section className="rounded-[30px] border border-[#eadcdc] bg-white p-5 shadow-[0_18px_50px_rgba(75,0,9,0.08)] sm:p-7">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-[#f0e3e4] pb-5">
        <div>
          <p className="text-[11px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Trắc nghiệm bài học</p>
          <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{assessment?.title}</h2>
          {assessment?.description ? <RichTextHtml className="mt-2 text-sm leading-6 text-[#584140]" value={assessment.description} /> : null}
        </div>
        <div className="rounded-2xl bg-[#fff0f1] px-4 py-3 text-sm font-bold text-[#730014]">
          {questions.length} câu · {assessment?.timeLimitMinutes
            ? `${assessment.timeLimitMinutes} phút`
            : 'Không giới hạn thời gian'}
        </div>
      </div>

      {!questions.length ? (
        <div className="mt-6 rounded-2xl border border-[#f0d4d7] bg-[#fff8f8] p-5 text-sm font-semibold text-[#93000a]">
          Bài trắc nghiệm chưa có câu hỏi. Hãy liên hệ người quản lý nội dung.
        </div>
      ) : (
        <div className="mt-6 space-y-5">
          {questions.map((question, index) => {
            const selectedValues = String(answers[question.number] || '').split(',').filter(Boolean);
            return (
              <article key={question.key} className="rounded-3xl border border-[#eadcdc] bg-[#fffdfc] p-5">
                <div className="flex items-start gap-3">
                  <span className="inline-flex h-9 min-w-9 items-center justify-center rounded-xl bg-[#730014] px-2 text-sm font-extrabold text-white">{index + 1}</span>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">{question.partTitle}</p>
                    {question.descriptionHtml ? <RichTextHtml className="mt-3 text-sm leading-6 text-[#584140]" value={question.descriptionHtml} /> : null}
                    <p className="mt-2 text-base font-bold leading-7 text-[#2b2828]">{question.prompt}</p>
                    {question.imageUrl ? <img alt="Minh họa câu hỏi" className="mt-4 max-h-80 rounded-2xl object-contain" src={question.imageUrl} /> : null}
                    {question.audioUrl ? <audio className="mt-4 w-full" controls src={question.audioUrl} /> : null}

                    {question.answerType === 'text' ? (
                      <input
                        className="mt-4 w-full rounded-2xl border border-[#dfbfbd] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014] focus:ring-4 focus:ring-[#730014]/10"
                        disabled={completed || isLocked}
                        onChange={(event) => updateAnswer(question.number, event.target.value)}
                        placeholder="Nhập câu trả lời"
                        value={answers[question.number] || ''}
                      />
                    ) : (
                      <div className="mt-4 grid gap-3 sm:grid-cols-2">
                        {(question.options || []).map((option, optionIndex) => {
                          const value = String(option.value || String.fromCharCode(65 + optionIndex));
                          const checked = question.answerType === 'multi_select_letters'
                            ? selectedValues.includes(value)
                            : String(answers[question.number] || '') === value;
                          return (
                            <button
                              aria-pressed={checked}
                              className={`min-h-12 rounded-2xl border px-4 py-3 text-left text-sm font-semibold transition ${checked ? 'border-[#730014] bg-[#fff0f1] text-[#730014] ring-2 ring-[#730014]/10' : 'border-[#eadcdc] bg-white text-[#584140] hover:border-[#d2aaa8]'}`}
                              disabled={completed || isLocked}
                              key={value}
                              onClick={() => question.answerType === 'multi_select_letters'
                                ? toggleMultiAnswer(question, value)
                                : updateAnswer(question.number, value)}
                              type="button"
                            >
                              <span className="mr-2 font-extrabold">{value}.</span>{option.label || option.text || value}
                            </button>
                          );
                        })}
                      </div>
                    )}
                    {question.promptAfter ? <p className="mt-3 text-sm text-[#584140]">{question.promptAfter}</p> : null}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {error ? <p className="mt-4 rounded-2xl bg-[#ffdad6] px-4 py-3 text-sm font-semibold text-[#93000a]" role="alert">{error}</p> : null}
      {result ? (
        <div className="mt-6 rounded-3xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-900">
          <div className="flex items-center gap-2 font-extrabold"><CheckCircle2 className="h-5 w-5" /> Đã chấm xong</div>
          <p className="mt-2 text-sm">Kết quả: {feedback.correctCount ?? result.aiScore ?? 0}/{feedback.totalQuestions ?? questions.length} câu đúng.</p>
          {feedback.summary ? <p className="mt-2 text-sm leading-6">{feedback.summary}</p> : null}
        </div>
      ) : null}

      <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-[#f0e3e4] pt-5">
        <button className="inline-flex min-h-11 items-center gap-2 rounded-2xl border border-[#dfbfbd] px-4 py-3 text-sm font-bold text-[#730014]" onClick={() => onMoveStep(-1)} type="button">
          <ChevronLeft className="h-4 w-4" /> Bài trước
        </button>
        {!completed ? (
          <button className="min-h-11 rounded-2xl bg-[#730014] px-6 py-3 text-sm font-extrabold text-white disabled:cursor-not-allowed disabled:opacity-45" disabled={!questions.length || submitting || isLocked} onClick={submitQuiz} type="button">
            {submitting ? 'Đang chấm...' : 'Nộp bài trắc nghiệm'}
          </button>
        ) : (
          <button className="inline-flex min-h-11 items-center gap-2 rounded-2xl bg-[#730014] px-5 py-3 text-sm font-bold text-white" onClick={() => onMoveStep(1)} type="button">
            Bài tiếp theo <ChevronRight className="h-4 w-4" />
          </button>
        )}
      </div>
    </section>
  );
}
