import { useCallback, useEffect, useRef, useState } from 'react';

const joinTranscript = (...parts) => parts
  .filter(Boolean)
  .join(' ')
  .replace(/\s+/g, ' ')
  .trim();

export default function useLiveSpeechTranscript(language = 'en-US') {
  const [transcript, setTranscript] = useState('');
  const [interimTranscript, setInterimTranscript] = useState('');
  const recognitionRef = useRef(null);
  const transcriptRef = useRef('');
  const interimTranscriptRef = useRef('');
  const shouldRecognizeRef = useRef(false);
  const generationRef = useRef(0);

  const supported = typeof window !== 'undefined'
    && Boolean(window.SpeechRecognition || window.webkitSpeechRecognition);

  const stop = useCallback(() => {
    shouldRecognizeRef.current = false;
    generationRef.current += 1;
    const recognition = recognitionRef.current;
    recognitionRef.current = null;
    recognition?.stop?.();
    setInterimTranscript('');
  }, []);

  const reset = useCallback(() => {
    stop();
    transcriptRef.current = '';
    interimTranscriptRef.current = '';
    setTranscript('');
    setInterimTranscript('');
  }, [stop]);

  const getTranscript = useCallback(() => (
    joinTranscript(transcriptRef.current, interimTranscriptRef.current)
  ), []);

  const start = useCallback(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) return false;
    if (recognitionRef.current) return true;

    shouldRecognizeRef.current = true;
    const generation = generationRef.current;

    const launch = () => {
      if (!shouldRecognizeRef.current || generation !== generationRef.current || recognitionRef.current) return;

      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = language;
      recognition.onresult = (event) => {
        if (generation !== generationRef.current) return;
        let finalText = '';
        let interimText = '';
        for (let index = event.resultIndex; index < event.results.length; index += 1) {
          const text = String(event.results[index]?.[0]?.transcript || '').trim();
          if (event.results[index].isFinal) finalText += `${text} `;
          else interimText += `${text} `;
        }
        if (finalText.trim()) {
          const nextTranscript = joinTranscript(transcriptRef.current, finalText);
          transcriptRef.current = nextTranscript;
          setTranscript(nextTranscript);
        }
        interimTranscriptRef.current = interimText.trim();
        setInterimTranscript(interimTranscriptRef.current);
      };
      recognition.onerror = (event) => {
        if (['not-allowed', 'service-not-allowed'].includes(event.error)) {
          shouldRecognizeRef.current = false;
        }
      };
      recognition.onend = () => {
        if (recognitionRef.current === recognition) recognitionRef.current = null;
        if (shouldRecognizeRef.current && generation === generationRef.current) {
          window.setTimeout(launch, 150);
        }
      };
      recognitionRef.current = recognition;
      try {
        recognition.start();
      } catch {
        recognitionRef.current = null;
      }
    };

    launch();
    return true;
  }, [language]);

  useEffect(() => () => stop(), [stop]);

  return {
    getTranscript,
    interimTranscript,
    resetTranscript: reset,
    startTranscription: start,
    stopTranscription: stop,
    supportsLiveTranscription: supported,
    transcript,
  };
}
