import { describe, expect, it } from 'vitest';
import { parseFlashcardSetCards } from './WorkspaceFlashcards';

describe('parseFlashcardSetCards', () => {
  it('reads the direct card array returned by the normalized classroom API', () => {
    expect(parseFlashcardSetCards({
      cardsJson: '[{"term":"book","meaning":"sách"}]',
    })).toHaveLength(1);
  });

  it('reads legacy classroom payloads that wrap cards in content data', () => {
    expect(parseFlashcardSetCards({
      cardsJson: '{"type":"FLASHCARD","cards":[{"term":"learn","meaning":"học"}]}',
    })).toEqual([{ term: 'learn', meaning: 'học' }]);
  });

  it('returns an empty list for malformed content', () => {
    expect(parseFlashcardSetCards({ cardsJson: '{broken' })).toEqual([]);
  });
});
