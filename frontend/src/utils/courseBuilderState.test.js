import { describe, expect, it } from 'vitest';
import { createCourseBuilderFingerprint } from './courseBuilderState';

describe('createCourseBuilderFingerprint', () => {
  it('bỏ qua khóa tạm chỉ dùng cho giao diện', () => {
    const first = createCourseBuilderFingerprint({
      modules: [{ id: 1, tempId: 'module-a', title: 'Listening', lessons: [] }],
    }, [{ id: 2, localKey: 'assessment-a', title: 'Module test' }]);
    const second = createCourseBuilderFingerprint({
      modules: [{ id: 1, tempId: 'module-b', title: 'Listening', lessons: [] }],
    }, [{ id: 2, localKey: 'assessment-b', title: 'Module test' }]);

    expect(first).toBe(second);
  });

  it('phát hiện thay đổi nội dung module', () => {
    const saved = createCourseBuilderFingerprint({
      modules: [{ id: 1, title: 'Listening', lessons: [] }],
    });
    const edited = createCourseBuilderFingerprint({
      modules: [{ id: 1, title: 'Listening nâng cao', lessons: [] }],
    });

    expect(edited).not.toBe(saved);
  });
});
