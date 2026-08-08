import createDOMPurify from 'dompurify';

const ALLOWED_TAGS = [
  'a', 'b', 'blockquote', 'br', 'code', 'div', 'em', 'h1', 'h2', 'h3', 'hr',
  'i', 'li', 'ol', 'p', 'pre', 's', 'strong', 'u', 'ul', 'span',
];

const ALLOWED_TEXT_ALIGNMENTS = new Set(['left', 'center', 'right', 'justify']);

const hasSafeHref = (value = '') => {
  const href = String(value).trim();
  return /^(https?:|mailto:|tel:|\/|#)/i.test(href);
};

export const looksLikeRichTextHtml = (value = '') => /<\/?(?:h[1-3]|p|div|strong|em|u|s|ul|ol|li|blockquote|pre|a|br|hr)\b/i.test(String(value));

/** Strip HTML tags for card/list previews that expect plain text. */
export const stripRichTextToPlain = (value = '') => String(value || '')
  .replace(/<br\s*\/?>/gi, '\n')
  .replace(/<\/(p|div|h[1-6]|li|blockquote)>/gi, '\n')
  .replace(/<[^>]+>/g, ' ')
  .replace(/&nbsp;/gi, ' ')
  .replace(/&amp;/gi, '&')
  .replace(/&lt;/gi, '<')
  .replace(/&gt;/gi, '>')
  .replace(/&quot;/gi, '"')
  .replace(/\s+\n/g, '\n')
  .replace(/\n{3,}/g, '\n\n')
  .replace(/[ \t]{2,}/g, ' ')
  .trim();

export const sanitizeLessonHtml = (value = '') => {
  if (typeof window === 'undefined' || typeof window.DOMParser === 'undefined') return '';
  const purifier = typeof createDOMPurify?.sanitize === 'function'
    ? createDOMPurify
    : createDOMPurify(window);
  const sanitized = purifier.sanitize(String(value), {
    ALLOWED_ATTR: ['href', 'style'],
    ALLOWED_TAGS,
    ALLOW_DATA_ATTR: false,
    ALLOW_UNKNOWN_PROTOCOLS: false,
  });
  const documentNode = new window.DOMParser().parseFromString(String(sanitized), 'text/html');

  [...documentNode.body.querySelectorAll('*')].forEach((element) => {
    const originalHref = element.getAttribute('href');
    const sourceStyle = String(element.getAttribute('style') || '');
    [...element.attributes].forEach((attribute) => element.removeAttribute(attribute.name));

    const alignment = sourceStyle.match(/text-align\s*:\s*(left|center|right|justify)/i)?.[1]?.toLowerCase();
    if (alignment && ALLOWED_TEXT_ALIGNMENTS.has(alignment)) element.style.textAlign = alignment;

    if (element.tagName === 'A' && originalHref && hasSafeHref(originalHref)) {
      element.setAttribute('href', originalHref);
      element.setAttribute('target', '_blank');
      element.setAttribute('rel', 'noopener noreferrer');
    }
  });

  return documentNode.body.innerHTML;
};
