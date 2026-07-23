import { looksLikeRichTextHtml, sanitizeLessonHtml, stripRichTextToPlain } from '../../utils/lessonRichText';

/**
 * Render stored description HTML safely. Plain text falls back to a paragraph.
 */
export default function RichTextHtml({ value = '', className = '', asPlain = false }) {
  const raw = String(value || '').trim();
  if (!raw) return null;

  if (asPlain || !looksLikeRichTextHtml(raw)) {
    const text = asPlain ? stripRichTextToPlain(raw) : raw;
    if (!text) return null;
    return <div className={className}>{text}</div>;
  }

  return (
    <div
      className={`rich-text-html [&_a]:font-semibold [&_a]:text-[#730014] [&_a]:underline [&_blockquote]:my-3 [&_blockquote]:border-l-4 [&_blockquote]:border-[#dfbfbd] [&_blockquote]:bg-[#fffafb] [&_blockquote]:px-4 [&_blockquote]:py-2 [&_h2]:mb-2 [&_h2]:mt-4 [&_h2]:text-xl [&_h2]:font-extrabold [&_h3]:mb-2 [&_h3]:mt-3 [&_h3]:text-lg [&_h3]:font-extrabold [&_ol]:list-decimal [&_ol]:pl-6 [&_p]:my-2 [&_ul]:list-disc [&_ul]:pl-6 ${className}`}
      dangerouslySetInnerHTML={{ __html: sanitizeLessonHtml(raw) }}
    />
  );
}
