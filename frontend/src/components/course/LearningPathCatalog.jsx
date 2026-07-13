import { Link } from 'react-router-dom';
import { ArrowRight, Route } from 'lucide-react';

export default function LearningPathCatalog({ courses = [] }) {
  const paths = Object.values(courses.reduce((groups, course) => {
    const code = String(course.learningPathCode || '').trim();
    if (!code) return groups;
    if (!groups[code]) {
      groups[code] = {
        code,
        name: course.learningPathName || code,
        courses: [],
      };
    }
    groups[code].courses.push(course);
    return groups;
  }, {})).map((path) => ({
    ...path,
    courses: path.courses.sort((left, right) => Number(left.learningPathOrder || 0) - Number(right.learningPathOrder || 0)),
  }));

  if (!paths.length) return null;

  return (
    <section className="mb-10 rounded-3xl border border-[#ead9db] bg-[#fffdfc] p-6 shadow-sm md:p-8">
      <div className="mb-6 flex items-start gap-3">
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-[#fff0f2] text-[#8a0018]"><Route className="h-5 w-5" /></span>
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">Học theo từng bước</p>
          <h2 className="mt-1 font-['Manrope'] text-2xl font-extrabold text-[#4b0009]">Lộ trình học</h2>
          <p className="mt-1 text-sm text-[#6f5553]">Chọn lộ trình phù hợp và theo dõi thứ tự các khóa học.</p>
        </div>
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        {paths.map((path) => (
          <article className="rounded-2xl border border-[#ead9db] bg-white p-5" key={path.code}>
            <h3 className="font-['Manrope'] text-lg font-extrabold text-[#4b0009]">{path.name}</h3>
            <ol className="mt-4 space-y-3">
              {path.courses.map((course, index) => (
                <li className="flex items-center justify-between gap-3" key={course.id}>
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#fff0f2] text-xs font-extrabold text-[#8a0018]">{course.learningPathOrder || index + 1}</span>
                    <span className="truncate text-sm font-bold text-[#2d2020]">{course.title}</span>
                  </div>
                  <Link aria-label={`Xem ${course.title}`} className="shrink-0 text-[#8a0018]" to={`/courses/${course.slug}`}><ArrowRight className="h-4 w-4" /></Link>
                </li>
              ))}
            </ol>
          </article>
        ))}
      </div>
    </section>
  );
}
