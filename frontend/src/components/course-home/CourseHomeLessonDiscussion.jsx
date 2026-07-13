import { useMemo, useState } from 'react';
import WorkspaceLessonDiscussion from '../course-workspace/WorkspaceLessonDiscussion';

const formatModuleLabel = (module, index) => {
  const title = String(module?.title || '').trim();
  if (/^(module|mô-đun)\s*\d+\s*:/i.test(title)) return title;
  return `Mô-đun ${index + 1}: ${title || 'Chưa đặt tên'}`;
};

const CourseHomeLessonDiscussion = ({ courseId, modules = [] }) => {
  const [moduleId, setModuleId] = useState('');
  const [lessonId, setLessonId] = useState('');

  const selectedModule = useMemo(
    () => modules.find((module) => String(module.id) === String(moduleId)),
    [moduleId, modules],
  );
  const lessons = selectedModule?.lessons || [];

  const handleModuleChange = (event) => {
    setModuleId(event.target.value);
    setLessonId('');
  };

  return (
    <section className="mt-8 rounded-3xl border border-[#ead9db] bg-[#fffdfc] p-5 shadow-sm md:p-6">
      <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-[#8a0018]">Hỏi đáp theo bài học</p>
      <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#1e1e1e]">Chọn mô-đun và bài học</h2>
      <p className="mt-2 text-sm text-[#6a5352]">Chọn đúng bài đang học để xem và gửi câu hỏi liên quan.</p>

      <div className="mt-5 grid gap-4 md:grid-cols-2">
        <label className="text-sm font-bold text-[#4b0009]">
          Mô-đun
          <select
            className="mt-1.5 w-full rounded-xl border border-[#ead9db] bg-white px-3 py-2.5 text-sm font-medium text-[#4b0009] outline-none focus:border-[#8a0018]"
            onChange={handleModuleChange}
            value={moduleId}
          >
            <option value="">-- Chọn mô-đun --</option>
            {modules.map((module, index) => (
              <option key={module.id || module.title || index} value={module.id}>
                {formatModuleLabel(module, index)}
              </option>
            ))}
          </select>
        </label>

        <label className="text-sm font-bold text-[#4b0009]">
          Bài học
          <select
            className="mt-1.5 w-full rounded-xl border border-[#ead9db] bg-white px-3 py-2.5 text-sm font-medium text-[#4b0009] outline-none focus:border-[#8a0018] disabled:cursor-not-allowed disabled:bg-slate-100"
            disabled={!moduleId}
            onChange={(event) => setLessonId(event.target.value)}
            value={lessonId}
          >
            <option value="">-- Chọn bài học --</option>
            {lessons.map((lesson) => (
              <option key={lesson.id} value={lesson.id}>{lesson.title || `Bài học ${lesson.id}`}</option>
            ))}
          </select>
        </label>
      </div>

      {lessonId ? (
        <div className="mt-5">
          <WorkspaceLessonDiscussion canPersist courseId={courseId} lessonId={lessonId} />
        </div>
      ) : (
        <div className="mt-5 rounded-xl border border-dashed border-[#ead9db] bg-white px-4 py-8 text-center text-sm text-[#8c716f]">
          Chọn mô-đun, sau đó chọn bài học để xem hỏi đáp.
        </div>
      )}
    </section>
  );
};

export default CourseHomeLessonDiscussion;
