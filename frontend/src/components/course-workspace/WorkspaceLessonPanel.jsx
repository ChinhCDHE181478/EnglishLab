import { useEffect, useMemo, useRef, useState } from 'react';

const getVideoEmbedUrl = (url) => {
  if (!url) return '';
  const value = String(url).trim();
  if (/iframe\.mediadelivery\.net\/embed\//i.test(value)) return value;

  const youtubeMatch = value.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([^&?/]+)/);
  return youtubeMatch?.[1] ? `https://www.youtube.com/embed/${youtubeMatch[1]}` : '';
};

const renderInlineMarkdown = (text = '') => {
  const parts = String(text).split(/(\*\*[^*]+\*\*)/g).filter(Boolean);
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={`${part}-${index}`}>{part.slice(2, -2)}</strong>;
    }
    return <span key={`${part}-${index}`}>{part.replace(/\*\*/g, '')}</span>;
  });
};

const renderLine = (line, key) => {
  if (!line.trim()) return <div key={key} className="h-3" />;
  if (line.startsWith('### ')) return <h4 key={key} className="mt-4 text-base font-extrabold text-[#2b2828]">{renderInlineMarkdown(line.slice(4))}</h4>;
  if (line.startsWith('## ')) return <h3 key={key} className="mt-5 text-lg font-extrabold text-[#2b2828]">{renderInlineMarkdown(line.slice(3))}</h3>;
  if (line.startsWith('# ')) return <h2 key={key} className="text-2xl font-extrabold text-[#2b2828]">{renderInlineMarkdown(line.slice(2))}</h2>;
  if (/^\d+\.\s+/.test(line)) return <p key={key} className="pl-4 font-medium text-[#3f3030]">{renderInlineMarkdown(line)}</p>;
  if (line.startsWith('- ')) return <p key={key} className="pl-4 before:mr-2 before:content-['•']">{renderInlineMarkdown(line.slice(2))}</p>;
  return <p key={key}>{renderInlineMarkdown(line)}</p>;
};

const LessonContent = ({ content }) => {
  if (!content) return null;

  return (
    <div
      id="khu-vuc-noi-dung-bai-hoc"
      className="mt-5 select-text rounded-[24px] border border-[#ead9db] bg-[#fffdfc] p-5 text-sm leading-7 text-[#3f3030] selection:bg-[#fff0f1] selection:text-[#4b0009]"
    >
      {String(content).split('\n').map((line, index) => renderLine(line, `${index}-${line.slice(0, 16)}`))}
    </div>
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
  const activeIndex = lessonItems.findIndex((item) => String(item.id) === String(activeLessonId));
  const embedUrl = getVideoEmbedUrl(activeLesson?.videoUrl);
  const [iframeStartSeconds, setIframeStartSeconds] = useState(0);
  const directVideoUrl = activeLesson?.videoUrl && !embedUrl ? activeLesson.videoUrl : '';
  const hasMaterial = Boolean(activeLesson?.materialUrl);
  const isCompleted = activeLessonId ? completedLessonIds.has(activeLessonId) : false;
  const isSaving = activeLessonId && String(savingLessonId) === String(activeLessonId);
  const nextLessonItem = lessonItems[activeIndex + 1];
  const nextLessonLocked = Boolean(nextLessonItem?.isLocked);
  const [selectedLessonText, setSelectedLessonText] = useState('');
  const [selectionButton, setSelectionButton] = useState(null);
  const [lessonNoteMessage, setLessonNoteMessage] = useState('');
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

  useEffect(() => {
    setSelectedLessonText('');
    setSelectionButton(null);
    setLessonNoteMessage('');
    window.getSelection?.()?.removeAllRanges?.();
  }, [activeLessonId]);

  const captureLessonSelection = () => {
    const selection = window.getSelection?.();
    const text = selection?.toString().trim();
    const range = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
    const container = lessonContentRef.current;

    if (!text || !range || !container || !container.contains(range.commonAncestorContainer)) {
      setSelectionButton(null);
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
  };

  const saveSelectedLessonText = () => {
    if (!canPersist) {
      setLessonNoteMessage('Bạn cần đăng nhập để lưu ghi chú.');
      return;
    }
    if (!selectedLessonText.trim()) {
      setLessonNoteMessage('Vui lòng bôi đen một đoạn trong bài học trước khi lưu.');
      return;
    }

    onSaveLessonNote?.({
      content: selectedLessonText.trim(),
      selectedText: selectedLessonText.trim(),
    });
    setSelectedLessonText('');
    setSelectionButton(null);
    window.getSelection?.()?.removeAllRanges?.();
    setLessonNoteMessage('Đã lưu đoạn đã chọn vào ghi chú.');
    onOpenNotes?.();
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

        <div ref={lessonContentRef} className="relative" onMouseUp={captureLessonSelection}>
          <LessonContent content={activeLesson?.contentText} />
          {selectionButton ? (
            <button
              className="absolute z-10 rounded-[8px] bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white shadow-[0_12px_24px_rgba(75,0,9,0.22)] transition hover:bg-[#730014]"
              style={{ top: selectionButton.top, left: selectionButton.left }}
              type="button"
              onMouseDown={(event) => event.preventDefault()}
              onClick={saveSelectedLessonText}
            >
              Lưu ghi chú
            </button>
          ) : null}
        </div>

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
