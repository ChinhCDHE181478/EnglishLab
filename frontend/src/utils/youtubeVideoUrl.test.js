import { describe, expect, it } from 'vitest';
import { canAutoFetchTranscript, isBunnyVideoUrl, isYouTubeVideoUrl } from './youtubeVideoUrl';

describe('youtubeVideoUrl helpers', () => {
  it('detects YouTube urls', () => {
    expect(isYouTubeVideoUrl('https://www.youtube.com/watch?v=abc123')).toBe(true);
    expect(isYouTubeVideoUrl('https://youtu.be/abc123')).toBe(true);
    expect(isYouTubeVideoUrl('https://example.com')).toBe(false);
  });

  it('detects Bunny urls and ids', () => {
    expect(isBunnyVideoUrl('https://iframe.mediadelivery.net/embed/729032/bc1feea2-7cc5-46b5-9073-aaaaaaaaaaaa')).toBe(true);
    expect(isBunnyVideoUrl('bc1feea2-7cc5-46b5-9073-aaaaaaaaaaaa')).toBe(true);
    expect(isBunnyVideoUrl('https://www.youtube.com/watch?v=abc')).toBe(false);
  });

  it('allows auto fetch for YouTube or Bunny sources', () => {
    expect(canAutoFetchTranscript({ videoUrl: 'https://youtu.be/abc' })).toBe(true);
    expect(canAutoFetchTranscript({
      videoUrl: 'https://iframe.mediadelivery.net/embed/1/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
    })).toBe(true);
    expect(canAutoFetchTranscript({ videoUrl: 'https://iframe.mediadelivery.net/embed/1/guid' })).toBe(true);
    expect(canAutoFetchTranscript({ videoUrl: 'https://vimeo.com/1' })).toBe(false);
  });
});
