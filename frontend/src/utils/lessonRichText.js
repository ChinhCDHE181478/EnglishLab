const ALLOWED_TAGS = new Set([
  'A', 'B', 'BLOCKQUOTE', 'BR', 'CODE', 'DIV', 'EM', 'H1', 'H2', 'H3', 'HR',
  'I', 'LI', 'OL', 'P', 'PRE', 'S', 'STRONG', 'U', 'UL', 'SPAN',
]);

const ALLOWED_TEXT_ALIGNMENTS = new Set(['left', 'center', 'right', 'justify']);

const hasSafeHref = (value = '') => {
  const href = String(value).trim();
  return /^(https?:|mailto:|tel:|\/|#)/i.test(href);
};

export const looksLikeRichTextHtml = (value = '') => /<\/?(?:h[1-3]|p|div|strong|em|u|s|ul|ol|li|blockquote|pre|a|br|hr)\b/i.test(String(value));

export const sanitizeLessonHtml = (value = '') => {
  if (typeof window === 'undefined' || typeof window.DOMParser === 'undefined') return '';
  const documentNode = new window.DOMParser().parseFromString(String(value), 'text/html');

  const cleanNode = (node) => {
    [...node.children].forEach((child) => {
      if (!ALLOWED_TAGS.has(child.tagName)) {
        child.replaceWith(...child.childNodes);
        return;
      }

      const originalHref = child.getAttribute('href');
      const sourceStyle = String(child.getAttribute('style') || '');
      [...child.attributes].forEach((attribute) => child.removeAttribute(attribute.name));
      const alignment = sourceStyle.match(/text-align\s*:\s*(left|center|right|justify)/i)?.[1]?.toLowerCase();
      if (alignment && ALLOWED_TEXT_ALIGNMENTS.has(alignment)) child.style.textAlign = alignment;

      if (child.tagName === 'A') {
        if (originalHref && hasSafeHref(originalHref)) {
          child.setAttribute('href', originalHref);
          child.setAttribute('target', '_blank');
          child.setAttribute('rel', 'noopener noreferrer');
        }
      }
      cleanNode(child);
    });
  };

  cleanNode(documentNode.body);
  return documentNode.body.innerHTML;
};
