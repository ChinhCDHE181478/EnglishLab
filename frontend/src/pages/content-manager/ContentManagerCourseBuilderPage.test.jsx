import { describe, expect, it } from 'vitest';
import { validateBuilderState } from './ContentManagerCourseBuilderPage';

const modules = [
  { id: 11, title: 'Listening Foundation', lessons: [] },
  { id: 22, title: 'Speaking Practice', lessons: [] },
];

const createAssessment = (overrides = {}) => ({
  id: 101,
  localKey: 'assessment-101',
  moduleKey: '22',
  title: 'IELTS Speaking Mock Test',
  skill: 'SPEAKING',
  type: 'MOCK_TEST',
  aiEvaluationMode: 'NONE',
  rubricId: '',
  passingScore: '',
  maxScore: '9',
  uiConfigJson: JSON.stringify({ variants: [] }),
  ...overrides,
});

describe('validateBuilderState', () => {
  it('chỉ rõ bài kiểm tra Speaking và module cần sửa', () => {
    const issue = validateBuilderState(modules, [createAssessment()]);

    expect(issue).toEqual(expect.objectContaining({
      assessmentId: 101,
      assessmentKey: 'assessment-101',
      moduleKey: '22',
      message: 'Bài kiểm tra "IELTS Speaking Mock Test" trong Module 2: Speaking Practice: cấu hình đề Speaking phải có ít nhất một đề.',
    }));
  });

  it('chỉ rõ khi đề Speaking lỗi nằm ở bài kiểm tra cuối khóa', () => {
    const issue = validateBuilderState(modules, [createAssessment({ moduleKey: 'course' })]);

    expect(issue.message).toBe(
      'Bài kiểm tra "IELTS Speaking Mock Test" ở phần bài kiểm tra cuối khóa: cấu hình đề Speaking phải có ít nhất một đề.',
    );
  });

  it('cho phép lưu khi đề Speaking đã có ít nhất một đề', () => {
    const issue = validateBuilderState(modules, [createAssessment({
      uiConfigJson: JSON.stringify({ variants: [{ key: 'test_1', parts: [] }] }),
    })]);

    expect(issue).toBeNull();
  });
});
