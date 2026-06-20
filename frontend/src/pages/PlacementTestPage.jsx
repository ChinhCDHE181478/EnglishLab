import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, CheckCircle2, CircleDot, Headphones, Hourglass, Mic, PenLine, Play } from 'lucide-react';
import placementTestApi from '../api/placementTestApi';
import Header from '../components/ai-learning/Header';
import ListeningExamMode from '../components/course-assessment/ListeningExamMode';
import ReadingExamMode from '../components/course-assessment/ReadingExamMode';
import WritingExamMode from '../components/course-assessment/WritingExamMode';
import SpeakingExamMode from '../components/course-assessment/SpeakingExamMode';
import CourseFooter from '../components/course/CourseFooter';
import BrandedSelect from '../components/ui/BrandedSelect';

const SKILLS = [
  { key: 'listening', label: 'Listening', icon: Headphones },
  { key: 'reading', label: 'Reading', icon: BookOpen },
  { key: 'writing', label: 'Writing', icon: PenLine },
  { key: 'speaking', label: 'Speaking', icon: Mic },
];

const DRAFT_KEY = 'englishlab.placement-test.mock-1.draft';

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

function DeviceCheck({ onComplete }) {
  const [inputs, setInputs] = useState([]);
  const [outputs, setOutputs] = useState([]);
  const [inputId, setInputId] = useState('');
  const [outputId, setOutputId] = useState('');
  const [soundPassed, setSoundPassed] = useState(false);
  const [micState, setMicState] = useState('idle');
  const [micLevel, setMicLevel] = useState(0);
  const [micCountdown, setMicCountdown] = useState(5);
  const [previewUrl, setPreviewUrl] = useState('');
  const audioRef = useRef(null);
  const micStreamRef = useRef(null);
  const micRecorderRef = useRef(null);
  const micContextRef = useRef(null);
  const micAnalyserRef = useRef(null);
  const micFrameRef = useRef(null);
  const micTimeoutRef = useRef(null);
  const micIntervalRef = useRef(null);
  const micChunksRef = useRef([]);

  const stopMicTest = () => {
    if (micFrameRef.current) window.cancelAnimationFrame(micFrameRef.current);
    if (micTimeoutRef.current) window.clearTimeout(micTimeoutRef.current);
    if (micIntervalRef.current) window.clearInterval(micIntervalRef.current);
    micFrameRef.current = null;
    micTimeoutRef.current = null;
    micIntervalRef.current = null;
    if (micRecorderRef.current?.state === 'recording') micRecorderRef.current.stop();
    micStreamRef.current?.getTracks?.().forEach((track) => track.stop());
    micStreamRef.current = null;
    micRecorderRef.current = null;
    micAnalyserRef.current = null;
    if (micContextRef.current && micContextRef.current.state !== 'closed') {
      micContextRef.current.close().catch(() => {});
    }
    micContextRef.current = null;
    setMicLevel(0);
  };

  useEffect(() => {
    navigator.mediaDevices?.enumerateDevices?.().then((devices) => {
      const microphones = devices.filter((device) => device.kind === 'audioinput');
      const speakers = devices.filter((device) => device.kind === 'audiooutput');
      setInputs(microphones);
      setOutputs(speakers);
      setInputId(microphones[0]?.deviceId || '');
      setOutputId(speakers[0]?.deviceId || '');
    }).catch(() => {});
  }, []);

  useEffect(() => () => {
    stopMicTest();
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
  }, [previewUrl]);

  const playTone = async () => {
    const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextCtor) return;

    const context = new AudioContextCtor();
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    const destination = context.createMediaStreamDestination();

    oscillator.connect(gain).connect(destination);
    oscillator.frequency.value = 523.25;
    gain.gain.value = 0.15;
    audioRef.current.srcObject = destination.stream;

    if (outputId && audioRef.current.setSinkId) {
      await audioRef.current.setSinkId(outputId).catch(() => {});
    }

    await audioRef.current.play();
    oscillator.start();
    oscillator.stop(context.currentTime + 1.2);
    window.setTimeout(() => context.close(), 1600);
    setSoundPassed(true);
  };

  const testMic = async () => {
    stopMicTest();
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl('');
    }
    setMicState('testing');
    setMicCountdown(5);
    setMicLevel(0);

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: inputId ? { deviceId: { exact: inputId } } : true,
      });
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextCtor || typeof MediaRecorder === 'undefined') {
        stream.getTracks().forEach((track) => track.stop());
        setMicState('unsupported');
        return;
      }
      const context = new AudioContextCtor();
      if (context.state === 'suspended') await context.resume();
      const analyser = context.createAnalyser();
      analyser.fftSize = 256;
      context.createMediaStreamSource(stream).connect(analyser);
      navigator.mediaDevices.enumerateDevices().then((devices) => {
        setInputs(devices.filter((device) => device.kind === 'audioinput'));
        setOutputs(devices.filter((device) => device.kind === 'audiooutput'));
      }).catch(() => {});
      const recorder = new MediaRecorder(stream);
      micStreamRef.current = stream;
      micRecorderRef.current = recorder;
      micContextRef.current = context;
      micAnalyserRef.current = analyser;
      micChunksRef.current = [];

      recorder.ondataavailable = (event) => {
        if (event.data.size) micChunksRef.current.push(event.data);
      };

      recorder.onstop = () => {
        const blob = new Blob(micChunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        micChunksRef.current = [];
        setPreviewUrl((current) => {
          if (current) URL.revokeObjectURL(current);
          return blob.size ? URL.createObjectURL(blob) : '';
        });
        setMicState(blob.size ? 'passed' : 'failed');
      };

      const data = new Uint8Array(analyser.fftSize);
      const updateLevel = () => {
        if (!micAnalyserRef.current) return;
        micAnalyserRef.current.getByteTimeDomainData(data);
        const rms = Math.sqrt(data.reduce((sum, value) => {
          const normalized = (value - 128) / 128;
          return sum + (normalized * normalized);
        }, 0) / Math.max(data.length, 1));
        setMicLevel(Math.min(100, Math.round(rms * 260)));
        micFrameRef.current = window.requestAnimationFrame(updateLevel);
      };

      recorder.start();
      micFrameRef.current = window.requestAnimationFrame(updateLevel);
      micIntervalRef.current = window.setInterval(() => {
        setMicCountdown((current) => (current > 1 ? current - 1 : 0));
      }, 1000);
      micTimeoutRef.current = window.setTimeout(() => {
        stopMicTest();
      }, 5000);
    } catch {
      setMicState('failed');
      stopMicTest();
    }
  };

  const ready = soundPassed && micState === 'passed';

  return (
    <div className="mx-auto max-w-5xl rounded-[32px] border border-[#dfbfbd]/35 bg-white p-6 shadow-[0_24px_70px_rgba(75,0,9,0.10)] md:p-9">
      <audio ref={audioRef} className="hidden" />
      <h1 className="text-center font-['Manrope'] text-2xl font-extrabold text-[#21446d]">Kiểm tra thiết bị</h1>

      <div className="mt-8 space-y-9">
        <section className="grid gap-5 md:grid-cols-[56px_1fr]">
          <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[#8a0018]/25 text-[#8a0018]">
            <Headphones aria-hidden="true" size={23} />
          </div>
          <div>
            <h2 className="text-2xl font-extrabold text-[#21446d]"><span className="mr-2 text-[#21446d]/55">1.</span>Kiểm tra tai nghe</h2>
            <p className="mt-3 text-sm leading-7 text-[#584140]">Hãy thử phát âm thanh mẫu để chắc rằng tai nghe hoặc loa của bạn nghe rõ trước khi bắt đầu bài thi.</p>
            <div className="mt-5 flex flex-wrap items-center gap-4 rounded-[24px] border border-[#dfbfbd]/40 bg-[#fffdfc] px-5 py-5">
              <button aria-label="Phát âm thanh kiểm tra" className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] text-white shadow-[0_10px_24px_rgba(75,0,9,0.24)]" onClick={playTone} type="button">
                <Play aria-hidden="true" className="ml-0.5" fill="currentColor" size={20} />
              </button>
              <div className="min-w-[180px] flex-1">
                <div className="h-2 rounded-full bg-[#f3d7dd]">
                  <div className="h-full rounded-full bg-[linear-gradient(90deg,#8a0018,#b4233f)] transition-all" style={{ width: soundPassed ? '100%' : '0%' }} />
                </div>
              </div>
              <span className="text-sm font-semibold text-[#7a6766]">{soundPassed ? '00:08' : '00:00'}</span>
              <BrandedSelect
                buttonClassName="min-w-[260px] border-[#dfbfbd]/50 py-3 text-sm font-medium text-[#584140] shadow-none"
                onChange={(event) => {
                  setOutputId(event.target.value);
                  setSoundPassed(false);
                }}
                options={outputs.length
                  ? outputs.map((device, index) => ({ label: device.label || `Loa hoặc tai nghe ${index + 1}`, value: device.deviceId }))
                  : [{ label: 'Thiết bị phát mặc định', value: '' }]}
                value={outputId}
              />
            </div>
          </div>
        </section>

        <section className="grid gap-5 md:grid-cols-[56px_1fr]">
          <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[#8a0018]/25 text-[#8a0018]">
            <Mic aria-hidden="true" size={23} />
          </div>
          <div>
            <h2 className="text-2xl font-extrabold text-[#21446d]"><span className="mr-2 text-[#21446d]/55">2.</span>Kiểm tra microphone</h2>
            <p className="mt-3 text-sm leading-7 text-[#584140]">Hãy bấm kiểm tra micro và đọc to câu sau để xem tiếng thu vào có ổn định hay không.</p>
            <p className="mt-5 text-center text-base font-semibold leading-8 text-[#8c716f]">Hãy đọc to:<br />“I love English. My English is great and I practice it every day!”</p>
            <div className="mt-5 flex flex-wrap items-center gap-4 rounded-[24px] border border-[#dfbfbd]/40 bg-[#fffdfc] px-5 py-5">
              <button className={`flex h-14 shrink-0 items-center gap-2 rounded-full px-5 text-sm font-extrabold ${micState === 'testing' ? 'bg-[#fff0f1] text-[#8a0018]' : 'bg-[linear-gradient(135deg,#8a0018,#650012)] text-white shadow-[0_10px_24px_rgba(75,0,9,0.24)]'}`} disabled={micState === 'testing'} onClick={testMic} type="button">
                {micState === 'testing' ? <CircleDot aria-hidden="true" size={19} /> : <Mic aria-hidden="true" size={19} />}
                {micState === 'testing' ? `Đang ghi thử ${micCountdown}s` : 'Bắt đầu kiểm tra'}
              </button>
              <div className="min-w-[180px] flex-1">
                <div className="h-2 rounded-full bg-[#f3d7dd]">
                  <div className="h-full rounded-full bg-[linear-gradient(90deg,#8a0018,#b4233f)] transition-all" style={{ width: `${micLevel}%` }} />
                </div>
              </div>
              <span className="min-w-8 text-sm font-semibold text-[#7a6766]">{micLevel}%</span>
              <BrandedSelect
                buttonClassName="min-w-[260px] border-[#dfbfbd]/50 py-3 text-sm font-medium text-[#584140] shadow-none"
                onChange={(event) => {
                  setInputId(event.target.value);
                  setMicState('idle');
                  setPreviewUrl((current) => {
                    if (current) URL.revokeObjectURL(current);
                    return '';
                  });
                }}
                options={inputs.length
                  ? inputs.map((device, index) => ({ label: device.label || `Micro ${index + 1}`, value: device.deviceId }))
                  : [{ label: 'Micro mặc định', value: '' }]}
                value={inputId}
              />
            </div>
            {previewUrl ? <div className="mt-4 rounded-[24px] border border-[#dfbfbd]/40 bg-[#fff7f7] p-4"><p className="text-sm font-bold text-[#4b0009]">Bản ghi thử 5 giây đã sẵn sàng. Hãy nghe lại để kiểm tra chất lượng âm thanh.</p><audio className="mt-3 w-full" controls src={previewUrl} /></div> : null}
            <p className={`mt-3 text-sm leading-7 ${['failed', 'unsupported'].includes(micState) ? 'font-semibold text-red-700' : 'text-[#7a6766]'}`}>
              {micState === 'idle' && 'Bấm Bắt đầu kiểm tra để cấp quyền micro và ghi thử trong 5 giây.'}
              {micState === 'testing' && 'Micro đang được ghi thử. Hãy đọc câu mẫu thật rõ để kiểm tra chất lượng thu âm.'}
              {micState === 'passed' && 'Micro đã ghi thử xong. Nếu bản nghe lại rõ, thiết bị của bạn đã sẵn sàng.'}
              {micState === 'failed' && 'Không truy cập được micro. Hãy cấp quyền cho trình duyệt rồi thử lại.'}
              {micState === 'unsupported' && 'Trình duyệt hiện không hỗ trợ kiểm tra micro. Hãy thử bằng Chrome hoặc Edge.'}
            </p>
          </div>
        </section>

        <section className="grid gap-5 md:grid-cols-[56px_1fr]">
          <div className={`flex h-12 w-12 items-center justify-center rounded-full border ${ready ? 'border-[#8a0018]/25 bg-[#fff0f1] text-[#8a0018]' : 'border-[#f2d6dc] text-[#d9a4af]'}`}>
            {ready ? <CheckCircle2 aria-hidden="true" size={23} /> : <Hourglass aria-hidden="true" size={23} />}
          </div>
          <div>
            <h2 className={`text-2xl font-extrabold ${ready ? 'text-[#21446d]' : 'text-[#9aa8b8]'}`}><span className="mr-2 opacity-60">3.</span>Sẵn sàng vào phòng thi</h2>
            <p className={`mt-3 text-sm leading-7 ${ready ? 'text-[#584140]' : 'text-[#a89b9d]'}`}>{ready ? 'Tai nghe và micro đã sẵn sàng. Bạn có thể bắt đầu bài đánh giá đầu vào.' : 'Hoàn thành hai bước kiểm tra phía trên để bắt đầu bài thi.'}</p>
          </div>
        </section>
      </div>

      <button
        className="mt-8 ml-auto block rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] px-7 py-3 text-sm font-black text-white shadow-[0_10px_24px_rgba(75,0,9,0.20)] disabled:cursor-not-allowed disabled:opacity-40"
        disabled={!ready}
        onClick={() => onComplete({ completed: true, soundPassed, microphonePassed: true, inputDeviceId: inputId, outputDeviceId: outputId, checkedAt: new Date().toISOString() })}
        type="button"
      >
        Bắt đầu bài đánh giá đầu vào
      </button>
    </div>
  );
}

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
      window.alert('Không thể truy cập microphone. Hãy kiểm tra quyền của trình duyệt.');
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
  const [warning, setWarning] = useState(null);
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);
  const [violations, setViolations] = useState([]);
  const submittedRef = useRef(false);
  const intentionalExitRef = useRef(false);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setRemainingSeconds((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (remainingSeconds !== 0 || submittedRef.current || submitting) return;
    submittedRef.current = true;
    void onSubmit(true);
  }, [onSubmit, remainingSeconds, submitting]);

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
            disabled={submitting}
            onClick={() => onSubmit(false)}
            type="button"
          >
            {submitting ? 'Đang lưu...' : submitLabel}
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
  const [test, setTest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [stage, setStage] = useState('intro');
  const [skillIndex, setSkillIndex] = useState(0);
  const [draft, setDraft] = useState(readDraft);
  const [deviceCheck, setDeviceCheck] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [result, setResult] = useState(null);

  const activeSkill = SKILLS[skillIndex];
  const activeConfig = test?.sections?.[activeSkill?.key];

  useEffect(() => {
    placementTestApi.getMockOne()
      .then((response) => {
        const sections = response?.sections;
        const missingSkills = SKILLS.filter((skill) => !sections?.[skill.key]);

        if (missingSkills.length) {
          throw new Error(`Đề thi đang thiếu dữ liệu: ${missingSkills.map((skill) => skill.label).join(', ')}`);
        }

        setTest(response);
      })
      .catch((error) => setLoadError(error?.response?.data?.message || error?.message || 'Không tải được đề thi thử.'))
      .finally(() => setLoading(false));
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

  const goToNextSkill = () => {
    setSubmitError('');
    setSkillIndex((current) => current + 1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const submitAll = async ({ skipSpeakingValidation = false } = {}) => {
    if (!skipSpeakingValidation && !draft.speakingAudioUrl && !draft.speakingTranscript.trim()) {
      setSubmitError('Hãy hoàn thành bản ghi âm cho phần Nói trước khi nộp bài.');
      return;
    }

    setSubmitting(true);
    setSubmitError('');

    try {
      const response = await placementTestApi.submitMockOne({
        testCode: test.testCode,
        listeningAnswers: draft.listeningAnswers,
        readingAnswers: draft.readingAnswers,
        writingAnswers: draft.writingAnswers,
        speakingTranscript: draft.speakingTranscript,
        speakingAudioUrl: draft.speakingAudioUrl,
        deviceCheck,
      });

      setResult(response);
      setStage('result');

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
      const response = await placementTestApi.submitMockOne({
        testCode: test.testCode,
        listeningAnswers: draft.listeningAnswers,
        readingAnswers: draft.readingAnswers,
        writingAnswers: draft.writingAnswers,
        speakingTranscript,
        speakingAudioUrl,
        deviceCheck,
      });

      setResult(response);
      setStage('result');
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
      const nextAnswers = toPlacementObjectiveAnswers(test?.sections?.listening, payload);
      setDraft((current) => ({ ...current, listeningAnswers: nextAnswers }));
      goToNextSkill();
    } finally {
      setSubmitting(false);
    }
  };

  const handleReadingSubmit = async (payload) => {
    setSubmitting(true);
    setSubmitError('');

    try {
      const nextAnswers = toPlacementObjectiveAnswers(test?.sections?.reading, payload);
      setDraft((current) => ({ ...current, readingAnswers: nextAnswers }));
      goToNextSkill();
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
      goToNextSkill();
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="flex min-h-screen items-center justify-center bg-[#f8f4f1] font-bold text-[#8a0018]">Đang tải bài đánh giá đầu vào...</div>;
  }

  if (loadError) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-[#f8f4f1] px-6 text-center">
        <p className="font-bold text-red-700">{loadError}</p>
        <button className="rounded-xl bg-[#8a0018] px-5 py-3 font-bold text-white" onClick={() => window.location.reload()} type="button">Thử lại</button>
      </div>
    );
  }

  if (stage === 'device') {
    return (
      <main className="min-h-screen bg-[#f8f4f1] px-4 py-10">
        <DeviceCheck
          onComplete={(value) => {
            setDeviceCheck(value);
            setStage('exam');
          }}
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
            <p className="text-xs font-black uppercase tracking-[0.18em] text-[#8a0018]">Kết quả đánh giá đầu vào</p>
            <h1 className="mt-3 font-['Manrope'] text-4xl font-black text-[#341c1d]">Band tổng quan: {result.overallScore ?? 'Đang chấm'}</h1>

            <div className="mt-7 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {SKILLS.map((skill) => (
                <div className="rounded-2xl bg-[#fff0f1] p-5" key={skill.key}>
                  <p className="text-sm font-bold text-[#7a4a4e]">{skill.label}</p>
                  <p className="mt-2 text-3xl font-black text-[#8a0018]">{result[`${skill.key}Score`] ?? '—'}</p>
                </div>
              ))}
            </div>

            <p className="mt-6 rounded-2xl border border-[#ead7d5] bg-[#fffaf9] p-5 text-sm leading-7 text-[#584140]">
              Listening: {result.correctListening}/40 câu đúng · Reading: {result.correctReading}/40 câu đúng.
              {result.status === 'OBJECTIVE_EVALUATED'
                ? ' Writing và Speaking chưa có điểm do dịch vụ AI tạm thời không sẵn sàng. Bản nháp vẫn được giữ để bạn nộp lại.'
                : ' Kết quả đã được lưu vào hồ sơ đánh giá đầu vào.'}
            </p>

            <div className="mt-6 flex flex-wrap gap-3">
              <button
                className="rounded-2xl border border-[#8a0018]/25 px-6 py-4 font-black text-[#8a0018] transition hover:bg-[#fff0f1]"
                onClick={() => {
                  setResult(null);
                  setStage('intro');
                  window.scrollTo({ top: 0, behavior: 'smooth' });
                }}
                type="button"
              >
                Quay lại trang đánh giá
              </button>
              <button className="rounded-2xl bg-[#8a0018] px-6 py-4 font-black text-white" onClick={() => navigate('/courses')} type="button">
                Xem khóa học phù hợp
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
            initialAnswers={toExamModeInitialObjectiveAnswers(activeConfig, draft.listeningAnswers)}
            onClose={() => navigate('/')}
            onSubmit={handleListeningSubmit}
            preserveFullscreenOnUnmount
            skipAudioCheck
            submitLabel="Hoàn thành phần Listening"
            submitting={submitting}
          />
          {submitError ? <div className="fixed bottom-4 left-1/2 z-[140] w-[min(92vw,640px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700 shadow-xl">{submitError}</div> : null}
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
            submitLabel="Hoàn thành phần Reading"
            submitting={submitting}
          />
          {submitError ? <div className="fixed bottom-4 left-1/2 z-[140] w-[min(92vw,640px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700 shadow-xl">{submitError}</div> : null}
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
            submitLabel="Hoàn thành phần Writing"
            submitting={submitting}
          />
          {submitError ? <div className="fixed bottom-4 left-1/2 z-[140] w-[min(92vw,640px)] -translate-x-1/2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700 shadow-xl">{submitError}</div> : null}
        </>
      );
    }

    return (
      <>
        <SpeakingExamMode
          config={{ ...activeConfig, submissionLabel: 'Đánh giá đầu vào' }}
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
            <p className="text-xs font-black uppercase tracking-[0.2em] text-[#8a0018]">Đánh giá đầu vào</p>
            <h1 className="mt-4 font-['Manrope'] text-4xl font-black leading-tight text-[#341c1d]">Một bài thi thử hoàn chỉnh cho cả 4 kỹ năng</h1>
            <p className="mt-5 max-w-2xl leading-8 text-[#584140]">Bạn sẽ kiểm tra thiết bị một lần, sau đó làm lần lượt Listening, Reading, Writing và Speaking. Kết quả được dùng để đề xuất lộ trình học phù hợp.</p>

            <div className="mt-7 grid gap-3 sm:grid-cols-2">
              {SKILLS.map((skill) => {
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
                  setStage('result');
                  window.scrollTo({ top: 0, behavior: 'smooth' });
                }}
                type="button"
              >
                <span>
                  Lần gần nhất: band {test.latestAttempt.overallScore ?? 'đang chấm'} · {new Date(test.latestAttempt.submittedAt).toLocaleDateString('vi-VN')}
                </span>
                <span className="shrink-0 font-extrabold text-[#8a0018]">Xem kết quả</span>
              </button>
            ) : null}
          </div>

          <aside className="rounded-[28px] bg-[linear-gradient(145deg,#4b0009,#8a0018)] p-7 text-white">
            <h2 className="font-['Manrope'] text-2xl font-black">Trước khi bắt đầu</h2>
            <ul className="mt-5 space-y-4 text-sm leading-7 text-white/85">
              <li>• Chuẩn bị khoảng 2 giờ 50 phút.</li>
              <li>• Dùng Chrome hoặc Edge và cấp quyền microphone.</li>
              <li>• Không tải lại trang; bản nháp được lưu tự động trên thiết bị.</li>
              <li>• Nếu mất mạng lúc nộp, hãy thử lại — bài làm không bị xóa.</li>
            </ul>

            <button className="mt-7 w-full rounded-2xl bg-white px-6 py-4 font-black text-[#650012]" onClick={() => setStage('device')} type="button">
              Kiểm tra thiết bị
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
