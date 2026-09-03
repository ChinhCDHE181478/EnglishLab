import { describe, expect, it } from 'vitest';
import {
  getEnglishProfileDefaults,
  normalizeEnglishEntryLevel,
  normalizeEnglishExamCategory,
  validateEnglishProgramProfile,
} from './englishProgramProfile';

describe('englishProgramProfile', () => {
  it('normalizes legacy general-English categories', () => {
    expect(normalizeEnglishExamCategory('GENERAL')).toBe('GENERAL_ENGLISH');
    expect(normalizeEnglishExamCategory('COMMUNICATION')).toBe('GENERAL_ENGLISH');
  });

  it('provides exam-specific defaults', () => {
    expect(getEnglishProfileDefaults('IELTS')).toMatchObject({
      entryLevel: '4.0',
      targetBand: 6.5,
      targetScore: '',
    });
    expect(getEnglishProfileDefaults('TOEIC')).toMatchObject({
      entryLevel: '450',
      targetBand: '',
      targetScore: 650,
    });
  });

  it('normalizes legacy entry-level labels into selectable values', () => {
    expect(normalizeEnglishEntryLevel('Band 5.0 - 5.5', 'IELTS')).toBe('5.0');
    expect(normalizeEnglishEntryLevel('TOEIC 450+', 'TOEIC')).toBe('450');
    expect(normalizeEnglishEntryLevel('Trình độ B1', 'GENERAL_ENGLISH')).toBe('B1');
  });

  it('rejects a TOEIC score on the wrong scale', () => {
    expect(validateEnglishProgramProfile({
      examCategory: 'TOEIC',
      focusSkills: ['LISTENING', 'READING'],
      entryLevel: '450',
      targetScore: 6.5,
    })).toContain('Điểm TOEIC');
  });

  it('rejects an entry score higher than the target', () => {
    expect(validateEnglishProgramProfile({
      examCategory: 'TOEIC',
      focusSkills: ['LISTENING', 'READING'],
      entryLevel: '750',
      targetScore: 650,
    })).toBe('Điểm TOEIC đầu vào không thể cao hơn điểm mục tiêu.');
  });

  it('requires a valid CEFR level for General English', () => {
    expect(validateEnglishProgramProfile({
      examCategory: 'GENERAL_ENGLISH',
      focusSkills: ['LISTENING', 'SPEAKING'],
      entryLevel: 'Sơ cấp',
    })).toContain('CEFR');
  });

  it('rejects a score scale from another exam', () => {
    expect(validateEnglishProgramProfile({
      examCategory: 'IELTS',
      focusSkills: ['LISTENING', 'READING'],
      entryLevel: '4.0',
      targetBand: 6.5,
      targetScore: 650,
    })).toContain('không sử dụng thang điểm TOEIC');
  });

  it('accepts a complete IELTS profile', () => {
    expect(validateEnglishProgramProfile({
      examCategory: 'IELTS',
      focusSkills: ['LISTENING', 'READING', 'WRITING', 'SPEAKING'],
      entryLevel: '5.0',
      targetBand: 6.5,
    })).toBe('');
  });
});
