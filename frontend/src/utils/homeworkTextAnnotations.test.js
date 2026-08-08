import { describe, expect, it } from 'vitest';
import {
  buildAnnotatedTextSegments,
  normalizeHomeworkAnnotations,
  overlapsExistingAnnotation,
} from './homeworkTextAnnotations';

describe('homework text annotations', () => {
  const text = 'I go to school yesterday.';
  const correction = {
    id: 'a-1',
    type: 'CORRECTION',
    startOffset: 2,
    endOffset: 4,
    selectedText: 'go',
    replacementText: 'went',
  };

  it('splits the original answer without replacing learner text', () => {
    const segments = buildAnnotatedTextSegments(text, [correction]);
    expect(segments.map((item) => item.text).join('')).toBe(text);
    expect(segments[1].annotation.replacementText).toBe('went');
  });

  it('drops stale ranges whose selected text no longer matches', () => {
    expect(normalizeHomeworkAnnotations([{ ...correction, selectedText: 'do' }], text)).toEqual([]);
  });

  it('detects overlapping feedback while allowing adjacent ranges', () => {
    expect(overlapsExistingAnnotation({ startOffset: 3, endOffset: 8 }, [correction])).toBe(true);
    expect(overlapsExistingAnnotation({ startOffset: 4, endOffset: 8 }, [correction])).toBe(false);
  });
});
