import { Link } from 'react-router-dom';

const getLessonId = (module, lesson, lessonIndex) => lesson.id ?? `${module.id ?? module.title}-${lesson.title}-${lessonIndex}`;
const getAssessmentStepId = (moduleId) => `__ai_assessment__:${moduleId ?? 'course'}`;

const WorkspaceSidebar = ({ course, activeLessonId, assessmentLockByModule, assessmentModuleIds, completedLessonIds, hasAssessments, onSelectLesson }) => (
  <aside className="sticky top-[96px] hidden h-[calc(100vh-96px)] w-80 shrink-0 self-start overflow-y-auto overscroll-contain border-r border-[#dfbfbd]/20 bg-[#fffdfc] p-6 xl:block">
    <div className="rounded-3xl border border-[#dfbfbd]/20 bg-[#fcf8f7] p-5">
      <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Đang học</p>
      <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">{course.title}</h2>
      <p className="mt-2 text-sm leading-6 text-[#584140]">{course.shortDescription}</p>
      <Link className="mt-4 inline-flex text-sm font-bold text-[#8a0018] hover:underline" to={`/courses/${course.slug}`} state={{ course }}>
        Xem thông tin khóa học
      </Link>
    </div>

    <div className="mt-6 space-y-3">
      {(course.modules || []).map((module, moduleIndex) => {
        const moduleLessons = module.lessons || [];
        const assessmentStepId = getAssessmentStepId(module.id);
        const hasModuleAssessment = hasAssessments && assessmentModuleIds?.has(String(module.id));
        const assessmentLocked = assessmentLockByModule?.get(String(module.id));
        const moduleHasActiveLesson = moduleLessons.some((lesson, lessonIndex) => String(getLessonId(module, lesson, lessonIndex)) === String(activeLessonId))
          || String(activeLessonId) === assessmentStepId;

        return (
          <div key={module.id ?? `${module.title}-${moduleIndex}`} className={`rounded-3xl border p-4 transition-all ${moduleHasActiveLesson ? 'border-[#8a0018]/20 bg-[#fff0f1] shadow-sm' : 'border-[#dfbfbd]/20 bg-white'}`}>
            <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Module {moduleIndex + 1}</p>
            <h3 className={`mt-2 text-sm font-extrabold ${moduleHasActiveLesson ? 'text-[#8a0018]' : 'text-[#2b2828]'}`}>{module.title}</h3>

            <div className="mt-3 space-y-2">
              {moduleLessons.map((lesson, lessonIndex) => {
                const lessonId = getLessonId(module, lesson, lessonIndex);
                const isActive = String(lessonId) === String(activeLessonId);
                const isCompleted = completedLessonIds.has(lessonId);

                return (
                  <button
                    key={lessonId}
                    className={`flex w-full cursor-pointer items-start gap-2 rounded-2xl px-3 py-2 text-left text-xs transition hover:bg-white ${isActive ? 'bg-white text-[#8a0018] shadow-sm' : 'text-[#584140]'}`}
                    type="button"
                    onClick={() => onSelectLesson(lessonId)}
                  >
                    <span className="material-symbols-outlined mt-0.5 text-[16px]">
                      {isCompleted ? 'check_circle' : isActive ? 'play_circle' : 'radio_button_unchecked'}
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="line-clamp-2 font-bold">{lesson.title}</span>
                      <span className="mt-1 block text-[11px] text-[#8c716f]">{lesson.durationMinutes || 0} phút</span>
                    </span>
                  </button>
                );
              })}

              {hasModuleAssessment ? (
                <button
                  className={`flex w-full items-start gap-2 rounded-2xl px-3 py-2 text-left text-xs transition ${assessmentLocked ? 'cursor-not-allowed opacity-70' : 'cursor-pointer hover:bg-white'} ${String(activeLessonId) === assessmentStepId ? 'bg-white text-[#8a0018] shadow-sm' : 'text-[#584140]'}`}
                  type="button"
                  disabled={assessmentLocked}
                  onClick={() => onSelectLesson(assessmentStepId)}
                >
                  <span className={`material-symbols-outlined mt-0.5 text-[16px] ${assessmentLocked ? 'text-[#8c716f]' : 'text-[#8a0018]'}`}>
                    {assessmentLocked ? 'lock' : 'assignment_turned_in'}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="line-clamp-2 font-bold">Bài kiểm tra cuối module</span>
                    <span className="mt-1 block text-[11px] text-[#8c716f]">
                      {assessmentLocked ? 'Hoàn thành các bài học trong module để mở' : 'Đánh giá theo rubric của module này'}
                    </span>
                  </span>
                </button>
              ) : null}
            </div>
          </div>
        );
      })}
    </div>
  </aside>
);

export default WorkspaceSidebar;
