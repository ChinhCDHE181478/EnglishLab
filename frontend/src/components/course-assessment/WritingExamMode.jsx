import { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import ExamSectionChangeDialog from './ExamSectionChangeDialog';
import { getAssessmentSubmissionErrorMessage } from '../../utils/assessmentSubmissionError';
import { exitExamFullscreenWhenDetached } from '../../utils/examFullscreen';
import { sanitizeLessonHtml } from '../../utils/lessonRichText';

const formatTimer = (seconds) => {
  const safeSeconds = Math.max(0, Number(seconds) || 0);
  const minutes = String(Math.floor(safeSeconds / 60)).padStart(2, '0');
  const remainder = String(safeSeconds % 60).padStart(2, '0');
  return `${minutes}:${remainder}`;
};

const countWords = (value) => {
  const text = String(value || '').trim();
  if (!text) return 0;
  return text.split(/\s+/).filter(Boolean).length;
};

const buildInitialResponses = (tasks = [], initialSubmissionText = '') => {
  const initial = tasks.reduce((accumulator, task, index) => ({
    ...accumulator,
    [task.key || `task_${index + 1}`]: '',
  }), {});

  const raw = String(initialSubmissionText || '').trim();
  if (!raw) return initial;

  const blocks = raw.split(/\n\s*\n(?=\[.+?\])/g);
  blocks.forEach((block) => {
    const match = block.match(/^\[(.+?)\]\n([\s\S]*)$/);
    if (!match) return;
    const taskLabel = match[1];
    const content = match[2]?.trim() || '';
    const task = tasks.find((item, index) => (
      item.key === taskLabel
      || item.title === taskLabel
      || `Task ${index + 1}` === taskLabel
    ));
    if (task) {
      initial[task.key || `task_${tasks.indexOf(task) + 1}`] = content;
    }
  });

  return initial;
};

const renderPromptContent = (task) => {
  const paragraphs = Array.isArray(task?.promptParagraphs) ? task.promptParagraphs : [];
  if (paragraphs.length) {
    return paragraphs.map((paragraph, index) => (
      <p key={`${task?.key || 'task'}-paragraph-${index}`} className="text-[15px] leading-8 text-[#3d2728]">
        {paragraph}
      </p>
    ));
  }

  if (task?.promptHtml) {
    return (
      <div
        className="space-y-4 text-[15px] leading-8 text-[#3d2728]"
        dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(task.promptHtml) }}
      />
    );
  }

  return (
    <p className="text-[15px] leading-8 text-[#3d2728]">
      {task?.question || task?.prompt || 'Đề bài Writing sẽ hiển thị ở đây sau khi bạn gắn nội dung vào uiConfigJson.'}
    </p>
  );
};

export default function WritingExamMode({
  assessment,
  config,
  exitDestinationLabel = 'màn hình khóa học',
  initialSubmissionText = '',
  isLocked = false,
  submitting = false,
  onClose,
  onSubmit,
  preserveFullscreenOnUnmount = false,
  submitLabel = 'Nộp bài',
}) {
  const tasks = Array.isArray(config?.tasks) ? config.tasks : [];
  const [activeTaskKey, setActiveTaskKey] = useState(tasks[0]?.key || 'task_1');
  const [responses, setResponses] = useState(() => buildInitialResponses(tasks, initialSubmissionText));
  const [remainingSeconds, setRemainingSeconds] = useState(() => Math.max(1, Number(config?.durationMinutes || assessment?.timeLimitMinutes || 60)) * 60);
  const [submissionPending, setSubmissionPending] = useState(false);
  const [submissionError, setSubmissionError] = useState('');
  const [warning, setWarning] = useState(null);
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);
  const [pendingTaskChange, setPendingTaskChange] = useState(null);
  const [violations, setViolations] = useState([]);
  const rootRef = useRef(null);
  const submittedRef = useRef(false);
  const submissionInFlightRef = useRef(false);
  const intentionalExitRef = useRef(false);
  const fullscreenSessionStartedRef = useRef(false);

  const activeTask = tasks.find((task) => task.key === activeTaskKey) || tasks[0] || null;
  const activeTaskIndex = tasks.findIndex((task) => task.key === activeTaskKey);
  const totalWordCount = useMemo(
    () => Object.values(responses).reduce((sum, value) => sum + countWords(value), 0),
    [responses]
  );

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
    fullscreenSessionStartedRef.current = Boolean(document.fullscreenElement);
    return () => {
      if (!preserveFullscreenOnUnmount) {
        exitExamFullscreenWhenDetached(rootRef);
      }
    };
  }, [preserveFullscreenOnUnmount]);

  useEffect(() => {
    const pushExamState = () => {
      window.history.pushState({ englishlabWritingExam: true }, '', window.location.href);
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
      if (document.hidden) warn('Bạn vừa rời khỏi tab hoặc thu nhỏ cửa sổ trong lúc làm bài Writing.');
    };
    const handleBlur = () => {
      warn('Cửa sổ bài thi Writing đã mất focus.');
    };
    const handlePopState = () => {
      pushExamState();
      warn('Không thể quay lại trang khác trong lúc đang làm bài Writing.');
    };
    const handleFullscreen = () => {
      if (document.fullscreenElement) {
        fullscreenSessionStartedRef.current = true;
        return;
      }
      if (fullscreenSessionStartedRef.current && !intentionalExitRef.current) {
        fullscreenSessionStartedRef.current = false;
        warn('Không thể thoát toàn màn hình trong lúc đang thi Writing.');
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
        || ((event.ctrlKey || event.metaKey) && ['r', 'w', 't', 'n', 'l', 'p', 'u'].includes(loweredKey));
      if (!isBlockedShortcut) return;
      event.preventDefault();
      event.stopPropagation();
      warn('Một thao tác điều hướng ra ngoài bài thi Writing vừa bị chặn.');
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
    try {
      await document.documentElement?.requestFullscreen?.();
      fullscreenSessionStartedRef.current = Boolean(document.fullscreenElement);
    } catch {
      // Do not count a browser capability/permission failure as a violation.
    }
  };

  const handleCloseExam = async () => {
    intentionalExitRef.current = true;
    if (document.fullscreenElement) {
      await document.exitFullscreen?.().catch(() => {});
    }
    onClose?.();
  };

  const updateResponse = (taskKey, value) => {
    if (isLocked || submitting || submissionPending) return;
    setResponses((current) => ({ ...current, [taskKey]: value }));
  };

  const buildPayload = (autoSubmitted = false) => ({
    fullscreenExitCount: violations.filter((item) => String(item.reason || '').toLowerCase().includes('toàn màn hình')).length,
    tabSwitchCount: violations.filter((item) => !String(item.reason || '').toLowerCase().includes('toàn màn hình')).length,
    submittedText: tasks.map((task, index) => {
      const taskKey = task.key || `task_${index + 1}`;
      return [
        `[${task.title || `Task ${index + 1}`}]`,
        String(responses[taskKey] || '').trim(),
      ].join('\n');
    }).join('\n\n'),
    objectiveAnswersJson: JSON.stringify({
      mode: 'ielts_writing_exam',
      testKey: config?.key,
      testTitle: config?.title || assessment?.title,
      autoSubmitted,
      remainingSeconds,
      totalWordCount,
      violations,
      tasks: tasks.map((task, index) => {
        const taskKey = task.key || `task_${index + 1}`;
        return {
          key: taskKey,
          title: task.title || `Task ${index + 1}`,
          minimumWords: Number(task.minimumWords || task.minWords || 0),
          recommendedMinutes: Number(task.recommendedMinutes || task.durationMinutes || 0),
          wordCount: countWords(responses[taskKey]),
        };
      }),
    }),
  });

  const handleSubmitExam = async (autoSubmitted = false) => {
    if (isLocked || submitting || submissionPending || submissionInFlightRef.current) return;
    submissionInFlightRef.current = true;
    setSubmissionPending(true);
    setSubmissionError('');
    try {
      await onSubmit(buildPayload(autoSubmitted));
    } catch (error) {
      submittedRef.current = false;
      setSubmissionError(getAssessmentSubmissionErrorMessage(error));
    } finally {
      submissionInFlightRef.current = false;
      setSubmissionPending(false);
    }
  };

  const requestTaskChange = (task, index) => {
    const targetKey = task?.key || `task_${index + 1}`;
    if (!task || targetKey === activeTaskKey) return;
    const currentMinimum = Number(activeTask?.minimumWords || activeTask?.minWords || 0);
    const currentWords = countWords(responses[activeTaskKey]);
    if (currentWords < currentMinimum) {
      setPendingTaskChange({
        task,
        targetKey,
        missingCount: Math.max(1, currentMinimum - currentWords),
      });
      return;
    }
    setActiveTaskKey(targetKey);
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
      {submissionError ? (
        <div className="fixed bottom-5 left-1/2 z-[140] w-[min(92vw,680px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm font-bold text-red-700 shadow-xl" role="alert">
          {submissionError}
        </div>
      ) : null}
      <header className="flex min-h-[78px] flex-wrap items-center justify-between gap-4 border-b border-[#ead8d5] bg-white px-5 shadow-sm">
        <div>
          <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">EnglishLab Writing Exam</p>
          <h2 className="font-['Manrope'] text-lg font-extrabold text-[#341c1d]">{config?.title || assessment?.title}</h2>
        </div>
        <div className="flex items-center gap-3">
          <div className="rounded-full bg-[#fff0f1] px-5 py-2 text-xl font-black text-[#8a0018] shadow-[0_10px_24px_rgba(138,0,24,0.10)]">
            {formatTimer(remainingSeconds)}
          </div>
          <span className="rounded-full bg-[#8a0018] px-4 py-2 text-xs font-bold text-white">
            Vi phạm: {violations.length}
          </span>
          <button
            className="rounded-full border border-[#8a0018]/20 px-5 py-2 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
            onClick={() => setExitConfirmOpen(true)}
            type="button"
          >
            Thoát
          </button>
          <button
            className="rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-3 text-sm font-black text-white shadow-[0_14px_28px_rgba(138,0,24,0.24)] transition hover:brightness-105 disabled:opacity-60"
            disabled={isLocked || submitting || submissionPending}
            onClick={() => handleSubmitExam(false)}
            type="button"
          >
            {submitting || submissionPending ? 'Đang lưu...' : submitLabel}
          </button>
        </div>
      </header>

      <main className="grid min-h-0 flex-1 grid-cols-1 overflow-hidden xl:grid-cols-[minmax(0,1fr)_minmax(420px,0.95fr)]">
        <section className="min-h-0 overflow-y-auto border-r border-[#ead8d5] bg-[radial-gradient(circle_at_top,_rgba(138,0,24,0.07),_transparent_48%),linear-gradient(180deg,#fffafa,#fcf7f5)] px-6 py-8">
          <div className="mx-auto max-w-4xl">
            <p className="text-sm font-black uppercase tracking-[0.16em] text-[#8a0018]">
              {activeTask?.title || 'Writing task'}
            </p>
            <h1 className="mt-2 font-['Manrope'] text-4xl font-black text-[#341c1d]">
              {activeTask?.heading || activeTask?.title || 'Writing Prompt'}
            </h1>
            <div className="mt-4 flex flex-wrap gap-2 text-xs font-bold uppercase tracking-[0.14em] text-[#8c716f]">
              <span>Gợi ý: {activeTask?.recommendedMinutes || activeTask?.durationMinutes || 20} phút</span>
              <span>•</span>
              <span>Tối thiểu {activeTask?.minimumWords || activeTask?.minWords || 150} từ</span>
            </div>

            <article className="mt-7 rounded-[30px] bg-white p-6 shadow-[0_20px_60px_rgba(86,35,37,0.10)]">
              <div className="space-y-5">
                {renderPromptContent(activeTask)}
              </div>

              {activeTask?.imageUrl ? (
                <div className="mt-6 overflow-hidden rounded-[24px] border border-[#ead8d5] bg-[#fffdfc] p-3">
                  <img
                    alt={activeTask?.title || 'Writing reference'}
                    className="mx-auto max-h-[640px] w-auto max-w-full rounded-[18px] object-contain"
                    src={activeTask.imageUrl}
                  />
                </div>
              ) : null}
            </article>
          </div>
        </section>

        <section className="min-h-0 overflow-y-auto bg-white px-6 py-8">
          <div className="mx-auto flex h-full max-w-3xl flex-col">
            <div className="rounded-[28px] border border-[#ead8d5] bg-[#fffdfc] p-4 shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">Bài làm hiện tại</p>
                  <h3 className="mt-1 font-['Manrope'] text-2xl font-black text-[#341c1d]">{activeTask?.title || 'Task'}</h3>
                </div>
                <div className="rounded-full bg-[#fff0f1] px-4 py-2 text-sm font-extrabold text-[#8a0018]">
                  {countWords(responses[activeTask?.key || 'task_1'])} từ
                </div>
              </div>
              <textarea
                className="mt-4 min-h-[62vh] w-full rounded-[24px] border border-[#dfbfbd]/60 bg-white px-5 py-4 text-[15px] leading-8 text-[#2b1718] outline-none transition focus:border-[#8a0018]"
                onChange={(event) => updateResponse(activeTask?.key || 'task_1', event.target.value)}
                placeholder="Nhập bài viết của bạn tại đây..."
                readOnly={isLocked || submitting}
                spellCheck={false}
                value={responses[activeTask?.key || 'task_1'] || ''}
              />
              <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-[#6f5a58]">
                <span>Tổng số từ cả bài thi: {totalWordCount}</span>
                <span>
                  Mục tiêu task này: tối thiểu {activeTask?.minimumWords || activeTask?.minWords || 150} từ
                </span>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="grid gap-3 border-t border-[#ead8d5] bg-white px-5 py-3 md:grid-cols-[1fr_auto]">
        <div className="grid gap-3 md:grid-cols-2">
          {tasks.map((task, index) => {
            const taskKey = task.key || `task_${index + 1}`;
            const wordCount = countWords(responses[taskKey]);
            return (
              <button
                key={taskKey}
                className={`rounded-[24px] border px-5 py-4 text-left transition ${taskKey === activeTaskKey ? 'border-[#8a0018] bg-[#fff0f1]' : 'border-[#ead8d5] bg-white hover:bg-[#fff7f7]'}`}
                onClick={() => requestTaskChange(task, index)}
                type="button"
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="font-['Manrope'] text-xl font-black text-[#341c1d]">{task.title || `Task ${index + 1}`}</span>
                  <span className={`rounded-full px-3 py-1 text-[11px] font-bold ${wordCount > 0 ? 'bg-[#8a0018] text-white' : 'bg-[#f6ecea] text-[#8c716f]'}`}>
                    {wordCount} từ
                  </span>
                </div>
                <p className="mt-2 text-sm leading-6 text-[#6f5a58]">
                  {task.summary || `Viết ít nhất ${task.minimumWords || task.minWords || 150} từ trong khoảng ${task.recommendedMinutes || task.durationMinutes || 20} phút.`}
                </p>
              </button>
            );
          })}
        </div>

        <div className="flex items-center gap-3 self-center">
          <button
            className="rounded-full border border-[#dfbfbd] px-4 py-3 text-[#8a0018] transition hover:bg-[#fff0f1] disabled:opacity-40"
            disabled={activeTaskIndex <= 0}
            onClick={() => {
              const nextIndex = Math.max(0, activeTaskIndex - 1);
              requestTaskChange(tasks[nextIndex], nextIndex);
            }}
            type="button"
          >
            <ArrowLeft aria-hidden="true" size={20} strokeWidth={2.2} />
          </button>
          <button
            className="rounded-full border border-[#dfbfbd] px-4 py-3 text-[#8a0018] transition hover:bg-[#fff0f1] disabled:opacity-40"
            disabled={activeTaskIndex < 0 || activeTaskIndex >= tasks.length - 1}
            onClick={() => {
              const nextIndex = Math.min(tasks.length - 1, activeTaskIndex + 1);
              requestTaskChange(tasks[nextIndex], nextIndex);
            }}
            type="button"
          >
            <ArrowRight aria-hidden="true" size={20} strokeWidth={2.2} />
          </button>
        </div>
      </footer>

      {warning ? (
        <div className="fixed inset-0 z-[130] flex items-center justify-center bg-[#261112]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#b26a00]">Cảnh báo bài thi</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">Hãy quay lại bài Writing</h3>
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
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">Bài viết hiện chưa được nộp</h3>
            <p className="mt-3 text-sm leading-7 text-[#584140]">
              Nếu bạn thoát bây giờ, EnglishLab sẽ quay về {exitDestinationLabel} và lần làm bài này chưa được ghi nhận nộp.
            </p>
            <div className="mt-5 flex gap-3">
              <button
                className="flex-1 rounded-2xl border border-[#dfbfbd] px-5 py-3 text-sm font-bold text-[#8a0018]"
                onClick={() => setExitConfirmOpen(false)}
                type="button"
              >
                Ở lại
              </button>
              <button
                className="flex-1 rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white"
                onClick={handleCloseExam}
                type="button"
              >
                Thoát
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {pendingTaskChange ? (
        <ExamSectionChangeDialog
          currentLabel={activeTask?.title || 'Task hiện tại'}
          missingCount={pendingTaskChange.missingCount}
          onCancel={() => setPendingTaskChange(null)}
          onConfirm={() => {
            setActiveTaskKey(pendingTaskChange.targetKey);
            setPendingTaskChange(null);
          }}
          targetLabel={pendingTaskChange.task.title || 'Task tiếp theo'}
          unitLabel="từ"
        />
      ) : null}
    </div>
  );
}
