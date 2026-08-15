import { describe, expect, it } from 'vitest';
import { buildMockLibrary, monthProgress, parseMockTestMeta, splitMockTests } from './mockTestLibrary';

describe('mockTestLibrary', () => {
  it('groups Practise Test 2 as January 2026 test 2', () => {
    const meta = parseMockTestMeta({
      title: 'IELTS Mock Test 2026 January Speaking Practise Test 2',
      skill: 'SPEAKING',
    });
    expect(meta).toMatchObject({
      dated: true,
      year: 2026,
      monthKey: 'january',
      testNumber: 2,
      skill: 'SPEAKING',
    });
  });

  it('uses source set id when the saved title year is wrong', () => {
    const meta = parseMockTestMeta({
      title: 'IELTS Mock Test 2025 January Reading Practice Test 1',
      skill: 'READING',
      uiConfigJson: JSON.stringify({ sourceLabel: 'Imported from local HTML 2026de1r.html' }),
    });
    expect(meta).toMatchObject({
      year: 2026,
      monthKey: 'january',
      testNumber: 1,
    });
  });

  it('splits undated EnglishLab mocks into the practice list', () => {
    const { libraryTests, practiceTests } = splitMockTests([
      { id: 1, title: 'EnglishLab IELTS Listening Mock 1', skill: 'LISTENING' },
      { id: 2, title: 'IELTS Mock Test 2025 February Listening Practice Test 1', skill: 'LISTENING' },
    ]);
    expect(practiceTests).toHaveLength(1);
    expect(libraryTests).toHaveLength(1);
  });

  it('builds year / month / pack structure from real titles', () => {
    const library = buildMockLibrary([
      { id: 1, title: 'IELTS Mock Test 2025 January Listening Practice Test 2', skill: 'LISTENING' },
      { id: 2, title: 'IELTS Mock Test 2025 January Reading Practice Test 2', skill: 'READING' },
      { id: 3, title: 'IELTS Mock Test 2025 February Speaking Practice Test 1', skill: 'SPEAKING' },
      { id: 4, title: 'IELTS Mock Test 2026 January Writing Practise Test 2', skill: 'WRITING' },
    ]);
    expect(library.map((item) => item.year)).toEqual([2026, 2025]);
    const jan2025 = library.find((item) => item.year === 2025).months.find((month) => month.monthKey === 'january');
    expect(jan2025.packs).toHaveLength(1);
    expect(jan2025.packs[0].testNumber).toBe(2);
    expect(jan2025.packs[0].skills.LISTENING.id).toBe(1);
  });

  it('summarizes completed skills across packs in a month', () => {
    const library = buildMockLibrary([
      { id: 1, title: 'IELTS Mock Test 2025 January Listening Practice Test 2', skill: 'LISTENING' },
      { id: 2, title: 'IELTS Mock Test 2025 January Reading Practice Test 2', skill: 'READING' },
    ]);
    const january = library[0].months[0];
    expect(monthProgress(january, { 1: { score: 7 } })).toMatchObject({
      completed: 1,
      total: 2,
      percent: 50,
    });
  });

  it('groups TOEIC collection tests separately from IELTS', () => {
    const tests = [
      { id: 1, title: 'IELTS Mock Test 2026 January Listening Practice Test 1', skill: 'LISTENING' },
      {
        id: 2,
        title: 'TOEIC Mock Test 2026 Collection Listening Practice Test 2',
        skill: 'LISTENING',
        uiConfigJson: JSON.stringify({ sourceLabel: 'toeicde2', examType: 'TOEIC' }),
      },
      {
        id: 3,
        title: 'TOEIC Mock Test 2026 Collection Reading Practice Test 2',
        skill: 'READING',
        uiConfigJson: JSON.stringify({ sourceLabel: 'toeicde2', examType: 'TOEIC' }),
      },
    ];
    const ielts = buildMockLibrary(tests, 'IELTS');
    const toeic = buildMockLibrary(tests, 'TOEIC');
    expect(ielts).toHaveLength(1);
    expect(toeic).toHaveLength(1);
    expect(toeic[0].title).toBe('TOEIC Mock Test 2026');
    expect(toeic[0].months[0].monthKey).toBe('collection');
    expect(toeic[0].months[0].packs[0].skills.LISTENING.id).toBe(2);
    expect(toeic[0].months[0].packs[0].skills.READING.id).toBe(3);
  });
});
