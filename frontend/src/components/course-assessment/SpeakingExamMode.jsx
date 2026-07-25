import { useEffect, useMemo, useRef, useState } from 'react';
import { Mic, UserRound } from 'lucide-react';
import ExamDeviceCheck from './ExamDeviceCheck';

const formatSeconds = (value) => {
  const seconds = Math.max(0, Number(value) || 0);
  const minutes = String(Math.floor(seconds / 60)).padStart(2, '0');
  return `${minutes}:${String(seconds % 60).padStart(2, '0')}`;
};

const normalizePrompt = (prompt) => (
  typeof prompt === 'string' ? { text: prompt, videoUrl: '' } : {
    text: String(prompt?.text || ''),
    videoUrl: String(prompt?.videoUrl || ''),
  }
);

const normalizeParts = (config = {}) => (config.parts || []).map((part, index) => {
  const prompts = (part.prompts || []).map(normalizePrompt);
  return {
    ...part,
    key: part.key || `part_${index + 1}`,
    label: `Part ${index + 1}`,
    caption: String(part.title || '').replace(/^Part\s*\d+\s*[·:|-]?\s*/i, '') || `Phần ${index + 1}`,
    prompts: prompts.length ? prompts : [{
      text: String(part.cueCardTitle || ''),
      videoUrl: String(part.videoUrl || ''),
    }],
  };
});

export default function SpeakingExamMode({
  config = {},
  initialAudioUrl = '',
  onAudioReady,
  onClose,
  onSubmit,
  skipDeviceCheck = false,
  submitting = false,
  uploadAudio,
}) {
  const parts = useMemo(() => normalizeParts(config), [config]);
  const [stage, setStage] = useState(() => (skipDeviceCheck ? 'exam' : 'device_check'));
  const [selectedInputDeviceId, setSelectedInputDeviceId] = useState('');
  const [partIndex, setPartIndex] = useState(0);
  const [questionIndex, setQuestionIndex] = useState(0);
  const [remainingSeconds, setRemainingSeconds] = useState(parts[0]?.answerSeconds || 300);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingDuration, setRecordingDuration] = useState(0);
  const [recordingLevel, setRecordingLevel] = useState(0);
  const [hasVoiceSignal, setHasVoiceSignal] = useState(false);
  const [audioUrl, setAudioUrl] = useState(initialAudioUrl);
  const [uploading, setUploading] = useState(false);
  const [pendingSubmit, setPendingSubmit] = useState(false);
  const [error, setError] = useState('');
  const [warning, setWarning] = useState(null);
  const [violations, setViolations] = useState([]);
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);
  const recorderRef = useRef(null);
  const streamRef = useRef(null);
  const chunksRef = useRef([]);
  const analyserRef = useRef(null);
  const audioContextRef = useRef(null);
  const animationRef = useRef(null);
  const recordingDurationRef = useRef(0);
  const hasVoiceSignalRef = useRef(false);
  const pendingSubmitRef = useRef(false);
  const intentionalExitRef = useRef(false);
  const fullscreenSessionStartedRef = useRef(false);
  const violationLockRef = useRef(false);

  const activePart = parts[partIndex] || parts[0];
  const activePrompts = activePart?.prompts || [];
  const activePrompt = activePrompts[questionIndex] || activePrompts[0] || {};
  const isLastQuestion = questionIndex >= activePrompts.length - 1;
  const isLastPart = partIndex >= parts.length - 1;
  const isFinalPrompt = isLastPart && isLastQuestion;

  const stopMeter = () => {
    if (animationRef.current) window.cancelAnimationFrame(animationRef.current);
    animationRef.current = null;
    analyserRef.current = null;
    if (audioContextRef.current && audioContextRef.current.state !== 'closed') {
      audioContextRef.current.close().catch(() => { });
    }
    audioContextRef.current = null;
    setRecordingLevel(0);
  };

  const stopStream = () => {
    streamRef.current?.getTracks?.().forEach((track) => track.stop());
    streamRef.current = null;
  };

  const buildSubmissionText = () => [
    `Speaking mock test: ${config.submissionLabel || config.title || 'IELTS Speaking'}`,
    `Recording duration seconds: ${recordingDurationRef.current}`,
    `Voice signal detected: ${hasVoiceSignalRef.current ? 'yes' : 'no'}`,
    '',
    'Part prompts shown to the learner:',
    parts.map((part) => [
      `${part.label} - ${part.caption}`,
      part.cueCardTitle ? `Cue card: ${part.cueCardTitle}` : '',
      part.prompts.map((prompt, index) => `${index + 1}. ${prompt.text}`).join('\n'),
    ].filter(Boolean).join('\n')).join('\n\n'),
  ].join('\n');

  const submitRecording = async (uploadedUrl = audioUrl) => {
    if (recordingDurationRef.current < 5 || !hasVoiceSignalRef.current) {
      setError('Bài nói chưa có đủ nội dung để đánh giá. Hãy trả lời rõ ràng trước khi nộp bài.');
      setPendingSubmit(false);
      return;
    }
    if (!uploadedUrl) {
      setError('Bản ghi đang được lưu. Hãy đợi một chút trước khi nộp bài.');
      return;
    }
    setError('');
    try {
      await onSubmit?.({
        fullscreenExitCount: violations.filter((item) => String(item.reason || '').toLowerCase().includes('toàn màn hình')).length,
        tabSwitchCount: violations.filter((item) => !String(item.reason || '').toLowerCase().includes('toàn màn hình')).length,
        submittedText: buildSubmissionText(),
        submittedAudioUrl: uploadedUrl,
      });
      intentionalExitRef.current = true;
      if (document.fullscreenElement) {
        await document.exitFullscreen?.().catch(() => { });
      }
    } catch {
      pendingSubmitRef.current = false;
      setPendingSubmit(false);
      setError('Chưa thể nộp bài lúc này. Bản ghi vẫn được giữ để bạn thử lại.');
    }
  };

  const finishRecording = () => {
    if (recorderRef.current?.state === 'recording') {
      recorderRef.current.stop();
    }
    setIsRecording(false);
    stopMeter();
  };

  const startRecording = async () => {
    if (isRecording || uploading || submitting) return;
    try {
      setError('');
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: selectedInputDeviceId ? { deviceId: { exact: selectedInputDeviceId } } : true,
      });
      const recorder = new MediaRecorder(stream);
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      streamRef.current = stream;
      recorderRef.current = recorder;
      chunksRef.current = [];

      if (AudioContextCtor) {
        const audioContext = new AudioContextCtor();
        if (audioContext.state === 'suspended') await audioContext.resume();
        const analyser = audioContext.createAnalyser();
        analyser.fftSize = 256;
        audioContext.createMediaStreamSource(stream).connect(analyser);
        audioContextRef.current = audioContext;
        analyserRef.current = analyser;
        const values = new Uint8Array(analyser.fftSize);
        const tick = () => {
          if (!analyserRef.current) return;
          analyserRef.current.getByteTimeDomainData(values);
          const rms = Math.sqrt(values.reduce((sum, value) => {
            const normalized = (value - 128) / 128;
            return sum + (normalized * normalized);
          }, 0) / values.length);
          const level = Math.min(100, Math.round(rms * 260));
          setRecordingLevel(level);
          if (level >= 12) {
            hasVoiceSignalRef.current = true;
            setHasVoiceSignal(true);
          }
          animationRef.current = window.requestAnimationFrame(tick);
        };
        animationRef.current = window.requestAnimationFrame(tick);
      }

      recorder.ondataavailable = (event) => {
        if (event.data?.size) chunksRef.current.push(event.data);
      };
      recorder.onstop = async () => {
        stopMeter();
        stopStream();
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        if (!blob.size) {
          setError('Không thể lưu bản ghi âm. Hãy thử lại.');
          setPendingSubmit(false);
          return;
        }
        setUploading(true);
        try {
          const file = new File([blob], 'placement-speaking.webm', { type: blob.type || 'audio/webm' });
          const response = await uploadAudio(file);
          const uploadedUrl = response?.url || response?.audioUrl || '';
          setAudioUrl(uploadedUrl);
          onAudioReady?.(uploadedUrl);
          if (pendingSubmitRef.current) {
            pendingSubmitRef.current = false;
            await submitRecording(uploadedUrl);
          }
        } catch {
          setError('Bản ghi chưa được lưu thành công. Hãy thử lại.');
          setPendingSubmit(false);
        } finally {
          setUploading(false);
        }
      };

      recorder.start();
      setIsRecording(true);
    } catch {
      setError('Không thể sử dụng micro. Hãy kiểm tra quyền micro của trình duyệt.');
    }
  };

  const advance = () => {
    if (isFinalPrompt) {
      pendingSubmitRef.current = true;
      setPendingSubmit(true);
      if (isRecording) {
        finishRecording();
      } else if (!uploading) {
        pendingSubmitRef.current = false;
        void submitRecording();
      }
      return;
    }
    if (!isLastQuestion) {
      setQuestionIndex((current) => current + 1);
      return;
    }
    const nextPartIndex = partIndex + 1;
    setPartIndex(nextPartIndex);
    setQuestionIndex(0);
    setRemainingSeconds(parts[nextPartIndex]?.answerSeconds || 300);
  };

  useEffect(() => {
    if (stage !== 'exam' || remainingSeconds > 0 || pendingSubmit || submitting || uploading) return;
    advance();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [remainingSeconds, stage, pendingSubmit, submitting, uploading]);

  useEffect(() => {
    recordingDurationRef.current = recordingDuration;
  }, [recordingDuration]);

  useEffect(() => {
    if (!isRecording) return undefined;
    const interval = window.setInterval(() => setRecordingDuration((current) => current + 1), 1000);
    return () => window.clearInterval(interval);
  }, [isRecording]);

  useEffect(() => {
    if (stage !== 'exam' || pendingSubmit || submitting || uploading) return undefined;
    const interval = window.setInterval(() => {
      setRemainingSeconds((current) => Math.max(0, current - 1));
    }, 1000);
    return () => window.clearInterval(interval);
  }, [partIndex, pendingSubmit, stage, submitting, uploading]);

  useEffect(() => {
    if (stage !== 'exam' || activePrompt.videoUrl || isRecording) return undefined;
    const timeout = window.setTimeout(() => void startRecording(), 350);
    return () => window.clearTimeout(timeout);
  }, [partIndex, questionIndex, activePrompt.videoUrl, stage]);

  useEffect(() => {
    intentionalExitRef.current = false;
    fullscreenSessionStartedRef.current = Boolean(document.fullscreenElement);
    const recordViolation = (reason) => {
      if (violationLockRef.current) return;
      violationLockRef.current = true;
      const entry = { reason, at: new Date().toISOString() };
      setViolations((current) => [...current, entry]);
      setWarning(entry);
      window.setTimeout(() => {
        violationLockRef.current = false;
      }, 300);
    };
    const visibility = () => document.hidden && recordViolation('Hệ thống ghi nhận bạn đã rời khỏi trang làm bài.');
    const blur = () => recordViolation('Cửa sổ làm bài đã mất focus.');
    const fullscreen = () => {
      if (document.fullscreenElement) {
        fullscreenSessionStartedRef.current = true;
        return;
      }
      if (fullscreenSessionStartedRef.current && !intentionalExitRef.current) {
        fullscreenSessionStartedRef.current = false;
        recordViolation('Bạn không thể thoát chế độ toàn màn hình trong khi đang thi.');
      }
    };
    const beforeUnload = (event) => {
      event.preventDefault();
      event.returnValue = '';
    };
    document.addEventListener('visibilitychange', visibility);
    document.addEventListener('fullscreenchange', fullscreen);
    window.addEventListener('blur', blur);
    window.addEventListener('beforeunload', beforeUnload);
    return () => {
      document.removeEventListener('visibilitychange', visibility);
      document.removeEventListener('fullscreenchange', fullscreen);
      window.removeEventListener('blur', blur);
      window.removeEventListener('beforeunload', beforeUnload);
      finishRecording();
      stopStream();
    };
  }, []);

  const bars = Array.from({ length: 40 }, (_, index) => {
    if (!isRecording) return 3;
    const curve = Math.max(0.15, 1 - Math.abs(index - 19.5) / 19.5);
    return Math.max(5, Math.round(5 + recordingLevel * 0.2 * curve));
  });

  const closeExam = async () => {
    intentionalExitRef.current = true;
    finishRecording();
    if (document.fullscreenElement) await document.exitFullscreen?.().catch(() => { });
    onClose?.();
  };

  return (
    <div className="fixed inset-0 z-[120] overflow-y-auto bg-[#f8f4f1] text-[#2b2828]">
      <div className="mx-auto min-h-screen max-w-6xl px-4 py-6 md:px-8">
        <header className="flex flex-wrap items-center justify-between gap-3 rounded-[28px] border border-[#dfbfbd]/25 bg-white px-5 py-4 shadow-sm">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#8c716f]">Chế độ làm bài</p>
            <h1 className="mt-1 text-xl font-extrabold text-[#2b2828]">{config.title || 'IELTS Speaking Mock Test'}</h1>
          </div>
          <div className="flex items-center gap-3">
            <span className="rounded-full bg-[#8a0018] px-4 py-2 text-xs font-bold text-white">Vi phạm đã ghi nhận: {violations.length}</span>
            <button className="rounded-full border border-[#8a0018] px-5 py-2 text-sm font-bold text-[#8a0018]" onClick={() => setExitConfirmOpen(true)} type="button">Thoát bài thi</button>
          </div>
        </header>

        {stage === 'device_check' ? (
          <main className="mt-6">
            <ExamDeviceCheck
              includeMicrophone
              onCancel={closeExam}
              onComplete={({ inputDeviceId }) => {
                setSelectedInputDeviceId(inputDeviceId || '');
                setStage('exam');
              }}
              title="Kiểm tra thiết bị"
            />
          </main>
        ) : (
          <main className="mt-6 overflow-hidden rounded-[30px] border border-[#dfbfbd]/25 bg-white">
            <div className="flex items-center justify-end border-b border-[#f0e6e6] px-6 py-4">
              <p className="text-3xl font-extrabold text-[#8a0018]">
                {formatSeconds(remainingSeconds)}
                <span className="ml-1 text-sm font-medium text-[#2b2828]">phút còn lại</span>
              </p>
            </div>

            <div className="px-6 py-8 text-center">
              <p className="text-3xl font-extrabold text-[#21446d]">
                {activePart?.label?.toUpperCase()}
                <span className="font-medium text-[#2b2828]">: {activePart?.caption}</span>
              </p>

              <div className="mx-auto mt-8 max-w-[430px]">
                {activePrompt.videoUrl ? (
                  <video
                    key={activePrompt.videoUrl}
                    className="h-[250px] w-full rounded-[10px] object-cover"
                    autoPlay
                    controls={false}
                    controlsList="nodownload noplaybackrate noremoteplayback nofullscreen"
                    disablePictureInPicture
                    onEnded={() => void startRecording()}
                    onContextMenu={(event) => event.preventDefault()}
                    playsInline
                    preload="auto"
                    src={activePrompt.videoUrl}
                  />
                ) : (
                  <div className="flex h-[250px] w-full items-center justify-center rounded-[10px] bg-[linear-gradient(135deg,#eef2f8,#fbfcfe)]">
                    <UserRound className="text-[#cf6f83]" size={76} strokeWidth={1.5} />
                  </div>
                )}
              </div>

              {activePart?.cueCardTitle ? (
                <div className="mx-auto mt-6 max-w-3xl rounded-[24px] border border-[#efd9de] bg-[#fffdfc] p-5 text-left">
                  <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8c716f]">Thẻ gợi ý</p>
                  <h2 className="mt-2 text-xl font-extrabold">{activePart.cueCardTitle}</h2>
                  <div className="mt-4 grid gap-3 md:grid-cols-2">
                    {(activePart.cueCardBullets || []).map((bullet) => <div className="rounded-2xl bg-[#faf7f7] px-4 py-3 text-sm font-semibold text-[#4b0009]" key={bullet}>{bullet}</div>)}
                  </div>
                </div>
              ) : null}

              <div className="mx-auto mt-8 max-w-[560px]">
                <div className="relative flex items-center justify-center">
                  <div className="absolute left-0 right-0 flex items-center justify-between gap-2 px-3">
                    <div className="flex h-10 items-center gap-[2px]">{bars.map((height, index) => <span className={`w-[2px] rounded-full ${isRecording ? 'bg-[#8a0018]' : 'bg-[#dfbfbd]'}`} key={`l-${index}`} style={{ height }} />)}</div>
                    <div className="w-20 shrink-0" />
                    <div className="flex h-10 items-center gap-[2px]">{[...bars].reverse().map((height, index) => <span className={`w-[2px] rounded-full ${isRecording ? 'bg-[#8a0018]' : 'bg-[#dfbfbd]'}`} key={`r-${index}`} style={{ height }} />)}</div>
                  </div>
                  <div className={`relative z-10 flex h-16 w-16 items-center justify-center rounded-full border-4 border-white shadow-[0_12px_30px_rgba(75,0,9,0.16)] ${isRecording ? 'bg-[#8a0018] text-white' : 'bg-white text-[#8a0018]'}`}>
                    <Mic size={34} strokeWidth={2} />
                  </div>
                </div>
                <p className="mt-4 text-2xl font-extrabold text-[#8a0018]">{formatSeconds(recordingDuration)}</p>
              </div>

              {error ? <p className="mx-auto mt-4 max-w-2xl rounded-2xl bg-[#fff0f1] px-4 py-3 text-sm font-semibold text-[#8a0018]">{error}</p> : null}

              <button
                className="mt-6 rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-3 text-base font-extrabold text-white shadow-[0_10px_24px_rgba(75,0,9,0.18)] disabled:opacity-60"
                disabled={submitting || uploading || pendingSubmit}
                onClick={advance}
                type="button"
              >
                {isFinalPrompt ? (submitting || uploading || pendingSubmit ? 'Đang gửi...' : 'Nộp bài') : (!isLastQuestion ? 'Câu tiếp theo' : 'Phần tiếp theo')}
              </button>
            </div>

            <div className="grid gap-4 border-t border-[#f0e6e6] px-6 py-4 md:grid-cols-3">
              {parts.map((part, index) => (
                <div className={`rounded-[18px] border px-5 py-4 text-center text-xl font-extrabold ${index === partIndex ? 'border-[#8a0018]' : 'border-[#dfe8e0]'} text-[#21446d]`} key={part.key}>{part.label}</div>
              ))}
            </div>
          </main>
        )}
      </div>

      {warning ? (
        <div className="fixed inset-0 z-[130] flex items-center justify-center bg-[#1c120f]/45 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#b26a00]">Cảnh báo bài thi</p>
            <h2 className="mt-2 text-2xl font-black">Hệ thống đã ghi nhận vi phạm</h2>
            <p className="mt-3 text-sm leading-7 text-[#584140]">{warning.reason}</p>
            <button className="mt-5 w-full rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white" onClick={async () => {
              try {
                await document.documentElement?.requestFullscreen?.();
                fullscreenSessionStartedRef.current = Boolean(document.fullscreenElement);
              } catch {
                // Do not turn an unsupported/denied fullscreen request into another violation.
              }
              setWarning(null);
            }} type="button">Quay lại toàn màn hình và tiếp tục làm bài</button>
          </div>
        </div>
      ) : null}

      {exitConfirmOpen ? (
        <div className="fixed inset-0 z-[131] flex items-center justify-center bg-[#1c120f]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8c716f]">Xác nhận thoát</p>
            <h2 className="mt-2 text-2xl font-black">Thoát khỏi bài thi này?</h2>
            <p className="mt-3 text-sm leading-7 text-[#584140]">Lần làm hiện tại chưa được nộp. Bản nháp của các phần trước vẫn được giữ.</p>
            <div className="mt-5 flex gap-3">
              <button className="flex-1 rounded-2xl border border-[#dfbfbd] px-5 py-3 text-sm font-bold" onClick={() => setExitConfirmOpen(false)} type="button">Ở lại làm bài</button>
              <button className="flex-1 rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white" onClick={closeExam} type="button">Thoát bài thi</button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
