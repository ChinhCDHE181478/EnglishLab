import CourseActionButton, { detailCourseButtonClassName } from './CourseActionButton';
import MaterialIcon from './MaterialIcon';
import { categoryLabels, levelLabels } from './courseConstants';
import { formatCoursePrice } from './courseFormatters';
import { stripRichTextToPlain } from '../../utils/lessonRichText';

const PopularCourseCard = ({ course }) => (
  <div className="course-card flex flex-col overflow-hidden rounded-xl border border-[#dfbfbd]/20 bg-white shadow-sm">
    <div className="relative h-48 overflow-hidden bg-[#e2e2e2]">
      <img alt={course.title} className="h-full w-full object-cover grayscale-[20%] transition-all duration-500 hover:grayscale-0" src={course.thumbnailUrl || '/course-covers/classroom-offline.png'} />
      <div className="absolute left-4 top-4 flex gap-2">
        <span className="rounded bg-[#4b0009] px-2 py-1 text-[10px] font-semibold uppercase leading-none tracking-[0.1em] text-white">
          {course.featured ? 'Phổ biến' : course.categoryName || categoryLabels[course.category] || course.category}
        </span>
      </div>
    </div>
    <div className="flex flex-grow flex-col p-5">
      <div className="mb-3 flex items-center gap-2 text-[12px] font-semibold uppercase leading-none tracking-[0.1em] text-[#8c716f]">
        <MaterialIcon name="signal_cellular_alt" className="text-[14px]" /> {course.targetScore || levelLabels[course.level]}
      </div>
      <h4 className="font-headline-md mb-2 text-[24px] font-semibold leading-tight">{course.title}</h4>
      <p className="mb-4 line-clamp-2 text-sm text-[#584140]">{stripRichTextToPlain(course.shortDescription)}</p>
      <div className="mt-auto border-t border-[#dfbfbd]/10 pt-4">
        <div className="mb-4 flex items-center justify-between gap-3">
          <span className="font-bold text-[#4b0009]">{course.duration}</span>
          <span className="text-sm font-bold text-[#4b0009]">{formatCoursePrice(course.price)}</span>
        </div>
        <CourseActionButton className={course.registered ? 'block w-full cursor-pointer rounded bg-[#4b0009] px-4 py-2 text-center text-[14px] font-semibold leading-none tracking-[0.02em] text-white transition-all hover:bg-[#730014]' : `w-full ${detailCourseButtonClassName}`} course={course}>
          {course.registered ? 'Tiếp tục học' : 'Xem chi tiết khóa học'}
        </CourseActionButton>
      </div>
    </div>
  </div>
);

export default PopularCourseCard;
