import { Link } from 'react-router-dom';
import { buildCourseDetailPath, buildCourseWorkspacePath } from '../../utils/courseModels';

const CourseActionButton = ({ course, className = '', children }) => {
  const to = course.registered ? buildCourseWorkspacePath(course) : buildCourseDetailPath(course);

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
