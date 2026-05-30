import MaterialIcon from './MaterialIcon';

const CatalogCourseCard = ({ course }) => (
  <article className="group flex items-start gap-6 rounded-xl border border-[#dfbfbd]/20 bg-white p-6">
    <div className="h-24 w-24 flex-shrink-0 overflow-hidden rounded-lg bg-[#eeeeed]">
      <img
        src={course.image}
        alt={course.title}
        className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
      />
    </div>

    <div>
      <span className="font-['Inter'] text-[10px] font-semibold uppercase leading-none tracking-[0.1em] text-[#4b0009]">
        {course.category}
      </span>
      <h4 className="mb-2 mt-1 font-['Manrope'] text-2xl font-semibold leading-[1.3] text-[#1a1c1c]">
        {course.title}
      </h4>
      <p className="mb-4 text-sm text-[#584140]">{course.description}</p>
      <div className="flex items-center gap-4 text-xs font-semibold text-[#8c716f]">
        <span className="flex items-center gap-1">
          <MaterialIcon name="schedule" className="text-sm" /> {course.duration}
        </span>
        <span className="flex items-center gap-1">
          <MaterialIcon name={course.icon} className="text-sm" /> {course.meta}
        </span>
      </div>
    </div>
  </article>
);

export default CatalogCourseCard;
