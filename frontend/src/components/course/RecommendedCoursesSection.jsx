import { useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import CatalogCourseCard from './CatalogCourseCard';

const RecommendedCoursesSection = ({ courses = [], loading = false, error = '', hasCurrentBand = false, currentBand = null, profileBased = false, onRetry }) => {
  const [startIndex, setStartIndex] = useState(0);
  const daCoBandHienTai = hasCurrentBand || Number(currentBand) > 0;
  const visibleCourses = courses.slice(startIndex, startIndex + 3);
  useEffect(() => setStartIndex(0), [courses]);
  const move = (direction) => setStartIndex((current) => courses.length > 3 ? (current + direction + courses.length) % courses.length : 0);
  const description = profileBased ? 'Gợi ý cá nhân hóa từ hồ sơ, band hiện tại, mục tiêu và kỹ năng cần cải thiện của bạn.' : daCoBandHienTai ? 'Gợi ý dựa trên trình độ hiện tại, mục tiêu đầu ra và dữ liệu khóa học hiện có.' : 'Hãy cập nhật trình độ hiện tại để nhận gợi ý chính xác hơn. Trong lúc này, EnglishLab vẫn hiển thị các khóa học nổi bật.';
  return <section className="mb-[88px]"><div className="mb-6 flex items-end justify-between gap-4"><div><h2 className="font-['Manrope'] text-[32px] font-extrabold leading-[1.2] text-[#4b0009]">Khóa học phù hợp với bạn</h2><p className="mt-2 text-sm leading-7 text-[#584140]">{description}</p></div>{courses.length > 3 ? <div className="flex shrink-0 gap-2"><button aria-label="Khóa học trước" className="rounded-xl border border-[#dfbfbd] bg-white p-2 text-[#730014]" onClick={() => move(-1)} type="button"><ChevronLeft className="h-5 w-5" /></button><button aria-label="Khóa học sau" className="rounded-xl border border-[#dfbfbd] bg-white p-2 text-[#730014]" onClick={() => move(1)} type="button"><ChevronRight className="h-5 w-5" /></button></div> : null}</div>{loading ? <div className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-8 text-center text-[#584140]">Đang tải danh sách gợi ý...</div> : error ? <div className="rounded-[28px] border border-[#f0d4d7] bg-white p-8 text-center text-[#93000a]"><p>{error}</p><button className="mt-4 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white" onClick={onRetry} type="button">Thử lại</button></div> : courses.length ? <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">{visibleCourses.map((course) => <div key={course.id} className="max-w-[380px] space-y-4"><CatalogCourseCard compact course={course} /><div className="rounded-2xl border border-[#dfbfbd]/20 bg-white px-4 py-3 text-center text-sm font-semibold text-[#584140]">{course.recommendationReason}</div></div>)}</div> : <div className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-8 text-center text-[#584140]">Hiện chưa có khóa học phù hợp với trình độ của bạn.</div>}</section>;
};

export default RecommendedCoursesSection;
