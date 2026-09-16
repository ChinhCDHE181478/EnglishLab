import { describe, expect, it } from 'vitest';
import { buildLessonWorkspacePath, getRequestedLessonId } from './courseWorkspaceNavigation';

describe('course workspace navigation', () => {
  it('builds a deep link for the selected lesson', () => {
    expect(buildLessonWorkspacePath('e2-ielts-practice-tests', 42))
      .toBe('/courses/e2-ielts-practice-tests/learn?lessonId=42');
  });

  it('preserves fallback lesson identifiers through URL encoding', () => {
    const path = buildLessonWorkspacePath('course-a', 'module 2-Bài đọc-1');
    expect(getRequestedLessonId(new URL(path, 'https://englishlab.io.vn').search))
      .toBe('module 2-Bài đọc-1');
  });

  it('returns no requested lesson when the query parameter is absent', () => {
    expect(getRequestedLessonId('?tab=notes')).toBeNull();
  });
});
