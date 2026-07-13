import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Route } from 'lucide-react';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import CourseFooter from '../components/course/CourseFooter';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';
import { normalizeCourse } from '../utils/courseModels';

export default function LearningPathReferencePage() {
  const { code } = useParams();
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  useEffect(() => { const load = async () => { try { const result = await courseApi.getOnlineCourses({ page: 0, size: 100 }); setCourses((result.content || []).map(normalizeCourse)); } finally { setLoading(false); } }; load(); }, []);
  const path = useMemo(() => { const items = courses.filter((course) => String(course.learningPathCode) === String(code)).sort((left, right) => Number(left.learningPathOrder || 0) - Number(right.learningPathOrder || 0)); return items.length ? { name: items[0].learningPathName || code, courses: items } : null; }, [code, courses]);
  return <div className="min-h-screen bg-[#f9f9f9] text-[#2b2828]"><CourseGlobalStyles /><Header /><main className="mx-auto max-w-[1120px] px-4 py-10 md:px-8"><Link className="inline-flex items-center gap-2 text-sm font-bold text-[#730014]" to="/courses"><ArrowLeft className="h-4 w-4" />Quay lại khóa học</Link>{loading ? <p className="py-16 text-center text-[#584140]">Đang tải lộ trình...</p> : !path ? <p className="py-16 text-center text-[#584140]">Không tìm thấy lộ trình này.</p> : <><section className="mt-6 rounded-3xl border border-[#ead9db] bg-[#fffdfc] p-7 shadow-sm"><div className="flex items-center gap-3"><span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#fff0f2] text-[#8a0018]"><Route className="h-6 w-6" /></span><div><p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">Lộ trình tham khảo</p><h1 className="font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">{path.name}</h1></div></div><p className="mt-4 text-sm text-[#584140]">Theo dõi thứ tự các khóa học trong lộ trình này và mở từng khóa để xem thông tin chi tiết.</p></section><section className="mt-7 space-y-4">{path.courses.map((course, index) => <article className="flex flex-col gap-4 rounded-2xl border border-[#ead9db] bg-white p-5 shadow-sm sm:flex-row sm:items-center" key={course.id}><span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#fff0f2] font-extrabold text-[#8a0018]">{course.learningPathOrder || index + 1}</span><div className="min-w-0 flex-1"><h2 className="font-['Manrope'] text-lg font-extrabold">{course.title}</h2><p className="mt-1 text-sm text-[#584140]">{course.shortDescription || course.description}</p></div><Link className="inline-flex shrink-0 items-center gap-2 rounded-xl border border-[#dfbfbd] px-4 py-2.5 text-sm font-bold text-[#730014]" to={`/courses/${course.slug}`}>Xem khóa học <ArrowRight className="h-4 w-4" /></Link></article>)}</section></>}</main><CourseFooter /></div>;
}
