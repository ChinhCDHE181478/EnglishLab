import { Link } from 'react-router-dom';
import { formatCoursePrice, isPaidCourse } from '../course/courseFormatters';

const formatBandRange = (course) => {
  if (course.recommendedCurrentBandMin && course.recommendedCurrentBandMax) {
    return `Band ${course.recommendedCurrentBandMin} - ${course.recommendedCurrentBandMax}`;
  }
  return course.level || 'Tự học';
};

const statItems = (course) => [
  { label: 'Band đầu vào', value: formatBandRange(course) },
  { label: 'Target band', value: course.targetBand ? `Band ${course.targetBand}` : course.targetScore || course.categoryName || course.category || 'Online' },
  { label: 'Thời lượng', value: course.duration || 'Tự học linh hoạt' },
  { label: 'Bài học', value: `${course.totalLessons || 0} bài` },
  { label: 'Hình thức', value: 'Tự học 100%' },
];

const CourseDetailHero = ({ course, isAuthenticated, purchasing, onPurchase }) => {
  const ctaLabel = !isAuthenticated
    ? 'Đăng nhập để mua khóa học'
    : purchasing
      ? 'Đang xử lý...'
      : course.registered
        ? 'Đến khóa học'
        : isPaidCourse(course)
          ? `Mua khóa học - ${formatCoursePrice(course.price)}`
          : 'Đăng ký miễn phí';

  return (
    <section className="relative overflow-hidden rounded-[32px] border border-[#dfbfbd]/30 bg-[radial-gradient(circle_at_top_left,_rgba(139,0,20,0.08),_transparent_45%),linear-gradient(135deg,_#fffaf9,_#ffffff)] p-8 md:p-12">
      <div className="mb-5 flex flex-wrap items-center gap-2 text-xs font-bold uppercase tracking-[0.14em] text-[#8c716f]">
        <Link className="hover:text-[#4b0009]" to="/courses">Khóa học</Link>
        <span>/</span>
        <span>{course.categoryName || course.category || 'Online'}</span>
      </div>
      <div className="grid gap-8 lg:grid-cols-[1.3fr_0.7fr]">
        <div>
          <span className="mb-4 inline-flex rounded-full bg-[#4b0009] px-4 py-2 text-[11px] font-bold uppercase tracking-[0.14em] text-white">
            Tự học online
          </span>
          <h1 className="font-['Manrope'] text-4xl font-extrabold tracking-tight text-[#2b2828] md:text-6xl">{course.title}</h1>
          <p className="mt-5 max-w-3xl text-base leading-8 text-[#584140] md:text-lg">
            {course.description || course.shortDescription}
          </p>
          {course.targetOutcome ? (
            <div className="mt-5 rounded-2xl border border-[#8a0018]/15 bg-white/85 p-4 text-sm leading-7 text-[#584140]">
              <span className="font-extrabold text-[#4b0009]">Target đầu ra:</span> {course.targetOutcome}
            </div>
          ) : null}
          {course.learningPathName ? (
            <div className="mt-3 inline-flex rounded-full bg-[#fff0f1] px-4 py-2 text-xs font-extrabold uppercase tracking-[0.12em] text-[#8a0018]">
              Lộ trình: {course.learningPathName} · Step {course.learningPathOrder || 1}
            </div>
          ) : null}
          {course.recommendedNextCourseSlug ? (
            <div className="mt-3 rounded-2xl border border-[#dfbfbd]/25 bg-white/85 p-4 text-sm leading-7 text-[#584140]">
              <span className="font-extrabold text-[#4b0009]">Recommended next course:</span> {course.recommendedNextCourseSlug}
            </div>
          ) : null}
          <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
            {statItems(course).map((item) => (
              <div key={item.label} className="rounded-2xl border border-[#dfbfbd]/25 bg-white/80 p-4 shadow-sm">
                <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">{item.label}</p>
                <p className="mt-2 text-sm font-extrabold text-[#2b2828]">{item.value}</p>
              </div>
            ))}
          </div>
          <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
            {course.registered ? (
              <Link className="inline-flex cursor-pointer items-center justify-center rounded-2xl bg-[#8a0018] px-8 py-4 text-sm font-extrabold text-white transition-all hover:-translate-y-0.5 hover:bg-[#650012]" to={`/courses/${course.slug}/learn`} state={{ course }}>
                {ctaLabel}
              </Link>
            ) : isAuthenticated ? (
              <button
                className="inline-flex cursor-pointer items-center justify-center rounded-2xl bg-[#8a0018] px-8 py-4 text-sm font-extrabold text-white transition-all hover:-translate-y-0.5 hover:bg-[#650012] disabled:cursor-not-allowed disabled:opacity-60"
                disabled={purchasing}
                onClick={onPurchase}
                type="button"
              >
                {ctaLabel}
              </button>
            ) : (
              <Link className="inline-flex cursor-pointer items-center justify-center rounded-2xl bg-[#8a0018] px-8 py-4 text-sm font-extrabold text-white transition-all hover:-translate-y-0.5 hover:bg-[#650012]" to="/login" state={{ from: `/courses/${course.slug}` }}>
                {ctaLabel}
              </Link>
            )}
            <span className="hidden h-px w-6 bg-[#dfbfbd] sm:block" />
            <span className="inline-flex items-center gap-2 text-sm font-bold text-[#584140]">
              <span className="material-symbols-outlined text-[18px] text-[#8a0018]">groups</span>
              {Number(course.enrollmentCount || 0).toLocaleString('vi-VN')} người đã tham gia
            </span>
          </div>
        </div>
        <div className="overflow-hidden rounded-[28px] border border-[#dfbfbd]/25 bg-white shadow-sm">
          <img alt={course.title} className="h-full min-h-[290px] w-full object-cover" src={course.thumbnailUrl} />
        </div>
      </div>
    </section>
  );
};

export default CourseDetailHero;
