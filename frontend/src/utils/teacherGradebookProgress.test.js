import { describe, expect, it } from 'vitest';
import {
  buildGradebookLessons,
  getStudentLessonProgress,
  LESSON_GRADING_STATUS,
  LESSON_POSITION_STATUS,
} from './teacherGradebookProgress';

const buildUnits = (count) => Array.from({ length: count }, (_, index) => ({
  id: index + 1,
  displayOrder: index + 1,
  title: `Bài ${index + 1}`,
}));

const buildHomeworks = (count) => Array.from({ length: count }, (_, index) => ({
  id: index + 101,
  curriculumUnitId: index + 1,
  title: `Bài tập ${index + 1}`,
  maxScore: 10,
}));

describe('buildGradebookLessons', () => {
  it('đánh dấu bài 8 đang học và khóa bài 9–15 khi đã hoàn thành 7 buổi', () => {
    const homeworks = buildHomeworks(15);
    const gradebook = [{
      studentId: 1,
      homeworks: homeworks.map((homework, index) => ({
        ...homework,
        score: index < 7 ? 9 : null,
        status: index < 7 ? 'GRADED' : 'NOT_SUBMITTED',
      })),
    }];
    const sessions = Array.from({ length: 7 }, (_, index) => ({ id: index + 1, status: 'COMPLETED' }));

    const lessons = buildGradebookLessons({
      curriculumUnits: buildUnits(15),
      gradebook,
      homeworks,
      sessions,
    });

    expect(lessons.slice(0, 7).every((lesson) => (
      lesson.positionStatus === LESSON_POSITION_STATUS.PASSED
      && lesson.gradingStatus === LESSON_GRADING_STATUS.GRADED
    ))).toBe(true);
    expect(lessons[7].positionStatus).toBe(LESSON_POSITION_STATUS.CURRENT);
    expect(lessons.slice(8).every((lesson) => lesson.positionStatus === LESSON_POSITION_STATUS.NOT_REACHED)).toBe(true);
  });

  it('ưu tiên báo cần chấm khi bài đã học còn lượt nộp đang chờ', () => {
    const homeworks = buildHomeworks(2);
    const lessons = buildGradebookLessons({
      curriculumUnits: buildUnits(2),
      gradebook: [{
        studentId: 1,
        homeworks: [
          { ...homeworks[0], status: 'SUBMITTED' },
          { ...homeworks[1], status: 'NOT_SUBMITTED' },
        ],
      }],
      homeworks,
      sessions: [{ id: 1, status: 'COMPLETED' }],
    });

    expect(lessons[0].positionStatus).toBe(LESSON_POSITION_STATUS.PASSED);
    expect(lessons[0].gradingStatus).toBe(LESSON_GRADING_STATUS.PENDING);
    expect(lessons[0].stats.pendingCount).toBe(1);
  });

  it('giữ trạng thái cần chấm độc lập khi bài hiện tại có bài nộp chờ xử lý', () => {
    const homeworks = buildHomeworks(2);
    const lessons = buildGradebookLessons({
      curriculumUnits: buildUnits(2),
      gradebook: [{
        studentId: 1,
        homeworks: [
          { ...homeworks[0], status: 'GRADED' },
          { ...homeworks[1], status: 'SUBMITTED' },
        ],
      }],
      homeworks,
      sessions: [{ id: 1, status: 'COMPLETED' }],
    });

    expect(lessons[1].positionStatus).toBe(LESSON_POSITION_STATUS.CURRENT);
    expect(lessons[1].gradingStatus).toBe(LESSON_GRADING_STATUS.PENDING);
  });
});

describe('getStudentLessonProgress', () => {
  it('ghép điểm của học viên theo đúng bài tập trong bài học', () => {
    const assignments = buildHomeworks(2);
    const progress = getStudentLessonProgress({
      homeworks: [
        { id: assignments[0].id, score: 8.5, maxScore: 10, status: 'GRADED' },
        { id: assignments[1].id, score: null, maxScore: 10, status: 'SUBMITTED' },
      ],
    }, assignments);

    expect(progress.gradedCount).toBe(1);
    expect(progress.pendingCount).toBe(1);
    expect(progress.isComplete).toBe(false);
    expect(progress.results[0].score).toBe(8.5);
  });
});
