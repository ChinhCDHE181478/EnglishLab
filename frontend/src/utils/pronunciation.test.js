import { describe, expect, it, vi } from 'vitest';
import { speakEnglishText } from './pronunciation';

describe('speakEnglishText', () => {
  it('uses an English voice and resolves when pronunciation finishes', async () => {
    const englishVoice = { lang: 'en-US', name: 'English' };
    const speechSynthesis = {
      cancel: vi.fn(),
      getVoices: () => [{ lang: 'vi-VN', name: 'Vietnamese' }, englishVoice],
      speak: vi.fn((utterance) => utterance.onend()),
    };
    class SpeechUtterance {
      constructor(text) {
        this.text = text;
      }
    }

    await speakEnglishText('resilience', { speechSynthesis, SpeechUtterance });

    const utterance = speechSynthesis.speak.mock.calls[0][0];
    expect(speechSynthesis.cancel).toHaveBeenCalledOnce();
    expect(utterance.text).toBe('resilience');
    expect(utterance.lang).toBe('en-US');
    expect(utterance.voice).toBe(englishVoice);
  });

  it('rejects when the browser does not support speech synthesis', async () => {
    await expect(speakEnglishText('hello', {
      speechSynthesis: null,
      SpeechUtterance: null,
    })).rejects.toThrow('Speech synthesis is unavailable.');
  });
});
