import { describe, expect, it } from 'vitest';
import {
  resolveActiveFlashcardUnitId,
  toFlashcardCourse,
} from './ClassroomFlashcardsPanel';

const flashcardUnits = [
  { id: 1, title: 'Unit 1', flashcards: [] },
  { id: 2, title: 'Unit 2', flashcards: [] },
];

describe('ClassroomFlashcardsPanel helpers', () => {
  it('keeps the unit selected by the learner instead of restoring the initial unit', () => {
    expect(resolveActiveFlashcardUnitId(flashcardUnits, '2', '1')).toBe('2');
  });

  it('uses the requested unit when there is no active unit yet', () => {
    expect(resolveActiveFlashcardUnitId(flashcardUnits, '', '2')).toBe('2');
  });

  it('maps classroom flashcard content into the workspace course shape', () => {
    const course = toFlashcardCourse({ id: 9, title: 'IELTS' }, {
      id: 2,
      title: 'Unit 2',
      flashcards: [{ resourceId: 15, title: 'Vocabulary', contentJson: '[{"term":"book","meaning":"sách"}]' }],
    });

    expect(course.modules[0].lessons[0].flashcardSets[0]).toMatchObject({
      id: 15,
      cardsJson: '[{"term":"book","meaning":"sách"}]',
    });
  });
});
