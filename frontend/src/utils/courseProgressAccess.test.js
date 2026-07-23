import { describe, expect, it } from 'vitest';
import { findFurthestReachedModuleIndex, isReachedModuleUnlocked } from './courseProgressAccess';

const getLessonId = (_module, lesson) => lesson.id;

describe('course progress access across published versions', () => {
  it('does not lock earlier modules again when the learner already reached a later module', () => {
    const modules = [
      { id: 1, lessons: [{ id: 11 }, { id: 12 }] },
      { id: 2, lessons: [{ id: 21 }] },
      { id: 3, lessons: [{ id: 31 }] },
    ];
    const furthestReachedModuleIndex = findFurthestReachedModuleIndex({
      modules,
      completedLessonIds: new Set([31]),
      assessmentsByModule: new Map(),
      getLessonId,
    });

    expect(furthestReachedModuleIndex).toBe(2);
    expect(isReachedModuleUnlocked({
      sequentiallyUnlocked: false,
      moduleIndex: 1,
      furthestReachedModuleIndex,
    })).toBe(true);
  });

  it('treats a historical assessment submission as evidence that the module was reached', () => {
    const modules = [{ id: 1, lessons: [] }, { id: 2, lessons: [] }];
    const assessmentsByModule = new Map([
      ['2', [{ id: 202, latestSubmission: { id: 9001, status: 'PASSED' } }]],
    ]);

    expect(findFurthestReachedModuleIndex({
      modules,
      completedLessonIds: new Set(),
      assessmentsByModule,
      getLessonId,
    })).toBe(1);
  });
});
