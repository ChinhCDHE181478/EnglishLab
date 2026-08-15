export function isYouTubeVideoUrl(value) {
  const text = String(value || '').trim();
  if (!text) return false;
  return /(?:youtube\.com|youtu\.be|youtube-nocookie\.com)/i.test(text);
}
