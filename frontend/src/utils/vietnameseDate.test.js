import { describe, expect, it } from 'vitest';
import {
  combineLocalDateTime,
  formatIsoDateForDisplay,
  maskVietnameseDate,
  parseVietnameseDate,
} from './vietnameseDate';

describe('Vietnamese date helpers', () => {
  it('formats and parses dates as dd/MM/yyyy', () => {
    expect(formatIsoDateForDisplay('2026-07-24')).toBe('24/07/2026');
    expect(parseVietnameseDate('24/07/2026')).toBe('2026-07-24');
  });

  it('rejects invalid calendar dates', () => {
    expect(parseVietnameseDate('31/02/2026')).toBeNull();
    expect(parseVietnameseDate('07/24/2026')).toBeNull();
  });

  it('masks digits and builds the backend datetime value', () => {
    expect(maskVietnameseDate('24072026')).toBe('24/07/2026');
    expect(combineLocalDateTime('2026-07-24', '09:30')).toBe('2026-07-24T09:30:00');
  });
});
