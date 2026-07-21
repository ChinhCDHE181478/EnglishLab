const EPSILON = 0.000001;

/**
 * Converts rolling-caption timestamps (commonly returned by YouTube) into a
 * deterministic, non-overlapping timeline suitable for the lesson player.
 */
export function normalizeTranscriptTimeline(segments = []) {
  const sorted = (Array.isArray(segments) ? segments : [])
    .map((segment) => ({
      startSeconds: Number(segment?.startSeconds),
      endSeconds: Number(segment?.endSeconds),
      text: String(segment?.text || '').trim(),
    }))
    .filter((segment) => (
      segment.text
      && Number.isFinite(segment.startSeconds)
      && Number.isFinite(segment.endSeconds)
      && segment.startSeconds >= 0
      && segment.endSeconds > segment.startSeconds
    ))
    .sort((left, right) => left.startSeconds - right.startSeconds || left.endSeconds - right.endSeconds);

  const normalized = [];
  sorted.forEach((segment) => {
    const previous = normalized.at(-1);
    if (!previous) {
      normalized.push({ ...segment });
      return;
    }

    if (Math.abs(segment.startSeconds - previous.startSeconds) <= EPSILON) {
      previous.endSeconds = Math.max(previous.endSeconds, segment.endSeconds);
      if (segment.text.length > previous.text.length) previous.text = segment.text;
      return;
    }

    if (segment.startSeconds < previous.endSeconds) {
      previous.endSeconds = segment.startSeconds;
    }
    normalized.push({ ...segment });
  });

  return normalized;
}
