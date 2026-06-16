import { useMemo, useState } from 'react';

const WorkspaceStudyTools = ({
  course,
  activeLesson,
  notes = [],
  reviewFlags = [],
  recentLessons = [],
  canPersist = false,
  onSaveNote,
  onToggleReviewFlag,
  onSelectRecentLesson,
}) => {
  const [noteContent, setNoteContent] = useState('');
  const [message, setMessage] = useState('');
  const currentLessonNotes = useMemo(
    () => notes.filter((item) => String(item.lessonId) === String(activeLesson?.id)),
    [notes, activeLesson?.id],
  );
  const flagged = reviewFlags.some((item) => String(item.lessonId) === String(activeLesson?.id));

  const handleSaveNote = () => {
    if (!canPersist) {
      setMessage('Bạn cần đăng nhập để lưu ghi chú.');
      return;
    }
    if (!noteContent.trim()) {
      setMessage('Vui lòng nhập nội dung ghi chú.');
      return;
    }
    if (noteContent.trim().length > 800) {
      setMessage('Ghi chú quá dài. Vui lòng rút gọn nội dung.');
      return;
    }
    onSaveNote(noteContent.trim());
    setNoteContent('');
    setMessage('Đã lưu ghi chú cho bài học này.');
  };

  return (
    <section id="khu-vuc-ghi-chu" className="grid gap-6 xl:grid-cols-[1.3fr_0.7fr]">
      <div className="rounded-[30px] border border-[#ead9db] bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8c716f]">Ghi chú</p>
            <h3 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#1f2430]">
              Ghi chú cho bài học hiện tại
            </h3>
          </div>
          <button
            className={`rounded-2xl px-4 py-3 text-sm font-extrabold transition ${
              flagged
                ? 'bg-[#fff1f3] text-[#730014]'
                : 'bg-[#f5f1f1] text-[#584140] hover:bg-[#eee6e6]'
            }`}
            onClick={onToggleReviewFlag}
            type="button"
          >
            {flagged ? 'Đã đánh dấu học lại' : 'Đánh dấu học lại'}
          </button>
        </div>

        <textarea
          className="mt-4 min-h-36 w-full rounded-[24px] border border-[#ead9db] bg-[#fcf8f8] px-4 py-4 text-sm leading-7 text-[#2b2828] outline-none transition focus:border-[#730014]"
          placeholder="Thêm ghi chú cho bài học này..."
          value={noteContent}
          onChange={(event) => setNoteContent(event.target.value)}
        />

        {message ? <p className="mt-3 text-sm font-semibold text-[#730014]">{message}</p> : null}

        <div className="mt-4 flex flex-wrap gap-3">
          <button
            className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]"
            onClick={handleSaveNote}
            type="button"
          >
            Lưu ghi chú
          </button>
          <button
            className="rounded-2xl border border-[#dcb6bb] bg-white px-5 py-3 text-sm font-extrabold text-[#8a0018] transition hover:bg-[#fff0f1]"
            type="button"
            onClick={() => document.getElementById('khu-vuc-noi-dung-bai-hoc')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
          >
            Xem lại nội dung bài học
          </button>
        </div>

        <div className="mt-6 space-y-3">
          {currentLessonNotes.length ? currentLessonNotes.map((note) => (
            <div key={note.id} className="rounded-[22px] bg-[#fcf8f8] px-4 py-4">
              <p className="text-sm leading-7 text-[#584140]">{note.content}</p>
            </div>
          )) : (
            <div className="rounded-[22px] border border-dashed border-[#dfbfbd] bg-[#fcf8f8] px-4 py-6 text-center text-sm text-[#584140]">
              Bạn chưa có ghi chú nào cho bài học này.
            </div>
          )}
        </div>
      </div>

      <div className="space-y-6">
        <div className="rounded-[30px] border border-[#ead9db] bg-white p-6 shadow-sm">
          <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8c716f]">Bài học gần đây</p>
          <h3 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#1f2430]">
            Tiếp tục nơi bạn đang dừng
          </h3>

          <div className="mt-4 space-y-3">
            {recentLessons.length ? recentLessons.slice(0, 4).map((item) => (
              <button
                key={item.lessonId}
                className="w-full rounded-[22px] border border-[#ead9db] bg-[#fcf8f8] px-4 py-3 text-left transition hover:border-[#d3b3b7] hover:bg-white"
                onClick={() => onSelectRecentLesson(item.lessonId)}
                type="button"
              >
                <p className="text-sm font-extrabold text-[#2b2828]">{item.lessonTitle}</p>
                <p className="mt-1 text-xs text-[#584140]">{item.courseTitle || course?.title}</p>
              </button>
            )) : (
              <div className="rounded-[22px] border border-dashed border-[#dfbfbd] bg-[#fcf8f8] px-4 py-6 text-center text-sm text-[#584140]">
                Chưa có bài học nào được xem gần đây.
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
};

export default WorkspaceStudyTools;
