const findEnglishVoice = (voices) => (
  voices.find((voice) => /^en-US$/i.test(voice.lang))
  || voices.find((voice) => /^en[-_]/i.test(voice.lang))
  || null
);

export const speakEnglishText = (text, options = {}) => {
  const speechSynthesis = Object.hasOwn(options, 'speechSynthesis')
    ? options.speechSynthesis
    : globalThis.window?.speechSynthesis;
  const SpeechUtterance = Object.hasOwn(options, 'SpeechUtterance')
    ? options.SpeechUtterance
    : globalThis.window?.SpeechSynthesisUtterance;
  const normalizedText = String(text || '').trim();

  return new Promise((resolve, reject) => {
    if (!normalizedText || !speechSynthesis || !SpeechUtterance) {
      reject(new Error('Speech synthesis is unavailable.'));
      return;
    }

    speechSynthesis.cancel();
    const utterance = new SpeechUtterance(normalizedText);
    utterance.lang = 'en-US';
    utterance.rate = 0.9;
    utterance.pitch = 1;
    utterance.voice = findEnglishVoice(speechSynthesis.getVoices?.() || []);
    utterance.onend = resolve;
    utterance.onerror = () => reject(new Error('Speech synthesis failed.'));
    speechSynthesis.speak(utterance);
  });
};
