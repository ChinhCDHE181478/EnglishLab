export function isYouTubeVideoUrl(value) {
  const text = String(value || '').trim();
  if (!text) return false;
  return /(?:youtube\.com|youtu\.be|youtube-nocookie\.com)/i.test(text);
}

export function isBunnyVideoUrl(value) {
  const text = String(value || '').trim();
  if (!text) return false;
  return /(?:mediadelivery\.net|b-cdn\.net|bunnycdn\.com)/i.test(text)
    || /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(text);
}

export function canAutoFetchTranscript({ videoUrl, bunnyVideoId } = {}) {
  return Boolean(bunnyVideoId) || isYouTubeVideoUrl(videoUrl) || isBunnyVideoUrl(videoUrl);
}
