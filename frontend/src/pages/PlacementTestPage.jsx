import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { BookOpen, Headphones, Mic, PenLine } from 'lucide-react';
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
import { formatBandValue } from '../utils/selfPacedHelpers';

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
    prompt: question.prompt || question.question || question.text || `CĂ¢u ${fallbackNumber}`,
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
      window.alert('KhĂ´ng thá»ƒ truy cáº­p microphone. HĂ£y kiá»ƒm tra quyá»n cá»§a trĂ¬nh duyá»‡t.');
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
                <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">Báº£n ghi Speaking</p>
                <h3 className="mt-1 font-['Manrope'] text-2xl font-black text-[#341c1d]">Ghi má»™t láº§n cho toĂ n bá»™ bĂ i nĂ³i</h3>
              </div>
              <div className={`rounded-full px-4 py-2 text-sm font-extrabold ${audioUrl ? 'bg-[#8a0018] text-white' : 'bg-[#fff0f1] text-[#8a0018]'}`}>
                {audioUrl ? 'ÄĂ£ cĂ³ báº£n ghi' : 'ChÆ°a cĂ³ báº£n ghi'}
              </div>
            </div>

            <p className="mt-4 text-sm leading-7 text-[#584140]">Thiáº¿t bá»‹ Ä‘Ă£ Ä‘Æ°á»£c kiá»ƒm tra trÆ°á»›c khi báº¯t Ä‘áº§u bĂ i Ä‘Ă¡nh giĂ¡. HĂ£y ghi Ă¢m má»™t láº§n cho cáº£ ba pháº§n Speaking.</p>

            <div className="mt-5 flex flex-wrap gap-3">
              <button
                className={`rounded-full px-5 py-3 text-sm font-black text-white transition ${recording ? 'bg-red-700 hover:bg-red-800' : 'bg-[linear-gradient(135deg,#8a0018,#650012)] hover:brightness-105'}`}
                onClick={recording ? stopRecording : startRecording}
                type="button"
              >
                {recording ? 'Dá»«ng vĂ  táº£i báº£n ghi' : 'Báº¯t Ä‘áº§u ghi Ă¢m'}
              </button>
              <div className="rounded-full border border-[#dfbfbd] px-4 py-3 text-sm font-semibold text-[#6f5a58]">
                {uploading ? 'Äang táº£i báº£n ghi...' : audioUrl ? 'Báº£n ghi Ä‘Ă£ Ä‘Æ°á»£c lÆ°u.' : 'ChÆ°a táº£i báº£n ghi nĂ o.'}
              </div>
            </div>

            {previewUrl ? <audio className="mt-4 w-full" controls src={previewUrl} /> : null}

            <label className="mt-6 block text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">Ná»™i dung Ä‘Ă£ nĂ³i, náº¿u cáº§n bá»• sung</label>
            <textarea
              className="mt-3 min-h-[46vh] w-full rounded-[24px] border border-[#dfbfbd]/60 bg-white px-5 py-4 text-[15px] leading-8 text-[#2b1718] outline-none transition focus:border-[#8a0018]"
              onChange={(event) => onTranscriptChange(event.target.value)}
              placeholder="Náº¿u cáº§n, báº¡n cĂ³ thá»ƒ ghi láº¡i ná»™i dung hoáº·c Ă½ chĂ­nh cá»§a pháº§n nĂ³i á»Ÿ Ä‘Ă¢y..."
              spellCheck={false}
              value={transcript}
            />

            <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-[#6f5a58]">
              <span>Sá»‘ tá»« Ä‘Ă£ ghi láº¡i: {countWords(transcript)}</span>
              <span>Báº¡n cĂ³ thá»ƒ ná»™p báº±ng báº£n ghi Ă¢m, ná»™i dung Ä‘Ă£ nĂ³i hoáº·c cáº£ hai.</span>
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
  submitLabel = 'Ná»™p toĂ n bá»™ bĂ i thi',
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
        warn('Báº¡n vá»«a rá»i khá»i tab hoáº·c thu nhá» cá»­a sá»• trong lĂºc lĂ m bĂ i Speaking.');
      }
    };

    const handleBlur = () => {
      warn('Cá»­a sá»• bĂ i thi Speaking Ä‘Ă£ máº¥t focus.');
    };

    const handlePopState = () => {
      pushExamState();
      warn('KhĂ´ng thá»ƒ quay láº¡i trang khĂ¡c trong lĂºc Ä‘ang lĂ m bĂ i Speaking.');
    };

    const handleFullscreen = () => {
      if (!document.fullscreenElement && !intentionalExitRef.current) {
        void restoreFullscreen();
        warn('KhĂ´ng thá»ƒ thoĂ¡t toĂ n mĂ n hĂ¬nh trong lĂºc Ä‘ang thi Speaking.');
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
          ? 'Báº¡n khĂ´ng thá»ƒ dĂ¹ng phĂ­m Esc Ä‘á»ƒ thoĂ¡t toĂ n mĂ n hĂ¬nh trong khi Ä‘ang thi Speaking.'
          : 'Má»™t thao tĂ¡c Ä‘iá»u hÆ°á»›ng hoáº·c sao chĂ©p ngoĂ i bĂ i thi Speaking vá»«a bá»‹ cháº·n.'
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
          <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#9a6e67]">ÄĂ¡nh giĂ¡ Ä‘áº§u vĂ o EnglishLab</p>
          <h2 className="font-['Manrope'] text-lg font-extrabold text-[#341c1d]">{assessment?.title || config?.title}</h2>
        </div>

        <div className="flex items-center gap-3">
          <div className="rounded-full bg-[#fff0f1] px-5 py-2 text-xl font-black text-[#8a0018] shadow-[0_10px_24px_rgba(138,0,24,0.10)]">
            {formatTimer(remainingSeconds)}
          </div>
          <span className="rounded-full bg-[#8a0018] px-4 py-2 text-xs font-bold text-white">Vi pháº¡m: {violations.length}</span>
          <button
            className="rounded-full border border-[#8a0018]/20 px-5 py-2 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
            onClick={() => setExitConfirmOpen(true)}
            type="button"
          >
            ThoĂ¡t
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
            {submitting || submissionPending ? 'Äang lÆ°u...' : submitLabel}
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
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#b26a00]">Cáº£nh bĂ¡o bĂ i thi</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">HĂ£y quay láº¡i bĂ i Speaking</h3>
            <p className="mt-3 text-sm leading-7 text-[#584140]">{warning.reason}</p>
            <button
              className="mt-5 w-full rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white"
              onClick={async () => {
                await restoreFullscreen();
                setWarning(null);
              }}
              type="button"
            >
              Tiáº¿p tá»¥c lĂ m bĂ i
            </button>
          </div>
        </div>
      ) : null}

      {exitConfirmOpen ? (
        <div className="fixed inset-0 z-[130] flex items-center justify-center bg-[#261112]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8a0018]">ThoĂ¡t cháº¿ Ä‘á»™ thi?</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">BĂ i Speaking hiá»‡n chÆ°a Ä‘Æ°á»£c ná»™p</h3>
            <p className="mt-3 text-sm leading-7 text-[#584140]">Náº¿u báº¡n thoĂ¡t bĂ¢y giá», EnglishLab sáº½ quay vá» mĂ n hĂ¬nh báº¯t Ä‘áº§u Ä‘Ă¡nh giĂ¡. Báº£n nhĂ¡p váº«n Ä‘Æ°á»£c giá»¯ vĂ  pháº§n Speaking nĂ y chÆ°a Ä‘Æ°á»£c ghi nháº­n ná»™p.</p>
            <div className="mt-5 flex gap-3">
              <button
                className="flex-1 rounded-2xl border border-[#dfbfbd] px-5 py-3 text-sm font-bold text-[#8a0018]"
                onClick={() => setExitConfirmOpen(false)}
                type="button"
              >
                á» láº¡i
              </button>
              <button
                className="flex-1 rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white"
                onClick={handleCloseExam}
                type="button"
              >
                ThoĂ¡t
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
  const [skillIndex, setSkillIndex] = useState(0);
  const [draft, setDraft] = useState(readDraft);
  const [deviceCheck, setDeviceCheck] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [result, setResult] = useState(null);
  const [pendingSkillAdvance, setPendingSkillAdvance] = useState(null);

  const activeSkills = selectedExamType === 'TOEIC' ? TOEIC_SKILLS : SKILLS;
  const activeSkill = activeSkills[skillIndex];
  const activeConfig = selectedExamType === 'TOEIC'
    ? toToeicExamSection(test?.sections?.toeic, activeSkill?.key)
    : test?.sections?.[activeSkill?.key];
  const attemptCount = Number(test?.attemptCount || 0);
  const maxAttempts = Number(test?.maxAttempts || 3);
  const canRetake = Boolean(test?.canRetake) && attemptCount < maxAttempts;
  const resultExamType = result?.examType || selectedExamType || 'IELTS';
  const isToeicResult = resultExamType === 'TOEIC';
  const resultSkills = isToeicResult ? TOEIC_SKILLS : SKILLS;

  useEffect(() => {
    const loadCurrentTest = async () => {
      try {
        const response = await placementTestApi.getCurrent();
        const sections = response?.sections;
        const missingSkills = SKILLS.filter((skill) => !sections?.[skill.key]);

        if (missingSkills.length) {
          throw new Error(`Äá» thi Ä‘ang thiáº¿u dá»¯ liá»‡u: ${missingSkills.map((skill) => skill.label).join(', ')}`);
        }

        setTest(response);

        if (searchParams.get('view') === 'result' && response.latestAttempt) {
          setResult(response.latestAttempt);
          setSelectedExamType(response.latestAttempt.examType || 'IELTS');
          setStage('result');
        }
      } catch (error) {
        setLoadError(error?.response?.data?.message || error?.message || 'KhĂ´ng táº£i Ä‘Æ°á»£c Ä‘á» thi thá»­.');
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
        skipSpeakingValidation: selectedExamType === 'TOEIC',
        draftOverride,
      });
      return;
    }
    setSkillIndex((current) => Math.min(current + 1, activeSkills.length - 1));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const submitAll = async ({ skipSpeakingValidation = false, draftOverride = null } = {}) => {
    const submissionDraft = draftOverride || draft;
    if (selectedExamType !== 'TOEIC' && !skipSpeakingValidation && !submissionDraft.speakingAudioUrl && !submissionDraft.speakingTranscript.trim()) {
      setSubmitError('HĂ£y hoĂ n thĂ nh báº£n ghi Ă¢m cho pháº§n NĂ³i trÆ°á»›c khi ná»™p bĂ i.');
      return;
    }

    setSubmitting(true);
    setSubmitError('');

    try {
      const response = await placementTestApi.submitCurrent({
        testCode: test.testCode,
        examType: selectedExamType,
        listeningAnswers: submissionDraft.listeningAnswers,
        readingAnswers: submissionDraft.readingAnswers,
        writingAnswers: selectedExamType === 'TOEIC' ? {} : submissionDraft.writingAnswers,
        speakingTranscript: selectedExamType === 'TOEIC' ? '' : submissionDraft.speakingTranscript,
        speakingAudioUrl: selectedExamType === 'TOEIC' ? '' : submissionDraft.speakingAudioUrl,
        deviceCheck,
      });

      setResult(response);
      setStage('result');
      setSearchParams({ view: 'result' }, { replace: true });

      setTest((current) => current ? {
        ...current,
        latestAttempt: response,
        attemptCount: Number(current.attemptCount || 0) + 1,
        canRetake: Number(current.attemptCount || 0) + 1 < Number(current.maxAttempts || 3),
      } : current);

      if (response.status === 'COMPLETED') {
        localStorage.removeItem(DRAFT_KEY);
      }
    } catch (error) {
      setSubmitError(error?.response?.data?.message || 'ChÆ°a thá»ƒ ná»™p bĂ i. Báº£n nhĂ¡p váº«n Ä‘Æ°á»£c giá»¯ trĂªn thiáº¿t bá»‹ nĂ y.');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePlacementSpeakingSubmit = async (_assessmentId, payload = {}) => {
    const speakingTranscript = String(payload.submittedText || '').trim();
    const speakingAudioUrl = String(payload.submittedAudioUrl || '').trim();

    if (!speakingAudioUrl && !speakingTranscript) {
      setSubmitError('HĂ£y hoĂ n thĂ nh báº£n ghi Ă¢m cho pháº§n NĂ³i trÆ°á»›c khi ná»™p bĂ i.');
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
        canRetake: Number(current.attemptCount || 0) + 1 < Number(current.maxAttempts || 3),
      } : current);
      if (response.status === 'COMPLETED') {
        localStorage.removeItem(DRAFT_KEY);
      }
      return response;
    } catch (error) {
      setSubmitError(error?.response?.data?.message || 'ChÆ°a thá»ƒ ná»™p bĂ i. BĂ i lĂ m cá»§a báº¡n váº«n Ä‘Æ°á»£c lÆ°u an toĂ n.');
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
        setPendingSkillAdvance({ missingCount, unitLabel: 'cĂ¢u' });
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
        setPendingSkillAdvance({ missingCount, unitLabel: 'cĂ¢u' });
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
      setDraft((current) => ({ ...current, writingAnswers: { ...current.writingAnswers, ...nextAnswers } }));
      const parsed = parseObjectivePayload(payload);
      const incompleteTasks = (parsed.tasks || []).filter((task) => Number(task.wordCount || 0) < Number(task.minimumWords || 0)).length;
      if (incompleteTasks > 0) {
        setPendingSkillAdvance({ missingCount: incompleteTasks, unitLabel: 'task' });
        return;
      }
      await goToNextSkill();
    } finally {
      setSubmitting(false);
    }
  };

  const renderSkillAdvanceDialog = () => pendingSkillAdvance ? (
    <ExamSectionChangeDialog
      currentLabel={activeSkill?.label || 'Pháº§n hiá»‡n táº¡i'}
      missingCount={pendingSkillAdvance.missingCount}
      onCancel={() => setPendingSkillAdvance(null)}
      onConfirm={() => {
        setPendingSkillAdvance(null);
        void goToNextSkill();
      }}
      targetLabel={activeSkills[skillIndex + 1]?.label || 'ná»™p bĂ i'}
      unitLabel={pendingSkillAdvance.unitLabel}
    />
  ) : null;

  const startExamType = (examType) => {
    setSelectedExamType(examType);
    setSkillIndex(0);
    setSubmitError('');
    setPendingSkillAdvance(null);
    setStage('intro');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const startRetake = () => {
    if (!canRetake) return;
    const cleanDraft = { ...emptyDraft, writingAnswers: { ...emptyDraft.writingAnswers } };
    localStorage.removeItem(DRAFT_KEY);
    setDraft(cleanDraft);
    setResult(null);
    setSubmitError('');
    setSkillIndex(0);
    setDeviceCheck(null);
    setSelectedExamType('IELTS');
    setStage('select');
    setSearchParams({}, { replace: true });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#f8f4f1] px-4">
        <BrandLoadingState className="w-full max-w-3xl rounded-[28px]" message="Äang táº£i bĂ i Ä‘Ă¡nh giĂ¡ Ä‘áº§u vĂ o..." />
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[#f8f4f1] px-6 text-center">
        <p className="font-bold text-red-700">{loadError}</p>
        <button className="rounded-xl bg-[#8a0018] px-5 py-3 font-bold text-white" onClick={() => window.location.reload()} type="button">Thá»­ láº¡i</button>
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
                <h1 className="mt-4 font-['Manrope'] text-4xl font-black leading-tight text-[#341c1d]">Chá»n dáº¡ng bĂ i Ä‘Ă¡nh giĂ¡ Ä‘áº§u vĂ o</h1>
                <p className="mt-5 leading-8 text-[#584140]">Báº¡n cĂ³ thá»ƒ lĂ m bĂ i theo format IELTS Ä‘áº§y Ä‘á»§ 4 ká»¹ nÄƒng hoáº·c TOEIC Listening & Reading. Káº¿t quáº£ Ä‘Æ°á»£c dĂ¹ng Ä‘á»ƒ gá»£i Ă½ lá»™ trĂ¬nh há»c phĂ¹ há»£p.</p>
              </div>
              <button
                className="rounded-2xl border border-[#8a0018]/25 px-5 py-3 text-sm font-black text-[#8a0018] transition hover:bg-[#fff0f1]"
                onClick={() => navigate('/mock-tests')}
                type="button"
              >
                VĂ o Mock Test
              </button>
            </div>

            <div className="mt-8 grid gap-5 md:grid-cols-2">
              <button
                className="group rounded-[28px] border border-[#ead7d5] bg-[#fffaf9] p-6 text-left transition hover:-translate-y-0.5 hover:border-[#8a0018]/45 hover:shadow-[0_20px_45px_rgba(86,35,37,0.12)]"
                disabled={!canRetake}
                onClick={() => startExamType('IELTS')}
                type="button"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#fff0f1] text-[#8a0018]">
                  <PenLine aria-hidden="true" size={22} />
                </div>
                <h2 className="mt-5 font-['Manrope'] text-2xl font-black text-[#341c1d]">IELTS Placement</h2>
                <p className="mt-3 text-sm leading-7 text-[#584140]">LĂ m láº§n lÆ°á»£t Listening, Reading, Writing vĂ  Speaking theo giao diá»‡n thi hiá»‡n táº¡i.</p>
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
                  {canRetake ? 'Chá»n IELTS' : 'ÄĂ£ dĂ¹ng háº¿t lÆ°á»£t lĂ m'}
                </span>
              </button>

              <button
                className="group rounded-[28px] border border-[#ead7d5] bg-[#f7fbff] p-6 text-left transition hover:-translate-y-0.5 hover:border-[#21446d]/45 hover:shadow-[0_20px_45px_rgba(33,68,109,0.12)]"
                disabled={!canRetake}
                onClick={() => startExamType('TOEIC')}
                type="button"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white text-[#21446d]">
                  <Headphones aria-hidden="true" size={22} />
                </div>
                <h2 className="mt-5 font-['Manrope'] text-2xl font-black text-[#21446d]">TOEIC Placement</h2>
                <p className="mt-3 text-sm leading-7 text-[#40536a]">LĂ m Listening vĂ  Reading theo format TOEIC má»›i (ETS 2026 Test 10). Há»‡ thá»‘ng cháº¥m khĂ¡ch quan theo answer key.</p>
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
                  {canRetake ? 'Chá»n TOEIC' : 'ÄĂ£ dĂ¹ng háº¿t lÆ°á»£t lĂ m'}
                </span>
              </button>
            </div>

            {test.latestAttempt ? (
              <button
                className="mt-6 flex w-full items-center justify-between gap-4 rounded-2xl border border-[#ead7d5] bg-[#fffaf9] p-4 text-left text-sm font-semibold text-[#341c1d] transition hover:border-[#8a0018]/40 hover:bg-[#fff3f4]"
                onClick={() => {
                  setResult(test.latestAttempt);
                  setSelectedExamType(test.latestAttempt.examType || 'IELTS');
                  setStage('result');
                  setSearchParams({ view: 'result' }, { replace: true });
                  window.scrollTo({ top: 0, behavior: 'smooth' });
                }}
                type="button"
              >
                <span>
                  Láº§n gáº§n nháº¥t: {test.latestAttempt.examType === 'TOEIC'
                    ? `TOEIC ${test.latestAttempt.overallScore ?? 'Ä‘ang cháº¥m'}`
                    : `band ${test.latestAttempt.overallScore != null ? formatBandValue(test.latestAttempt.overallScore) : 'Ä‘ang cháº¥m'}`} Â· {new Date(test.latestAttempt.submittedAt).toLocaleDateString('vi-VN')}
                </span>
                <span className="shrink-0 font-extrabold text-[#8a0018]">Xem káº¿t quáº£</span>
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
          description={selectedExamType === 'TOEIC'
            ? 'Kiá»ƒm tra tai nghe trÆ°á»›c khi báº¯t Ä‘áº§u bĂ i TOEIC. Cháº¿ Ä‘á»™ toĂ n mĂ n hĂ¬nh sáº½ Ä‘Æ°á»£c báº­t khi báº¡n vĂ o phĂ²ng thi.'
            : 'Kiá»ƒm tra tai nghe vĂ  microphone trÆ°á»›c khi báº¯t Ä‘áº§u bĂ i Ä‘Ă¡nh giĂ¡ Ä‘áº§u vĂ o. Cháº¿ Ä‘á»™ toĂ n mĂ n hĂ¬nh sáº½ Ä‘Æ°á»£c báº­t khi báº¡n vĂ o phĂ²ng thi.'}
          onComplete={(value) => {
            setDeviceCheck(value);
            setStage('exam');
          }}
          requireFullscreen={false}
          requireMic={selectedExamType !== 'TOEIC'}
          title="Kiá»ƒm tra thiáº¿t bá»‹ trÆ°á»›c khi lĂ m bĂ i"
        />
      </main>
    );
  }

  if (stage === 'result') {
    return (
      <div className="flex min-h-[100dvh] flex-col bg-[#f8f4f1]">
        <Header />
        <main className="mx-auto flex w-full max-w-5xl flex-1 items-center px-4 py-12">
          <div className="w-full rounded-[34px] border border-[#dfbfbd]/40 bg-white p-7 shadow-xl md:p-10">
            <p className="text-xs font-black uppercase tracking-[0.18em] text-[#8a0018]">Káº¿t quáº£ Ä‘Ă¡nh giĂ¡ Ä‘áº§u vĂ o</p>
            <h1 className="mt-3 font-['Manrope'] text-4xl font-black text-[#341c1d]">
              {isToeicResult
                ? `TOEIC tá»•ng: ${result.overallScore != null ? Math.round(Number(result.overallScore)) : 'Äang cháº¥m'}`
                : `Band tá»•ng quan: ${result.overallScore != null ? formatBandValue(result.overallScore) : 'Äang cháº¥m'}`}
            </h1>

            <div className={`mt-7 grid gap-4 sm:grid-cols-2 ${isToeicResult ? 'lg:grid-cols-2' : 'lg:grid-cols-4'}`}>
              {resultSkills.map((skill) => (
                <div className="rounded-2xl bg-[#fff0f1] p-5" key={skill.key}>
                  <p className="text-sm font-bold text-[#7a4a4e]">{skill.label}</p>
                  <p className="mt-2 text-3xl font-black text-[#8a0018]">
                    {result[`${skill.key}Score`] != null
                      ? (isToeicResult ? Math.round(Number(result[`${skill.key}Score`])) : formatBandValue(result[`${skill.key}Score`]))
                      : 'â€”'}
                  </p>
                </div>
              ))}
            </div>

            <p className="mt-6 rounded-2xl border border-[#ead7d5] bg-[#fffaf9] p-5 text-sm leading-7 text-[#584140]">
              {isToeicResult
                ? `Listening: ${result.correctListening ?? 0} cĂ¢u Ä‘Ăºng Â· Reading: ${result.correctReading ?? 0} cĂ¢u Ä‘Ăºng. Káº¿t quáº£ TOEIC Ä‘Ă£ Ä‘Æ°á»£c lÆ°u theo bĂ i Listening & Reading.`
                : (
                  <>
                    Listening: {result.correctListening}/40 cĂ¢u Ä‘Ăºng Â· Reading: {result.correctReading}/40 cĂ¢u Ä‘Ăºng.
                    {result.status === 'OBJECTIVE_EVALUATED'
                      ? ' Writing vĂ  Speaking chÆ°a cĂ³ Ä‘iá»ƒm do dá»‹ch vá»¥ AI táº¡m thá»i khĂ´ng sáºµn sĂ ng. Báº£n nhĂ¡p váº«n Ä‘Æ°á»£c giá»¯ Ä‘á»ƒ báº¡n ná»™p láº¡i.'
                      : ' Káº¿t quáº£ Ä‘Ă£ Ä‘Æ°á»£c lÆ°u vĂ o há»“ sÆ¡ Ä‘Ă¡nh giĂ¡ Ä‘áº§u vĂ o.'}
                  </>
                )}
            </p>

            <div className="mt-6 flex flex-wrap gap-3">
              <button
                className="rounded-2xl border border-[#8a0018]/25 px-6 py-4 font-black text-[#8a0018] transition hover:bg-[#fff0f1]"
                onClick={() => {
                  setResult(null);
                  setStage('select');
                  setSearchParams({}, { replace: true });
                  window.scrollTo({ top: 0, behavior: 'smooth' });
                }}
                type="button"
              >
                Quay láº¡i chá»n Ä‘á»
              </button>
              {canRetake ? (
                <button className="rounded-2xl border border-[#8a0018]/25 px-6 py-4 font-black text-[#8a0018] transition hover:bg-[#fff0f1]" onClick={startRetake} type="button">
                  LĂ m láº¡i ({attemptCount + 1}/{maxAttempts})
                </button>
              ) : null}
              <button className="rounded-2xl bg-[#8a0018] px-6 py-4 font-black text-white" onClick={() => navigate('/complete-profile')} type="button">
                Tiáº¿p tá»¥c hoĂ n thiá»‡n há»“ sÆ¡
              </button>
            </div>
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
          <p className="font-bold text-red-700">KhĂ´ng tĂ¬m tháº¥y dá»¯ liá»‡u pháº§n thi {activeSkill?.label || ''}. Báº£n nhĂ¡p cá»§a báº¡n váº«n Ä‘Æ°á»£c giá»¯.</p>
          <button className="rounded-xl bg-[#8a0018] px-5 py-3 font-bold text-white" onClick={() => setStage('intro')} type="button">
            Quay láº¡i
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
            initialAnswers={toExamModeInitialObjectiveAnswers(activeConfig, draft.listeningAnswers)}
            onClose={() => navigate('/')}
            onSubmit={handleListeningSubmit}
            preserveFullscreenOnUnmount
            skipAudioCheck
            submitLabel="HoĂ n thĂ nh pháº§n Listening"
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
            initialAnswers={toExamModeInitialObjectiveAnswers(activeConfig, draft.readingAnswers)}
            onClose={() => navigate('/')}
            onSubmit={handleReadingSubmit}
            preserveFullscreenOnUnmount
            submitLabel="HoĂ n thĂ nh pháº§n Reading"
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
            initialSubmissionText={toWritingSubmissionText(activeConfig, draft.writingAnswers)}
            onClose={() => navigate('/')}
            onSubmit={handleWritingSubmit}
            preserveFullscreenOnUnmount
            submitLabel="HoĂ n thĂ nh pháº§n Writing"
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
          config={{ ...toSpeakingExamConfig(activeConfig), submissionLabel: 'ÄĂ¡nh giĂ¡ Ä‘áº§u vĂ o' }}
          initialAudioUrl={draft.speakingAudioUrl}
          onAudioReady={(speakingAudioUrl) => {
            setDraft((current) => ({ ...current, speakingAudioUrl }));
          }}
          onClose={() => {
            setSubmitError('');
            setStage('intro');
          }}
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
            <p className="text-xs font-black uppercase tracking-[0.2em] text-[#8a0018]">ÄĂ¡nh giĂ¡ Ä‘áº§u vĂ o</p>
            <h1 className="mt-4 font-['Manrope'] text-4xl font-black leading-tight text-[#341c1d]">
              {selectedExamType === 'TOEIC' ? 'Placement TOEIC Listening & Reading' : 'Placement IELTS Ä‘á»§ 4 ká»¹ nÄƒng'}
            </h1>
            <p className="mt-5 max-w-2xl leading-8 text-[#584140]">
              {selectedExamType === 'TOEIC'
                ? 'Báº¡n sáº½ kiá»ƒm tra thiáº¿t bá»‹ má»™t láº§n, sau Ä‘Ă³ lĂ m Listening vĂ  Reading theo format TOEIC. Káº¿t quáº£ Ä‘Æ°á»£c dĂ¹ng Ä‘á»ƒ Ä‘á» xuáº¥t lá»™ trĂ¬nh há»c phĂ¹ há»£p.'
                : 'Báº¡n sáº½ kiá»ƒm tra thiáº¿t bá»‹ má»™t láº§n, sau Ä‘Ă³ lĂ m láº§n lÆ°á»£t Listening, Reading, Writing vĂ  Speaking. Káº¿t quáº£ Ä‘Æ°á»£c dĂ¹ng Ä‘á»ƒ Ä‘á» xuáº¥t lá»™ trĂ¬nh há»c phĂ¹ há»£p.'}
            </p>

            <div className="mt-5 rounded-2xl border border-[#e9c9c2] bg-[#fff8f6] p-4 text-sm font-semibold leading-7 text-[#7a3430]">
              HĂ£y lĂ m bĂ i cáº©n trá»ng vĂ¬ káº¿t quáº£ Ä‘Æ°á»£c dĂ¹ng Ä‘á»ƒ Ä‘Ă¡nh giĂ¡ trĂ¬nh Ä‘á»™ Ä‘áº§u vĂ o vĂ  gá»£i Ă½ lá»™ trĂ¬nh há»c phĂ¹ há»£p.
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
                  setStage('result');
                  setSearchParams({ view: 'result' }, { replace: true });
                  window.scrollTo({ top: 0, behavior: 'smooth' });
                }}
                type="button"
              >
                <span>
                  Láº§n gáº§n nháº¥t: {test.latestAttempt.examType === 'TOEIC'
                    ? `TOEIC ${test.latestAttempt.overallScore ?? 'Ä‘ang cháº¥m'}`
                    : `band ${test.latestAttempt.overallScore != null ? formatBandValue(test.latestAttempt.overallScore) : 'Ä‘ang cháº¥m'}`} Â· {new Date(test.latestAttempt.submittedAt).toLocaleDateString('vi-VN')}
                </span>
                <span className="shrink-0 font-extrabold text-[#8a0018]">Xem káº¿t quáº£</span>
              </button>
            ) : null}
          </div>

          <aside className="rounded-[28px] bg-[linear-gradient(145deg,#4b0009,#8a0018)] p-7 text-white">
            <h2 className="font-['Manrope'] text-2xl font-black">TrÆ°á»›c khi báº¯t Ä‘áº§u</h2>
            <ul className="mt-5 space-y-4 text-sm leading-7 text-white/85">
              <li>â€¢ Chuáº©n bá»‹ khoáº£ng {selectedExamType === 'TOEIC' ? '2 giá»' : '2 giá» 50 phĂºt'}.</li>
              <li>â€¢ DĂ¹ng Chrome hoáº·c Edge{selectedExamType === 'TOEIC' ? '.' : ' vĂ  cáº¥p quyá»n microphone.'}</li>
              <li>â€¢ KhĂ´ng táº£i láº¡i trang; báº£n nhĂ¡p Ä‘Æ°á»£c lÆ°u tá»± Ä‘á»™ng trĂªn thiáº¿t bá»‹.</li>
              <li>â€¢ Náº¿u máº¥t máº¡ng lĂºc ná»™p, hĂ£y thá»­ láº¡i â€” bĂ i lĂ m khĂ´ng bá»‹ xĂ³a.</li>
              <li>â€¢ Báº¡n cĂ³ tá»‘i Ä‘a {maxAttempts} lÆ°á»£t lĂ m; hiá»‡n Ä‘Ă£ dĂ¹ng {attemptCount}/{maxAttempts} lÆ°á»£t.</li>
            </ul>

            <button className="mt-7 w-full rounded-2xl bg-white px-6 py-4 font-black text-[#650012] disabled:cursor-not-allowed disabled:opacity-50" disabled={!canRetake} onClick={() => setStage('device')} type="button">
              {canRetake ? (attemptCount ? `LĂ m láº¡i bĂ i (${attemptCount + 1}/${maxAttempts})` : 'Kiá»ƒm tra thiáº¿t bá»‹') : 'ÄĂ£ dĂ¹ng háº¿t lÆ°á»£t lĂ m'}
            </button>

            <button
              className="mt-3 w-full rounded-2xl border border-white/40 px-6 py-3 text-sm font-black text-white transition hover:bg-white/10"
              onClick={() => setStage('select')}
              type="button"
            >
              Äá»•i dáº¡ng bĂ i
            </button>

            {hasDraftProgress ? (
              <p className="mt-4 text-sm text-white/80">ÄĂ£ cĂ³ báº£n nhĂ¡p trĂªn thiáº¿t bá»‹ nĂ y.</p>
            ) : null}
          </aside>
        </div>
      </main>
      <CourseFooter />
    </div>
  );
}

