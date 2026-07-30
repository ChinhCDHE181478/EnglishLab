import { Link } from 'react-router-dom';
import { formatCoursePrice, isPaidCourse } from '../course/courseFormatters';

const CoursePurchaseCard = ({ course, isAuthenticated, purchasing, onPurchase }) => {
  const ctaLabel = !isAuthenticated
    ? 'Đăng nhập để mua khóa học'
    : purchasing
      ? 'Đang xử lý...'
      : course.registered
        ? 'Đến khóa học'
        : isPaidCourse(course)
          ? 'Mua khóa học'
          : 'Đăng ký miễn phí';

  return (
    <aside className="sticky top-24 rounded-[28px] border border-[#dfbfbd]/30 bg-white p-6 shadow-[0_24px_60px_rgba(43,40,40,0.08)]">
      <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Truy cập ngay</p>
      <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">{formatCoursePrice(course.price)}</h2>
      <div className="mt-5 space-y-4 rounded-2xl border border-[#dfbfbd]/20 bg-[#fcf8f7] p-4 text-sm text-[#584140]">
        <div className="flex items-start justify-between gap-4">
          <span>Hình thức</span>
          <strong className="text-right text-[#2b2828]">Tự học 100%</strong>
        </div>
        <div className="flex items-start justify-between gap-4">
          <span>Truy cập</span>
          <strong className="text-right text-[#2b2828]">Ngay sau khi mua</strong>
        </div>
        <div className="flex items-start justify-between gap-4">
          <span>Tài liệu</span>
          <strong className="text-right text-[#2b2828]">Video + PDF + bài tập</strong>
        </div>
      </div>
      {course.registered ? (
        <Link className="mt-6 flex w-full items-center justify-center rounded-2xl bg-[#8a0018] px-5 py-4 text-center text-sm font-extrabold text-white transition-all hover:-translate-y-0.5 hover:bg-[#650012]" to={`/courses/${course.slug}/home`} state={{ course }}>
          Đến khóa học
        </Link>
      ) : isAuthenticated ? (
        <button
          className="mt-6 flex w-full cursor-pointer items-center justify-center rounded-2xl bg-[#8a0018] px-5 py-4 text-center text-sm font-extrabold text-white transition-all hover:-translate-y-0.5 hover:bg-[#650012] disabled:cursor-not-allowed disabled:opacity-60"
          disabled={purchasing}
          onClick={onPurchase}
          type="button"
        >
          {ctaLabel}
        </button>
      ) : (
        <Link className="mt-6 flex w-full items-center justify-center rounded-2xl bg-[#8a0018] px-5 py-4 text-center text-sm font-extrabold text-white transition-all hover:-translate-y-0.5 hover:bg-[#650012]" to="/login" state={{ from: `/courses/${course.slug}` }}>
          {ctaLabel}
        </Link>
      )}
      <p className="mt-4 text-sm leading-6 text-[#584140]">
        Khóa học trả phí sẽ được kích hoạt sau khi hệ thống xác nhận thanh toán; khóa miễn phí có thể ghi danh ngay.
      </p>
    </aside>
  );
};

export default CoursePurchaseCard;
