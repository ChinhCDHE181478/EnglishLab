import { catalogCourses } from './courseData';
import CatalogCourseCard from './CatalogCourseCard';
import CourseFilters from './CourseFilters';

const CourseCatalog = () => (
  <section className="mb-20 grid grid-cols-1 gap-6 lg:grid-cols-4">
    <CourseFilters />

    <div className="lg:col-span-3">
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        {catalogCourses.map((course) => (
          <CatalogCourseCard key={course.title} course={course} />
        ))}
      </div>

      <div className="mt-12 flex justify-center">
        <button className="rounded-lg border border-[#8c716f] bg-white px-10 py-3 font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] text-[#4b0009] transition-all hover:bg-[#eeeeed]">
          Xem thêm khóa học
        </button>
      </div>
    </div>
  </section>
);

export default CourseCatalog;
