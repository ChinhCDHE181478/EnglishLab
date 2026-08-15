import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { sanitizeLessonHtml } from '../../utils/lessonRichText';

const getVideoEmbedUrl = (url) => {
  if (!url) return '';
  const value = String(url).trim();
  if (/iframe\.mediadelivery\.net\/embed\//i.test(value)) return value;
  const bunnyPlayMatch = value.match(/player\.mediadelivery\.net\/play\/(\d+)\/([0-9a-f-]+)/i);
  if (bunnyPlayMatch) {
    return `https://iframe.mediadelivery.net/embed/${bunnyPlayMatch[1]}/${bunnyPlayMatch[2]}`;
  }
  const youtubeMatch = value.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([^&?/]+)/);
  return youtubeMatch?.[1] ? `https://www.youtube.com/embed/${youtubeMatch[1]}` : '';
};

const renderInlineMarkdown = (text = '') => {
  const parts = String(text).split(/(\*\*[^*]+\*\*)/g).filter(Boolean);
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={`${part}-${index}`} className="font-bold text-[#1f2430]">{part.slice(2, -2)}</strong>;
    }
    return <span key={`${part}-${index}`}>{part.replace(/\*\*/g, '')}</span>;
  });
};

const renderLine = (line, key) => {
  if (!line.trim()) return <div key={key} className="h-3" />;
  const boldHeadingMatch = line.match(/^\*\*([^*]+)\*\*:?$/);
  if (boldHeadingMatch) return <h4 key={key} className="mt-5 text-[15px] font-bold text-[#1f2430]">{boldHeadingMatch[1]}</h4>;
  if (line.startsWith('### ')) return <h4 key={key} className="mt-4 text-[13px] font-bold uppercase tracking-wide text-[#5f5353]">{renderInlineMarkdown(line.slice(4))}</h4>;
  if (line.startsWith('## ')) return <h3 key={key} className="mt-5 text-base font-bold text-[#1f2430]">{renderInlineMarkdown(line.slice(3))}</h3>;
  if (line.startsWith('# ')) return <h2 key={key} className="text-lg font-bold text-[#1f2430]">{renderInlineMarkdown(line.slice(2))}</h2>;
  if (/^\d+\.\s+/.test(line)) return <p key={key} className="pl-2 font-medium text-[#3f3030]"><span className="font-bold text-[#4b0009]">{line.match(/^\d+/)?.[0]}. </span>{renderInlineMarkdown(line.replace(/^\d+\.\s+/, ''))}</p>;
  if (line.startsWith('- ')) return <p key={key} className="relative pl-5 text-[#3f3030]"><span className="absolute left-2 top-[0.7em] h-1.5 w-1.5 rounded-full bg-[#4b0009]" />{renderInlineMarkdown(line.slice(2))}</p>;
  return <p key={key} className="text-[#3f3030]">{renderInlineMarkdown(line)}</p>;
};

const LESSON_HTML_CLASSES = [
  'mt-5 select-text rounded-[24px] border border-[#ead9db] bg-[#fffdfc] p-5 text-sm leading-7 text-[#3f3030]',
  'selection:bg-[#fff0f1] selection:text-[#4b0009]',
  /* links */
  '[&_a]:font-semibold [&_a]:text-[#730014] [&_a]:underline',
  /* headings — bold sentence-case, not uppercase */
  '[&_h1]:mb-3 [&_h1]:text-xl [&_h1]:font-bold [&_h1]:text-[#1f2430]',
  '[&_h2]:mb-2 [&_h2]:mt-6 [&_h2]:text-base [&_h2]:font-bold [&_h2]:text-[#1f2430]',
  '[&_h3]:mb-1.5 [&_h3]:mt-5 [&_h3]:text-sm [&_h3]:font-bold [&_h3]:uppercase [&_h3]:tracking-wide [&_h3]:text-[#5f5353]',
  '[&_h4]:mb-1 [&_h4]:mt-4 [&_h4]:text-sm [&_h4]:font-bold [&_h4]:text-[#1f2430]',
  /* paragraphs, lists — avoid flex on <li> so learners can highlight exercise text */
  '[&_p]:my-1.5',
  '[&_ul]:my-3 [&_ul]:list-none [&_ul]:space-y-1 [&_ul]:pl-4',
  '[&_ul>li]:relative [&_ul>li]:pl-3',
  '[&_ul>li]:before:absolute [&_ul>li]:before:left-0 [&_ul>li]:before:top-[0.7em] [&_ul>li]:before:h-1.5 [&_ul>li]:before:w-1.5 [&_ul>li]:before:rounded-full [&_ul>li]:before:bg-[#4b0009] [&_ul>li]:before:content-[""]',
  '[&_ol]:my-3 [&_ol]:list-decimal [&_ol]:pl-6 [&_ol>li]:pl-1 [&_ol>li]:marker:font-bold [&_ol>li]:marker:text-[#4b0009]',
  /* blockquote */
  '[&_blockquote]:my-4 [&_blockquote]:border-l-4 [&_blockquote]:border-[#dfbfbd] [&_blockquote]:bg-[#fff7f7] [&_blockquote]:px-4 [&_blockquote]:py-2 [&_blockquote]:italic [&_blockquote]:text-[#584140]',
  /* strong / b inside rich text */
  '[&_strong]:font-bold [&_strong]:text-[#1f2430] [&_b]:font-bold [&_b]:text-[#1f2430]',
  /* code/pre */
  '[&_pre]:overflow-x-auto [&_pre]:rounded-xl [&_pre]:bg-slate-900 [&_pre]:p-4 [&_pre]:text-white',
].join(' ');

/** Convert a subset of Markdown to HTML so both storage formats render correctly. */
const markdownToHtml = (text = '') => {
  const lines = String(text).split('\n');
  const out = [];
  let inUl = false;
  let inOl = false;

  const flushList = () => {
    if (inUl) { out.push('</ul>'); inUl = false; }
    if (inOl) { out.push('</ol>'); inOl = false; }
  };

  const inlineMarkdown = (s) =>
    s
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/\*([^*]+)\*/g, '<em>$1</em>');

  for (const raw of lines) {
    const line = raw.trimEnd();
    if (!line.trim()) {
      flushList();
      out.push('<br>');
      continue;
    }
    if (line.startsWith('# '))  { flushList(); out.push(`<h1>${inlineMarkdown(line.slice(2))}</h1>`); continue; }
    if (line.startsWith('## ')) { flushList(); out.push(`<h2>${inlineMarkdown(line.slice(3))}</h2>`); continue; }
    if (line.startsWith('### ')) { flushList(); out.push(`<h3>${inlineMarkdown(line.slice(4))}</h3>`); continue; }
    if (line.startsWith('#### ')) { flushList(); out.push(`<h4>${inlineMarkdown(line.slice(5))}</h4>`); continue; }

    const olMatch = line.match(/^(\d+)\.\s+(.*)/);
    if (olMatch) {
      if (!inOl) { if (inUl) { out.push('</ul>'); inUl = false; } out.push('<ol>'); inOl = true; }
      out.push(`<li>${inlineMarkdown(olMatch[2])}</li>`);
      continue;
    }

    if (line.startsWith('- ') || line.startsWith('* ')) {
      if (!inUl) { if (inOl) { out.push('</ol>'); inOl = false; } out.push('<ul>'); inUl = true; }
      out.push(`<li>${inlineMarkdown(line.slice(2))}</li>`);
      continue;
    }

    flushList();
    const boldHeading = line.match(/^\*\*([^*]+)\*\*:?\s*$/);
    if (boldHeading) { out.push(`<h4>${boldHeading[1]}</h4>`); continue; }
    out.push(`<p>${inlineMarkdown(line)}</p>`);
  }
  flushList();
  return out.join('\n');
};

/** True when content is stored as rich-text HTML (from the Quill/Tiptap editor). */
const isRichTextHtml = (value = '') => /\s*<\/?(?:h[1-6]|p|div|strong|em|u|s|ul|ol|li|blockquote|pre|a|br|hr)\b/i.test(String(value));

const LessonContent = ({ content }) => {
  if (!content) return null;

  // Convert to HTML regardless of source format so markdown is always rendered.
  const html = isRichTextHtml(content)
    ? sanitizeLessonHtml(content)
    : sanitizeLessonHtml(markdownToHtml(content));

  return (
    <div
      id="khu-vuc-noi-dung-bai-hoc"
      className={LESSON_HTML_CLASSES}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
};

const WorkspaceLessonPanel = ({
  activeLessonItem,
  completedLessonIds,
  lessonItems,
  savingLessonId,
  canPersist = false,
  onMoveLesson,
  onToggleComplete,
  onSaveLessonNote,
  onOpenNotes,
  seekRequest,
}) => {
  const directVideoRef = useRef(null);
  const lessonContentRef = useRef(null);
  const activeLesson = activeLessonItem?.lesson;
  const activeModule = activeLessonItem?.module;
  const activeLessonId = activeLessonItem?.id;
  const lessonContent = activeLesson?.contentText || activeLesson?.description;
  const activeIndex = lessonItems.findIndex((item) => String(item.id) === String(activeLessonId));
  const embedUrl = getVideoEmbedUrl(activeLesson?.bunnyCdnUrl || activeLesson?.videoUrl);
  const sourceUrl = activeLesson?.bunnyCdnUrl || activeLesson?.videoUrl;
  const directVideoUrl = sourceUrl && !embedUrl ? sourceUrl : '';
  const [iframeStartSeconds, setIframeStartSeconds] = useState(0);
  const hasMaterial = Boolean(activeLesson?.materialUrl);
  const isCompleted = activeLessonId ? completedLessonIds.has(activeLessonId) : false;
  const isSaving = activeLessonId && String(savingLessonId) === String(activeLessonId);
  const nextLessonItem = lessonItems[activeIndex + 1];
  const nextLessonLocked = Boolean(nextLessonItem?.isLocked);
  const [selectedLessonText, setSelectedLessonText] = useState('');
  const [selectionButton, setSelectionButton] = useState(null);
  const [lessonNoteMessage, setLessonNoteMessage] = useState('');
  const [savingSelectedNote, setSavingSelectedNote] = useState(false);
  const iframeSrc = useMemo(() => {
    if (!embedUrl) return '';
    if (!iframeStartSeconds) return embedUrl;
    const separator = embedUrl.includes('?') ? '&' : '?';
    return `${embedUrl}${separator}start=${Math.floor(iframeStartSeconds)}&autoplay=1`;
  }, [embedUrl, iframeStartSeconds]);

  useEffect(() => {
    if (!seekRequest) return;
    const seconds = Number(seekRequest.seconds || 0);
    if (directVideoRef.current) {
      directVideoRef.current.currentTime = seconds;
      directVideoRef.current.play?.().catch(() => {});
      return;
    }
    if (embedUrl) setIframeStartSeconds(seconds);
  }, [embedUrl, seekRequest]);

  const clearLessonSelectionUi = useCallback(() => {
    setSelectedLessonText('');
    setSelectionButton(null);
  }, []);

  useEffect(() => {
    clearLessonSelectionUi();
    setLessonNoteMessage('');
    window.getSelection?.()?.removeAllRanges?.();
  }, [activeLessonId, clearLessonSelectionUi]);

  const captureLessonSelection = useCallback((event) => {
    const selection = window.getSelection?.();
    const text = selection?.toString().trim();
    const range = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
    const container = lessonContentRef.current;
    const ancestor = range?.commonAncestorContainer;
    const ancestorElement = ancestor?.nodeType === Node.TEXT_NODE ? ancestor.parentElement : ancestor;

    if (!text || !range || !container || !ancestorElement || !container.contains(ancestorElement)) {
      // React re-render after a successful capture often collapses the native
      // selection before this follow-up read. Only dismiss when the user
      // actually clicked outside the lesson content.
      const target = event?.target;
      if (container && target instanceof Node && !container.contains(target)) {
        clearLessonSelectionUi();
      }
      return;
    }

    const rect = range.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();
    const preferAbove = rect.bottom > containerRect.bottom - 70;
    const top = preferAbove
      ? rect.top - containerRect.top + container.scrollTop - 44
      : rect.bottom - containerRect.top + container.scrollTop + 8;
    const left = Math.min(
      Math.max(12, rect.left - containerRect.left + container.scrollLeft),
      Math.max(12, container.clientWidth - 150),
    );

    setSelectedLessonText(text);
    setSelectionButton({ top: Math.max(8, top), left });
    setLessonNoteMessage('');
  }, [clearLessonSelectionUi]);

  useEffect(() => {
    const handleMouseUp = (event) => {
      if (event.target?.closest?.('[data-lesson-note-save]')) return;
      window.requestAnimationFrame(() => captureLessonSelection(event));
    };
    document.addEventListener('mouseup', handleMouseUp);
    return () => document.removeEventListener('mouseup', handleMouseUp);
  }, [captureLessonSelection]);

  const saveSelectedLessonText = async () => {
    const noteText = selectedLessonText.trim();
    if (!canPersist) {
      setLessonNoteMessage('Bạn cần đăng nhập để lưu ghi chú.');
      return;
    }
    if (!noteText) {
      setLessonNoteMessage('Vui lòng bôi đen một đoạn trong bài học trước khi lưu.');
      return;
    }

    if (!onSaveLessonNote) {
      setLessonNoteMessage('Không thể lưu ghi chú. Vui lòng tải lại trang và thử lại.');
      return;
    }

    setSavingSelectedNote(true);
    try {
      const savedNote = await onSaveLessonNote({
        content: noteText,
        selectedText: noteText,
      });
      if (!savedNote) {
        setLessonNoteMessage('Không thể lưu ghi chú. Vui lòng thử lại.');
        return;
      }
      clearLessonSelectionUi();
      window.getSelection?.()?.removeAllRanges?.();
      setLessonNoteMessage('Đã lưu đoạn đã chọn vào ghi chú.');
      onOpenNotes?.();
    } finally {
      setSavingSelectedNote(false);
    }
  };

  return (
    <section className="space-y-5">
      {embedUrl || directVideoUrl ? (
        <div className="rounded-[8px] bg-[#eef2f7] p-4 md:p-5">
          {embedUrl ? (
            <div className="overflow-hidden rounded-[26px] bg-black">
              <div className="aspect-video">
                <iframe
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  allowFullScreen
                  className="h-full w-full"
                  src={iframeSrc}
                  title={activeLesson?.title || 'Video bài học'}
                />
              </div>
            </div>
          ) : null}

          {directVideoUrl ? (
            <div className="overflow-hidden rounded-[26px] bg-black">
              <video ref={directVideoRef} className="aspect-video h-full w-full" controls preload="metadata" src={directVideoUrl}>
                <track kind="captions" />
              </video>
            </div>
          ) : null}
        </div>
      ) : null}

      <div className="rounded-[8px] border border-[#e0e6ef] bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div className="min-w-0">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8c716f]">
              {activeModule?.title || 'Bài học'}
            </p>
            <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold leading-tight text-[#1f2430]">
              {activeLesson?.title || 'Bài học hiện tại'}
            </h2>
            <p className="mt-3 text-sm leading-7 text-[#5f5353]">
              {activeLesson?.description || 'Nội dung bài học gồm video, phần giải thích và các bước tự luyện tập theo tiến độ của bạn.'}
            </p>
          </div>

          <div className="grid min-w-[220px] grid-cols-2 gap-3 rounded-[8px] bg-[#f4f7fb] p-4">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Loại bài</p>
              <p className="mt-1 text-sm font-extrabold text-[#4b0009]">
                {activeLesson?.videoUrl ? 'Video' : activeLesson?.materialUrl ? 'Tài liệu' : 'Bài học'}
              </p>
            </div>
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Thời lượng</p>
              <p className="mt-1 text-sm font-extrabold text-[#4b0009]">{activeLesson?.durationMinutes || 0} phút</p>
            </div>
          </div>
        </div>

        <div ref={lessonContentRef} className="relative select-text">
          <LessonContent content={lessonContent} />
          {selectionButton ? (
            <button
              className="absolute z-10 rounded-[8px] bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white shadow-[0_12px_24px_rgba(75,0,9,0.22)] transition hover:bg-[#730014] disabled:cursor-wait disabled:opacity-65"
              data-lesson-note-save="true"
              style={{ top: selectionButton.top, left: selectionButton.left }}
              type="button"
              disabled={savingSelectedNote}
              onMouseDown={(event) => event.preventDefault()}
              onClick={saveSelectedLessonText}
            >
              {savingSelectedNote ? 'Đang lưu...' : 'Lưu ghi chú'}
            </button>
          ) : null}
        </div>

        {selectedLessonText ? (
          <div className="mt-3 flex flex-col gap-3 rounded-[8px] border border-[#dcb6bb] bg-[#fff8f8] p-3 sm:flex-row sm:items-center sm:justify-between">
            <p className="min-w-0 flex-1 text-sm leading-6 text-[#584140]">
              <span className="font-bold text-[#4b0009]">Đã chọn: </span>
              <span className="line-clamp-2">“{selectedLessonText}”</span>
            </p>
            <div className="flex shrink-0 flex-wrap gap-2">
              <button
                className="rounded-[8px] border border-[#dcb6bb] bg-white px-3 py-2 text-xs font-extrabold text-[#8a0018] transition hover:bg-[#fff0f1]"
                type="button"
                onClick={clearLessonSelectionUi}
              >
                Hủy
              </button>
              <button
                className="rounded-[8px] bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white transition hover:bg-[#730014] disabled:cursor-wait disabled:opacity-65"
                data-lesson-note-save="true"
                type="button"
                disabled={savingSelectedNote}
                onMouseDown={(event) => event.preventDefault()}
                onClick={saveSelectedLessonText}
              >
                {savingSelectedNote ? 'Đang lưu...' : 'Lưu ghi chú'}
              </button>
            </div>
          </div>
        ) : null}

        {lessonNoteMessage ? (
          <p className="mt-3 text-sm font-semibold text-[#730014]">{lessonNoteMessage}</p>
        ) : null}

        {hasMaterial ? (
          <div id="khu-vuc-tai-lieu" className="mt-5">
            <a
              className="inline-flex items-center gap-2 rounded-2xl border border-[#dcb6bb] bg-[#fff8f8] px-4 py-3 text-sm font-extrabold text-[#8a0018] transition hover:bg-[#fff0f1]"
              href={activeLesson.materialUrl}
              rel="noreferrer"
              target="_blank"
            >
              <span className="material-symbols-outlined text-[18px]">description</span>
              Mở tài liệu bài học
            </a>
          </div>
        ) : null}

        <div className="mt-6 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex flex-wrap gap-3">
            <button
              className="rounded-[8px] border border-[#dcb6bb] bg-white px-4 py-3 text-sm font-extrabold text-[#8a0018] transition hover:bg-[#fff0f1] disabled:cursor-not-allowed disabled:opacity-40"
              type="button"
              disabled={activeIndex <= 0}
              onClick={() => onMoveLesson(-1)}
            >
              Bài trước
            </button>
            <button
              className="rounded-[8px] border border-[#dfbfbd] bg-white px-4 py-3 text-sm font-extrabold text-[#4b0009] transition hover:border-[#730014] hover:bg-[#fff0f1] disabled:cursor-not-allowed disabled:opacity-40"
              type="button"
              disabled={activeIndex < 0 || activeIndex >= lessonItems.length - 1 || nextLessonLocked}
              onClick={() => onMoveLesson(1)}
            >
              {nextLessonLocked ? 'Hoàn thành bài hiện tại để mở bài tiếp theo' : 'Chuyển đến mục tiếp theo'}
            </button>
          </div>

          <button
            className={`inline-flex items-center justify-center gap-2 rounded-[8px] px-5 py-3 text-sm font-extrabold transition disabled:cursor-wait disabled:opacity-70 ${
              isCompleted ? 'bg-[#fff0f1] text-[#4b0009]' : 'bg-[#4b0009] text-white hover:bg-[#730014]'
            }`}
            type="button"
            disabled={isSaving}
            onClick={() => activeLessonId && onToggleComplete(activeLessonId)}
          >
            <span className="material-symbols-outlined text-[18px]">
              {isCompleted ? 'check_circle' : 'done'}
            </span>
            {isSaving ? 'Đang lưu tiến độ...' : isCompleted ? 'Đã hoàn thành' : 'Đánh dấu hoàn thành'}
          </button>
        </div>
      </div>
    </section>
  );
};

export default WorkspaceLessonPanel;
