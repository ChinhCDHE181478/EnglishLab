import { useEffect, useState } from 'react';

const getLessonId = (module, lesson, lessonIndex) => lesson.id ?? `${module.id ?? module.title}-${lesson.title}-${lessonIndex}`;
const getAssessmentStepId = (moduleId) => `__ai_assessment__:${moduleId ?? 'course'}`;

const WorkspaceSidebar = ({
  course,
  activeLessonId,
  assessmentLockByModule,
  assessmentModuleIds,
  completedLessonIds,
  lessonItems = [],
  moduleProgress,
  hasAssessments,
  collapsed = false,
  onCollapse,
  onExpand,
  onSelectLesson,
}) => {
  const [openModuleIds, setOpenModuleIds] = useState(() => new Set());
  const lockedLessonIds = new Set(
    lessonItems
      .filter((item) => item.isLocked)
      .map((item) => String(item.id)),
  );

  useEffect(() => {
    const activeModule = (course?.modules || []).find((module) => {
      const lessonActive = (module.lessons || []).some(
        (lesson, lessonIndex) => String(getLessonId(module, lesson, lessonIndex)) === String(activeLessonId),
      );
      return lessonActive || String(getAssessmentStepId(module.id)) === String(activeLessonId);
    });

    if (activeModule) {
      setOpenModuleIds((current) => new Set([...current, String(activeModule.id ?? activeModule.title)]));
      return;
    }

    const firstModule = course?.modules?.[0];
    if (firstModule) {
      setOpenModuleIds((current) => new Set([...current, String(firstModule.id ?? firstModule.title)]));
    }
  }, [activeLessonId, course?.modules]);

  const toggleModule = (module) => {
    const key = String(module.id ?? module.title);
    setOpenModuleIds((current) => {
      const next = new Set(current);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  if (collapsed) {
    return (
      <aside className="sticky top-[96px] hidden h-[calc(100vh-112px)] self-start rounded-[24px] bg-white shadow-sm xl:flex xl:w-14 xl:items-start xl:justify-center xl:border xl:border-[#e6dadd] xl:py-4">
        <button
          aria-label="Mở danh sách bài học"
          className="flex h-10 w-10 items-center justify-center rounded-2xl text-[#1f2430] transition hover:bg-[#f2f5fa]"
          type="button"
          onClick={onExpand}
        >
          <span className="material-symbols-outlined text-[22px]">menu</span>
        </button>
      </aside>
    );
  }

  return (
    <aside className="sticky top-[96px] hidden h-[calc(100vh-112px)] self-start overflow-hidden rounded-[8px] border border-[#dce2ec] bg-white shadow-sm xl:flex xl:flex-col">
      <div className="shrink-0 border-b border-[#e5e9f1] bg-white px-5 py-5">
        <div className="flex items-start justify-between gap-4">
          <h2 className="font-['Manrope'] text-xl font-extrabold leading-tight text-[#1f2430]">
            {course?.title || 'Khóa học đang học'}
          </h2>
          <button
            aria-label="Thu gọn danh sách bài học"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-[#1f2430] transition hover:bg-[#f2f5fa]"
            type="button"
            onClick={onCollapse}
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-4">
        <div className="space-y-3">
          {(course?.modules || []).map((module, moduleIndex) => {
            const moduleKey = String(module.id ?? module.title);
            const moduleLessons = module.lessons || [];
            const assessmentStepId = getAssessmentStepId(module.id);
            const hasModuleAssessment = hasAssessments && assessmentModuleIds?.has(String(module.id));
            const assessmentLocked = assessmentLockByModule?.get(String(module.id));
            const moduleUnlocked = moduleProgress?.get(String(module.id))?.moduleUnlocked ?? true;
            const open = openModuleIds.has(moduleKey);

            return (
              <section key={moduleKey} className="border-b border-[#dfe4ed] pb-3 last:border-b-0">
                <button
                  className="flex w-full items-start justify-between gap-3 rounded-[8px] px-2 py-3 text-left transition hover:bg-[#f4f7fb]"
                  type="button"
                  onClick={() => toggleModule(module)}
                >
                  <span>
                    <span className="block text-xs font-bold text-[#63718a]">Mô-đun {moduleIndex + 1}</span>
                    <span className="mt-1 block text-sm font-extrabold leading-6 text-[#1f2430]">{module.title}</span>
                  </span>
                  <span className="material-symbols-outlined mt-1 text-[20px] text-[#40516d]">
                    {open ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
                  </span>
                </button>

                {open ? (
                  <div className="space-y-1 pt-1">
                    {moduleLessons.map((lesson, lessonIndex) => {
                      const lessonId = getLessonId(module, lesson, lessonIndex);
                      const isActive = String(lessonId) === String(activeLessonId);
                      const isCompleted = completedLessonIds.has(lessonId);
                      const isLocked = lockedLessonIds.has(String(lessonId));
                      const statusText = isLocked
                        ? lessonIndex === 0 && !moduleUnlocked
                          ? 'Hoàn thành và đạt yêu cầu ở bài đánh giá cuối mô-đun trước để mở.'
                          : 'Hoàn thành bài học trước để mở.'
                        : `${lesson.videoUrl ? 'Video' : lesson.materialUrl ? 'Tài liệu' : 'Bài học'} • ${lesson.durationMinutes || 0} phút`;

                      return (
                        <button
                          key={lessonId}
                          className={`flex w-full items-start gap-3 rounded-[8px] px-3 py-3 text-left transition ${
                            isActive ? 'bg-[#fff0f1]' : isLocked ? 'cursor-not-allowed opacity-60' : 'hover:bg-[#f7f9fc]'
                          }`}
                          type="button"
                          disabled={isLocked}
                          onClick={() => onSelectLesson(lessonId)}
                        >
                          <span className={`material-symbols-outlined mt-0.5 text-[20px] ${isCompleted ? 'text-[#4b0009]' : 'text-[#63718a]'}`}>
                            {isCompleted ? 'check_circle' : isLocked ? 'lock' : isActive ? 'play_circle' : 'radio_button_unchecked'}
                          </span>
                          <span className="min-w-0 flex-1">
                            <span className="block line-clamp-2 text-sm font-semibold leading-6 text-[#1f2430]">{lesson.title}</span>
                            <span className="mt-1 block text-xs text-[#63718a]">{statusText}</span>
                          </span>
                        </button>
                      );
                    })}

                    {hasModuleAssessment ? (
                      <button
                        className={`flex w-full items-start gap-3 rounded-[8px] px-3 py-3 text-left transition ${
                          String(activeLessonId) === assessmentStepId ? 'bg-[#eef4ff]' : 'hover:bg-[#f7f9fc]'
                        } ${assessmentLocked ? 'cursor-not-allowed opacity-70' : ''}`}
                        type="button"
                        disabled={assessmentLocked}
                        onClick={() => onSelectLesson(assessmentStepId)}
                      >
                        <span className="material-symbols-outlined mt-0.5 text-[20px] text-[#63718a]">
                          {assessmentLocked ? 'lock' : 'assignment_turned_in'}
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block text-sm font-semibold leading-6 text-[#1f2430]">Bài đánh giá cuối mô-đun</span>
                          <span className="mt-1 block text-xs text-[#63718a]">
                            {assessmentLocked
                              ? moduleUnlocked
                                ? 'Hoàn thành toàn bộ bài học trong mô-đun để mở.'
                                : 'Hoàn thành và đạt yêu cầu ở mô-đun trước để mở.'
                              : 'Nộp bài để hoàn tất mô-đun.'}
                          </span>
                        </span>
                      </button>
                    ) : null}
                  </div>
                ) : null}
              </section>
            );
          })}
        </div>
      </div>
    </aside>
  );
};

export default WorkspaceSidebar;
