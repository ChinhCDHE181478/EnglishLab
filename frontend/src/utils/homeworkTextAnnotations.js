export const HOMEWORK_ANNOTATION_TYPES = {
  CORRECTION: 'CORRECTION',
  NOTE: 'NOTE',
};

export function normalizeHomeworkAnnotations(annotations = [], text = '') {
  if (!Array.isArray(annotations)) return [];
  return annotations
    .filter((item) => {
      const start = Number(item?.startOffset);
      const end = Number(item?.endOffset);
      return item?.id
        && Object.values(HOMEWORK_ANNOTATION_TYPES).includes(item.type)
        && Number.isInteger(start)
        && Number.isInteger(end)
        && start >= 0
        && end > start
        && end <= text.length
        && text.slice(start, end) === item.selectedText;
    })
    .map((item) => ({
      ...item,
      startOffset: Number(item.startOffset),
      endOffset: Number(item.endOffset),
      replacementText: item.replacementText?.trim() || '',
      note: item.note?.trim() || '',
    }))
    .sort((left, right) => left.startOffset - right.startOffset);
}

export function buildAnnotatedTextSegments(text = '', annotations = []) {
  const normalized = normalizeHomeworkAnnotations(annotations, text);
  const segments = [];
  let cursor = 0;
  normalized.forEach((annotation) => {
    if (annotation.startOffset < cursor) return;
    if (annotation.startOffset > cursor) {
      segments.push({ text: text.slice(cursor, annotation.startOffset), annotation: null });
    }
    segments.push({
      text: text.slice(annotation.startOffset, annotation.endOffset),
      annotation,
    });
    cursor = annotation.endOffset;
  });
  if (cursor < text.length) segments.push({ text: text.slice(cursor), annotation: null });
  return segments;
}

export function selectionToTextRange(container, selection = window.getSelection()) {
  if (!container || !selection || selection.rangeCount !== 1 || selection.isCollapsed) return null;
  const range = selection.getRangeAt(0);
  if (!container.contains(range.commonAncestorContainer)) return null;

  const beforeStart = range.cloneRange();
  beforeStart.selectNodeContents(container);
  beforeStart.setEnd(range.startContainer, range.startOffset);
  const rawStart = beforeStart.toString().length;
  const rawText = range.toString();
  const leadingWhitespace = rawText.match(/^\s*/)?.[0].length || 0;
  const trailingWhitespace = rawText.match(/\s*$/)?.[0].length || 0;
  const selectedText = rawText.slice(leadingWhitespace, rawText.length - trailingWhitespace);
  if (!selectedText) return null;
  return {
    startOffset: rawStart + leadingWhitespace,
    endOffset: rawStart + rawText.length - trailingWhitespace,
    selectedText,
  };
}

export function overlapsExistingAnnotation(candidate, annotations = [], ignoredId = null) {
  return annotations.some((item) => item.id !== ignoredId
    && candidate.startOffset < item.endOffset
    && candidate.endOffset > item.startOffset);
}
