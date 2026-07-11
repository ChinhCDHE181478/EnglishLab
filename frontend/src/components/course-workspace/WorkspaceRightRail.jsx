import { useEffect, useMemo, useRef, useState } from 'react';
import WorkspaceLessonDiscussion from './WorkspaceLessonDiscussion';

const formatTime = (seconds = 0) => {
  const value = Math.max(0, Number(seconds) || 0);
  const minutes = Math.floor(value / 60);
  const remainingSeconds = Math.floor(value % 60);
  return `${minutes}:${String(remainingSeconds).padStart(2, '0')}`;
};

const parseTimeToSeconds = (value) => {
  if (typeof value === 'number') return value;
  const text = String(value || '').trim();
  const parts = text.split(':').map((item) => Number(item));
  if (parts.some((item) => !Number.isFinite(item))) return 0;
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
  if (parts.length === 2) return parts[0] * 60 + parts[1];
  return parts[0] || 0;
};

const normalizeTranscriptSegments = (lesson) => {
  const rawSegments = lesson?.transcriptSegments || lesson?.transcriptItems || lesson?.captions;

  if (Array.isArray(rawSegments) && rawSegments.length) {
    return rawSegments
      .map((item, index) => ({
        id: item.id ?? `transcript-${index}`,
        startSeconds: parseTimeToSeconds(item.startSeconds ?? item.startTime ?? item.time ?? item.timestamp),
        endSeconds: parseTimeToSeconds(item.endSeconds ?? item.endTime),
        text: String(item.text ?? item.content ?? item.caption ?? '').trim(),
      }))
      .filter((item) => item.text);
  }

  const transcriptText = typeof lesson?.transcript === 'string' ? lesson.transcript : '';
  if (!transcriptText.trim()) return [];

  return transcriptText
    .split(/\n{2,}|\r?\n/)
    .map((line, index) => {
      const match = line.match(/^\s*(?:\[)?(\d{1,2}:\d{2}(?::\d{2})?)(?:\])?\s+(.+)$/);
      return {
        id: `transcript-text-${index}`,
        startSeconds: match ? parseTimeToSeconds(match[1]) : index * 45,
        endSeconds: 0,
        text: match ? match[2].trim() : line.trim(),
      };
    })
    .filter((item) => item.text);
};

const getModeButtons = (hasVideo) => [
  ...(hasVideo ? [{ key: 'transcript', label: 'Bản chép lời', icon: 'subtitles' }] : []),
  { key: 'notes', label: 'Ghi chú', icon: 'edit_note' },
  { key: 'discussion', label: 'Hỏi đáp', icon: 'forum' },
];

const WorkspaceRightRail = ({
  activeLesson,
  courseId,
  mode = null,
  notes = [],
  reviewFlags = [],
  recentLessons = [],
  canPersist = false,
  onModeChange,
  onSeekTranscript,
  onSaveTranscriptNote,
  onSaveManualNote,
  onUpdateNote,
  onDeleteNote,
  onToggleReviewFlag,
  onSelectRecentLesson,
}) => {
  const transcriptContainerRef = useRef(null);
  const [selectedText, setSelectedText] = useState('');
  const [selectedSegment, setSelectedSegment] = useState(null);
  const [selectionButton, setSelectionButton] = useState(null);
  const [showNoteForm, setShowNoteForm] = useState(false);
  const [manualNote, setManualNote] = useState('');
  const [editingNoteId, setEditingNoteId] = useState('');
  const [editingContent, setEditingContent] = useState('');
  const [deletingNoteId, setDeletingNoteId] = useState('');
  const [message, setMessage] = useState('');
  const lessonId = activeLesson?.id;
  const isAssessmentStep = activeLesson?.type === 'assessment';
  const lesson = activeLesson?.lesson || activeLesson;
  const hasVideo = Boolean(lesson?.videoUrl);
  const modeButtons = useMemo(() => (isAssessmentStep ? [] : getModeButtons(hasVideo)), [hasVideo, isAssessmentStep]);
  const panelMode = hasVideo || mode !== 'transcript' ? mode : null;
  const transcriptSegments = useMemo(() => (
    hasVideo ? normalizeTranscriptSegments(lesson) : []
  ), [hasVideo, lesson]);
  const currentNotes = useMemo(
    () => notes.filter((item) => String(item.lessonId) === String(lessonId)),
    [lessonId, notes],
  );
  const flagged = reviewFlags.some((item) => String(item.lessonId) === String(lessonId));

  useEffect(() => {
    setSelectedText('');
    setSelectedSegment(null);
    setSelectionButton(null);
    setDeletingNoteId('');
    setMessage('');
  }, [lessonId]);

  const captureSelection = (segment) => {
    const selection = window.getSelection?.();
    const text = selection?.toString().trim();
    const range = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
    const container = transcriptContainerRef.current;

    if (!text || !range || !container) {
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

    setSelectedText(text);
    setSelectedSegment(segment);
    setSelectionButton({ top: Math.max(8, top), left });
    setMessage('');
  };

  const saveSelectedTranscript = () => {
    if (!canPersist) {
      setMessage('Bạn cần đăng nhập để lưu ghi chú.');
      return;
    }
    if (!selectedText.trim()) {
      setMessage('Vui lòng bôi đen một đoạn trong bản chép lời trước khi lưu.');
      return;
    }
    onSaveTranscriptNote?.({
      content: selectedText.trim(),
      selectedText: selectedText.trim(),
      transcriptStartSeconds: selectedSegment?.startSeconds ?? null,
    });
    setSelectedText('');
    setSelectedSegment(null);
    setSelectionButton(null);
    window.getSelection?.()?.removeAllRanges?.();
    setMessage('Đã lưu đoạn đã chọn vào ghi chú.');
    onModeChange?.('notes');
  };

  const saveManualNote = () => {
    if (!canPersist) {
      setMessage('Bạn cần đăng nhập để lưu ghi chú.');
      return;
    }
    if (!manualNote.trim()) {
      setMessage('Vui lòng nhập nội dung ghi chú.');
      return;
    }
    if (manualNote.trim().length > 800) {
      setMessage('Ghi chú quá dài. Vui lòng rút gọn nội dung.');
      return;
    }
    onSaveManualNote?.(manualNote.trim());
    setManualNote('');
    setShowNoteForm(false);
    setMessage('Đã lưu ghi chú.');
  };

  const beginEditNote = (note) => {
    setEditingNoteId(note.id);
    setEditingContent(note.content || '');
  };

  const saveEditedNote = (noteId) => {
    if (!editingContent.trim()) {
      setMessage('Vui lòng nhập nội dung ghi chú.');
      return;
    }
    onUpdateNote?.(noteId, editingContent.trim());
    setEditingNoteId('');
    setEditingContent('');
    setMessage('Đã cập nhật ghi chú.');
  };

  const confirmDeleteNote = (noteId) => {
    onDeleteNote?.(noteId);
    if (editingNoteId === noteId) {
      setEditingNoteId('');
      setEditingContent('');
    }
    setDeletingNoteId('');
    setMessage('Đã xóa ghi chú.');
  };

  if (isAssessmentStep) {
    return null;
  }

  const rail = (
    <div className="w-[76px] shrink-0 border-l border-[#ead8d6] bg-[linear-gradient(180deg,_#fffdfa,_#fff7f5)] px-2 py-4">
      <div className="flex flex-col items-center gap-3">
        {modeButtons.map((item) => (
          <button
            key={item.key}
            className={`flex w-full flex-col items-center gap-2 rounded-[8px] px-2 py-3 text-center transition ${
              panelMode === item.key
                ? 'border border-[#e8c6cb] bg-[linear-gradient(180deg,_#fff4f5,_#ffecee)] text-[#6e0012] shadow-[0_8px_20px_rgba(110,0,18,0.08)]'
                : 'border border-transparent text-[#6a5352] hover:border-[#f1d8dc] hover:bg-[#fff6f7] hover:text-[#4b0009]'
            }`}
            type="button"
            onClick={() => onModeChange?.(panelMode === item.key ? null : item.key)}
          >
            <span className="material-symbols-outlined text-[22px]">{item.icon}</span>
            <span className="text-xs font-extrabold leading-4">{item.label}</span>
          </button>
        ))}
      </div>
    </div>
  );

  if (!panelMode) {
    return (
      <aside className="flex h-full min-h-0 overflow-hidden rounded-[8px] border border-[#dce2ec] bg-white shadow-sm">
        {rail}
      </aside>
    );
  }

  return (
    <aside className="flex h-full min-h-0 overflow-hidden rounded-[8px] border border-[#dce2ec] bg-white shadow-sm">
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#f0e3e4] px-5 py-5">
          <div>
            <h2 className="text-xl font-extrabold text-[#1f2430]">
              {panelMode === 'transcript' ? 'Bản chép lời' : panelMode === 'notes' ? 'Ghi chú' : 'Hỏi đáp'}
            </h2>
            {panelMode === 'transcript' ? (
              <p className="mt-2 text-sm font-semibold text-[#3f4d63]">Ngôn ngữ: Tiếng Anh</p>
            ) : null}
          </div>
          <button
            aria-label="Đóng bảng công cụ"
            className="flex h-8 w-8 items-center justify-center rounded-[8px] text-[#1f2430] transition hover:bg-[#f2f5fa]"
            type="button"
            onClick={() => onModeChange?.(null)}
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <div ref={transcriptContainerRef} className="relative min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 pb-5">
        {panelMode === 'transcript' ? (
          <div className="mt-6">
            {message ? <p className="mb-4 text-sm font-semibold text-[#730014]">{message}</p> : null}
            {transcriptSegments.length ? (
              <div className="space-y-7 text-[15px] leading-8 text-[#101828]">
                {transcriptSegments.map((segment) => (
                  <div key={segment.id} onMouseUp={() => captureSelection(segment)}>
                    <button
                      className="mb-2 text-xs font-semibold text-[#730014] hover:underline"
                      type="button"
                      onClick={() => onSeekTranscript?.(segment.startSeconds)}
                    >
                      {formatTime(segment.startSeconds)}
                    </button>
                    <p className="select-text rounded-[6px] px-1 transition selection:bg-[#fff0f1] selection:text-[#4b0009] hover:bg-[#fff7f8]">
                      {segment.text}
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <div className="rounded-[8px] border border-dashed border-[#dfbfbd] bg-[#fff8f8] p-5 text-sm leading-7 text-[#584140]">
                Bài học video này chưa có bản chép lời.
              </div>
            )}

            {selectionButton ? (
              <button
                className="absolute z-10 rounded-[8px] bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white shadow-[0_12px_24px_rgba(75,0,9,0.22)] transition hover:bg-[#730014]"
                style={{ top: selectionButton.top, left: selectionButton.left }}
                type="button"
                onMouseDown={(event) => event.preventDefault()}
                onClick={saveSelectedTranscript}
              >
                Lưu ghi chú
              </button>
            ) : null}
          </div>
        ) : null}

        {panelMode === 'notes' ? (
          <div className="mt-6 space-y-5">
            <div className="flex items-center justify-between gap-3">
              <button
                className="inline-flex items-center gap-2 rounded-[8px] bg-[#4b0009] px-4 py-2 text-sm font-extrabold text-white transition hover:bg-[#730014]"
                type="button"
                onClick={() => {
                  setShowNoteForm(true);
                  setMessage('');
                }}
              >
                <span className="material-symbols-outlined text-[18px]">add</span>
                Thêm ghi chú
              </button>
            </div>

            {showNoteForm ? (
              <div className="rounded-[8px] border border-[#dfbfbd] bg-[#fffdfd] p-4">
                <div className="flex justify-end">
                  <button
                    aria-label="Đóng form ghi chú"
                    className="flex h-8 w-8 items-center justify-center rounded-[8px] text-[#4b0009] hover:bg-[#fff0f1]"
                    type="button"
                    onClick={() => {
                      setShowNoteForm(false);
                      setManualNote('');
                    }}
                  >
                    <span className="material-symbols-outlined text-[18px]">close</span>
                  </button>
                </div>
                <textarea
                  className="min-h-28 w-full resize-none rounded-[8px] border border-[#dfbfbd] bg-white px-3 py-3 text-sm leading-7 outline-none transition focus:border-[#730014]"
                  placeholder="Nhập ghi chú cho bài học này..."
                  value={manualNote}
                  onChange={(event) => setManualNote(event.target.value)}
                />
                <button className="mt-3 rounded-[8px] bg-[#4b0009] px-4 py-2 text-sm font-extrabold text-white hover:bg-[#730014]" type="button" onClick={saveManualNote}>
                  Lưu ghi chú
                </button>
              </div>
            ) : null}

            {message ? <p className="text-sm font-semibold text-[#730014]">{message}</p> : null}

            {currentNotes.length ? currentNotes.map((note) => {
              const editing = editingNoteId === note.id;
              const displayText = note.content || note.selectedText || '';
              return (
                <article key={note.id} className="rounded-[8px] bg-[#f1f5fb] p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-extrabold text-[#1f2430]">Lưu ý</p>
                      {note.transcriptStartSeconds != null ? (
                        <button
                          className="mt-1 text-sm font-extrabold text-[#730014] hover:underline"
                          type="button"
                          onClick={() => onSeekTranscript?.(note.transcriptStartSeconds)}
                        >
                          {formatTime(note.transcriptStartSeconds)}
                        </button>
                      ) : null}
                    </div>
                    <span className="material-symbols-outlined text-[#1f2430]">keyboard_arrow_up</span>
                  </div>

                  {editing ? (
                    <div className="mt-4 border-l-4 border-[#4b0009] pl-3">
                      <textarea
                        className="min-h-32 w-full resize-none rounded-[8px] border border-[#dfbfbd] bg-white px-3 py-3 text-sm leading-7 text-[#1f2430] outline-none focus:border-[#730014]"
                        value={editingContent}
                        onChange={(event) => setEditingContent(event.target.value)}
                      />
                      <div className="mt-3 flex gap-2">
                        <button className="rounded-[8px] bg-[#4b0009] px-3 py-2 text-xs font-extrabold text-white" type="button" onClick={() => saveEditedNote(note.id)}>
                          Lưu
                        </button>
                        <button className="rounded-[8px] bg-white px-3 py-2 text-xs font-extrabold text-[#4b0009]" type="button" onClick={() => setEditingNoteId('')}>
                          Hủy
                        </button>
                      </div>
                    </div>
                  ) : (
                    <p className="mt-4 border-l-4 border-[#4b0009] pl-3 text-sm leading-7 text-[#1f2430]">
                      {displayText}
                    </p>
                  )}

                  <div className="mt-5 flex justify-end gap-3">
                    {deletingNoteId === note.id ? (
                      <div className="mr-auto flex items-center gap-2 rounded-[8px] bg-white px-3 py-2 text-xs font-bold text-[#4b0009]">
                        <span>Xóa ghi chú này?</span>
                        <button className="font-extrabold text-[#93000a] hover:underline" type="button" onClick={() => confirmDeleteNote(note.id)}>
                          Xóa
                        </button>
                        <button className="font-extrabold text-[#584140] hover:underline" type="button" onClick={() => setDeletingNoteId('')}>
                          Hủy
                        </button>
                      </div>
                    ) : null}
                    <button
                      aria-label="Sửa ghi chú"
                      className="flex h-9 w-9 items-center justify-center rounded-[8px] text-[#1f2430] hover:bg-white"
                      type="button"
                      onClick={() => beginEditNote({ ...note, content: displayText })}
                    >
                      <span className="material-symbols-outlined text-[20px]">edit</span>
                    </button>
                    <button
                      aria-label="Xóa ghi chú"
                      className="flex h-9 w-9 items-center justify-center rounded-[8px] text-[#1f2430] hover:bg-white hover:text-[#93000a]"
                      type="button"
                      onClick={() => {
                        setDeletingNoteId(note.id);
                        setMessage('');
                      }}
                    >
                      <span className="material-symbols-outlined text-[20px]">delete</span>
                    </button>
                  </div>
                </article>
              );
            }) : (
              <div className="rounded-[8px] border border-dashed border-[#dfbfbd] bg-[#fff8f8] p-5 text-sm text-[#584140]">
                Bạn chưa có ghi chú nào cho bài học này.
              </div>
            )}

            {recentLessons.length ? (
              <div className="space-y-2">
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#8c716f]">Bài học gần đây</p>
                {recentLessons.slice(0, 3).map((item) => (
                  <button
                    key={item.lessonId}
                    className="w-full rounded-[8px] bg-[#fff8f8] px-3 py-3 text-left text-sm font-semibold text-[#1f2430] hover:bg-[#fff0f1]"
                    type="button"
                    onClick={() => onSelectRecentLesson?.(item.lessonId)}
                  >
                    {item.lessonTitle}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
        ) : null}
        {panelMode === 'discussion' ? (
          <div className="mt-6">
            <WorkspaceLessonDiscussion courseId={courseId} lessonId={lessonId} canPersist={canPersist} />
          </div>
        ) : null}
        </div>
      </div>

      {rail}
    </aside>
  );
};

export default WorkspaceRightRail;
