import { describe, expect, it } from 'vitest';
import { isYouTubeVideoUrl } from './youtubeVideoUrl';

describe('isYouTubeVideoUrl', () => {
  it('detects common YouTube link formats', () => {
    expect(isYouTubeVideoUrl('https://www.youtube.com/watch?v=dQw4w9WgXcQ')).toBe(true);
    expect(isYouTubeVideoUrl('https://youtu.be/dQw4w9WgXcQ')).toBe(true);
    expect(isYouTubeVideoUrl('https://www.youtube.com/embed/dQw4w9WgXcQ')).toBe(true);
    expect(isYouTubeVideoUrl('https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ')).toBe(true);
  });

  it('rejects bunny and empty urls', () => {
    expect(isYouTubeVideoUrl('')).toBe(false);
    expect(isYouTubeVideoUrl('https://iframe.mediadelivery.net/embed/1/abc')).toBe(false);
    expect(isYouTubeVideoUrl('https://vimeo.com/123')).toBe(false);
  });
});
