import CourseActionButton, { detailCourseButtonClassName } from './CourseActionButton';
import MaterialIcon from './MaterialIcon';
import { categoryLabels } from './courseConstants';
import { formatCoursePrice } from './courseFormatters';
import CourseCommerceActions from '../learner/CourseCommerceActions';
import { formatBandRangeText, formatBandValue } from '../../utils/selfPacedHelpers';
import { stripRichTextToPlain } from '../../utils/lessonRichText';

const GIOI_HAN_TU_MO_TA = 22;

const rutGonMoTa = (value, maxWords = GIOI_HAN_TU_MO_TA) => {
  const text = stripRichTextToPlain(value);
  if (!text) return 'Khóa học đang được cập nhật mô tả ngắn gọn.';

  const words = text.split(/\s+/);
  if (words.length <= maxWords) return text;
  return `${words.slice(0, maxWords).join(' ')}...`;
};

const renderStars = (course) => {
  if (!Number(course.averageRating || 0)) return 'Chưa có đánh giá';
  return `${Number(course.averageRating).toFixed(1)}/5 • ${course.reviewCount || 0} lượt đánh giá`;
};

const scoreProfile = (course) => {
  if (course.category === 'IELTS') {
    return {
      entry: formatBandRangeText(course),
      target: course.targetBand ? `Mục tiêu Band ${formatBandValue(course.targetBand)}` : 'Chưa có band mục tiêu',
    };
  }
  if (course.category === 'TOEIC') {
    return {
      entry: 'Đầu vào theo bài kiểm tra',
      target: course.targetScore ? `Mục tiêu ${course.targetScore} TOEIC` : 'Chưa có điểm mục tiêu',
    };
  }
  return {
    entry: `Trình độ ${String(course.level || 'BEGINNER').toLowerCase()}`,
    target: course.targetScore || rutGonMoTa(course.targetOutcome, 8) || 'Chuẩn đầu ra tiếng Anh',
  };
};

const CatalogCourseCard = ({ course, compact = false }) => {
  const shortDescription = rutGonMoTa(course.shortDescription || course.description);
  const profile = scoreProfile(course);

  return (
    <article className={`group flex h-full flex-col overflow-hidden rounded-[28px] border border-[#dfbfbd]/25 bg-white shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-[0_20px_45px_rgba(75,0,9,0.08)] ${compact ? 'p-4' : 'p-5'}`}>
      <div className={`mb-4 overflow-hidden rounded-[22px] bg-[#f2ecec] ${compact ? 'rounded-[20px]' : ''}`}>
        <img
          alt={course.title}
          className={`w-full object-cover transition-transform duration-500 group-hover:scale-[1.03] ${compact ? 'h-[150px]' : 'h-[180px]'}`}
          src={course.thumbnailUrl || '/course-covers/classroom-offline.png'}
        />
      </div>

      <div className="flex flex-1 flex-col">
        <span className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">
          {course.categoryName || categoryLabels[course.category] || course.category}
        </span>

        <h4 className={`mt-2 line-clamp-2 font-extrabold leading-[1.18] text-[#1f1717] ${compact ? 'min-h-[58px] text-[20px]' : 'min-h-[64px] text-[24px]'}`}>
          {course.title}
        </h4>

        <p className={`mt-3 text-sm text-[#584140] ${compact ? 'min-h-[48px] leading-6' : 'min-h-[64px] leading-7'}`}>
          {shortDescription}
        </p>

        <div className="mt-4 flex flex-wrap gap-2 text-xs font-semibold">
          <span className="rounded-full bg-[#f5f1f1] px-3 py-2 text-[#584140]">{profile.entry}</span>
          <span className="rounded-full bg-[#f5f1f1] px-3 py-2 text-[#584140]">{profile.target}</span>
          {(course.focusSkills || []).slice(0, 2).map((skill) => (
            <span className="rounded-full border border-[#ead9db] bg-white px-3 py-2 text-[#730014]" key={skill}>{skill}</span>
          ))}
        </div>

        <div className="mt-4 grid grid-cols-2 gap-x-4 gap-y-3 text-xs font-semibold text-[#8c716f]">
          <span className="flex items-center gap-1.5"><MaterialIcon name="schedule" className="text-sm" /> {course.duration}</span>
          <span className="flex items-center gap-1.5"><MaterialIcon name="menu_book" className="text-sm" /> {course.totalLessons || 0} bài học</span>
          <span className="flex items-center gap-1.5"><MaterialIcon name="star" className="text-sm" /> {renderStars(course)}</span>
          <span className="flex items-center gap-1.5 text-[#4b0009]"><MaterialIcon name="payments" className="text-sm" /> {formatCoursePrice(course.price)}</span>
        </div>

        <div className="mt-auto flex flex-wrap items-center justify-center gap-3 pt-5">
          <CourseActionButton className={course.registered ? 'inline-flex rounded-2xl bg-[#4b0009] px-4 py-2.5 text-[14px] font-extrabold leading-none tracking-[0.01em] text-white transition-all hover:bg-[#730014]' : detailCourseButtonClassName} course={course}>
            {course.registered ? 'Tiếp tục học' : 'Xem chi tiết khóa học'}
          </CourseActionButton>
          {!course.registered ? <CourseCommerceActions compact course={course} /> : null}
          </div>
      </div>
    </article>
  );
};

export default CatalogCourseCard;
