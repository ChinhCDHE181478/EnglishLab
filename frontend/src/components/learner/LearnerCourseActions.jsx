import { Link } from 'react-router-dom';
import { buildCourseDetailPath, buildCourseWorkspacePath } from '../../utils/courseModels';
import { detailCourseButtonClassName } from '../course/CourseActionButton';

const baseButtonClassName =
  'inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-extrabold transition-all';

const LearnerCourseActions = ({ course, compact = false, className = '', onDetailPage = false }) => {
  const sizeClassName = compact ? 'px-3 py-2 text-xs' : '';

  if (course?.registered) {
    return (
      <Link
        className={`${baseButtonClassName} bg-[#4b0009] text-white hover:-translate-y-0.5 hover:bg-[#730014] ${sizeClassName} ${className}`}
        to={buildCourseWorkspacePath(course)}
        state={{ course, workspaceMode: 'learn' }}
      >
        Tiếp tục học
      </Link>
    );
  }

  if (onDetailPage) {
    return (
      <Link
        className={`${baseButtonClassName} bg-[#4b0009] text-white hover:-translate-y-0.5 hover:bg-[#730014] ${sizeClassName} ${className}`}
        to="/checkout"
        state={{ course, from: buildCourseDetailPath(course) }}
      >
        Mua khóa học
      </Link>
    );
  }

  return (
    <Link
      className={`${detailCourseButtonClassName} hover:-translate-y-0.5 ${sizeClassName} ${className}`}
      to={buildCourseDetailPath(course)}
      state={{ course }}
    >
      Xem chi tiết khóa học
    </Link>
  );
};

export default LearnerCourseActions;
