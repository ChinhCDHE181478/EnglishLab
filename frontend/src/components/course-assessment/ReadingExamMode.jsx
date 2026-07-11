import { useEffect, useMemo, useRef, useState } from 'react';
import BrandedSelect from '../ui/BrandedSelect';
import ExamSectionChangeDialog from './ExamSectionChangeDialog';

const formatTimer = (seconds) => {
  const safeSeconds = Math.max(0, Number(seconds) || 0);
  const minutes = String(Math.floor(safeSeconds / 60)).padStart(2, '0');
  const remainder = String(safeSeconds % 60).padStart(2, '0');
  return `${minutes}:${remainder}`;
};

const flattenQuestionNumbers = (parts = []) => parts.flatMap((part) => (
  (part.questionGroups || []).flatMap((group) => (
    group.questionNumbers || (group.questions || []).map((question) => question.number)
  ))
));

const buildInitialAnswers = (parts = []) => {
  const answers = {};
  parts.forEach((part) => {
    (part.questionGroups || []).forEach((group) => {
      if (group.type === 'multi_select_letters') {
        answers[group.questionNumbers?.join('-') || 'multi'] = [];
        return;
      }
      (group.questions || []).forEach((question) => {
        answers[String(question.number)] = '';
      });
    });
  });
  return answers;
};

const answerIsFilled = (value) => (Array.isArray(value) ? value.length > 0 : String(value || '').trim().length > 0);

const isQuestionAnswered = (part, questionNumber, answers) => {
  const groups = Array.isArray(part?.questionGroups) ? part.questionGroups : [];
  const multiGroup = groups.find((group) => (
    group.type === 'multi_select_letters' && (group.questionNumbers || []).includes(questionNumber)
  ));

  if (multiGroup) {
    const groupKey = multiGroup.questionNumbers?.join('-') || 'multi';
    const selectedLetters = Array.isArray(answers[groupKey]) ? answers[groupKey] : [];
    const questionIndex = (multiGroup.questionNumbers || []).indexOf(questionNumber);
    return questionIndex !== -1 && selectedLetters.length > questionIndex;
  }

  return answerIsFilled(answers[String(questionNumber)]);
};

export default function ReadingExamMode({
  assessment,
  config,
  initialAnswers = null,
  isLocked = false,
  submitting = false,
  onClose,
  onSubmit,
  preserveFullscreenOnUnmount = false,
  submitLabel = 'Nộp bài',
}) {
  const parts = Array.isArray(config?.parts) ? config.parts : [];
  const [activePartKey, setActivePartKey] = useState(parts[0]?.key || 'part_1');
  const [answers, setAnswers] = useState(() => initialAnswers || buildInitialAnswers(parts));
  const [remainingSeconds, setRemainingSeconds] = useState(() => Math.max(1, Number(config?.durationMinutes || assessment?.timeLimitMinutes || 60)) * 60);
  const [submissionPending, setSubmissionPending] = useState(false);
  const [warning, setWarning] = useState(null);
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);
  const [pendingPartChange, setPendingPartChange] = useState(null);
  const [violations, setViolations] = useState([]);
  const rootRef = useRef(null);
  const submittedRef = useRef(false);
  const intentionalExitRef = useRef(false);

  const activePart = parts.find((part) => part.key === activePartKey) || parts[0] || null;
  const allQuestionNumbers = useMemo(() => flattenQuestionNumbers(parts), [parts]);
  const answeredCount = useMemo(() => {
    let total = 0;
    Object.values(answers).forEach((value) => {
      if (Array.isArray(value)) {
        total += value.length;
      } else if (answerIsFilled(value)) {
        total += 1;
      }
    });
    return Math.min(total, allQuestionNumbers.length);
  }, [answers, allQuestionNumbers.length]);

  useEffect(() => {
    if (isLocked || submitting || submissionPending) return undefined;
    const timer = window.setInterval(() => {
      setRemainingSeconds((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [isLocked, submissionPending, submitting]);

  useEffect(() => {
    if (remainingSeconds !== 0 || submittedRef.current || submitting || submissionPending || isLocked) return;
    submittedRef.current = true;
    handleSubmitExam(true);
  }, [remainingSeconds, submitting, submissionPending, isLocked]);

  useEffect(() => {
    intentionalExitRef.current = false;
    document.documentElement?.requestFullscreen?.().catch(() => {});
    return () => {
      if (!preserveFullscreenOnUnmount && document.fullscreenElement) {
        document.exitFullscreen?.().catch(() => {});
      }
    };
  }, [preserveFullscreenOnUnmount]);

  useEffect(() => {
    const pushExamState = () => {
      window.history.pushState({ englishlabReadingExam: true }, '', window.location.href);
    };
    const warn = (reason) => {
      const entry = {
        reason,
        at: new Date().toISOString(),
      };
      setViolations((current) => [...current, entry]);
      setWarning(entry);
    };
    const handleVisibility = () => {
      if (document.hidden) warn('Bạn vừa rời khỏi tab hoặc thu nhỏ cửa sổ trong lúc làm bài Reading.');
    };
    const handleBlur = () => {
      warn('Cửa sổ bài thi Reading đã mất focus.');
    };
    const handlePopState = () => {
      pushExamState();
      warn('Không thể quay lại trang khác trong lúc đang làm bài Reading.');
    };
    const handleFullscreen = () => {
      if (!document.fullscreenElement && !intentionalExitRef.current) {
        void restoreFullscreen();
        warn('Không thể thoát toàn màn hình trong lúc đang thi Reading.');
      }
    };
    const handleBeforeUnload = (event) => {
      event.preventDefault();
      event.returnValue = '';
    };
    const handleKeyDown = (event) => {
      const loweredKey = String(event.key || '').toLowerCase();
      const isBlockedShortcut = event.key === 'F5'
        || event.key === 'Escape'
        || (event.altKey && loweredKey === 'arrowleft')
        || (event.altKey && loweredKey === 'arrowright')
        || ((event.ctrlKey || event.metaKey) && ['r', 'w', 't', 'n', 'l', 'c', 'v', 'x', 'a', 'p', 's', 'u'].includes(loweredKey));
      if (!isBlockedShortcut) return;
      event.preventDefault();
      event.stopPropagation();
      warn(
        event.key === 'Escape'
          ? 'Bạn không thể dùng phím Esc để thoát toàn màn hình trong khi đang thi Reading.'
          : 'Một thao tác điều hướng hoặc sao chép ngoài bài thi Reading vừa bị chặn.'
      );
    };
    pushExamState();
    document.addEventListener('visibilitychange', handleVisibility);
    document.addEventListener('fullscreenchange', handleFullscreen);
    window.addEventListener('blur', handleBlur);
    window.addEventListener('beforeunload', handleBeforeUnload);
    window.addEventListener('popstate', handlePopState);
    window.addEventListener('keydown', handleKeyDown, true);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibility);
      document.removeEventListener('fullscreenchange', handleFullscreen);
      window.removeEventListener('blur', handleBlur);
      window.removeEventListener('beforeunload', handleBeforeUnload);
      window.removeEventListener('popstate', handlePopState);
      window.removeEventListener('keydown', handleKeyDown, true);
    };
  }, []);

  const restoreFullscreen = async () => {
    if (document.fullscreenElement) return;
    await document.documentElement?.requestFullscreen?.().catch(() => {});
  };

  const handleCloseExam = async () => {
    intentionalExitRef.current = true;
    if (document.fullscreenElement) {
      await document.exitFullscreen?.().catch(() => {});
    }
    onClose?.();
  };

  const updateAnswer = (key, value) => {
    if (isLocked || submitting || submissionPending) return;
    setAnswers((current) => ({ ...current, [String(key)]: value }));
  };

  const toggleLetter = (groupKey, letter, maxSelections) => {
    if (isLocked || submitting || submissionPending) return;
    setAnswers((current) => {
      const currentValues = Array.isArray(current[groupKey]) ? current[groupKey] : [];
      if (currentValues.includes(letter)) {
        return { ...current, [groupKey]: currentValues.filter((value) => value !== letter) };
      }
      if (currentValues.length >= maxSelections) return current;
      return { ...current, [groupKey]: [...currentValues, letter] };
    });
  };

  const buildPayload = (autoSubmitted = false) => {
    const responses = [];
    parts.forEach((part) => {
      (part.questionGroups || []).forEach((group) => {
        if (group.type === 'multi_select_letters') {
          const groupKey = group.questionNumbers?.join('-') || 'multi';
          responses.push({
            questionNumber: groupKey,
            part: part.key,
            answerType: group.type,
            answer: (answers[groupKey] || []).join(', '),
          });
          return;
        }
        (group.questions || []).forEach((question) => {
          responses.push({
            questionNumber: question.number,
            part: part.key,
            answerType: group.type,
            answer: String(answers[String(question.number)] || '').trim(),
          });
        });
      });
    });

    return {
      fullscreenExitCount: violations.filter((item) => String(item.reason || '').toLowerCase().includes('toàn màn hình')).length,
      tabSwitchCount: violations.filter((item) => !String(item.reason || '').toLowerCase().includes('toàn màn hình')).length,
      objectiveAnswersJson: JSON.stringify({
        mode: 'ielts_reading_exam',
        testKey: config?.key,
        testTitle: config?.title,
        autoSubmitted,
        remainingSeconds,
        answeredCount,
        totalQuestions: allQuestionNumbers.length,
        violations,
        responses,
      }),
    };
  };

  const handleSubmitExam = async (autoSubmitted = false) => {
    if (isLocked || submitting || submissionPending) return;
    setSubmissionPending(true);
    try {
      await onSubmit(buildPayload(autoSubmitted));
    } finally {
      setSubmissionPending(false);
    }
  };

  const requestPartChange = (part) => {
    if (part.key === activePartKey) return;
    const questionNumbers = flattenQuestionNumbers([activePart]);
    const answered = questionNumbers.filter((number) => isQuestionAnswered(activePart, number, answers)).length;
    const missingCount = Math.max(0, questionNumbers.length - answered);
    if (missingCount > 0) {
      setPendingPartChange({ part, missingCount });
      return;
    }
    setActivePartKey(part.key);
  };

  const renderQuestion = (group, question) => {
    if (group.type === 'select') {
      return (
        <label key={question.number} className="grid gap-3 rounded-2xl border border-[#ecd7db] bg-white p-4 sm:grid-cols-[92px_1fr]">
          <BrandedSelect
            buttonClassName="min-h-[44px] rounded-full border-[#dfbfbd] bg-[#fffafb] px-3 py-2 text-sm font-bold text-[#8a0018] shadow-none"
            onChange={(event) => updateAnswer(question.number, event.target.value)}
            options={(group.options || []).map((option) => ({ label: option, value: option }))}
            placeholder={`Q${question.number}`}
            value={answers[String(question.number)] || ''}
          />
          <span className="self-center text-sm leading-6 text-[#40292a]">{question.prompt}</span>
        </label>
      );
    }

    if (group.type === 'single_choice') {
      return (
        <div key={question.number} className="rounded-2xl border border-[#ecd7db] bg-white p-4">
          <p className="text-sm font-extrabold text-[#4b0009]">{question.number}. {question.prompt}</p>
          <div className="mt-3 grid gap-2">
            {(question.options || []).map((option) => (
              <label key={option.value} className="block cursor-pointer">
                {(() => {
                  const checked = answers[String(question.number)] === option.value;
                  return (
                    <>
                <input
                  checked={checked}
                  className="peer sr-only"
                  name={`q-${question.number}`}
                  onChange={() => updateAnswer(question.number, option.value)}
                  type="radio"
                />
                <span className={`flex gap-3 rounded-2xl border px-4 py-3 text-sm text-[#40292a] transition hover:border-[#d9b2ba] ${checked ? 'border-[#8a0018] bg-[#fff0f1] shadow-[0_10px_20px_rgba(138,0,24,0.08)]' : 'border-[#ecd7db] bg-[#fff7f7]'}`}>
                  <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border transition ${checked ? 'border-[#8a0018] bg-[#8a0018]' : 'border-[#d7b6bd] bg-white'}`}>
                    <span className={`h-2 w-2 rounded-full bg-white transition ${checked ? 'opacity-100' : 'opacity-0'}`} />
                  </span>
                  <span><b>{option.value}.</b> {option.label}</span>
                </span>
                    </>
                  );
                })()}
              </label>
            ))}
          </div>
        </div>
      );
    }

    return (
      <label key={question.number} className="block rounded-2xl border border-[#ecd7db] bg-white p-4 text-sm leading-7 text-[#40292a]">
        <span>{question.promptBefore}</span>
        <input
          className="mx-2 inline-block min-w-[180px] rounded-full border border-[#dfbfbd] bg-[#fffafb] px-4 py-2 font-semibold text-[#8a0018] outline-none focus:border-[#8a0018]"
          onChange={(event) => updateAnswer(question.number, event.target.value)}
          placeholder={`Q${question.number}`}
          value={answers[String(question.number)] || ''}
        />
        <span>{question.promptAfter}</span>
      </label>
    );
  };

  const renderGroup = (group) => {
    if (group.type === 'multi_select_letters') {
      const groupKey = group.questionNumbers?.join('-') || 'multi';
      const selectedLetters = Array.isArray(answers[groupKey]) ? answers[groupKey] : [];
      return (
        <section key={group.title} className="space-y-3">
          <div>
            <h3 className="font-['Manrope'] text-xl font-extrabold text-[#8a0018]">{group.title}</h3>
            <p className="mt-2 text-sm italic leading-6 text-[#6a4a46]">{group.instructions}</p>
          </div>
          <div className="grid gap-2">
            {(group.options || []).map((option) => (
              <label key={option.value} className="block cursor-pointer">
                {(() => {
                  const checked = selectedLetters.includes(option.value);
                  return (
                    <>
                <input
                  checked={checked}
                  className="peer sr-only"
                  onChange={() => toggleLetter(groupKey, option.value, group.maxSelections || 5)}
                  type="checkbox"
                />
                <span className={`flex gap-3 rounded-2xl border p-4 text-sm leading-6 text-[#40292a] transition hover:border-[#d9b2ba] ${checked ? 'border-[#8a0018] bg-[#fff0f1] shadow-[0_10px_20px_rgba(138,0,24,0.08)]' : 'border-[#ecd7db] bg-white'}`}>
                  <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-[7px] border text-xs font-black transition ${checked ? 'border-[#8a0018] bg-[#8a0018] text-white' : 'border-[#d7b6bd] bg-white text-white'}`}>
                    <span className={`transition ${checked ? 'opacity-100' : 'opacity-0'}`}>✓</span>
                  </span>
                  <span><b>{option.value}.</b> {option.label}</span>
                </span>
                    </>
                  );
                })()}
              </label>
            ))}
          </div>
        </section>
      );
    }

    return (
      <section key={group.title} className="space-y-3">
        <div>
          <h3 className="font-['Manrope'] text-xl font-extrabold text-[#8a0018]">{group.title}</h3>
          <p className="mt-2 text-sm italic leading-6 text-[#6a4a46]">{group.instructions}</p>
        </div>
        {(group.questions || []).map((question) => renderQuestion(group, question))}
      </section>
    );
  };

  return (
    <div
      ref={rootRef}
      className="fixed inset-0 z-[120] flex flex-col bg-[#fcf7f5] text-[#2b1718]"
      onContextMenu={(event) => event.preventDefault()}
      onCopy={(event) => event.preventDefault()}
      onCut={(event) => event.preventDefault()}
      onPaste={(event) => event.preventDefault()}
    >
      <header className="flex min-h-[76px] flex-wrap items-center justify-between gap-4 border-b border-[#ead8d5] bg-white px-5 shadow-sm">
        <div>
          <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">Bài thi đọc EnglishLab</p>
          <h2 className="font-['Manrope'] text-lg font-extrabold text-[#341c1d]">{config?.title || assessment?.title}</h2>
          {config?.rules?.length ? <p className="mt-1 max-w-3xl text-xs leading-5 text-[#6f5a58]">{config.rules.join(' · ')}</p> : null}
        </div>
        <div className="flex items-center gap-4">
          <div className="rounded-full bg-[#fff0f1] px-5 py-2 text-xl font-black text-[#8a0018]">
            {formatTimer(remainingSeconds)}
          </div>
          <span className="rounded-full bg-[#8a0018] px-4 py-2 text-xs font-bold text-white">
            Vi phạm: {violations.length}
          </span>
          <button
            className="rounded-full border border-[#dfbfbd] px-5 py-2 text-sm font-bold text-[#8a0018] hover:bg-[#fff0f1]"
            onClick={() => setExitConfirmOpen(true)}
            type="button"
          >
            Thoát
          </button>
          <button
            className="rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-3 text-sm font-black text-white shadow-[0_14px_28px_rgba(138,0,24,0.24)] hover:brightness-105 disabled:opacity-60"
            disabled={isLocked || submitting || submissionPending}
            onClick={() => handleSubmitExam(false)}
            type="button"
          >
            {submitting || submissionPending ? 'Đang lưu...' : submitLabel}
          </button>
        </div>
      </header>

      <main className="grid min-h-0 flex-1 grid-cols-1 overflow-hidden lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <section className="min-h-0 overflow-y-auto border-r border-[#ead8d5] bg-[radial-gradient(circle_at_top,_rgba(138,0,24,0.07),_transparent_48%),linear-gradient(180deg,#fffafa,#fcf7f5)] px-6 py-8">
          <p className="text-sm font-black uppercase tracking-[0.16em] text-[#8a0018]">Part {activePart?.partNumber}</p>
          <h1 className="mt-2 font-['Manrope'] text-4xl font-black text-[#341c1d]">{activePart?.title}</h1>
          <p className="mt-3 text-sm italic text-[#6a4a46]">You should spend about 20 minutes on {activePart?.questionRange}.</p>
          <article className="mt-7 rounded-[30px] bg-white p-6 shadow-[0_20px_60px_rgba(86,35,37,0.10)]">
            <h2 className="text-center font-['Manrope'] text-3xl font-black text-[#341c1d]">{activePart?.passage?.title}</h2>
            <div className="mt-6 space-y-5 text-[15px] leading-8 text-[#1e3025]">
              {(activePart?.passage?.paragraphs || []).map((paragraph, index) => (
                <p key={`${paragraph.label}-${index}`}>
                  {paragraph.label ? <b>{paragraph.label}. </b> : null}
                  {paragraph.text}
                </p>
              ))}
            </div>
          </article>
        </section>

        <section className="min-h-0 overflow-y-auto bg-white px-6 py-8">
          <div className="mx-auto max-w-3xl space-y-8">
            {(activePart?.questionGroups || []).map(renderGroup)}
          </div>
        </section>
      </main>

      <footer className="grid gap-3 border-t border-[#ead8d5] bg-white px-5 py-3 lg:grid-cols-3">
        {parts.map((part) => {
          const partQuestionNumbers = flattenQuestionNumbers([part]);
          const partAnswered = partQuestionNumbers.filter((number) => isQuestionAnswered(part, number, answers)).length;
          return (
            <button
              key={part.key}
              className={`rounded-2xl border px-4 py-3 text-left transition ${part.key === activePartKey ? 'border-[#8a0018] bg-[#fff0f1]' : 'border-[#ecd7db] bg-white hover:bg-[#fff7f7]'}`}
              onClick={() => requestPartChange(part)}
              type="button"
            >
              <span className="font-black text-[#341c1d]">Part {part.partNumber}</span>
              <span className="ml-2 text-sm font-semibold text-[#6f5a58]">
                {partAnswered} of {partQuestionNumbers.length} questions
              </span>
              <div className="mt-2 flex flex-wrap gap-1">
                {partQuestionNumbers.map((number) => (
                  <span
                    key={number}
                    className={`flex h-7 w-7 items-center justify-center rounded-full text-[11px] font-bold ${isQuestionAnswered(part, number, answers) ? 'bg-[#8a0018] text-white' : 'bg-[#f6ecea] text-[#8c716f]'}`}
                  >
                    {number}
                  </span>
                ))}
              </div>
            </button>
          );
        })}
      </footer>

      {warning ? (
        <div className="fixed inset-0 z-[130] flex items-center justify-center bg-[#261112]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#b26a00]">Cảnh báo bài thi</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">Hãy quay lại bài Reading</h3>
            <p className="mt-3 text-sm leading-7 text-[#584140]">{warning.reason}</p>
            <button
              className="mt-5 w-full rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white"
              onClick={async () => {
                await restoreFullscreen();
                setWarning(null);
              }}
              type="button"
            >
              Tiếp tục làm bài
            </button>
          </div>
        </div>
      ) : null}

      {exitConfirmOpen ? (
        <div className="fixed inset-0 z-[130] flex items-center justify-center bg-[#261112]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8a0018]">Thoát chế độ thi?</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">Bài Reading hiện chưa được nộp</h3>
            <p className="mt-3 text-sm leading-7 text-[#584140]">
              Nếu bạn thoát bây giờ, EnglishLab sẽ quay về màn hình khóa học. Bạn có thể mở lại bài thi, nhưng lần làm hiện tại chưa được nộp.
            </p>
            <div className="mt-5 flex gap-3">
              <button className="flex-1 rounded-2xl border border-[#dfbfbd] px-5 py-3 text-sm font-bold text-[#8a0018]" onClick={() => setExitConfirmOpen(false)} type="button">
                Ở lại
              </button>
              <button className="flex-1 rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white" onClick={handleCloseExam} type="button">
                Thoát
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {pendingPartChange ? (
        <ExamSectionChangeDialog
          currentLabel={`Part ${activePart?.partNumber || ''}`}
          missingCount={pendingPartChange.missingCount}
          onCancel={() => setPendingPartChange(null)}
          onConfirm={() => {
            setActivePartKey(pendingPartChange.part.key);
            setPendingPartChange(null);
          }}
          targetLabel={`Part ${pendingPartChange.part.partNumber || ''}`}
        />
      ) : null}
    </div>
  );
}
