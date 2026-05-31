const getYoutubeEmbedUrl = (url) => {
  if (!url) return '';

  const match = String(url).match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([^&?/]+)/);
  return match?.[1] ? `https://www.youtube.com/embed/${match[1]}` : '';
};

const WorkspaceLessonPanel = ({
  activeLessonItem,
  completedLessonIds,
  lessonItems,
  savingLessonId,
  onMoveLesson,
  onSelectLesson,
  onToggleComplete,
}) => {
  const activeLesson = activeLessonItem?.lesson;
  const activeModule = activeLessonItem?.module;
  const activeLessonId = activeLessonItem?.id;
  const activeIndex = lessonItems.findIndex((item) => String(item.id) === String(activeLessonId));
  const embedUrl = getYoutubeEmbedUrl(activeLesson?.videoUrl);
  const hasMaterial = Boolean(activeLesson?.materialUrl);
  const isCompleted = activeLessonId ? completedLessonIds.has(activeLessonId) : false;
  const isSaving = activeLessonId && String(savingLessonId) === String(activeLessonId);

  return (
    <section className="rounded-[28px] border border-[#dfbfbd]/20 bg-white shadow-sm">
      <div className="p-6">
        <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">{activeModule?.title || 'Đang học'}</p>
            <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">{activeLesson?.title || 'Bài học đầu tiên'}</h2>
        </div>
        <p className="mt-3 text-sm leading-7 text-[#584140]">
          {activeLesson?.description || 'Nội dung bài học gồm video, tài liệu PDF và các bài tập tự luyện theo module.'}
        </p>

        {embedUrl ? (
          <div className="mt-6 overflow-hidden rounded-[24px] bg-black">
            <div className="aspect-video">
              <iframe
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowFullScreen
                className="h-full w-full"
                src={embedUrl}
                title={activeLesson?.title}
              />
            </div>
          </div>
        ) : null}

        {hasMaterial ? (
          <a
            className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-2xl border border-[#8a0018]/20 px-4 py-3 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
            href={activeLesson.materialUrl}
            rel="noreferrer"
            target="_blank"
          >
            <span className="material-symbols-outlined text-[18px]">description</span>
            Mở tài liệu bài học
          </a>
        ) : null}

        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap gap-3">
            <button
              className="cursor-pointer rounded-2xl border border-[#8a0018]/20 px-4 py-2 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1] disabled:cursor-not-allowed disabled:opacity-40"
              type="button"
              disabled={activeIndex <= 0}
              onClick={() => onMoveLesson(-1)}
            >
              Bài trước
            </button>
            <button
              className="cursor-pointer rounded-2xl bg-[#2b2828] px-4 py-2 text-sm font-bold text-white transition hover:bg-[#8a0018] disabled:cursor-not-allowed disabled:opacity-40"
              type="button"
              disabled={activeIndex < 0 || activeIndex >= lessonItems.length - 1}
              onClick={() => onMoveLesson(1)}
            >
              Bài tiếp theo
            </button>
          </div>
          <button
            className={`inline-flex cursor-pointer items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-extrabold transition hover:-translate-y-0.5 disabled:cursor-wait disabled:opacity-70 ${isCompleted ? 'bg-[#e7f6ec] text-[#176b3a]' : 'bg-[#8a0018] text-white hover:bg-[#650012]'}`}
            type="button"
            disabled={isSaving}
            onClick={() => activeLessonId && onToggleComplete(activeLessonId)}
          >
            <span className="material-symbols-outlined text-[18px]">{isCompleted ? 'check_circle' : 'done'}</span>
            {isSaving ? 'Đang lưu...' : isCompleted ? 'Đã hoàn thành' : 'Đánh dấu hoàn thành'}
          </button>
        </div>

        <div className="mt-6 grid gap-3">
          {(activeModule?.lessons || []).map((lesson, index) => {
            const lessonId = lesson.id ?? `${activeModule.id ?? activeModule.title}-${lesson.title}-${index}`;
            const active = String(lessonId) === String(activeLessonId);
            const completed = completedLessonIds.has(lessonId);

            return (
              <button
                key={lessonId}
                className={`flex cursor-pointer items-center justify-between rounded-2xl border px-4 py-3 text-left transition hover:-translate-y-0.5 hover:border-[#8a0018]/30 hover:bg-[#fff7f6] ${active ? 'border-[#8a0018]/20 bg-[#fff0f1]' : 'border-[#dfbfbd]/20 bg-[#fffdfc]'}`}
                type="button"
                onClick={() => onSelectLesson(lessonId)}
              >
                <div>
                  <p className="text-sm font-extrabold text-[#2b2828]">{lesson.title}</p>
                  <p className="mt-1 text-xs leading-5 text-[#584140]">
                    {lesson.durationMinutes || 0} phút · {lesson.videoUrl ? 'Video tự học' : lesson.materialUrl ? 'Tài liệu học' : 'Bài đọc / thực hành'}
                  </p>
                </div>
                <span className="material-symbols-outlined text-[#8a0018]">{completed ? 'check_circle' : active ? 'play_circle' : 'radio_button_unchecked'}</span>
              </button>
            );
          })}
        </div>
      </div>
    </section>
  );
};

export default WorkspaceLessonPanel;
