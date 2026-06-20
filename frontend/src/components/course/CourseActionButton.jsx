import { Link } from 'react-router-dom';
import { buildCourseDetailPath, buildCourseHomePath } from '../../utils/courseModels';

export const detailCourseButtonClassName =
  'inline-flex items-center justify-center rounded-xl bg-[#eeeeed] px-5 py-3 text-[14px] font-extrabold leading-none tracking-[0.01em] text-[#5a0b12] transition-all hover:-translate-y-0.5 hover:bg-[#4b0009] hover:text-white';

const CourseActionButton = ({ course, className = '', children }) => {
  const to = course.registered ? buildCourseHomePath(course) : buildCourseDetailPath(course);

  return (
    <Link
      className={className}
      to={to}
      state={{ course }}
    >
      {children || (course.registered ? 'Đến khóa học' : 'Xem chi tiết khóa học')}
    </Link>
  );
};

export default CourseActionButton;
