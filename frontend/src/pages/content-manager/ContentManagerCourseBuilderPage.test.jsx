import { describe, expect, it } from 'vitest';
import {
  isPublishedReusableAssessment,
  validateBuilderState,
} from './ContentManagerCourseBuilderPage';

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

  it('chặn bài học quiz chưa có assessment thật', () => {
    const quizModules = [{
      id: 11,
      title: 'Vocabulary',
      lessons: [{ id: 55, title: 'Ôn từ vựng', contentType: 'QUIZ', durationMinutes: 10 }],
    }];

    expect(validateBuilderState(quizModules, [])).toEqual(expect.objectContaining({
      moduleKey: '11',
      lessonKey: '55',
      message: 'Bài học "Ôn từ vựng" chưa có nội dung trắc nghiệm.',
    }));
  });

  it('cho phép quiz có câu hỏi và đáp án liên kết đúng lesson', () => {
    const quizModules = [{
      id: 11,
      title: 'Vocabulary',
      lessons: [{ id: 55, title: 'Ôn từ vựng', contentType: 'QUIZ', durationMinutes: 10 }],
    }];
    const quiz = createAssessment({
      lessonKey: '55',
      lessonTitle: 'Ôn từ vựng',
      moduleKey: '11',
      type: 'QUIZ',
      skill: 'VOCABULARY',
      aiEvaluationMode: 'RUBRIC_FEEDBACK',
      maxScore: '1',
      passingScore: '1',
      uiConfigJson: JSON.stringify({ parts: [{ key: 'part_1', questionGroups: [{ questions: [{ number: 1 }] }] }] }),
      objectiveAnswerKey: JSON.stringify({ 1: 'A' }),
    });

    expect(validateBuilderState(quizModules, [quiz])).toBeNull();
  });
});

describe('isPublishedReusableAssessment', () => {
  it.each([
    ['bài luyện nghe', { status: 'PUBLISHED', skill: 'LISTENING', type: 'LESSON_PRACTICE' }],
    ['bài luyện đọc', { status: 'PUBLISHED', skill: 'READING', type: 'QUIZ' }],
    ['bài luyện viết', { status: 'PUBLISHED', skill: 'WRITING', type: 'WRITING_TASK' }],
    ['bài luyện nói', { status: 'PUBLISHED', skill: 'SPEAKING', type: 'SPEAKING_TASK' }],
    ['đề thi thử', { status: 'PUBLISHED', skill: 'LISTENING', type: 'MOCK_TEST' }],
    ['đề mô-đun cũ', { status: 'PUBLISHED', skill: 'WRITING', type: 'MODULE_TEST' }],
  ])('cho phép gắn %s đã xuất bản', (_label, item) => {
    expect(isPublishedReusableAssessment(item)).toBe(true);
  });

  it.each([
    ['đề nháp', { status: 'DRAFT', skill: 'LISTENING', type: 'MOCK_TEST' }],
    ['đề lưu trữ', { status: 'ARCHIVED', skill: 'READING', type: 'LESSON_PRACTICE' }],
    ['nội dung ngoài bốn kỹ năng', { status: 'PUBLISHED', skill: 'VOCABULARY', type: 'QUIZ' }],
    ['loại đề không phù hợp với kỹ năng', { status: 'PUBLISHED', skill: 'WRITING', type: 'QUIZ' }],
  ])('không cho phép gắn %s', (_label, item) => {
    expect(isPublishedReusableAssessment(item)).toBe(false);
  });
});
