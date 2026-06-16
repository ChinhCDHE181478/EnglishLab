import { useState } from 'react';

const CourseModuleAccordion = ({ modules = [] }) => {
  const [openId, setOpenId] = useState(modules[0]?.id ?? null);

  return (
    <section id="noi-dung-khoa-hoc" className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
      <div className="mb-6">
        <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Nội dung khóa học</p>
        <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Modules và bài học</h2>
      </div>
      <div className="space-y-4">
        {modules.map((module, index) => {
          const isOpen = openId === module.id;
          return (
            <div key={module.id ?? `${module.title}-${index}`} className="overflow-hidden rounded-3xl border border-[#dfbfbd]/25 bg-[#fcf8f7]">
              <button
                className="flex w-full cursor-pointer items-center justify-between gap-4 px-5 py-5 text-left"
                onClick={() => setOpenId(isOpen ? null : module.id)}
                type="button"
              >
                <div>
                  <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Module {index + 1}</p>
                  <h3 className="mt-2 text-xl font-extrabold text-[#2b2828]">{module.title}</h3>
                  <p className="mt-2 text-sm leading-6 text-[#584140]">{module.description}</p>
                </div>
                <span className={`material-symbols-outlined text-[#8a0018] transition-transform ${isOpen ? 'rotate-180' : ''}`}>expand_more</span>
              </button>
              {isOpen ? (
                <div className="border-t border-[#dfbfbd]/25 bg-white px-5 py-5">
                  <div className="grid gap-3">
                    {(module.lessons || []).map((lesson, lessonIndex) => (
                      <div key={lesson.id ?? `${lesson.title}-${lessonIndex}`} className="flex items-center justify-between gap-4 rounded-2xl border border-[#dfbfbd]/20 bg-[#fffdfc] px-4 py-3">
                        <div>
                          <p className="text-sm font-extrabold text-[#2b2828]">{lesson.title}</p>
                          <p className="mt-1 text-xs leading-5 text-[#584140]">{lesson.description}</p>
                        </div>
                        <div className="shrink-0 text-right">
                          <p className="text-xs font-bold uppercase tracking-[0.1em] text-[#8c716f]">{lesson.durationMinutes || 0} phút</p>
                          {lesson.preview ? (
                            <span className="mt-1 inline-flex rounded-full bg-[#fff0f1] px-2 py-1 text-[10px] font-bold uppercase tracking-[0.12em] text-[#8a0018]">Preview</span>
                          ) : null}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
          );
        })}
      </div>
    </section>
  );
};

export default CourseModuleAccordion;
