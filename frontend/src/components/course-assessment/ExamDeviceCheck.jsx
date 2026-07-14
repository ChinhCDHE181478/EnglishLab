import { useEffect, useRef, useState } from 'react';
import { CheckCircle2, Headphones, Mic, Play } from 'lucide-react';
import BrandedSelect from '../ui/BrandedSelect';

const deviceOptions = (devices, fallback) => (
  devices.length
    ? devices.map((device, index) => ({
      label: device.label || `${fallback} ${index + 1}`,
      value: device.deviceId,
    }))
    : [{ label: fallback, value: '' }]
);

export default function ExamDeviceCheck({
  includeMicrophone = false,
  requireMic = false,
  requireFullscreen = true,
  onCancel,
  onComplete,
  title = 'Kiểm tra thiết bị trước khi làm bài',
  description = 'Vui lòng bật toàn màn hình để đảm bảo tính nghiêm túc của bài đánh giá.',
}) {
  const microphoneRequired = includeMicrophone || requireMic;
  const [inputs, setInputs] = useState([]);
  const [outputs, setOutputs] = useState([]);
  const [inputId, setInputId] = useState('');
  const [outputId, setOutputId] = useState('');
  const [soundState, setSoundState] = useState('idle');
  const [micState, setMicState] = useState('idle');
  const [micLevel, setMicLevel] = useState(0);
  const [micCountdown, setMicCountdown] = useState(5);
  const [message, setMessage] = useState('');
  const audioRef = useRef(null);
  const streamRef = useRef(null);
  const recorderRef = useRef(null);
  const audioContextRef = useRef(null);
  const analyserRef = useRef(null);
  const frameRef = useRef(null);
  const timeoutRef = useRef(null);
  const intervalRef = useRef(null);

  const refreshDevices = async () => {
    if (!navigator.mediaDevices?.enumerateDevices) return;
    const devices = await navigator.mediaDevices.enumerateDevices();
    const nextInputs = devices.filter((device) => device.kind === 'audioinput');
    const nextOutputs = devices.filter((device) => device.kind === 'audiooutput');
    setInputs(nextInputs);
    setOutputs(nextOutputs);
    setInputId((current) => current || nextInputs[0]?.deviceId || '');
    setOutputId((current) => current || nextOutputs[0]?.deviceId || '');
  };

  const stopMic = () => {
    if (frameRef.current) window.cancelAnimationFrame(frameRef.current);
    if (timeoutRef.current) window.clearTimeout(timeoutRef.current);
    if (intervalRef.current) window.clearInterval(intervalRef.current);
    frameRef.current = null;
    timeoutRef.current = null;
    intervalRef.current = null;
    if (recorderRef.current?.state === 'recording') recorderRef.current.stop();
    streamRef.current?.getTracks?.().forEach((track) => track.stop());
    streamRef.current = null;
    recorderRef.current = null;
    analyserRef.current = null;
    if (audioContextRef.current && audioContextRef.current.state !== 'closed') {
      audioContextRef.current.close().catch(() => {});
    }
    audioContextRef.current = null;
    setMicLevel(0);
  };

  useEffect(() => {
    refreshDevices().catch(() => {});
    return stopMic;
  }, []);

  const playSound = async () => {
    try {
      setMessage('');
      setSoundState('playing');
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextCtor) throw new Error('unsupported');
      const context = new AudioContextCtor();
      if (context.state === 'suspended') await context.resume();
      const destination = context.createMediaStreamDestination();
      const gain = context.createGain();
      gain.gain.value = 0.08;
      gain.connect(context.destination);
      gain.connect(destination);
      if (audioRef.current && 'setSinkId' in HTMLMediaElement.prototype && outputId) {
        await audioRef.current.setSinkId(outputId).catch(() => {});
        audioRef.current.srcObject = destination.stream;
        await audioRef.current.play().catch(() => {});
      }
      const now = context.currentTime;
      [523.25, 659.25, 783.99].forEach((frequency, index) => {
        const oscillator = context.createOscillator();
        oscillator.frequency.value = frequency;
        oscillator.connect(gain);
        oscillator.start(now + index * 0.42);
        oscillator.stop(now + index * 0.42 + 0.32);
      });
      window.setTimeout(() => {
        setSoundState('passed');
        context.close().catch(() => {});
      }, 1500);
    } catch {
      setSoundState('failed');
      setMessage('Không thể phát âm thanh mẫu. Hãy kiểm tra quyền âm thanh của trình duyệt.');
    }
  };

  const testMicrophone = async () => {
    stopMic();
    try {
      setMessage('');
      setMicState('testing');
      setMicCountdown(5);
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: inputId ? { deviceId: { exact: inputId } } : true,
      });
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextCtor || typeof MediaRecorder === 'undefined') throw new Error('unsupported');
      const context = new AudioContextCtor();
      if (context.state === 'suspended') await context.resume();
      const analyser = context.createAnalyser();
      analyser.fftSize = 256;
      context.createMediaStreamSource(stream).connect(analyser);
      const recorder = new MediaRecorder(stream);
      const values = new Uint8Array(analyser.fftSize);
      let detectedVoice = false;
      streamRef.current = stream;
      recorderRef.current = recorder;
      audioContextRef.current = context;
      analyserRef.current = analyser;
      const tick = () => {
        if (!analyserRef.current) return;
        analyserRef.current.getByteTimeDomainData(values);
        const rms = Math.sqrt(values.reduce((sum, value) => {
          const normalized = (value - 128) / 128;
          return sum + normalized * normalized;
        }, 0) / values.length);
        const level = Math.min(100, Math.round(rms * 260));
        if (level >= 10) detectedVoice = true;
        setMicLevel(level);
        frameRef.current = window.requestAnimationFrame(tick);
      };
      recorder.onstop = () => setMicState(detectedVoice ? 'passed' : 'failed');
      recorder.start();
      frameRef.current = window.requestAnimationFrame(tick);
      intervalRef.current = window.setInterval(() => {
        setMicCountdown((current) => Math.max(0, current - 1));
      }, 1000);
      timeoutRef.current = window.setTimeout(() => {
        stopMic();
        if (!detectedVoice) {
          setMessage('Chưa phát hiện giọng nói rõ ràng. Hãy chọn đúng micro và thử lại.');
        }
      }, 5000);
      await refreshDevices();
    } catch {
      setMicState('failed');
      setMessage('Không thể sử dụng micro. Hãy cấp quyền micro cho trình duyệt rồi thử lại.');
      stopMic();
    }
  };

  const ready = soundState === 'passed' && (!microphoneRequired || micState === 'passed');

  const completeCheck = async () => {
    setMessage('');
    if (requireFullscreen && !document.fullscreenElement) {
      try {
        if (!document.documentElement?.requestFullscreen) throw new Error('unsupported');
        await document.documentElement.requestFullscreen();
      } catch {
        setMessage('Không thể bật toàn màn hình. Hãy cho phép trình duyệt mở toàn màn hình rồi thử lại.');
        return;
      }
    }
    onComplete?.({
      completed: true,
      soundPassed: true,
      microphoneChecked: microphoneRequired ? micState === 'passed' : false,
      microphonePassed: microphoneRequired ? micState === 'passed' : false,
      deviceCheckPassed: true,
      fullscreenConfirmed: requireFullscreen ? Boolean(document.fullscreenElement) : false,
      inputDeviceId: inputId,
      outputDeviceId: outputId,
      checkedAt: new Date().toISOString(),
    });
  };

  return (
    <div className="mx-auto w-full max-w-5xl rounded-[30px] border border-[#dfbfbd]/35 bg-white p-6 shadow-[0_18px_55px_rgba(75,0,9,0.10)] md:p-8">
      <audio className="hidden" ref={audioRef} />
      <h1 className="text-center font-['Manrope'] text-2xl font-extrabold text-[#21446d]">{title}</h1>
      {description ? <p className="mx-auto mt-3 max-w-2xl text-center text-sm leading-7 text-[#584140]">{description}</p> : null}

      <div className="mt-8 space-y-9">
        <section className="grid gap-5 md:grid-cols-[56px_1fr]">
          <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[#8a0018]/25 text-[#8a0018]">
            <Headphones aria-hidden="true" size={23} />
          </div>
          <div>
            <h2 className="text-2xl font-extrabold text-[#21446d]"><span className="mr-2 text-[#21446d]/55">1.</span>Kiểm tra tai nghe</h2>
            <p className="mt-3 text-sm leading-7 text-[#584140]">Hãy phát âm thanh mẫu để chắc rằng tai nghe hoặc loa của bạn nghe rõ trước khi bắt đầu bài thi.</p>
            <div className="mt-5 flex flex-wrap items-center gap-4 rounded-[24px] border border-[#dfbfbd]/40 bg-[#fffdfc] px-5 py-5">
              <button className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] text-white shadow-[0_10px_24px_rgba(75,0,9,0.24)]" onClick={playSound} type="button">
                {soundState === 'passed' ? <CheckCircle2 size={22} /> : <Play fill="currentColor" size={20} />}
              </button>
              <div className="min-w-[180px] flex-1">
                <div className="h-2 rounded-full bg-[#f3d7dd]">
                  <div className="h-full rounded-full bg-[linear-gradient(90deg,#8a0018,#b4233f)] transition-all" style={{ width: soundState === 'passed' ? '100%' : soundState === 'playing' ? '60%' : '0%' }} />
                </div>
              </div>
              <span className="text-sm font-semibold text-[#7a6766]">{soundState === 'passed' ? '00:08' : '00:00'}</span>
              <BrandedSelect
                buttonClassName="min-w-[280px] border-[#dfbfbd]/50 py-3 shadow-none"
                onChange={(event) => {
                  setOutputId(event.target.value);
                  setSoundState('idle');
                }}
                options={deviceOptions(outputs, 'Loa mặc định')}
                value={outputId}
              />
            </div>
          </div>
        </section>

        {microphoneRequired ? (
          <section className="grid gap-5 md:grid-cols-[56px_1fr]">
            <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[#8a0018]/25 text-[#8a0018]">
              <Mic aria-hidden="true" size={23} />
            </div>
            <div>
              <h2 className="text-2xl font-extrabold text-[#21446d]"><span className="mr-2 text-[#21446d]/55">2.</span>Kiểm tra micro</h2>
              <p className="mt-3 text-sm leading-7 text-[#584140]">Đọc rõ câu mẫu để kiểm tra micro có thu được giọng nói ổn định hay không.</p>
              <p className="mt-4 text-center text-sm font-semibold leading-7 text-[#8c716f]">“I love English. My English is great and I practice it every day!”</p>
              <div className="mt-5 flex flex-wrap items-center gap-4 rounded-[24px] border border-[#dfbfbd]/40 bg-[#fffdfc] px-5 py-5">
                <button className="flex h-14 shrink-0 items-center gap-2 rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] px-5 text-sm font-extrabold text-white disabled:opacity-60" disabled={micState === 'testing'} onClick={testMicrophone} type="button">
                  <Mic size={19} />
                  {micState === 'testing' ? `Đang ghi thử ${micCountdown}s` : micState === 'passed' ? 'Kiểm tra lại' : 'Bắt đầu kiểm tra'}
                </button>
                <div className="min-w-[180px] flex-1">
                  <div className="h-2 rounded-full bg-[#f3d7dd]">
                    <div className="h-full rounded-full bg-[linear-gradient(90deg,#8a0018,#b4233f)] transition-all" style={{ width: `${micState === 'passed' ? 100 : micLevel}%` }} />
                  </div>
                </div>
                <span className="min-w-10 text-sm font-semibold text-[#7a6766]">{micState === 'passed' ? 'Đạt' : `${micLevel}%`}</span>
                <BrandedSelect
                  buttonClassName="min-w-[280px] border-[#dfbfbd]/50 py-3 shadow-none"
                  onChange={(event) => {
                    setInputId(event.target.value);
                    setMicState('idle');
                  }}
                  options={deviceOptions(inputs, 'Micro mặc định')}
                  value={inputId}
                />
              </div>
            </div>
          </section>
        ) : null}
      </div>

      {message ? <p className="mt-5 rounded-2xl bg-[#fff0f1] px-4 py-3 text-sm font-semibold text-[#8a0018]">{message}</p> : null}

      <div className="mt-8 flex flex-wrap justify-end gap-3">
        <button className="rounded-2xl border border-[#8a0018]/20 px-5 py-3 text-sm font-bold text-[#8a0018]" onClick={onCancel} type="button">Thoát bài thi</button>
        <button
          className="rounded-2xl bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-3 text-sm font-black text-white shadow-[0_14px_28px_rgba(138,0,24,0.20)] disabled:cursor-not-allowed disabled:opacity-40"
          disabled={!ready}
          onClick={completeCheck}
          type="button"
        >
          Tiếp tục vào bài thi
        </button>
      </div>
    </div>
  );
}
