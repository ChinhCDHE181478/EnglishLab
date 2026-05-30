import { popularCourses } from './courseData';
import CourseSectionHeading from './CourseSectionHeading';
import MaterialIcon from './MaterialIcon';
import PopularCourseCard from './PopularCourseCard';

const PopularCourses = () => (
  <section className="mb-20">
    <div className="mb-8 flex items-end justify-between gap-6">
      <CourseSectionHeading eyebrow="Top Lựa Chọn" title="Khóa học phổ biến nhất" />
      <a
        href="#"
        className="flex items-center gap-2 font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] text-[#4b0009] hover:underline"
      >
        Xem tất cả <MaterialIcon name="arrow_forward" className="text-sm" />
      </a>
    </div>

    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
      {popularCourses.map((course) => (
        <PopularCourseCard key={course.title} course={course} />
      ))}
    </div>
  </section>
);

export default PopularCourses;
