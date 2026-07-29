import { describe, expect, it } from 'vitest';
import { toPersonalFlashcardTerm } from './FlashcardPracticePage';

describe('toPersonalFlashcardTerm', () => {
  it('maps a saved dictionary word into a personal flashcard with persisted status', () => {
    expect(toPersonalFlashcardTerm({
      id: 12,
      word: 'resilience',
      phonetic: '/rɪˈzɪliəns/',
      primaryDefinition: 'khả năng phục hồi',
      note: 'Resilience helps people recover from setbacks.',
      status: 'MASTERED',
    })).toMatchObject({
      termKey: 'personal-12',
      term: 'resilience',
      meaning: 'khả năng phục hồi',
      example: 'Resilience helps people recover from setbacks.',
      commonError: 'Phiên âm: /rɪˈzɪliəns/',
      moduleTitle: 'Flashcard cá nhân',
      status: 'MASTERED',
      savedVocabularyId: 12,
    });
  });

  it('keeps a newly saved word in the learning queue', () => {
    expect(toPersonalFlashcardTerm({
      id: 13,
      word: 'coherent',
      primaryDefinition: 'mạch lạc',
      status: 'LEARNING',
    })).toMatchObject({
      termKey: 'personal-13',
      status: 'LEARNING',
      example: '',
      commonError: '',
    });
  });
});
