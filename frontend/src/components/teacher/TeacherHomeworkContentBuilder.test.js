import { describe, expect, it } from 'vitest';
import {
  buildHomeworkActivityConfig,
  parseHomeworkSpreadsheetRows,
  parseHomeworkBuilderDrafts,
} from './TeacherHomeworkContentBuilder';

const buildConfig = (overrides = {}) => buildHomeworkActivityConfig({
  activityType: 'SKILL_PRACTICE',
  skill: 'READING',
  questions: [],
  writingTasks: [],
  speakingParts: [],
  flashcards: [],
  ...overrides,
});

describe('TeacherHomeworkContentBuilder data mapping', () => {
  it('stores objective questions with their answer key', () => {
    const config = JSON.parse(buildConfig({
      questions: [{
        prompt: 'Choose the correct answer.',
        options: ['First', 'Second', 'Third', 'Fourth'],
        correctAnswer: 'B',
      }],
    }));

    expect(config.questions[0].options[1]).toEqual({ value: 'B', label: 'Second' });
    expect(config.answerKey).toEqual({ 1: 'B' });
  });

  it('stores a direct Writing task without exposing JSON to the teacher', () => {
    const config = JSON.parse(buildConfig({
      activityType: 'TEXT_RESPONSE',
      skill: 'WRITING',
      writingTasks: [{
        title: 'Task 2',
        question: 'Discuss both views.',
        minimumWords: '250',
        recommendedMinutes: '40',
      }],
    }));

    expect(config.durationMinutes).toBe(40);
    expect(config.tasks[0]).toMatchObject({
      title: 'Task 2',
      question: 'Discuss both views.',
      minimumWords: 250,
    });
  });

  it('stores Speaking parts and prompts for the recording workspace', () => {
    const config = JSON.parse(buildConfig({
      activityType: 'TEXT_RESPONSE',
      skill: 'SPEAKING',
      speakingParts: [{
        title: 'Part 1',
        prompts: ['Where are you from?', 'What do you like about it?'],
        answerSeconds: '90',
      }],
    }));

    expect(config.parts[0].prompts).toHaveLength(2);
    expect(config.parts[0].answerSeconds).toBe(90);
  });

  it('round-trips teacher-authored flashcards', () => {
    const saved = buildConfig({
      activityType: 'FLASHCARD_REVIEW',
      flashcards: [{
        term: 'collocation',
        meaning: 'cụm từ thường đi cùng nhau',
        example: 'Make a decision.',
        commonMistake: 'Không dùng do a decision.',
      }],
    });
    const drafts = parseHomeworkBuilderDrafts(saved);

    expect(drafts.flashcards[0]).toMatchObject({
      term: 'collocation',
      meaning: 'cụm từ thường đi cùng nhau',
      example: 'Make a decision.',
      commonMistake: 'Không dùng do a decision.',
    });
  });

  it('imports objective questions and reports the exact invalid Excel row', () => {
    const result = parseHomeworkSpreadsheetRows([
      ['Câu hỏi', 'Đáp án A', 'Đáp án B', 'Đáp án C', 'Đáp án D', 'Đáp án đúng'],
      ['Choose one.', 'First', 'Second', 'Third', 'Fourth', 'B'],
      ['Missing options', 'First', '', 'Third', 'Fourth', 'A'],
    ], 'QUIZ');

    expect(result.items).toHaveLength(1);
    expect(result.items[0]).toMatchObject({
      prompt: 'Choose one.',
      options: ['First', 'Second', 'Third', 'Fourth'],
      correctAnswer: 'B',
    });
    expect(result.invalidRows).toEqual([3]);
  });

  it('groups imported Speaking prompts by part name', () => {
    const result = parseHomeworkSpreadsheetRows([
      ['Tên phần', 'Câu hỏi', 'Thời gian trả lời (giây)'],
      ['Part 1', 'Where are you from?', '90'],
      ['', 'What do you like about your hometown?', '90'],
      ['Part 2', 'Describe a memorable trip.', '120'],
    ], 'SPEAKING');

    expect(result.items).toHaveLength(2);
    expect(result.items[0].prompts).toHaveLength(2);
    expect(result.items[1]).toMatchObject({ title: 'Part 2', answerSeconds: '120' });
  });

  it('imports Writing tasks with defaults for optional numeric columns', () => {
    const result = parseHomeworkSpreadsheetRows([
      ['Tên đề', 'Đề bài', 'Số từ tối thiểu', 'Thời gian gợi ý (phút)'],
      ['Task 2', 'Discuss both views.', '', ''],
    ], 'WRITING');

    expect(result.invalidRows).toEqual([]);
    expect(result.items[0]).toMatchObject({
      title: 'Task 2',
      question: 'Discuss both views.',
      minimumWords: '150',
      recommendedMinutes: '40',
    });
  });
});
