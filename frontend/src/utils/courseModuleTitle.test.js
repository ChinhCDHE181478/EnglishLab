import { describe, expect, it } from 'vitest';
import { formatModuleTitle, stripModuleOrdinal } from './courseModuleTitle';

describe('courseModuleTitle', () => {
  it('removes legacy English and Vietnamese numbering prefixes', () => {
    expect(stripModuleOrdinal('Module 12: IELTS Listening')).toBe('IELTS Listening');
    expect(stripModuleOrdinal('Mô-đun 3 - IELTS Reading')).toBe('IELTS Reading');
    expect(stripModuleOrdinal('Mô đun 4 — IELTS Writing')).toBe('IELTS Writing');
  });

  it('renders numbering from the current array position', () => {
    expect(formatModuleTitle('Module 8: IELTS Speaking', 0)).toBe('Module 1: IELTS Speaking');
    expect(formatModuleTitle('IELTS Speaking', 3)).toBe('Module 4: IELTS Speaking');
    expect(formatModuleTitle('', 1)).toBe('Module 2');
  });
});
