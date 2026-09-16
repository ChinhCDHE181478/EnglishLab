import { describe, expect, it } from 'vitest';
import {
  alignStartDateToWeekdays,
  getWeekdayForDate,
  includeStartDateWeekday,
  toggleProposalWeekday,
} from './classroomProposalSchedule';

describe('classroom proposal schedule', () => {
  it('selects the weekday that matches the chosen start date', () => {
    expect(getWeekdayForDate('2026-09-12')).toBe('SATURDAY');
    expect(includeStartDateWeekday(['MONDAY'], '2026-09-12')).toEqual(['MONDAY', 'SATURDAY']);
  });

  it('moves the start date to the next remaining study day', () => {
    expect(toggleProposalWeekday(
      '2026-09-12',
      ['MONDAY', 'WEDNESDAY', 'FRIDAY', 'SATURDAY'],
      'SATURDAY',
    )).toEqual({
      plannedStartDate: '2026-09-14',
      weekdays: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
    });
  });

  it('keeps the only selected weekday to avoid an inconsistent form', () => {
    expect(toggleProposalWeekday('2026-09-12', ['SATURDAY'], 'SATURDAY')).toEqual({
      plannedStartDate: '2026-09-12',
      weekdays: ['SATURDAY'],
    });
  });

  it('aligns a legacy start boundary with the first actual study day', () => {
    expect(alignStartDateToWeekdays('2026-09-12', ['MONDAY', 'WEDNESDAY', 'FRIDAY']))
      .toBe('2026-09-14');
  });
});
