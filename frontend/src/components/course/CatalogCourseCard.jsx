import CourseActionButton from './CourseActionButton';
import MaterialIcon from './MaterialIcon';
import { categoryLabels } from './courseConstants';
import { formatCoursePrice } from './courseFormatters';

const CatalogCourseCard = ({ course }) => (
  <div className="group flex items-start gap-6 rounded-xl border border-[#dfbfbd]/20 bg-white p-6">
    <div className="h-24 w-24 flex-shrink-0 overflow-hidden rounded-lg bg-[#eeeeed]">
      <img alt={course.title} className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110" src={course.thumbnailUrl} />
    </div>
    <div className="min-w-0 flex-1">
      <span className="text-[10px] font-semibold uppercase text-[#4b0009]">{categoryLabels[course.category] || course.category}</span>
      <h4 className="font-headline-md mt-1 mb-2 text-[24px] font-semibold leading-[1.3]">{course.title}</h4>
      <p className="mb-4 text-sm text-[#584140]">{course.shortDescription}</p>
      <div className="mb-4 flex flex-wrap items-center gap-4 text-xs font-semibold text-[#8c716f]">
        <span className="flex items-center gap-1"><MaterialIcon name="schedule" className="text-sm" /> {course.duration}</span>
        <span className="flex items-center gap-1"><MaterialIcon name="menu_book" className="text-sm" /> {course.totalLessons || 0} bài học</span>
        <span className="flex items-center gap-1 text-[#4b0009]"><MaterialIcon name="payments" className="text-sm" /> {formatCoursePrice(course.price)}</span>
      </div>
      <CourseActionButton className="inline-flex cursor-pointer rounded-lg bg-[#eeeeed] px-4 py-2 text-[14px] font-semibold leading-none tracking-[0.02em] text-[#4b0009] transition-all hover:bg-[#4b0009] hover:text-white" course={course}>
        {course.registered ? 'Đến khóa học' : 'Xem chi tiết khóa học'}
      </CourseActionButton>
    </div>
  </div>
);

export default CatalogCourseCard;
