import MaterialIcon from './MaterialIcon';

const PopularCourseCard = ({ course }) => (
  <article className="course-card flex flex-col overflow-hidden rounded-xl border border-[#dfbfbd]/20 bg-white shadow-sm">
    <div className="relative h-48 overflow-hidden bg-[#e2e2e2]">
      <img
        src={course.image}
        alt={course.title}
        className="h-full w-full object-cover grayscale-[20%] transition-all duration-500 hover:grayscale-0"
      />
      {course.badge && (
        <div className="absolute left-4 top-4 flex gap-2">
          <span className={`${course.badgeColor} rounded px-2 py-1 font-['Inter'] text-[10px] font-semibold uppercase leading-none tracking-[0.1em] text-white`}>
            {course.badge}
          </span>
        </div>
      )}
    </div>

    <div className="flex flex-grow flex-col p-5">
      <div className="mb-3 flex items-center gap-2 font-['Inter'] text-[12px] font-semibold uppercase leading-none tracking-[0.1em] text-[#8c716f]">
        <MaterialIcon name={course.icon} className="text-[14px]" />
        {course.level}
      </div>
      <h4 className="mb-2 font-['Manrope'] text-2xl font-semibold leading-tight text-[#1a1c1c]">{course.title}</h4>
      <p className="mb-4 line-clamp-2 text-sm text-[#584140]">{course.description}</p>
      <div className="mt-auto flex items-center justify-between border-t border-[#dfbfbd]/10 pt-4">
        <span className="font-bold text-[#4b0009]">{course.duration}</span>
        <button className="rounded bg-[#eeeeed] px-4 py-2 font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] text-[#4b0009] transition-all hover:bg-[#4b0009] hover:text-white">
          Xem chi tiết
        </button>
      </div>
    </div>
  </article>
);

export default PopularCourseCard;
