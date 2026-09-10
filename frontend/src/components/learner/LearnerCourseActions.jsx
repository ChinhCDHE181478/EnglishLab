import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import courseApi from '../../api/courseApi';
import { useLearnerExperience } from '../../context/LearnerExperienceContext';
import { buildCourseDetailPath, buildCourseHomePath, normalizeCourse } from '../../utils/courseModels';
import { removeCourseFromCart } from '../../utils/commerceStore';
import { detailCourseButtonClassName } from '../course/CourseActionButton';

const baseButtonClassName =
  'inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-extrabold transition-all';

const LearnerCourseActions = ({ course, compact = false, className = '', onDetailPage = false }) => {
  const navigate = useNavigate();
  const { addNotification } = useLearnerExperience();
  const [registering, setRegistering] = useState(false);
  const sizeClassName = compact ? 'px-3 py-2 text-xs' : '';
  const completed = Number(course?.progressPercent || 0) >= 100 || course?.enrollmentStatus === 'COMPLETED';
  const isFreeCourse = Number(course?.salePrice ?? course?.price ?? 0) <= 0;

  const handleFreeRegistration = async () => {
    if (!course?.id || registering) return;
    setRegistering(true);
    try {
      const result = await courseApi.registerOnlineCourse(course.id);
      const registeredCourse = normalizeCourse({ ...course, ...result, registered: true });
      await removeCourseFromCart(course.id);
      addNotification({
        title: 'Đăng ký thành công',
        message: `Khóa học ${registeredCourse.title} đã được thêm vào tài khoản của bạn.`,
      });
      navigate(buildCourseHomePath(registeredCourse), { state: { course: registeredCourse } });
    } catch (error) {
      addNotification({
        title: 'Chưa thể đăng ký khóa học',
        message: error?.response?.data?.message || 'Vui lòng thử lại sau.',
        type: 'error',
      });
    } finally {
      setRegistering(false);
    }
  };

  if (course?.registered) {
    return (
      <Link
        className={`${baseButtonClassName} bg-[#4b0009] text-white hover:-translate-y-0.5 hover:bg-[#730014] ${sizeClassName} ${className}`}
        to={buildCourseHomePath(course)}
        state={{ course }}
      >
        {completed ? 'Xem lại khóa học' : 'Tiếp tục học'}
      </Link>
    );
  }

  if (course?.enrollmentAccessCheckFailed) {
    return (
      <button
        className={`${baseButtonClassName} cursor-pointer border border-[#dfbfbd] bg-white text-[#4b0009] hover:bg-[#fff4f5] ${sizeClassName} ${className}`}
        onClick={() => window.location.reload()}
        type="button"
      >
        Tải lại để kiểm tra
      </button>
    );
  }

  if (onDetailPage) {
    if (isFreeCourse) {
      return (
        <button
          className={`${baseButtonClassName} bg-[#4b0009] text-white hover:-translate-y-0.5 hover:bg-[#730014] disabled:cursor-not-allowed disabled:opacity-60 ${sizeClassName} ${className}`}
          disabled={registering}
          onClick={handleFreeRegistration}
          type="button"
        >
          {registering ? 'Đang đăng ký...' : 'Đăng ký miễn phí'}
        </button>
      );
    }
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
