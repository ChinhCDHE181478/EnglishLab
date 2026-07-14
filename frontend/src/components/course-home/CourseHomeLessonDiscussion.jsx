import { useMemo, useState } from 'react';
import WorkspaceLessonDiscussion from '../course-workspace/WorkspaceLessonDiscussion';
import BrandedSelect from '../ui/BrandedSelect';

const formatModuleLabel = (module, index) => {
  const title = String(module?.title || '').trim();
  if (/^(module|mô-đun)\s*\d+\s*:/i.test(title)) return title;
  return `Mô-đun ${index + 1}: ${title || 'Chưa đặt tên'}`;
};

const CourseHomeLessonDiscussion = ({ courseId, modules = [] }) => {
  const [moduleId, setModuleId] = useState('ALL');
  const [lessonId, setLessonId] = useState('ALL');
  const selectedModule = useMemo(
    () => modules.find((module) => String(module.id) === String(moduleId)),
    [moduleId, modules],
  );
  const lessons = moduleId === 'ALL'
    ? modules.flatMap((module) => module.lessons || [])
    : selectedModule?.lessons || [];
  const lessonIds = useMemo(
    () => lessons.map((lesson) => lesson.id).filter(Boolean),
    [lessons],
  );

  const handleModuleChange = (event) => {
    setModuleId(event.target.value);
    setLessonId('ALL');
  };

  return (
    <section className="mt-8 rounded-3xl border border-[#ead9db] bg-[#fffdfc] p-5 shadow-sm md:p-6">
      <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-[#8a0018]">Hỏi đáp theo bài học</p>
      <h2 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#1e1e1e]">Lọc câu hỏi theo mô-đun và bài học</h2>
      <p className="mt-2 text-sm text-[#6a5352]">Mặc định hiển thị tất cả câu hỏi. Bạn có thể thu hẹp kết quả theo bài học khi cần.</p>
      <div className="mt-5 grid gap-4 md:grid-cols-2">
        <label className="text-sm font-bold text-[#4b0009]">Mô-đun
          <div className="mt-1.5"><BrandedSelect onChange={handleModuleChange} options={[{ label: 'Tất cả mô-đun', value: 'ALL' }, ...modules.map((module, index) => ({ label: formatModuleLabel(module, index), value: module.id }))]} value={moduleId} /></div>
        </label>
        <label className="text-sm font-bold text-[#4b0009]">Bài học
          <div className="mt-1.5"><BrandedSelect disabled={!lessons.length} onChange={(event) => setLessonId(event.target.value)} options={[{ label: 'Tất cả bài học', value: 'ALL' }, ...lessons.map((lesson) => ({ label: lesson.title || `Bài học ${lesson.id}`, value: lesson.id }))]} value={lessonId} /></div>
        </label>
      </div>
      {lessons.length ? (
        <div className="mt-5">
          <WorkspaceLessonDiscussion canPersist courseId={courseId} lessonId={lessonId === 'ALL' ? null : lessonId} lessonIds={lessonId === 'ALL' ? lessonIds : undefined} />
        </div>
      ) : <div className="mt-5 rounded-xl border border-dashed border-[#ead9db] bg-white px-4 py-8 text-center text-sm text-[#8c716f]">Chưa có bài học để hiển thị hỏi đáp.</div>}
    </section>
  );
};

export default CourseHomeLessonDiscussion;
