import { Link } from 'react-router-dom';
import { ArrowRight, Route } from 'lucide-react';

export default function LearningPathCatalog({ courses = [] }) {
  const paths = Object.values(courses.reduce((groups, course) => {
    const code = String(course.learningPathCode || '').trim();
    if (!code) return groups;
    groups[code] ||= { code, name: course.learningPathName || code, courses: [] };
    groups[code].courses.push(course);
    return groups;
  }, {})).map((path) => ({ ...path, courses: path.courses.sort((left, right) => Number(left.learningPathOrder || 0) - Number(right.learningPathOrder || 0)) }));
  if (!paths.length) return null;
  return <section className="mb-[80px]"><div className="mb-8 flex items-end justify-between"><div><span className="mb-3 block text-[12px] font-extrabold uppercase leading-none tracking-[0.12em] text-[#4b0009]">Học theo từng bước</span><h2 className="text-[32px] font-bold leading-[1.2]">Lộ trình tham khảo</h2></div><a className="group flex items-center gap-2 text-[14px] font-semibold text-[#4b0009]" href="#catalog"><span className="group-hover:underline">Xem khóa học</span><ArrowRight className="h-4 w-4" /></a></div><div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">{paths.map((path) => <article className="flex min-h-[350px] flex-col overflow-hidden rounded-[24px] border border-[#dfbfbd]/30 bg-white p-5 shadow-sm transition hover:-translate-y-1 hover:shadow-md" key={path.code}><div className="flex h-36 items-center justify-center overflow-hidden rounded-2xl bg-[#fff0f2]">{path.courses[0]?.thumbnailUrl ? <img alt="" className="h-full w-full object-cover" src={path.courses[0].thumbnailUrl} /> : <Route className="h-10 w-10 text-[#8a0018]" />}</div><p className="mt-5 text-[11px] font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">{path.courses.length} khóa học · Lộ trình</p><h3 className="mt-2 font-['Manrope'] text-xl font-extrabold leading-snug text-[#2b2828]">{path.name}</h3><ol className="mt-4 space-y-2">{path.courses.slice(0, 3).map((course, index) => <li className="flex items-center gap-2 text-sm text-[#584140]" key={course.id}><span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#fff0f2] text-xs font-extrabold text-[#8a0018]">{course.learningPathOrder || index + 1}</span><span className="truncate font-semibold">{course.title}</span></li>)}</ol><Link className="mt-auto inline-flex items-center justify-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]" to={`/courses/${path.courses[0]?.slug}`}>Xem lộ trình <ArrowRight className="h-4 w-4" /></Link></article>)}</div></section>;
}
