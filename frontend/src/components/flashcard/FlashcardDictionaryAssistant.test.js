import { describe, expect, it } from 'vitest';
import { buildExampleOptions, buildMeaningOptions } from './FlashcardDictionaryAssistant';

describe('flashcard dictionary suggestions', () => {
  it('keeps the full Vietnamese translation and exposes alternatives', () => {
    const options = buildMeaningOptions({
      word: 'run',
      phonetic: '/rʌn/',
      meaningVietnamese: 'chạy; vận hành',
    });

    expect(options.map((item) => item.value)).toEqual(['chạy; vận hành', 'chạy', 'vận hành']);
  });

  it('collects unique examples from all dictionary meanings', () => {
    const options = buildExampleOptions({
      meanings: [
        { definitions: [{ example: 'I run every day.' }, { example: 'I run every day.' }] },
        { definitions: [{ example: 'She runs a small shop.' }, { definition: 'No example.' }] },
      ],
    });

    expect(options.map((item) => item.value)).toEqual([
      'I run every day.',
      'She runs a small shop.',
    ]);
  });
});
