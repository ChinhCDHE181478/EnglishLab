import MaterialIcon from './MaterialIcon';
import PopularCourseCard from './PopularCourseCard';

const PopularCourses = ({ courses }) => (
  <section id="popular-courses" className="mb-[80px]">
    <div className="mb-8 flex items-end justify-between">
      <div>
        <span className="mb-2 block text-[12px] font-semibold uppercase leading-none tracking-[0.1em] text-[#4b0009]">Top lựa chọn</span>
        <h2 className="font-headline-lg text-[32px] font-bold leading-[1.2]">Khóa học được xem nhiều</h2>
      </div>
      <a className="group flex items-center gap-2 text-[14px] font-semibold leading-none tracking-[0.02em] text-[#4b0009]" href="#catalog">
        <span className="group-hover:underline">Xem tất cả</span> <MaterialIcon name="arrow_forward" className="text-sm" />
      </a>
    </div>
    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
      {courses.map((course) => (
        <PopularCourseCard key={course.id} course={course} />
      ))}
    </div>
  </section>
);

export default PopularCourses;
