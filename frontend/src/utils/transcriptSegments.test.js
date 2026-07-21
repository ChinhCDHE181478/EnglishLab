import { describe, expect, it } from 'vitest';
import { normalizeTranscriptTimeline } from './transcriptSegments';

describe('normalizeTranscriptTimeline', () => {
  it('clips rolling YouTube captions at the next start time', () => {
    expect(normalizeTranscriptTimeline([
      { startSeconds: 0.16, endSeconds: 5.2, text: 'First caption' },
      { startSeconds: 2.64, endSeconds: 7.52, text: 'Second caption' },
      { startSeconds: 5.2, endSeconds: 7.52, text: 'Third caption' },
    ])).toEqual([
      { startSeconds: 0.16, endSeconds: 2.64, text: 'First caption' },
      { startSeconds: 2.64, endSeconds: 5.2, text: 'Second caption' },
      { startSeconds: 5.2, endSeconds: 7.52, text: 'Third caption' },
    ]);
  });

  it('sorts segments and keeps the more complete caption at a duplicate start', () => {
    expect(normalizeTranscriptTimeline([
      { startSeconds: 4, endSeconds: 7, text: 'Later' },
      { startSeconds: 1, endSeconds: 5, text: 'Short' },
      { startSeconds: 1, endSeconds: 6, text: 'More complete caption' },
    ])).toEqual([
      { startSeconds: 1, endSeconds: 4, text: 'More complete caption' },
      { startSeconds: 4, endSeconds: 7, text: 'Later' },
    ]);
  });
});
