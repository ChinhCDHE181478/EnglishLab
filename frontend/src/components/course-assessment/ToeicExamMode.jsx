import { useEffect, useMemo, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, Flag, LayoutGrid, Volume2, X } from 'lucide-react';
import ExamDeviceCheck from './ExamDeviceCheck';
import { getAssessmentSubmissionErrorMessage } from '../../utils/assessmentSubmissionError';
import { exitExamFullscreenWhenDetached } from '../../utils/examFullscreen';
import { sanitizeLessonHtml } from '../../utils/lessonRichText';

const AUTO_NEXT_DELAY_MS = 5000;

const formatTimer = (seconds) => {
  const safeSeconds = Math.max(0, Number(seconds) || 0);
  const minutes = String(Math.floor(safeSeconds / 60)).padStart(2, '0');
  const remainder = String(safeSeconds % 60).padStart(2, '0');
  return `${minutes}:${remainder}`;
};

const answerIsFilled = (value) => (Array.isArray(value) ? value.length > 0 : String(value || '').trim().length > 0);

const flattenQuestionNumbers = (parts = []) => parts.flatMap((part) => (
  (part.questionGroups || []).flatMap((group) => (
    group.questionNumbers || (group.questions || []).map((question) => question.number)
  ))
));

const buildInitialAnswers = (parts = []) => {
  const answers = {};
  parts.forEach((part) => {
    (part.questionGroups || []).forEach((group) => {
      (group.questions || []).forEach((question) => {
        answers[String(question.number)] = '';
      });
    });
  });
  return answers;
};

const hasSharedPassage = (group = {}) => Boolean(
  String(group.passageHtml || '').trim()
  || (typeof group.passage === 'string' && group.passage.trim())
  || (group.passage && typeof group.passage === 'object'),
);

/** TOEIC: one question at a time for photos / QR / incomplete sentences; groups for conversations & passages. */
const shouldStepByQuestion = (group = {}, part = {}) => {
  if (group.perQuestionAudio) return true;
  const questions = Array.isArray(group.questions) ? group.questions : [];
  if (questions.some((question) => question.imageUrl)) return true;
  if (hasSharedPassage(group)) return false;
  const partNumber = Number(part.partNumber || part.part || 0);
  if ([1, 2, 5].includes(partNumber) && questions.length > 1) return true;
  if (group.hideOptionText && questions.length > 1) return true;
  if (questions.length > 8) return true;
  return questions.length === 1;
};

const buildSteps = (parts = []) => {
  const steps = [];
  parts.forEach((part, partIndex) => {
    (part.questionGroups || []).forEach((group, groupIndex) => {
      const questions = Array.isArray(group.questions) ? group.questions : [];
      if (!questions.length) return;
      if (shouldStepByQuestion(group, part)) {
        questions.forEach((question, questionIndex) => {
          steps.push({
            id: `p${partIndex}-g${groupIndex}-q${question.number}`,
            kind: 'question',
            part,
            partIndex,
            group,
            groupIndex,
            questions: [question],
            questionNumbers: [Number(question.number)],
            focusNumber: Number(question.number),
            questionIndex,
          });
        });
        return;
      }
      steps.push({
        id: `p${partIndex}-g${groupIndex}`,
        kind: 'group',
        part,
        partIndex,
        group,
        groupIndex,
        questions,
        questionNumbers: questions.map((question) => Number(question.number)),
        focusNumber: Number(questions[0].number),
      });
    });
  });
  return steps;
};

const resolveStepAudioUrl = (step) => {
  if (!step) return '';
  if (step.kind === 'question') {
    return String(step.questions?.[0]?.audioUrl || step.group?.audioUrl || '').trim();
  }
  return String(step.group?.audioUrl || step.questions?.find((question) => question.audioUrl)?.audioUrl || '').trim();
};

const renderRichText = (value, className = '') => {
  const text = String(value || '').trim();
  if (!text) return null;
  return <div className={className} dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(text) }} />;
};

export default function ToeicExamMode({
  assessment,
  config,
  initialAnswers = null,
  isLocked = false,
  submitting = false,
  onClose,
  onSubmit,
  skipAudioCheck = false,
  preserveFullscreenOnUnmount = false,
  submitLabel = 'Nộp bài',
  skillLabel = 'TOEIC',
}) {
  const parts = Array.isArray(config?.parts) ? config.parts : [];
  const steps = useMemo(() => buildSteps(parts), [parts]);
  const skill = String(config?.type || '').toLowerCase().includes('reading') || String(skillLabel).toLowerCase().includes('reading')
    ? 'READING'
    : 'LISTENING';
  const hasStepAudio = useMemo(
    () => skill === 'LISTENING' && steps.some((step) => Boolean(resolveStepAudioUrl(step))),
    [skill, steps],
  );
  const needsAudioGate = Boolean(config?.audioUrl) && skill === 'LISTENING' && !skipAudioCheck && !hasStepAudio;

  const [stage, setStage] = useState(() => (needsAudioGate ? 'check_audio' : 'exam'));
  const [stepIndex, setStepIndex] = useState(0);
  const [answers, setAnswers] = useState(() => ({ ...buildInitialAnswers(parts), ...(initialAnswers || {}) }));
  const [flagged, setFlagged] = useState(() => new Set());
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [remainingSeconds, setRemainingSeconds] = useState(() => Math.max(1, Number(config?.durationMinutes || assessment?.timeLimitMinutes || 45)) * 60);
  const [submissionPending, setSubmissionPending] = useState(false);
  const [submissionError, setSubmissionError] = useState('');
  const [warning, setWarning] = useState(null);
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);
  const [violations, setViolations] = useState([]);
  const [stepAudioStatus, setStepAudioStatus] = useState('idle');
  const [autoNextInMs, setAutoNextInMs] = useState(0);

  const rootRef = useRef(null);
  const submittedRef = useRef(false);
  const stepAudioRef = useRef(null);
  const autoNextTimerRef = useRef(null);
  const autoNextTickRef = useRef(null);
  const intentionalExitRef = useRef(false);
  const fullscreenSessionStartedRef = useRef(false);
  const submissionInFlightRef = useRef(false);
  const stepIndexRef = useRef(0);

  const activeStep = steps[stepIndex] || steps[0] || null;
  const stepAudioUrl = skill === 'LISTENING' ? resolveStepAudioUrl(activeStep) : '';
  const allQuestionNumbers = useMemo(() => flattenQuestionNumbers(parts), [parts]);
  const answeredCount = useMemo(
    () => allQuestionNumbers.filter((number) => answerIsFilled(answers[String(number)])).length,
    [allQuestionNumbers, answers],
  );
  const progressPercent = allQuestionNumbers.length
    ? Math.round((answeredCount / allQuestionNumbers.length) * 100)
    : 0;

  const clearAutoNext = () => {
    if (autoNextTimerRef.current) {
      window.clearTimeout(autoNextTimerRef.current);
      autoNextTimerRef.current = null;
    }
    if (autoNextTickRef.current) {
      window.clearInterval(autoNextTickRef.current);
      autoNextTickRef.current = null;
    }
    setAutoNextInMs(0);
  };

  const goToStep = (index) => {
    if (index < 0 || index >= steps.length) return;
    clearAutoNext();
    stepIndexRef.current = index;
    setStepIndex(index);
  };

  const scheduleAutoNext = () => {
    clearAutoNext();
    if (isLocked || submitting || submissionPending) return;
    const fromIndex = stepIndexRef.current;
    if (fromIndex >= steps.length - 1) return;
    setAutoNextInMs(AUTO_NEXT_DELAY_MS);
    const startedAt = Date.now();
    autoNextTickRef.current = window.setInterval(() => {
      const left = Math.max(0, AUTO_NEXT_DELAY_MS - (Date.now() - startedAt));
      setAutoNextInMs(left);
    }, 200);
    autoNextTimerRef.current = window.setTimeout(() => {
      clearAutoNext();
      if (stepIndexRef.current !== fromIndex) return;
      goToStep(fromIndex + 1);
    }, AUTO_NEXT_DELAY_MS);
  };

  useEffect(() => {
    stepIndexRef.current = stepIndex;
  }, [stepIndex]);

  useEffect(() => () => clearAutoNext(), []);

  useEffect(() => {
    if (stage !== 'exam' || isLocked || submitting || submissionPending) return undefined;
    const timer = window.setInterval(() => {
      setRemainingSeconds((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [isLocked, stage, submissionPending, submitting]);

  useEffect(() => {
    if (stage !== 'exam' || remainingSeconds !== 0 || submittedRef.current || submitting || submissionPending || isLocked) return;
    submittedRef.current = true;
    void handleSubmitExam(true);
  }, [remainingSeconds, stage, submitting, submissionPending, isLocked]);

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
      window.history.pushState({ englishlabToeicExam: true }, '', window.location.href);
    };
    const warn = (reason) => {
      const entry = { reason, at: new Date().toISOString() };
      setViolations((current) => [...current, entry]);
      setWarning(entry);
    };
    const handleVisibility = () => {
      if (document.hidden) warn('Bạn vừa rời khỏi tab trong lúc làm bài TOEIC.');
    };
    const handleBlur = () => warn('Cửa sổ bài thi TOEIC đã mất focus.');
    const handlePopState = () => {
      pushExamState();
      warn('Không thể quay lại trang khác trong lúc đang làm bài TOEIC.');
    };
    const handleFullscreen = () => {
      if (document.fullscreenElement) {
        fullscreenSessionStartedRef.current = true;
        return;
      }
      if (fullscreenSessionStartedRef.current && !intentionalExitRef.current) {
        fullscreenSessionStartedRef.current = false;
        warn('Không thể thoát toàn màn hình trong lúc đang thi TOEIC.');
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
          ? 'Bạn không thể dùng phím Esc để thoát toàn màn hình trong khi đang thi TOEIC.'
          : 'Một thao tác điều hướng hoặc sao chép ngoài bài thi TOEIC vừa bị chặn.',
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

  // Hidden per-step audio: autoplay, no visible controls; after ended wait 5s then next (STUDY4-like).
  useEffect(() => {
    clearAutoNext();
    const audio = stepAudioRef.current;
    if (stage !== 'exam' || skill !== 'LISTENING' || !stepAudioUrl || !audio) {
      setStepAudioStatus('idle');
      return undefined;
    }

    let cancelled = false;
    setStepAudioStatus('loading');
    audio.pause();
    audio.src = stepAudioUrl;
    audio.load();

    const tryPlay = async () => {
      if (cancelled) return;
      try {
        audio.playbackRate = 1;
        await audio.play();
        if (!cancelled) setStepAudioStatus('playing');
      } catch {
        if (!cancelled) setStepAudioStatus('blocked');
      }
    };

    const handleEnded = () => {
      if (cancelled) return;
      setStepAudioStatus('ended');
      scheduleAutoNext();
    };
    const handleError = () => {
      if (cancelled) return;
      setStepAudioStatus('error');
      scheduleAutoNext();
    };

    audio.addEventListener('ended', handleEnded);
    audio.addEventListener('error', handleError);
    void tryPlay();

    return () => {
      cancelled = true;
      audio.removeEventListener('ended', handleEnded);
      audio.removeEventListener('error', handleError);
      audio.pause();
    };
  }, [stage, skill, stepAudioUrl, stepIndex, isLocked, submitting, submissionPending]);

  const restoreFullscreen = async () => {
    if (document.fullscreenElement) return;
    try {
      await document.documentElement?.requestFullscreen?.();
      fullscreenSessionStartedRef.current = Boolean(document.fullscreenElement);
    } catch {
      // Ignore capability failures.
    }
  };

  const handleCloseExam = async () => {
    intentionalExitRef.current = true;
    clearAutoNext();
    if (document.fullscreenElement) {
      try {
        await document.exitFullscreen();
      } catch {
        // ignore
      }
    }
    onClose?.();
  };

  const updateAnswer = (number, value) => {
    if (isLocked || submitting || submissionPending) return;
    setAnswers((current) => ({ ...current, [String(number)]: value }));
  };

  const toggleFlag = (number) => {
    setFlagged((current) => {
      const next = new Set(current);
      const key = String(number);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const goToQuestion = (number) => {
    const index = steps.findIndex((step) => step.questionNumbers.includes(Number(number)));
    if (index >= 0) goToStep(index);
  };

  const retryStepAudio = async () => {
    const audio = stepAudioRef.current;
    if (!audio || !stepAudioUrl) return;
    try {
      await audio.play();
      setStepAudioStatus('playing');
    } catch {
      setStepAudioStatus('blocked');
    }
  };

  const buildPayload = (autoSubmitted = false) => {
    const responses = [];
    parts.forEach((part) => {
      (part.questionGroups || []).forEach((group) => {
        (group.questions || []).forEach((question) => {
          responses.push({
            questionNumber: question.number,
            part: part.key,
            answerType: group.type || 'single_choice',
            answer: String(answers[String(question.number)] || '').trim(),
          });
        });
      });
    });

    return {
      fullscreenExitCount: violations.filter((item) => String(item.reason || '').toLowerCase().includes('toàn màn hình')).length,
      tabSwitchCount: violations.filter((item) => !String(item.reason || '').toLowerCase().includes('toàn màn hình')).length,
      objectiveAnswersJson: JSON.stringify({
        mode: 'toeic_exam',
        examType: 'TOEIC',
        skill,
        testKey: config?.key,
        testTitle: config?.title || assessment?.title,
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
    if (isLocked || submitting || submissionPending || submissionInFlightRef.current) return;
    clearAutoNext();
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

  const partDirections = activeStep?.part?.passage?.paragraphs
    ?.map((paragraph) => paragraph?.text)
    .filter(Boolean)
    .join('\n\n')
    || activeStep?.part?.instructions
    || '';
  const isFirstStepOfPart = Boolean(
    activeStep
    && steps.findIndex((step) => step.partIndex === activeStep.partIndex) === stepIndex,
  );
  const shownDirections = isFirstStepOfPart ? partDirections : '';

  const leftImageUrl = activeStep?.kind === 'question'
    ? activeStep.questions[0]?.imageUrl
    : activeStep?.questions?.find((question) => question.imageUrl)?.imageUrl;

  const leftPassageHtml = activeStep?.group?.passageHtml || '';
  const leftPassageText = typeof activeStep?.group?.passage === 'string'
    ? activeStep.group.passage
    : '';
  const hasLeftContent = Boolean(
    shownDirections
    || activeStep?.group?.instructions
    || leftImageUrl
    || leftPassageHtml
    || leftPassageText
    || activeStep?.group?.descriptionHtml,
  );

  const renderOption = (question, option, group) => {
    const checked = answers[String(question.number)] === option.value;
    return (
      <label key={option.value} className="block cursor-pointer">
        <input
          checked={checked}
          className="peer sr-only"
          name={`toeic-q-${question.number}`}
          onChange={() => updateAnswer(question.number, option.value)}
          type="radio"
        />
        <span
          className={`flex items-start gap-3 rounded-2xl border px-4 py-3 text-sm leading-6 transition ${
            checked
              ? 'border-[#0b5c49] bg-[#eef8f4] text-[#063024] shadow-[0_10px_22px_rgba(11,92,73,0.12)]'
              : 'border-[#d7e5df] bg-white text-[#24352f] hover:border-[#0b5c49]/45 hover:bg-[#f6fbf9]'
          }`}
        >
          <span
            className={`mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-black ${
              checked ? 'bg-[#0b5c49] text-white' : 'bg-[#e8f3ee] text-[#0b5c49]'
            }`}
          >
            {option.value}
          </span>
          <span className="min-w-0 flex-1 pt-0.5">
            {group.hideOptionText ? null : (option.label && option.label !== option.value ? option.label : null)}
          </span>
        </span>
      </label>
    );
  };

  const renderQuestionBlock = (group, question) => {
    const isFlagged = flagged.has(String(question.number));
    return (
      <article
        key={question.number}
        className="scroll-mt-4 space-y-3 border-b border-[#e4eee9] pb-5 last:border-b-0 last:pb-0"
        id={`toeic-q-${question.number}`}
      >
        <div className="flex items-start justify-between gap-3">
          <span className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-black ${isFlagged ? 'bg-[#f4a261] text-white' : 'bg-[#dceee7] text-[#0b5c49]'}`}>
            {question.number}
          </span>
          <button
            aria-label={isFlagged ? 'Bỏ đánh dấu' : 'Đánh dấu câu hỏi'}
            className={`rounded-full p-2 transition ${isFlagged ? 'text-[#e07a2f]' : 'text-[#8aa297] hover:text-[#0b5c49]'}`}
            onClick={() => toggleFlag(question.number)}
            type="button"
          >
            <Flag className={`h-4 w-4 ${isFlagged ? 'fill-current' : ''}`} />
          </button>
        </div>
        {!group.hidePrompt && question.prompt && !/^Câu\s*\d+$/i.test(String(question.prompt).trim()) ? (
          <p className="text-[15px] font-semibold leading-7 text-[#1d2f29]">{question.prompt}</p>
        ) : null}
        {question.imageUrl && activeStep?.kind === 'group' ? (
          <img
            alt={`Hình câu ${question.number}`}
            className="max-h-72 w-full rounded-2xl object-contain"
            src={question.imageUrl}
          />
        ) : null}
        <div className="grid gap-2">
          {(question.options || []).map((option) => renderOption(question, option, group))}
        </div>
      </article>
    );
  };

  return (
    <div
      ref={rootRef}
      className={`fixed inset-0 z-[120] flex flex-col bg-[#f4faf7] text-[#1d2f29] ${sidebarOpen ? 'el-toeic-drawer-open' : ''}`}
      onContextMenu={(event) => event.preventDefault()}
      onCopy={(event) => event.preventDefault()}
      onCut={(event) => event.preventDefault()}
      onPaste={(event) => event.preventDefault()}
    >
      <audio ref={stepAudioRef} className="hidden" preload="auto">
        <track kind="captions" />
      </audio>

      {submissionError ? (
        <div className="fixed bottom-5 left-1/2 z-[140] w-[min(92vw,680px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm font-bold text-red-700 shadow-xl" role="alert">
          {submissionError}
        </div>
      ) : null}

      <header className="flex min-h-[64px] flex-shrink-0 flex-wrap items-center gap-3 border-b border-[#d7e5df] bg-white px-4 py-3 shadow-sm sm:px-5">
        <div className="min-w-0 flex-1">
          <p className="text-[11px] font-black uppercase tracking-[0.16em] text-[#0b5c49]">EnglishLab · {skillLabel}</p>
          <h2 className="truncate font-['Manrope'] text-base font-extrabold text-[#0b1c30] sm:text-lg">
            {config?.title || assessment?.title}
          </h2>
        </div>
        <div className={`inline-flex items-center rounded-full border px-4 py-2 font-['Manrope'] text-lg font-black tabular-nums ${remainingSeconds <= 300 ? 'border-red-200 bg-red-50 text-red-700' : 'border-[#cfe3db] bg-[#eef8f4] text-[#0b5c49]'}`}>
          {formatTimer(remainingSeconds)}
        </div>
        {stepAudioUrl ? (
          <div className="inline-flex items-center gap-2 rounded-full border border-[#cfe3db] bg-[#f6fbf9] px-3 py-2 text-xs font-bold text-[#0b5c49]">
            <Volume2 className="h-3.5 w-3.5" />
            {stepAudioStatus === 'playing' && 'Đang phát'}
            {stepAudioStatus === 'loading' && 'Đang tải'}
            {stepAudioStatus === 'ended' && (autoNextInMs > 0 ? `Chuyển sau ${Math.ceil(autoNextInMs / 1000)}s` : 'Hết audio')}
            {stepAudioStatus === 'blocked' && (
              <button className="underline" onClick={retryStepAudio} type="button">Bấm để phát</button>
            )}
            {stepAudioStatus === 'error' && 'Lỗi audio'}
            {stepAudioStatus === 'idle' && 'Audio'}
          </div>
        ) : null}
        <span className="rounded-full bg-[#8a0018] px-3 py-2 text-[11px] font-bold text-white">
          Vi phạm: {violations.length}
        </span>
        <button
          className="rounded-full border border-[#cfe3db] px-4 py-2 text-sm font-bold text-[#0b5c49] hover:bg-[#eef8f4]"
          onClick={() => setExitConfirmOpen(true)}
          type="button"
        >
          Thoát
        </button>
        <button
          className="rounded-full bg-[linear-gradient(135deg,#0b5c49,#063024)] px-5 py-2.5 text-sm font-black text-white shadow-[0_12px_24px_rgba(11,92,73,0.22)] disabled:opacity-60"
          disabled={stage !== 'exam' || isLocked || submitting || submissionPending}
          onClick={() => handleSubmitExam(false)}
          type="button"
        >
          {submitting || submissionPending ? 'Đang lưu...' : submitLabel}
        </button>
      </header>

      {stage === 'exam' ? (
        <div className="flex-shrink-0 border-b border-[#d7e5df] bg-white px-4 py-3 sm:px-5">
          <div className="flex flex-wrap items-center gap-3">
            <p className="text-[11px] font-black uppercase tracking-[0.14em] text-[#0b5c49]">
              Part {activeStep?.part?.partNumber || activeStep?.part?.part} · {activeStep?.part?.title}
            </p>
            <p className="text-xs font-semibold text-[#5f746c]">
              {answeredCount}/{allQuestionNumbers.length} câu
            </p>
            <div className="ml-auto flex items-center gap-2">
              <button
                aria-expanded={sidebarOpen}
                aria-label="Bảng câu hỏi"
                className={`inline-flex items-center gap-2 rounded-full border px-3 py-2 text-xs font-bold ${sidebarOpen ? 'border-[#0b5c49] bg-[#eef8f4] text-[#0b5c49]' : 'border-[#cfe3db] text-[#5f746c]'}`}
                onClick={() => setSidebarOpen((current) => !current)}
                type="button"
              >
                <LayoutGrid className="h-4 w-4" />
                Câu hỏi
              </button>
            </div>
          </div>
          <div className="relative mt-3 h-1.5 overflow-hidden rounded-full bg-[#e4eee9]">
            <div
              className="absolute inset-y-0 left-0 rounded-full bg-[#0b5c49] transition-[width] duration-300"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </div>
      ) : null}

      <div className="flex min-h-0 flex-1 overflow-hidden">
        <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
          <main className="min-h-0 flex-1 overflow-hidden">
            {stage === 'check_audio' ? (
              <div className="h-full overflow-y-auto px-5 py-6">
                <ExamDeviceCheck
                  onCancel={handleCloseExam}
                  onComplete={() => setStage('exam')}
                  title="Kiểm tra thiết bị"
                />
              </div>
            ) : (
              <div className={`grid h-full min-h-0 grid-cols-1 ${hasLeftContent ? 'lg:grid-cols-2' : ''}`}>
                {hasLeftContent ? (
                  <section className="min-h-0 overflow-y-auto border-b border-[#d7e5df] bg-[linear-gradient(180deg,#f7fcfa,#eef6f2)] px-5 py-5 lg:border-b-0 lg:border-r">
                    {shownDirections || activeStep?.group?.instructions ? (
                      <>
                        <p className="font-['Manrope'] text-lg font-extrabold text-[#0b5c49]">Directions</p>
                        {shownDirections ? (
                          <p className="mt-2 whitespace-pre-line text-sm leading-7 text-[#40554c]">{shownDirections}</p>
                        ) : null}
                        {activeStep?.group?.instructions ? (
                          <p className="mt-2 text-sm italic leading-7 text-[#5f746c]">{activeStep.group.instructions}</p>
                        ) : null}
                      </>
                    ) : null}
                    {leftImageUrl ? (
                      <img
                        alt="TOEIC visual"
                        className="mt-3 max-h-[min(62vh,560px)] w-full rounded-2xl object-contain shadow-[0_16px_40px_rgba(6,48,36,0.12)]"
                        src={leftImageUrl}
                      />
                    ) : null}
                    {leftPassageHtml ? (
                      <article
                        className="prose prose-sm mt-3 max-w-none rounded-2xl border border-[#d7e5df] bg-white p-5 text-[#24352f] [&_img]:mx-auto [&_img]:max-h-[420px] [&_img]:object-contain [&_p]:mb-3"
                        dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(leftPassageHtml) }}
                      />
                    ) : null}
                    {leftPassageText ? (
                      <article className="mt-3 whitespace-pre-wrap rounded-2xl border border-[#d7e5df] bg-white p-5 text-sm leading-7 text-[#24352f]">
                        {leftPassageText}
                      </article>
                    ) : null}
                    {renderRichText(activeStep?.group?.descriptionHtml, 'mt-3 text-sm leading-7 text-[#40554c]')}
                  </section>
                ) : null}

                <section className="min-h-0 overflow-y-auto bg-white px-5 py-5">
                  <p className="font-['Manrope'] text-lg font-extrabold text-[#0b5c49]">
                    {activeStep?.kind === 'question'
                      ? `Question ${activeStep.focusNumber}`
                      : (activeStep?.group?.title || 'Questions')}
                  </p>
                  <div className="mt-4 space-y-5">
                    {(activeStep?.questions || []).map((question) => renderQuestionBlock(activeStep.group, question))}
                  </div>
                </section>
              </div>
            )}
          </main>

          {stage === 'exam' ? (
            <footer className="flex flex-shrink-0 flex-wrap items-center gap-3 border-t border-[#d7e5df] bg-white px-4 py-3 sm:px-5">
              <button
                className="inline-flex items-center gap-2 rounded-full border border-[#cfe3db] px-4 py-2.5 text-sm font-bold text-[#0b5c49] disabled:opacity-40"
                disabled={stepIndex <= 0}
                onClick={() => goToStep(stepIndex - 1)}
                type="button"
              >
                <ChevronLeft className="h-4 w-4" />
                Trước
              </button>
              <p className="text-xs font-semibold text-[#5f746c]">
                Bước {Math.min(stepIndex + 1, steps.length)} / {steps.length}
              </p>
              <div className="ml-auto flex flex-wrap items-center gap-2">
                <button
                  className="inline-flex items-center gap-2 rounded-full border border-[#cfe3db] px-4 py-2.5 text-sm font-bold text-[#0b5c49] disabled:opacity-40"
                  disabled={stepIndex >= steps.length - 1}
                  onClick={() => goToStep(stepIndex + 1)}
                  type="button"
                >
                  Tiếp
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </footer>
          ) : null}
        </div>

        {stage === 'exam' ? (
          <aside
            className={`el-toeic-sidebar flex-shrink-0 overflow-hidden border-l border-[#d7e5df] bg-[#f7fcfa] transition-all duration-200 ${
              sidebarOpen ? 'w-[280px] min-w-[280px]' : 'w-0 min-w-0 border-l-0'
            }`}
          >
            <div className="flex h-full w-[280px] flex-col">
              <div className="flex items-center justify-between border-b border-[#d7e5df] px-4 py-3">
                <p className="text-sm font-black text-[#0b5c49]">Bảng trả lời</p>
                <button
                  aria-label="Đóng bảng câu hỏi"
                  className="rounded-full p-1 text-[#5f746c] hover:bg-[#e4eee9]"
                  onClick={() => setSidebarOpen(false)}
                  type="button"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              <div className="min-h-0 flex-1 space-y-5 overflow-y-auto px-4 py-4">
                {parts.map((part) => {
                  const numbers = flattenQuestionNumbers([part]);
                  const answeredInPart = numbers.filter((number) => answerIsFilled(answers[String(number)])).length;
                  return (
                    <div key={part.key || part.partNumber || part.title}>
                      <div className="mb-2 flex items-center justify-between gap-2">
                        <p className="text-[11px] font-black uppercase tracking-[0.12em] text-[#0b1c30]">
                          Part {part.partNumber || part.part}
                        </p>
                        <p className="text-[11px] tabular-nums text-[#5f746c]">
                          {answeredInPart}/{numbers.length}
                        </p>
                      </div>
                      <div className="grid grid-cols-5 gap-1.5">
                        {numbers.map((number) => {
                          const filled = answerIsFilled(answers[String(number)]);
                          const current = activeStep?.questionNumbers?.includes(Number(number));
                          const isFlagged = flagged.has(String(number));
                          return (
                            <button
                              key={number}
                              className={`flex aspect-square items-center justify-center rounded-lg text-[11px] font-bold tabular-nums transition ${
                                isFlagged
                                  ? 'bg-[#f4a261] text-white'
                                  : filled
                                    ? 'bg-[#0b5c49] text-white'
                                    : 'border border-[#cfe3db] bg-white text-[#5f746c]'
                              } ${current ? 'ring-2 ring-[#8a0018] ring-offset-1' : ''}`}
                              onClick={() => goToQuestion(number)}
                              type="button"
                            >
                              {number}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  );
                })}
              </div>
              <div className="border-t border-[#d7e5df] p-4">
                <button
                  className="w-full rounded-2xl bg-[linear-gradient(135deg,#0b5c49,#063024)] px-4 py-3 text-sm font-black text-white disabled:opacity-60"
                  disabled={isLocked || submitting || submissionPending}
                  onClick={() => handleSubmitExam(false)}
                  type="button"
                >
                  {submitting || submissionPending ? 'Đang lưu...' : submitLabel}
                </button>
              </div>
            </div>
          </aside>
        ) : null}
      </div>

      {warning ? (
        <div className="fixed inset-0 z-[130] flex items-center justify-center bg-[#06241c]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#b26a00]">Cảnh báo bài thi</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#0b1c30]">Hãy quay lại bài TOEIC</h3>
            <p className="mt-3 text-sm leading-7 text-[#40554c]">{warning.reason}</p>
            <button
              className="mt-5 w-full rounded-2xl bg-[#0b5c49] px-5 py-3 text-sm font-black text-white"
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
        <div className="fixed inset-0 z-[130] flex items-center justify-center bg-[#06241c]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#0b5c49]">Thoát chế độ thi?</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#0b1c30]">Bài TOEIC hiện chưa được nộp</h3>
            <p className="mt-3 text-sm leading-7 text-[#40554c]">
              Nếu bạn thoát bây giờ, lần làm bài này chưa được ghi nhận nộp.
            </p>
            <div className="mt-5 flex gap-3">
              <button
                className="flex-1 rounded-2xl border border-[#cfe3db] px-5 py-3 text-sm font-bold text-[#0b5c49]"
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
    </div>
  );
}
