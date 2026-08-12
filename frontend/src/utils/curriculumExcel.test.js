import { describe, expect, it, vi } from 'vitest';
import { importCurriculumUnitsWithSessionPlans, parseCurriculumExcelRows } from './curriculumExcel';

const headers = ['Tên chương trình', 'Tên Unit', 'Mô tả Unit', 'Số buổi', 'Tên buổi học', 'Mô tả buổi học', 'Mục tiêu học tập'];

describe('curriculum Excel import', () => {
  it('groups multiple planned sessions under the same Unit', () => {
    const parsed = parseCurriculumExcelRows([
      headers,
      ['IELTS Reading', 'Reading Fundamentals', 'Nền tảng', '1', 'Skimming', 'Đọc lướt', 'Nắm ý chính'],
      ['', 'Reading Fundamentals', '', '2', 'Scanning', 'Tìm chi tiết', 'Xác định từ khóa'],
      ['', 'Question Types', 'Dạng câu hỏi', '3', 'Matching Headings', '', ''],
    ]);

    expect(parsed.units).toHaveLength(2);
    expect(parsed.units[0].sessionPlans).toHaveLength(2);
    expect(parsed.units[0].sessionPlans[1]).toMatchObject({ sessionNumber: 2, title: 'Scanning' });
    expect(parsed.units[1].sessionPlans[0]).toMatchObject({ sessionNumber: 3, title: 'Matching Headings' });
  });

  it('reports the exact row when a session number is duplicated', () => {
    expect(() => parseCurriculumExcelRows([
      headers,
      ['IELTS Reading', 'Unit 1', '', '1', 'Skimming', '', ''],
      ['', 'Unit 2', '', '1', 'Scanning', '', ''],
    ])).toThrow('Dòng 3: buổi 1 bị trùng');
  });

  it('creates each grouped Unit once before creating its planned sessions', async () => {
    const units = parseCurriculumExcelRows([
      headers,
      ['IELTS Reading', 'Reading Fundamentals', 'Nền tảng', '1', 'Skimming', '', ''],
      ['', 'Reading Fundamentals', '', '2', 'Scanning', '', ''],
    ]).units;
    const api = {
      createCurriculumUnit: vi.fn().mockResolvedValue({ id: 10 }),
      createCurriculumSessionPlan: vi.fn().mockResolvedValue({ id: 20 }),
    };

    const result = await importCurriculumUnitsWithSessionPlans(api, 7, units);

    expect(api.createCurriculumUnit).toHaveBeenCalledTimes(1);
    expect(api.createCurriculumSessionPlan).toHaveBeenCalledTimes(2);
    expect(result).toMatchObject({ createdUnits: 1, createdSessionPlans: 2, failures: [] });
  });
});
