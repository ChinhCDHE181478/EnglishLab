import { describe, expect, it } from 'vitest';
import {
  buildHomeworkActivityConfig,
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
});
