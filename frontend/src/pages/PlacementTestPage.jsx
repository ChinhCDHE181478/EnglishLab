import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  ArrowRight,
  Award,
  BarChart3,
  BookOpen,
  CheckCircle2,
  ChevronRight,
  FileCheck,
  Headphones,
  Mic,
  PenLine,
  RefreshCw,
  RotateCcw,
  ShieldCheck,
  Sparkles,
  Target,
  Trophy,
  Zap,
} from 'lucide-react';
import placementTestApi from '../api/placementTestApi';
import Header from '../components/ai-learning/Header';
import ListeningExamMode from '../components/course-assessment/ListeningExamMode';
import ReadingExamMode from '../components/course-assessment/ReadingExamMode';
import WritingExamMode from '../components/course-assessment/WritingExamMode';
import SpeakingExamMode from '../components/course-assessment/SpeakingExamMode';
import ExamSectionChangeDialog from '../components/course-assessment/ExamSectionChangeDialog';
import ExamDeviceCheck from '../components/course-assessment/ExamDeviceCheck';
import CourseFooter from '../components/course/CourseFooter';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import { useAppDialog } from '../components/ui/AppDialog';
import { formatBandValue } from '../utils/selfPacedHelpers';
import PlacementRecommendationSection from '../components/placement/PlacementRecommendationSection';
import { getPlacementLevelLabel } from '../utils/placementRecommendation';

const SKILLS = [
  { key: 'listening', label: 'Listening', icon: Headphones },
  { key: 'reading', label: 'Reading', icon: BookOpen },
  { key: 'writing', label: 'Writing', icon: PenLine },
  { key: 'speaking', label: 'Speaking', icon: Mic },
];

const TOEIC_SKILLS = SKILLS.slice(0, 2);

const DRAFT_KEY = 'englishlab.placement-test.current.draft';

const emptyDraft = {
  listeningAnswers: {},
  readingAnswers: {},
  writingAnswers: { task_1: '', task_2: '' },
  speakingTranscript: '',
  speakingAudioUrl: '',
};

const formatTimer = (seconds) => {
  const safeSeconds = Math.max(0, Number(seconds) || 0);
  const minutes = String(Math.floor(safeSeconds / 60)).padStart(2, '0');
  const remainder = String(safeSeconds % 60).padStart(2, '0');
  return `${minutes}:${remainder}`;
};

const countWords = (value) => String(value || '').trim().split(/\s+/).filter(Boolean).length;

const readDraft = () => {
  try {
    return { ...emptyDraft, ...JSON.parse(localStorage.getItem(DRAFT_KEY) || '{}') };
  } catch {
    return emptyDraft;
  }
};

const findMultiSelectGroup = (config = {}, groupKey = '') => {
  const parts = Array.isArray(config?.parts) ? config.parts : [];
  for (const part of parts) {
    const groups = Array.isArray(part?.questionGroups) ? part.questionGroups : [];
    for (const group of groups) {
      const key = group.questionNumbers?.join('-') || 'multi';
      if (group.type === 'multi_select_letters' && key === groupKey) {
        return group;
      }
    }
  }
  return null;
};

const toExamModeInitialObjectiveAnswers = (config = {}, draftAnswers = {}) => {
  const parts = Array.isArray(config?.parts) ? config.parts : [];
  const initialAnswers = {};

  parts.forEach((part) => {
    (part.questionGroups || []).forEach((group) => {
      if (group.type === 'multi_select_letters') {
        const groupKey = group.questionNumbers?.join('-') || 'multi';
        const questionNumbers = group.questionNumbers || [];
        if (Array.isArray(draftAnswers[groupKey])) {
          initialAnswers[groupKey] = draftAnswers[groupKey];
          return;
        }
        if (questionNumbers.length === 1 && Array.isArray(draftAnswers[String(questionNumbers[0])])) {
          initialAnswers[groupKey] = draftAnswers[String(questionNumbers[0])];
          return;
        }
        initialAnswers[groupKey] = questionNumbers
          .map((number) => draftAnswers[String(number)])
          .filter((value) => String(value || '').trim().length > 0);
        return;
      }

      (group.questions || []).forEach((question) => {
        initialAnswers[String(question.number)] = draftAnswers[String(question.number)] || '';
      });
    });
  });

  return initialAnswers;
};

const parseObjectivePayload = (payload = {}) => {
  try {
    return JSON.parse(payload?.objectiveAnswersJson || '{}');
  } catch {
    return {};
  }
};

const toPlacementObjectiveAnswers = (config = {}, payload = {}) => {
  const parsed = parseObjectivePayload(payload);
  const nextAnswers = {};

  (parsed.responses || []).forEach((response) => {
    if (response?.answerType === 'multi_select_letters') {
      const groupKey = String(response.questionNumber || '');
      const group = findMultiSelectGroup(config, groupKey);
      const selectedLetters = String(response.answer || '')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);

      if (group) {
        const numbers = group.questionNumbers || [];
        if (numbers.length === 1 && Number(group.maxSelections || 1) > 1) {
          nextAnswers[String(numbers[0])] = selectedLetters;
          return;
        }
        numbers.forEach((number, index) => {
          nextAnswers[String(number)] = selectedLetters[index] || '';
        });
        return;
      }

      nextAnswers[groupKey] = selectedLetters;
      return;
    }

    nextAnswers[String(response.questionNumber)] = String(response.answer || '').trim();
  });

  return nextAnswers;
};

const toWritingAnswers = (config = {}, payload = {}) => {
  const parsed = parseObjectivePayload(payload);
  const tasks = Array.isArray(parsed?.tasks) && parsed.tasks.length
    ? parsed.tasks
    : (Array.isArray(config?.tasks) ? config.tasks : []);

  const nextAnswers = {};
  tasks.forEach((task, index) => {
    const taskKey = task.key || `task_${index + 1}`;
    nextAnswers[taskKey] = String(task.response || '').trim();
  });

  return nextAnswers;
};

const toWritingSubmissionText = (config = {}, writingAnswers = {}) => {
  const tasks = Array.isArray(config?.tasks) ? config.tasks : [];
  return tasks.map((task, index) => {
    const taskKey = task.key || `task_${index + 1}`;
    return [
      `[${task.title || `Task ${index + 1}`}]`,
      String(writingAnswers[taskKey] || '').trim(),
    ].join('\n');
  }).join('\n\n');
};

const toSpeakingExamConfig = (config = {}) => {
  const variants = Array.isArray(config?.variants) ? config.variants.filter(Boolean) : [];
  const activeVariant = variants[0] || null;
  return {
    ...config,
    submissionLabel: activeVariant?.label || config.submissionLabel || config.title,
    parts: activeVariant?.parts || config.parts || [],
  };
};

const TOEIC_PART_START = {
  listening: { 1: 1, 2: 7, 3: 32, 4: 71 },
  reading: { 5: 101, 6: 131, 7: 147 },
};

const normalizeToeicOption = (option, index) => {
  if (typeof option === 'object' && option !== null) {
    const value = String(option.value || option.key || String.fromCharCode(65 + index)).trim();
    return { value, label: option.label || option.text || value };
  }
  const text = String(option || '').trim();
  const match = text.match(/^([A-D])[\).:\s-]*(.*)$/i);
  if (match) {
    return { value: match[1].toUpperCase(), label: match[2] || match[1].toUpperCase() };
  }
  const value = String.fromCharCode(65 + index);
  return { value, label: text || value };
};

const normalizeToeicQuestion = (question = {}, fallbackNumber) => {
  const options = Array.isArray(question.options) && question.options.length
    ? question.options.map(normalizeToeicOption)
    : ['A', 'B', 'C', 'D'].map(normalizeToeicOption);
  return {
    ...question,
    number: Number(question.number || question.id || fallbackNumber),
    prompt: question.prompt || question.question || question.text || `Câu ${fallbackNumber}`,
    options,
  };
};

const normalizeToeicGroup = (group = {}, partKey = '', fallbackQuestions = [], fallbackStart = 1) => ({
  ...group,
  title: group.title || 'Questions',
  instructions: group.instructions || group.description || '',
  type: group.type || 'single_choice',
  questions: (Array.isArray(group.questions) && group.questions.length ? group.questions : fallbackQuestions)
    .map((question, index) => normalizeToeicQuestion(question, question.number || fallbackStart + index)),
});

const buildToeicPlaceholderQuestions = (part = {}, sectionKey = '') => {
  const count = Number(part.questionCount || 0);
  if (!count) return [];
  const start = TOEIC_PART_START[sectionKey]?.[Number(part.part)] || Number(part.startQuestion || 1);
  return Array.from({ length: count }, (_, index) => normalizeToeicQuestion({}, start + index));
};

const normalizeToeicPart = (part = {}, index = 0, sectionKey = '') => {
  const partNumber = Number(part.partNumber || part.part || index + 1);
  const placeholderQuestions = buildToeicPlaceholderQuestions({ ...part, part: partNumber }, sectionKey);
  const fallbackStart = TOEIC_PART_START[sectionKey]?.[partNumber] || Number(part.startQuestion || 1);
  const questionGroups = Array.isArray(part.questionGroups) && part.questionGroups.length
    ? part.questionGroups.map((group) => normalizeToeicGroup(group, `part_${partNumber}`, [], fallbackStart))
    : [normalizeToeicGroup({
      title: part.groupTitle || part.title || `Part ${partNumber}`,
      instructions: part.instructions,
      type: part.type || 'single_choice',
      questions: Array.isArray(part.questions) && part.questions.length ? part.questions : placeholderQuestions,
    }, `part_${partNumber}`, placeholderQuestions, fallbackStart)];

  const questionNumbers = questionGroups.flatMap((group) => (
    group.questionNumbers || (group.questions || []).map((question) => question.number)
  ));

  return {
    ...part,
    key: part.key || `toeic_${sectionKey}_part_${partNumber}`,
    partNumber,
    title: part.title || `Part ${partNumber}`,
    questionRange: questionNumbers.length ? `Questions ${questionNumbers[0]}-${questionNumbers[questionNumbers.length - 1]}` : '',
    passage: part.passage || {
      title: part.title || `Part ${partNumber}`,
      paragraphs: part.description || part.instructions ? [{ text: part.description || part.instructions }] : [],
    },
    questionGroups,
  };
};

const toToeicExamSection = (toeicConfig = {}, sectionKey = '') => {
  const section = toeicConfig?.[sectionKey] || {};
  const title = section.title || (sectionKey === 'listening' ? 'TOEIC Listening' : 'TOEIC Reading');
  const parts = Array.isArray(section.parts) ? section.parts : [];
  const firstPartAudioUrl = parts.find((part) => part.audioUrl)?.audioUrl || '';
  return {
    ...section,
    key: section.key || `toeic_${sectionKey}`,
    title,
    durationMinutes: Number(section.durationMinutes || (sectionKey === 'listening' ? 45 : 75)),
    audioUrl: section.audioUrl || firstPartAudioUrl || toeicConfig.audioUrl || '',
    audioLabel: section.audioLabel || 'TOEIC audio',
    parts: parts.map((part, index) => normalizeToeicPart(part, index, sectionKey)),
  };
};

function SpeakingWorkspace({ config = {}, transcript, audioUrl, onTranscriptChange, onAudioReady }) {
  const { alert: alertDialog } = useAppDialog();
  const [recording, setRecording] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [previewUrl, setPreviewUrl] = useState('');
  const recorderRef = useRef(null);
  const streamRef = useRef(null);
  const chunksRef = useRef([]);

  useEffect(() => () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    streamRef.current?.getTracks().forEach((track) => track.stop());
  }, [previewUrl]);

  const startRecording = async () => {
    try {
      streamRef.current = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(streamRef.current);
      recorderRef.current = recorder;
      chunksRef.current = [];

      recorder.ondataavailable = (event) => {
        if (event.data.size) chunksRef.current.push(event.data);
      };

      recorder.onstop = async () => {
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        setPreviewUrl((current) => {
          if (current) URL.revokeObjectURL(current);
          return URL.createObjectURL(blob);
        });
        setUploading(true);

        try {
          const file = new File([blob], `placement-speaking-${Date.now()}.webm`, { type: blob.type });
          const response = await placementTestApi.uploadSpeakingAudio(file);
          onAudioReady(response?.url || response?.audioUrl || '');
        } finally {
          setUploading(false);
          streamRef.current?.getTracks().forEach((track) => track.stop());
        }
      };

      recorder.start();
      setRecording(true);
    } catch {
      await alertDialog('Không thể truy cập microphone. Hãy kiểm tra quyền của trình duyệt rồi thử lại.', {
        title: 'Không truy cập được microphone',
      });
    }
  };

  const stopRecording = () => {
    if (recorderRef.current?.state === 'recording') {
      recorderRef.current.stop();
    }
    setRecording(false);
  };

  return (
    <main className="grid min-h-0 flex-1 grid-cols-1 overflow-hidden xl:grid-cols-[minmax(0,1fr)_minmax(420px,0.95fr)]">
      <section className="min-h-0 overflow-y-auto border-r border-[#ead8d5] bg-[radial-gradient(circle_at_top,_rgba(138,0,24,0.07),_transparent_48%),linear-gradient(180deg,#fffafa,#fcf7f5)] px-6 py-8">
        <div className="mx-auto max-w-4xl space-y-5">
          {(config.parts || []).map((part) => (
            <section className="rounded-[30px] bg-white p-6 shadow-[0_20px_60px_rgba(86,35,37,0.10)]" key={part.key}>
              <p className="text-sm font-black uppercase tracking-[0.16em] text-[#8a0018]">{part.heading || part.title}</p>
              <h2 className="mt-2 font-['Manrope'] text-3xl font-black text-[#341c1d]">{part.title}</h2>

              {part.cueCardTitle ? (
                <div className="mt-5 rounded-[24px] border border-[#ead8d5] bg-[#fff7f7] p-5">
                  <p className="font-black text-[#8a0018]">{part.cueCardTitle}</p>
                  <ul className="mt-3 list-disc space-y-2 pl-5 text-sm leading-7 text-[#584140]">
                    {(part.cueCardBullets || []).map((item) => <li key={item}>{item}</li>)}
                  </ul>
                </div>
              ) : (
                <ol className="mt-5 list-decimal space-y-2 pl-5 text-sm leading-7 text-[#584140]">
                  {(part.prompts || []).map((prompt) => <li key={prompt}>{prompt}</li>)}
                </ol>
              )}
            </section>
          ))}
        </div>
      </section>

      <section className="min-h-0 overflow-y-auto bg-white px-6 py-8">
        <div className="mx-auto flex h-full max-w-3xl flex-col">
          <div className="rounded-[28px] border border-[#ead8d5] bg-[#fffdfc] p-4 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">Bản ghi Speaking</p>
                <h3 className="mt-1 font-['Manrope'] text-2xl font-black text-[#341c1d]">Ghi một lần cho toàn bộ bài nói</h3>
              </div>
              <div className={`rounded-full px-4 py-2 text-sm font-extrabold ${audioUrl ? 'bg-[#8a0018] text-white' : 'bg-[#fff0f1] text-[#8a0018]'}`}>
                {audioUrl ? 'Đã có bản ghi' : 'Chưa có bản ghi'}
              </div>
            </div>

            <p className="mt-4 text-sm leading-7 text-[#584140]">Thiết bị đã được kiểm tra trước khi bắt đầu bài đánh giá. Hãy ghi âm một lần cho cả ba phần Speaking.</p>

            <div className="mt-5 flex flex-wrap gap-3">
              <button
                className={`rounded-full px-5 py-3 text-sm font-black text-white transition ${recording ? 'bg-red-700 hover:bg-red-800' : 'bg-[linear-gradient(135deg,#8a0018,#650012)] hover:brightness-105'}`}
                onClick={recording ? stopRecording : startRecording}
                type="button"
              >
                {recording ? 'Dừng và tải bản ghi' : 'Bắt đầu ghi âm'}
              </button>
              <div className="rounded-full border border-[#dfbfbd] px-4 py-3 text-sm font-semibold text-[#6f5a58]">
                {uploading ? 'Đang tải bản ghi...' : audioUrl ? 'Bản ghi đã được lưu.' : 'Chưa tải bản ghi nào.'}
              </div>
            </div>

            {previewUrl ? <audio className="mt-4 w-full" controls src={previewUrl} /> : null}

            <label className="mt-6 block text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">Nội dung đã nói, nếu cần bổ sung</label>
            <textarea
              className="mt-3 min-h-[46vh] w-full rounded-[24px] border border-[#dfbfbd]/60 bg-white px-5 py-4 text-[15px] leading-8 text-[#2b1718] outline-none transition focus:border-[#8a0018]"
              onChange={(event) => onTranscriptChange(event.target.value)}
              placeholder="Nếu cần, bạn có thể ghi lại nội dung hoặc ý chính của phần nói ở đây..."
              spellCheck={false}
              value={transcript}
            />

            <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-[#6f5a58]">
              <span>Số từ đã ghi lại: {countWords(transcript)}</span>
              <span>Bạn có thể nộp bằng bản ghi âm, nội dung đã nói hoặc cả hai.</span>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}

function LegacyPlacementSpeakingExamMode({
  assessment,
  config,
  transcript,
  audioUrl,
  submitting = false,
  onClose,
  onSubmit,
  onTranscriptChange,
  onAudioReady,
  submitLabel = 'Nộp toàn bộ bài thi',
}) {
  const [remainingSeconds, setRemainingSeconds] = useState(() => Math.max(1, Number(config?.durationMinutes || assessment?.timeLimitMinutes || 15)) * 60);
  const [submissionPending, setSubmissionPending] = useState(false);
  const [warning, setWarning] = useState(null);
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);
  const [violations, setViolations] = useState([]);
  const submittedRef = useRef(false);
  const intentionalExitRef = useRef(false);

  useEffect(() => {
    if (submitting || submissionPending) return undefined;
    const timer = window.setInterval(() => {
      setRemainingSeconds((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [submissionPending, submitting]);

  useEffect(() => {
    if (remainingSeconds !== 0 || submittedRef.current || submitting || submissionPending) return;
    submittedRef.current = true;
    setSubmissionPending(true);
    Promise.resolve(onSubmit(true)).finally(() => setSubmissionPending(false));
  }, [onSubmit, remainingSeconds, submissionPending, submitting]);

  useEffect(() => {
    intentionalExitRef.current = false;
    document.documentElement?.requestFullscreen?.().catch(() => {});
    return () => {
      if (document.fullscreenElement) {
        document.exitFullscreen?.().catch(() => {});
      }
    };
  }, []);

  useEffect(() => {
    const pushExamState = () => {
      window.history.pushState({ englishlabPlacementSpeakingExam: true }, '', window.location.href);
    };

    const warn = (reason) => {
      const entry = { reason, at: new Date().toISOString() };
      setViolations((current) => [...current, entry]);
      setWarning(entry);
    };

    const handleVisibility = () => {
      if (document.hidden) {
        warn('Bạn vừa rời khỏi tab hoặc thu nhỏ cửa sổ trong lúc làm bài Speaking.');
      }
    };

    const handleBlur = () => {
      warn('Cửa sổ bài thi Speaking đã mất focus.');
    };

    const handlePopState = () => {
      pushExamState();
      warn('Không thể quay lại trang khác trong lúc đang làm bài Speaking.');
    };

    const handleFullscreen = () => {
      if (!document.fullscreenElement && !intentionalExitRef.current) {
        void restoreFullscreen();
        warn('Không thể thoát toàn màn hình trong lúc đang thi Speaking.');
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
          ? 'Bạn không thể dùng phím Esc để thoát toàn màn hình trong khi đang thi Speaking.'
          : 'Một thao tác điều hướng hoặc sao chép ngoài bài thi Speaking vừa bị chặn.'
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

  return (
    <div
      className="fixed inset-0 z-[120] flex flex-col bg-[#fcf7f5] text-[#2b1718]"
      onContextMenu={(event) => event.preventDefault()}
      onCopy={(event) => event.preventDefault()}
      onCut={(event) => event.preventDefault()}
      onPaste={(event) => event.preventDefault()}
    >
      <header className="flex min-h-[78px] flex-wrap items-center justify-between gap-4 border-b border-[#ead8d5] bg-white px-5 shadow-sm">
        <div>
          <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">Đánh giá đầu vào EnglishLab</p>
          <h2 className="font-['Manrope'] text-lg font-extrabold text-[#341c1d]">{assessment?.title || config?.title}</h2>
        </div>

        <div className="flex items-center gap-3">
          <div className="rounded-full bg-[#fff0f1] px-5 py-2 text-xl font-black text-[#8a0018] shadow-[0_10px_24px_rgba(138,0,24,0.10)]">
            {formatTimer(remainingSeconds)}
          </div>
          <span className="rounded-full bg-[#8a0018] px-4 py-2 text-xs font-bold text-white">Vi phạm: {violations.length}</span>
          <button
            className="rounded-full border border-[#8a0018]/20 px-5 py-2 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
            onClick={() => setExitConfirmOpen(true)}
            type="button"
          >
            Thoát
          </button>
          <button
            className="rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-3 text-sm font-black text-white shadow-[0_14px_28px_rgba(138,0,24,0.24)] transition hover:brightness-105 disabled:opacity-60"
            disabled={submitting || submissionPending}
            onClick={async () => {
              setSubmissionPending(true);
              try {
                await onSubmit(false);
              } finally {
                setSubmissionPending(false);
              }
            }}
            type="button"
          >
            {submitting || submissionPending ? 'Đang lưu...' : submitLabel}
          </button>
        </div>
      </header>

      <SpeakingWorkspace
        audioUrl={audioUrl}
        config={config}
        onAudioReady={onAudioReady}
        onTranscriptChange={onTranscriptChange}
        transcript={transcript}
      />

      {warning ? (
        <div className="fixed inset-0 z-[130] flex items-center justify-center bg-[#261112]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#b26a00]">Cảnh báo bài thi</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">Hãy quay lại bài Speaking</h3>
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
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">Bài Speaking hiện chưa được nộp</h3>
            <p className="mt-3 text-sm leading-7 text-[#584140]">Nếu bạn thoát bây giờ, EnglishLab sẽ quay về màn hình bắt đầu đánh giá. Bản nháp vẫn được giữ và phần Speaking này chưa được ghi nhận nộp.</p>
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
    </div>
  );
}

export default function PlacementTestPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [test, setTest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [stage, setStage] = useState('select');
  const [selectedExamType, setSelectedExamType] = useState('IELTS');
  const [selectedSkillKeys, setSelectedSkillKeys] = useState([]);
  const [skillIndex, setSkillIndex] = useState(0);
  const [draft, setDraft] = useState(readDraft);
  const [deviceCheck, setDeviceCheck] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [result, setResult] = useState(null);
  const [recommendation, setRecommendation] = useState(null);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [recommendationError, setRecommendationError] = useState('');
  const [pendingSkillAdvance, setPendingSkillAdvance] = useState(null);

  const activeSkills = selectedExamType === 'TOEIC'
    ? TOEIC_SKILLS
    : selectedExamType === 'SKILL'
      ? SKILLS.filter((skill) => selectedSkillKeys.includes(skill.key))
      : SKILLS;
  const activeSkill = activeSkills[skillIndex];
  const activeConfig = selectedExamType === 'TOEIC'
    ? toToeicExamSection(test?.sections?.toeic, activeSkill?.key)
    : test?.sections?.[activeSkill?.key];
  const attemptCount = Number(test?.attemptCount || 0);
  const resultExamType = result?.examType || selectedExamType || 'IELTS';
  const isToeicResult = resultExamType === 'TOEIC';
  const isSkillResult = resultExamType === 'SKILL';
  const resultSelectedSkillKeys = (result?.selectedSkills || []).map((skill) => String(skill).toLowerCase());
  const resultSkills = isToeicResult
    ? TOEIC_SKILLS
    : isSkillResult
      ? SKILLS.filter((skill) => resultSelectedSkillKeys.includes(skill.key))
      : SKILLS;
  const requiresMicrophone = selectedExamType !== 'TOEIC'
    && (selectedExamType !== 'SKILL' || selectedSkillKeys.includes('speaking'));
  const estimatedMinutes = selectedExamType === 'TOEIC'
    ? 120
    : activeSkills.reduce((total, skill) => total + Number(test?.sections?.[skill.key]?.durationMinutes || 0), 0);

  const retryRecommendations = async () => {
    if (!result?.id || resultExamType === 'SKILL') return;
    setRecommendationLoading(true);
    setRecommendationError('');
    try {
      const current = await placementTestApi.getCurrent();
      if (String(current?.latestAttempt?.id) === String(result.id)) {
        setResult(current.latestAttempt);
        setTest((existing) => existing ? { ...existing, ...current } : current);
      }
      setRecommendation(await placementTestApi.getRecommendations(result.id));
    } catch (error) {
      setRecommendationError(error?.response?.data?.message || 'Không thể tải gợi ý học tập.');
    } finally {
      setRecommendationLoading(false);
    }
  };

  useEffect(() => {
    if (stage !== 'result' || !result?.id || resultExamType === 'SKILL') return;
    let active = true;
    const loadRecommendations = async () => {
      setRecommendationLoading(true);
      setRecommendationError('');
      try {
        const response = await placementTestApi.getRecommendations(result.id);
        if (active) setRecommendation(response);
      } catch (error) {
        if (active) setRecommendationError(error?.response?.data?.message || 'Không thể tải gợi ý học tập.');
      } finally {
        if (active) setRecommendationLoading(false);
      }
    };
    void loadRecommendations();
    return () => { active = false; };
  }, [result?.id, resultExamType, stage]);

  useEffect(() => {
    const loadCurrentTest = async () => {
      try {
        const response = await placementTestApi.getCurrent();
        const sections = response?.sections;
        const missingSkills = SKILLS.filter((skill) => !sections?.[skill.key]);

        if (missingSkills.length) {
          throw new Error(`Đề thi đang thiếu dữ liệu: ${missingSkills.map((skill) => skill.label).join(', ')}`);
        }

        setTest(response);

        if (searchParams.get('view') === 'result' && response.latestAttempt) {
          setResult(response.latestAttempt);
          setSelectedExamType(response.latestAttempt.examType || 'IELTS');
          setSelectedSkillKeys((response.latestAttempt.selectedSkills || []).map((skill) => String(skill).toLowerCase()));
          setStage('result');
        }
      } catch (error) {
        setLoadError(error?.response?.data?.message || error?.message || 'Không tải được đề thi thử.');
      } finally {
        setLoading(false);
      }
    };

    loadCurrentTest();
  }, []);

  useEffect(() => {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
  }, [draft]);

  const answeredCount = useMemo(() => {
    if (activeSkill?.key === 'listening') {
      return Object.values(draft.listeningAnswers).reduce((sum, value) => sum + (Array.isArray(value) ? value.length : String(value || '').trim() ? 1 : 0), 0);
    }

    if (activeSkill?.key === 'reading') {
      return Object.values(draft.readingAnswers).reduce((sum, value) => sum + (Array.isArray(value) ? value.length : String(value || '').trim() ? 1 : 0), 0);
    }

    if (activeSkill?.key === 'writing') {
      return Object.values(draft.writingAnswers).filter((value) => countWords(value) > 0).length;
    }

    return Number(Boolean(draft.speakingAudioUrl || draft.speakingTranscript.trim()));
  }, [activeSkill?.key, draft]);

  const hasDraftProgress = useMemo(() => (
    Object.values(draft.listeningAnswers).some((value) => (Array.isArray(value) ? value.length > 0 : String(value || '').trim().length > 0))
    || Object.values(draft.readingAnswers).some((value) => (Array.isArray(value) ? value.length > 0 : String(value || '').trim().length > 0))
    || Object.values(draft.writingAnswers).some((value) => countWords(value) > 0)
    || Boolean(draft.speakingAudioUrl || draft.speakingTranscript.trim())
  ), [draft]);

  const goToNextSkill = async (draftOverride = null) => {
    setSubmitError('');
    if (skillIndex >= activeSkills.length - 1) {
      await submitAll({
        skipSpeakingValidation: !activeSkills.some((skill) => skill.key === 'speaking'),
        draftOverride,
      });
      return;
    }
    setSkillIndex((current) => Math.min(current + 1, activeSkills.length - 1));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const submitAll = async ({ skipSpeakingValidation = false, draftOverride = null } = {}) => {
    const submissionDraft = draftOverride || draft;
    if (!skipSpeakingValidation && !submissionDraft.speakingAudioUrl && !submissionDraft.speakingTranscript.trim()) {
      setSubmitError('Hãy hoàn thành bản ghi âm cho phần Nói trước khi nộp bài.');
      return;
    }

    setSubmitting(true);
    setSubmitError('');

    try {
      const response = await placementTestApi.submitCurrent({
        testCode: test.testCode,
        examType: selectedExamType,
        selectedSkills: selectedExamType === 'SKILL' ? activeSkills.map((skill) => skill.key.toUpperCase()) : undefined,
        listeningAnswers: submissionDraft.listeningAnswers,
        readingAnswers: submissionDraft.readingAnswers,
        writingAnswers: activeSkills.some((skill) => skill.key === 'writing') ? submissionDraft.writingAnswers : {},
        speakingTranscript: activeSkills.some((skill) => skill.key === 'speaking') ? submissionDraft.speakingTranscript : '',
        speakingAudioUrl: activeSkills.some((skill) => skill.key === 'speaking') ? submissionDraft.speakingAudioUrl : '',
        deviceCheck,
      });

      setResult(response);
      setStage('result');
      setSearchParams({ view: 'result' }, { replace: true });

      setTest((current) => current ? {
        ...current,
        latestAttempt: response,
        attemptCount: Number(current.attemptCount || 0) + 1,
        canRetake: true,
      } : current);

      if (response.status === 'COMPLETED') {
        localStorage.removeItem(DRAFT_KEY);
      }
    } catch (error) {
      setSubmitError(error?.response?.data?.message || 'Chưa thể nộp bài. Bản nháp vẫn được giữ trên thiết bị này.');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePlacementSpeakingSubmit = async (_assessmentId, payload = {}) => {
    const speakingTranscript = String(payload.submittedText || '').trim();
    const speakingAudioUrl = String(payload.submittedAudioUrl || '').trim();

    if (!speakingAudioUrl && !speakingTranscript) {
      setSubmitError('Hãy hoàn thành bản ghi âm cho phần Nói trước khi nộp bài.');
      return null;
    }

    setDraft((current) => ({
      ...current,
      speakingTranscript,
      speakingAudioUrl,
    }));
    setSubmitting(true);
    setSubmitError('');

    try {
      const response = await placementTestApi.submitCurrent({
        testCode: test.testCode,
        examType: selectedExamType,
        selectedSkills: selectedExamType === 'SKILL' ? activeSkills.map((skill) => skill.key.toUpperCase()) : undefined,
        listeningAnswers: draft.listeningAnswers,
        readingAnswers: draft.readingAnswers,
        writingAnswers: draft.writingAnswers,
        speakingTranscript,
        speakingAudioUrl,
        deviceCheck,
      });

      setResult(response);
      setStage('result');
      setSearchParams({ view: 'result' }, { replace: true });
      setTest((current) => current ? {
        ...current,
        latestAttempt: response,
        attemptCount: Number(current.attemptCount || 0) + 1,
        canRetake: true,
      } : current);
      if (response.status === 'COMPLETED') {
        localStorage.removeItem(DRAFT_KEY);
      }
      return response;
    } catch (error) {
      setSubmitError(error?.response?.data?.message || 'Chưa thể nộp bài. Bài làm của bạn vẫn được lưu an toàn.');
      throw error;
    } finally {
      setSubmitting(false);
    }
  };

  const handleListeningSubmit = async (payload) => {
    setSubmitting(true);
    setSubmitError('');

    try {
      const nextAnswers = toPlacementObjectiveAnswers(activeConfig, payload);
      const nextDraft = { ...draft, listeningAnswers: nextAnswers };
      setDraft(nextDraft);
      const parsed = parseObjectivePayload(payload);
      const missingCount = Math.max(0, Number(parsed.totalQuestions || 0) - Number(parsed.answeredCount || 0));
      if (missingCount > 0) {
        setPendingSkillAdvance({ missingCount, unitLabel: 'câu' });
        return;
      }
      await goToNextSkill(nextDraft);
    } finally {
      setSubmitting(false);
    }
  };

  const handleReadingSubmit = async (payload) => {
    setSubmitting(true);
    setSubmitError('');

    try {
      const nextAnswers = toPlacementObjectiveAnswers(activeConfig, payload);
      const nextDraft = { ...draft, readingAnswers: nextAnswers };
      setDraft(nextDraft);
      const parsed = parseObjectivePayload(payload);
      const missingCount = Math.max(0, Number(parsed.totalQuestions || 0) - Number(parsed.answeredCount || 0));
      if (missingCount > 0) {
        setPendingSkillAdvance({ missingCount, unitLabel: 'câu' });
        return;
      }
      await goToNextSkill(nextDraft);
    } finally {
      setSubmitting(false);
    }
  };

  const handleWritingSubmit = async (payload) => {
    setSubmitting(true);
    setSubmitError('');

    try {
      const nextAnswers = toWritingAnswers(test?.sections?.writing, payload);
      const nextDraft = { ...draft, writingAnswers: { ...draft.writingAnswers, ...nextAnswers } };
      setDraft(nextDraft);
      const parsed = parseObjectivePayload(payload);
      const incompleteTasks = (parsed.tasks || []).filter((task) => Number(task.wordCount || 0) < Number(task.minimumWords || 0)).length;
      if (incompleteTasks > 0) {
        setPendingSkillAdvance({ missingCount: incompleteTasks, unitLabel: 'task' });
        return;
      }
      await goToNextSkill(nextDraft);
    } finally {
      setSubmitting(false);
    }
  };

  const renderSkillAdvanceDialog = () => pendingSkillAdvance ? (
    <ExamSectionChangeDialog
      currentLabel={activeSkill?.label || 'Phần hiện tại'}
      missingCount={pendingSkillAdvance.missingCount}
      onCancel={() => setPendingSkillAdvance(null)}
      onConfirm={() => {
        setPendingSkillAdvance(null);
        void goToNextSkill();
      }}
      targetLabel={activeSkills[skillIndex + 1]?.label || 'nộp bài'}
      unitLabel={pendingSkillAdvance.unitLabel}
    />
  ) : null;

  const startExamType = (examType, skillKeys = null) => {
    const nextSkillKeys = examType === 'SKILL'
      ? SKILLS.filter((skill) => (skillKeys || selectedSkillKeys).includes(skill.key)).map((skill) => skill.key)
      : [];
    if (examType === 'SKILL' && nextSkillKeys.length === 0) return;
    setSelectedExamType(examType);
    setSelectedSkillKeys(nextSkillKeys);
    setSkillIndex(0);
    setSubmitError('');
    setRecommendation(null);
    setRecommendationError('');
    setPendingSkillAdvance(null);
    setStage('intro');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const startRetake = () => {
    const cleanDraft = { ...emptyDraft, writingAnswers: { ...emptyDraft.writingAnswers } };
    localStorage.removeItem(DRAFT_KEY);
    setDraft(cleanDraft);
    setResult(null);
    setRecommendation(null);
    setSubmitError('');
    setSkillIndex(0);
    setDeviceCheck(null);
    setSelectedExamType('IELTS');
    setSelectedSkillKeys([]);
    setStage('select');
    setSearchParams({}, { replace: true });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const exitToPlacementTest = () => {
    setSubmitError('');
    setPendingSkillAdvance(null);
    setSkillIndex(0);
    setStage('select');
    setSearchParams({}, { replace: true });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f8f4f1] px-4">
        <BrandLoadingState className="w-full max-w-3xl rounded-[28px]" message="Đang tải bài đánh giá đầu vào..." />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[#f8f4f1] px-6 text-center">
        <p className="font-bold text-red-700">{loadError}</p>
        <button className="rounded-xl bg-[#8a0018] px-5 py-3 font-bold text-white" onClick={() => window.location.reload()} type="button">Thử lại</button>
      </div>
    );
  }

  if (stage === 'select') {
    return (
      <div className="flex min-h-[100dvh] flex-col bg-[#f8f4f1]">
        <Header />
        <main className="mx-auto flex w-full max-w-6xl flex-1 items-center px-4 py-10">
          <div className="w-full rounded-[36px] border border-[#dfbfbd]/40 bg-white p-7 shadow-xl lg:p-11">
            <div className="flex flex-wrap items-start justify-between gap-6">
              <div className="max-w-3xl">
                <p className="text-xs font-black uppercase tracking-[0.2em] text-[#8a0018]">Placement Test</p>
                <h1 className="mt-4 font-['Manrope'] text-4xl font-black leading-tight text-[#341c1d]">Chọn dạng bài đánh giá đầu vào</h1>
                <p className="mt-5 leading-8 text-[#584140]">Chọn bài IELTS đầy đủ, TOEIC Listening & Reading hoặc chỉ đánh giá những kỹ năng bạn muốn kiểm tra.</p>
              </div>
              <button
                className="rounded-2xl border border-[#8a0018]/25 px-5 py-3 text-sm font-black text-[#8a0018] transition hover:bg-[#fff0f1]"
                onClick={() => navigate('/mock-tests')}
                type="button"
              >
                Kho đề thi thử
              </button>
            </div>

            <div className="mt-8 grid gap-5 lg:grid-cols-3">
              <button
                className="group rounded-[28px] border border-[#ead7d5] bg-[#fffaf9] p-6 text-left transition hover:-translate-y-0.5 hover:border-[#8a0018]/45 hover:shadow-[0_20px_45px_rgba(86,35,37,0.12)]"
                onClick={() => startExamType('IELTS')}
                type="button"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#fff0f1] text-[#8a0018]">
                  <PenLine aria-hidden="true" size={22} />
                </div>
                <h2 className="mt-5 font-['Manrope'] text-2xl font-black text-[#341c1d]">IELTS Placement</h2>
                <p className="mt-3 text-sm leading-7 text-[#584140]">Làm lần lượt Listening, Reading, Writing và Speaking theo giao diện thi hiện tại.</p>
                <div className="mt-5 grid gap-2 sm:grid-cols-2">
                  {SKILLS.map((skill) => {
                    const Icon = skill.icon;
                    return (
                      <span className="flex items-center gap-2 rounded-2xl bg-white px-4 py-3 text-sm font-black text-[#4b0009]" key={skill.key}>
                        <Icon aria-hidden="true" size={17} />
                        {skill.label}
                      </span>
                    );
                  })}
                </div>
                <span className="mt-6 inline-flex rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white group-disabled:opacity-50">
                  Chọn IELTS
                </span>
              </button>

              <button
                className="group rounded-[28px] border border-[#ead7d5] bg-[#f7fbff] p-6 text-left transition hover:-translate-y-0.5 hover:border-[#21446d]/45 hover:shadow-[0_20px_45px_rgba(33,68,109,0.12)]"
                onClick={() => startExamType('TOEIC')}
                type="button"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white text-[#21446d]">
                  <Headphones aria-hidden="true" size={22} />
                </div>
                <h2 className="mt-5 font-['Manrope'] text-2xl font-black text-[#21446d]">TOEIC Placement</h2>
                <p className="mt-3 text-sm leading-7 text-[#40536a]">Làm Listening và Reading theo format TOEIC mới (ETS 2026 Test 10). Hệ thống chấm khách quan theo answer key.</p>
                <div className="mt-5 grid gap-2 sm:grid-cols-2">
                  {TOEIC_SKILLS.map((skill) => {
                    const Icon = skill.icon;
                    return (
                      <span className="flex items-center gap-2 rounded-2xl bg-white px-4 py-3 text-sm font-black text-[#21446d]" key={skill.key}>
                        <Icon aria-hidden="true" size={17} />
                        {skill.label}
                      </span>
                    );
                  })}
                </div>
                <span className="mt-6 inline-flex rounded-2xl bg-[#21446d] px-5 py-3 text-sm font-black text-white group-disabled:opacity-50">
                  Chọn TOEIC
                </span>
              </button>

              <section className="rounded-[28px] border border-[#ead7d5] bg-[#f8f5ff] p-6 text-left">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white text-[#63368f]">
                  <Target aria-hidden="true" size={22} />
                </div>
                <h2 className="mt-5 font-['Manrope'] text-2xl font-black text-[#4d276f]">Đánh giá kỹ năng</h2>
                <p className="mt-3 text-sm leading-7 text-[#5d4a6e]">Chọn một hoặc nhiều kỹ năng và làm bài theo format IELTS tương ứng.</p>
                <div aria-label="Chọn kỹ năng cần đánh giá" className="mt-5 grid gap-2 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2" role="group">
                  {SKILLS.map((skill) => {
                    const Icon = skill.icon;
                    const selected = selectedSkillKeys.includes(skill.key);
                    return (
                      <button
                        aria-pressed={selected}
                        className={`flex min-h-12 items-center gap-2 rounded-2xl border px-4 py-3 text-sm font-black transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#63368f] focus-visible:ring-offset-2 ${selected ? 'border-[#63368f] bg-[#63368f] text-white' : 'border-[#ded3e8] bg-white text-[#4d276f] hover:border-[#63368f]/60'}`}
                        key={skill.key}
                        onClick={() => setSelectedSkillKeys((current) => current.includes(skill.key)
                          ? current.filter((key) => key !== skill.key)
                          : [...current, skill.key])}
                        type="button"
                      >
                        <Icon aria-hidden="true" size={17} />
                        {skill.label}
                        {selected ? <CheckCircle2 aria-hidden="true" className="ml-auto" size={16} /> : null}
                      </button>
                    );
                  })}
                </div>
                <button
                  className="mt-6 inline-flex min-h-12 items-center rounded-2xl bg-[#63368f] px-5 py-3 text-sm font-black text-white transition hover:bg-[#532c79] disabled:cursor-not-allowed disabled:opacity-45"
                  disabled={selectedSkillKeys.length === 0}
                  onClick={() => startExamType('SKILL', selectedSkillKeys)}
                  type="button"
                >
                  {selectedSkillKeys.length ? `Đánh giá ${selectedSkillKeys.length} kỹ năng` : 'Chọn ít nhất 1 kỹ năng'}
                </button>
              </section>
            </div>

            {test.latestAttempt ? (
              <button
                className="mt-6 flex w-full items-center justify-between gap-4 rounded-2xl border border-[#ead7d5] bg-[#fffaf9] p-4 text-left text-sm font-semibold text-[#341c1d] transition hover:border-[#8a0018]/40 hover:bg-[#fff3f4]"
                onClick={() => {
                  setResult(test.latestAttempt);
                  setSelectedExamType(test.latestAttempt.examType || 'IELTS');
                  setSelectedSkillKeys((test.latestAttempt.selectedSkills || []).map((skill) => String(skill).toLowerCase()));
                  setStage('result');
                  setSearchParams({ view: 'result' }, { replace: true });
                  window.scrollTo({ top: 0, behavior: 'smooth' });
                }}
                type="button"
              >
                <span>
                  Lần gần nhất: {test.latestAttempt.examType === 'TOEIC'
                    ? `TOEIC ${test.latestAttempt.overallScore ?? 'đang chấm'}`
                    : test.latestAttempt.examType === 'SKILL'
                      ? `Đánh giá ${test.latestAttempt.selectedSkills?.length || 1} kỹ năng · band ${test.latestAttempt.overallScore != null ? formatBandValue(test.latestAttempt.overallScore) : 'đang chấm'}`
                      : `band ${test.latestAttempt.overallScore != null ? formatBandValue(test.latestAttempt.overallScore) : 'đang chấm'}`} · {new Date(test.latestAttempt.submittedAt).toLocaleDateString('vi-VN')}
                </span>
                <span className="shrink-0 font-extrabold text-[#8a0018]">Xem kết quả</span>
              </button>
            ) : null}
          </div>
        </main>
        <CourseFooter />
      </div>
    );
  }

  if (stage === 'device') {
    return (
      <main className="min-h-screen bg-[#f8f4f1] px-4 py-10">
        <ExamDeviceCheck
          description={requiresMicrophone
            ? 'Kiểm tra microphone trước khi bắt đầu. Chế độ toàn màn hình sẽ được bật khi bạn vào phòng thi.'
            : selectedExamType === 'TOEIC' || activeSkills.some((skill) => skill.key === 'listening')
              ? 'Kiểm tra tai nghe trước khi bắt đầu. Chế độ toàn màn hình sẽ được bật khi bạn vào phòng thi.'
              : 'Xác nhận để bật chế độ toàn màn hình trước khi vào bài đánh giá.'}
          onComplete={(value) => {
            setDeviceCheck(value);
            setStage('exam');
          }}
          requireFullscreen
          requireMic={requiresMicrophone}
          requireSound={selectedExamType === 'TOEIC' || activeSkills.some((skill) => skill.key === 'listening')}
          title="Kiểm tra thiết bị trước khi làm bài"
        />
      </main>
    );
  }

  if (stage === 'result') {
    const recommendedLevel = isSkillResult ? null : recommendation?.recommendedLevel || result?.recommendedLevel;
    const levelLabel = recommendedLevel ? getPlacementLevelLabel(recommendedLevel) : 'Chưa phân loại';
    const weakSkills = recommendation?.weakSkills || [];

    return (
      <div className="flex min-h-[100dvh] flex-col bg-[#fcf9f8]">
        <Header />
        <main className="mx-auto flex w-full max-w-[1240px] flex-1 flex-col px-4 py-8 sm:px-6 lg:px-8 space-y-7">
          {/* Main Clean Card Container */}
          <div className="rounded-[28px] border border-[#ead9db] bg-white p-6 sm:p-9 shadow-sm space-y-7">
            {/* Header Title Section */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#ead9db] pb-6">
              <div>
                <span className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#730014]">
                  {isSkillResult ? 'Đánh giá kỹ năng' : 'Kết quả đánh giá đầu vào'}
                </span>
                <h1 className="mt-1.5 font-['Manrope'] text-2xl font-extrabold text-[#0b1c30] sm:text-3xl">
                  {isToeicResult ? 'Kết quả Placement TOEIC' : isSkillResult ? 'Kết quả đánh giá kỹ năng' : 'Kết quả Placement IELTS'}
                </h1>
                <p className="mt-1 text-xs text-[#584140]">
                  Ngày thực hiện: {new Date(result.submittedAt || Date.now()).toLocaleDateString('vi-VN')} · Lần thử {attemptCount}
                </p>
              </div>

              {/* Action CTAs inside Header */}
              <div className="flex flex-wrap items-center gap-2.5">
                <button
                  className="inline-flex items-center gap-1.5 rounded-xl border border-[#dfbfbd] bg-white px-4 py-2.5 text-xs font-bold text-[#730014] shadow-xs transition hover:bg-[#fff0f1] active:scale-95"
                  onClick={() => {
                    setResult(null);
                    setStage('select');
                    setSearchParams({}, { replace: true });
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }}
                  type="button"
                >
                  <RotateCcw className="h-3.5 w-3.5" /> Chọn lại đề
                </button>

                <button
                  className="inline-flex items-center gap-1.5 rounded-xl border border-[#730014]/30 bg-white px-4 py-2.5 text-xs font-bold text-[#730014] transition hover:bg-[#fff0f1] active:scale-95"
                  onClick={startRetake}
                  type="button"
                >
                  <RefreshCw className="h-3.5 w-3.5" /> Làm lại
                </button>

                <button
                  className="inline-flex items-center gap-1.5 rounded-xl bg-[#730014] px-4.5 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-[#8a0018] active:scale-95"
                  onClick={() => navigate('/complete-profile')}
                  type="button"
                >
                  Hoàn thiện hồ sơ <ArrowRight className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>

            {/* Score & Evaluation Summary Box */}
            <div className="grid gap-5">
              {/* Left Score Card */}
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-5 rounded-2xl border border-[#f5d0d3] bg-[#fff8f9] p-5 sm:p-6">
                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-[#8c716f]">
                    {isToeicResult ? 'Điểm TOEIC tổng' : isSkillResult ? (resultSkills.length > 1 ? 'Band trung bình kỹ năng đã chọn' : 'Band kỹ năng') : 'Band điểm tổng quan'}
                  </p>
                  <div className="mt-1 flex items-baseline gap-1.5">
                    <span className="font-['Manrope'] text-4xl sm:text-5xl font-black text-[#730014]">
                      {result.overallScore != null
                        ? (isToeicResult ? Math.round(Number(result.overallScore)) : formatBandValue(result.overallScore))
                        : '—'}
                    </span>
                    <span className="text-sm font-bold text-[#8c716f]">
                      {isToeicResult ? '/ 990' : '/ 9.0'}
                    </span>
                  </div>
                </div>

                <div className="flex flex-col items-start sm:items-end gap-2 border-t sm:border-t-0 sm:border-l border-[#ead9db] pt-3 sm:pt-0 sm:pl-6">
                  {recommendedLevel ? (
                    <div>
                      <span className="text-xs font-semibold text-slate-500">Trình độ đề xuất: </span>
                      <span className="font-extrabold text-[#730014]">{levelLabel}</span>
                    </div>
                  ) : null}

                  {weakSkills.length > 0 ? (
                    <div className="flex flex-wrap items-center gap-1.5 text-xs">
                      <span className="font-semibold text-slate-500">Cần cải thiện:</span>
                      {weakSkills.map((sk) => (
                        <span className="rounded-md bg-[#fff0f1] border border-[#f5d0d3] px-2 py-0.5 font-bold text-[#730014]" key={sk}>
                          {sk}
                        </span>
                      ))}
                    </div>
                  ) : null}

                  <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700">
                    <CheckCircle2 className="h-3.5 w-3.5" /> Hồ sơ đánh giá đã được lưu
                  </span>
                </div>
              </div>
            </div>

            {/* Detailed Skill Cards */}
            <div className="space-y-3">
              <h2 className="font-['Manrope'] text-lg font-bold text-[#0b1c30]">Kết quả từng kỹ năng</h2>
              <div className={`grid gap-4 sm:grid-cols-2 ${isToeicResult ? 'lg:grid-cols-2' : 'lg:grid-cols-4'}`}>
                {resultSkills.map((skill) => {
                  const Icon = skill.icon;
                  const rawScore = result[`${skill.key}Score`];
                  const displayScore = rawScore != null
                    ? (isToeicResult ? Math.round(Number(rawScore)) : formatBandValue(rawScore))
                    : '—';

                  return (
                    <div
                      className="flex flex-col justify-between rounded-2xl border border-[#ead9db] bg-[#fffaf9] p-4.5 transition hover:border-[#730014]"
                      key={skill.key}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2">
                          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-[#730014] border border-[#f5d0d3] shadow-xs">
                            <Icon className="h-4.5 w-4.5" />
                          </div>
                          <div>
                            <h3 className="font-bold text-[#0b1c30]">{skill.label}</h3>
                            <p className="text-[11px] text-slate-500">
                              {isToeicResult ? 'Tối đa 495' : 'Band 9.0'}
                            </p>
                          </div>
                        </div>
                      </div>

                      <div className="mt-4 flex items-baseline justify-between">
                        <span className="font-['Manrope'] text-3xl font-black text-[#730014]">
                          {displayScore}
                        </span>
                        <span className="text-xs font-semibold text-slate-500">
                          {skill.key === 'listening' ? (
                            `${result.correctListening ?? 0}/40 câu`
                          ) : skill.key === 'reading' ? (
                            `${result.correctReading ?? 0}/40 câu`
                          ) : (
                            'AI đánh giá'
                          )}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Recommendations Subsection */}
            {!isSkillResult ? (
              <PlacementRecommendationSection
                error={recommendationError}
                loading={recommendationLoading}
                onRetry={retryRecommendations}
                recommendation={recommendation}
              />
            ) : null}
          </div>
        </main>
        <CourseFooter />
      </div>
    );
  }

  if (stage === 'exam') {
    if (!activeConfig) {
      return (
        <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[#f8f4f1] px-6 text-center">
          <p className="font-bold text-red-700">Không tìm thấy dữ liệu phần thi {activeSkill?.label || ''}. Bản nháp của bạn vẫn được giữ.</p>
          <button className="rounded-xl bg-[#8a0018] px-5 py-3 font-bold text-white" onClick={() => setStage('intro')} type="button">
            Quay lại
          </button>
        </div>
      );
    }

    if (activeSkill.key === 'listening') {
      return (
        <>
          <ListeningExamMode
            assessment={{ title: activeConfig.title, timeLimitMinutes: activeConfig.durationMinutes }}
            config={activeConfig}
            exitDestinationLabel="màn hình chọn bài Placement Test"
            initialAnswers={toExamModeInitialObjectiveAnswers(activeConfig, draft.listeningAnswers)}
            onClose={exitToPlacementTest}
            onSubmit={handleListeningSubmit}
            preserveFullscreenOnUnmount
            skipAudioCheck
            submitLabel="Hoàn thành phần Listening"
            submitting={submitting}
          />
          {submitError ? <div className="fixed bottom-4 left-1/2 z-[140] w-[min(92vw,640px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700 shadow-xl">{submitError}</div> : null}
          {renderSkillAdvanceDialog()}
        </>
      );
    }

    if (activeSkill.key === 'reading') {
      return (
        <>
          <ReadingExamMode
            assessment={{ title: activeConfig.title, timeLimitMinutes: activeConfig.durationMinutes }}
            config={activeConfig}
            exitDestinationLabel="màn hình chọn bài Placement Test"
            initialAnswers={toExamModeInitialObjectiveAnswers(activeConfig, draft.readingAnswers)}
            onClose={exitToPlacementTest}
            onSubmit={handleReadingSubmit}
            preserveFullscreenOnUnmount
            submitLabel="Hoàn thành phần Reading"
            submitting={submitting}
          />
          {submitError ? <div className="fixed bottom-4 left-1/2 z-[140] w-[min(92vw,640px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700 shadow-xl">{submitError}</div> : null}
          {renderSkillAdvanceDialog()}
        </>
      );
    }

    if (activeSkill.key === 'writing') {
      return (
        <>
          <WritingExamMode
            assessment={{ title: activeConfig.title, timeLimitMinutes: activeConfig.durationMinutes }}
            config={activeConfig}
            exitDestinationLabel="màn hình chọn bài Placement Test"
            initialSubmissionText={toWritingSubmissionText(activeConfig, draft.writingAnswers)}
            onClose={exitToPlacementTest}
            onSubmit={handleWritingSubmit}
            preserveFullscreenOnUnmount
            submitLabel="Hoàn thành phần Writing"
            submitting={submitting}
          />
          {submitError ? <div className="fixed bottom-4 left-1/2 z-[140] w-[min(92vw,640px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700 shadow-xl">{submitError}</div> : null}
          {renderSkillAdvanceDialog()}
        </>
      );
    }

    return (
      <>
        <SpeakingExamMode
          config={{ ...toSpeakingExamConfig(activeConfig), submissionLabel: 'Đánh giá đầu vào' }}
          initialAudioUrl={draft.speakingAudioUrl}
          onAudioReady={(speakingAudioUrl) => {
            setDraft((current) => ({ ...current, speakingAudioUrl }));
          }}
          onClose={exitToPlacementTest}
          onSubmit={(payload) => {
            setDraft((current) => ({
              ...current,
              speakingTranscript: payload.submittedText,
              speakingAudioUrl: payload.submittedAudioUrl,
            }));
            return handlePlacementSpeakingSubmit('placement-speaking', payload);
          }}
          skipDeviceCheck
          submitting={submitting}
          uploadAudio={placementTestApi.uploadSpeakingAudio}
        />
        {submitError ? <div className="fixed bottom-4 left-1/2 z-[140] w-[min(92vw,640px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700 shadow-xl">{submitError}</div> : null}
      </>
    );
  }

  return (
    <div className="flex min-h-[100dvh] flex-col bg-[#f8f4f1]">
      <Header />
      <main className="mx-auto flex w-full max-w-6xl flex-1 items-center px-4 py-10">
        <div className="grid w-full gap-8 rounded-[36px] border border-[#dfbfbd]/40 bg-white p-7 shadow-xl lg:grid-cols-[1.1fr_0.9fr] lg:p-11">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.2em] text-[#8a0018]">Đánh giá đầu vào</p>
            <h1 className="mt-4 font-['Manrope'] text-4xl font-black leading-tight text-[#341c1d]">
              {selectedExamType === 'TOEIC'
                ? 'Placement TOEIC Listening & Reading'
                : selectedExamType === 'SKILL'
                  ? `Đánh giá ${activeSkills.length} kỹ năng đã chọn`
                  : 'Placement IELTS đủ 4 kỹ năng'}
            </h1>
            <p className="mt-5 max-w-2xl leading-8 text-[#584140]">
              {selectedExamType === 'TOEIC'
                ? 'Bạn sẽ kiểm tra thiết bị một lần, sau đó làm Listening và Reading theo format TOEIC. Kết quả được dùng để đề xuất lộ trình học phù hợp.'
                : selectedExamType === 'SKILL'
                  ? 'Bạn sẽ lần lượt làm các phần đã chọn theo format IELTS. Kết quả chỉ phản ánh những kỹ năng này.'
                  : 'Bạn sẽ kiểm tra thiết bị một lần, sau đó làm lần lượt Listening, Reading, Writing và Speaking. Kết quả được dùng để đề xuất lộ trình học phù hợp.'}
            </p>

            <div className="mt-5 rounded-2xl border border-[#e9c9c2] bg-[#fff8f6] p-4 text-sm font-semibold leading-7 text-[#7a3430]">
              {selectedExamType === 'SKILL'
                ? 'Kết quả đánh giá kỹ năng không thay thế kết quả Placement IELTS hoặc TOEIC.'
                : 'Hãy làm bài cẩn trọng vì kết quả được dùng để đánh giá trình độ đầu vào và gợi ý lộ trình học phù hợp.'}
            </div>

            <div className="mt-7 grid gap-3 sm:grid-cols-2">
              {activeSkills.map((skill) => {
                const Icon = skill.icon;
                return (
                  <div className="flex items-center gap-3 rounded-2xl bg-[#fff0f1] p-4" key={skill.key}>
                    <Icon className="text-[#8a0018]" size={21} />
                    <strong>{skill.label}</strong>
                  </div>
                );
              })}
            </div>

            {test.latestAttempt ? (
              <button
                className="mt-5 flex w-full items-center justify-between gap-4 rounded-2xl border border-[#ead7d5] bg-[#fffaf9] p-4 text-left text-sm font-semibold text-[#341c1d] transition hover:border-[#8a0018]/40 hover:bg-[#fff3f4]"
                onClick={() => {
                  setResult(test.latestAttempt);
                  setSelectedExamType(test.latestAttempt.examType || 'IELTS');
                  setSelectedSkillKeys((test.latestAttempt.selectedSkills || []).map((skill) => String(skill).toLowerCase()));
                  setStage('result');
                  setSearchParams({ view: 'result' }, { replace: true });
                  window.scrollTo({ top: 0, behavior: 'smooth' });
                }}
                type="button"
              >
                <span>
                  Lần gần nhất: {test.latestAttempt.examType === 'TOEIC'
                    ? `TOEIC ${test.latestAttempt.overallScore ?? 'đang chấm'}`
                    : test.latestAttempt.examType === 'SKILL'
                      ? `Đánh giá ${test.latestAttempt.selectedSkills?.length || 1} kỹ năng · band ${test.latestAttempt.overallScore != null ? formatBandValue(test.latestAttempt.overallScore) : 'đang chấm'}`
                      : `band ${test.latestAttempt.overallScore != null ? formatBandValue(test.latestAttempt.overallScore) : 'đang chấm'}`} · {new Date(test.latestAttempt.submittedAt).toLocaleDateString('vi-VN')}
                </span>
                <span className="shrink-0 font-extrabold text-[#8a0018]">Xem kết quả</span>
              </button>
            ) : null}
          </div>

          <aside className="rounded-[28px] bg-[linear-gradient(145deg,#4b0009,#8a0018)] p-7 text-white">
            <h2 className="font-['Manrope'] text-2xl font-black">Trước khi bắt đầu</h2>
            <ul className="mt-5 space-y-4 text-sm leading-7 text-white/85">
              <li>• Chuẩn bị khoảng {estimatedMinutes || (selectedExamType === 'TOEIC' ? 120 : 170)} phút.</li>
              <li>• Dùng Chrome hoặc Edge{requiresMicrophone ? ' và cấp quyền microphone.' : '.'}</li>
              <li>• Không tải lại trang; bản nháp được lưu tự động trên thiết bị.</li>
              <li>• Nếu mất mạng lúc nộp, hãy thử lại — bài làm không bị xóa.</li>
              <li>• Bạn có thể làm lại bài đánh giá khi muốn kiểm tra trình độ mới nhất.</li>
            </ul>

            <button className="mt-7 w-full rounded-2xl bg-white px-6 py-4 font-black text-[#650012]" onClick={() => setStage('device')} type="button">
              {attemptCount ? 'Làm lại bài' : 'Kiểm tra thiết bị'}
            </button>

            <button
              className="mt-3 w-full rounded-2xl border border-white/40 px-6 py-3 text-sm font-black text-white transition hover:bg-white/10"
              onClick={() => setStage('select')}
              type="button"
            >
              Đổi dạng bài
            </button>

            {hasDraftProgress ? (
              <p className="mt-4 text-sm text-white/80">Đã có bản nháp trên thiết bị này.</p>
            ) : null}
          </aside>
        </div>
      </main>
      <CourseFooter />
    </div>
  );
}

