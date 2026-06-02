import CourseFilters from './CourseFilters';
import CatalogCourseCard from './CatalogCourseCard';

const CourseCatalog = ({ courses, keyword, onKeywordChange, onClear, loading }) => (
  <section id="catalog" className="mb-[80px] grid grid-cols-1 gap-6 lg:grid-cols-4">
    <CourseFilters keyword={keyword} onKeywordChange={onKeywordChange} onClear={onClear} />
    <div className="lg:col-span-3">
      {loading ? (
        <div className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-8 text-center text-[#584140]">Đang tải khóa học...</div>
      ) : courses.length === 0 ? (
        <div className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-8 text-center text-[#584140]">Chưa có khóa học phù hợp.</div>
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {courses.map((course) => (
            <CatalogCourseCard key={course.id} course={course} />
          ))}
        </div>
      )}
      <div className="mt-12 flex justify-center">
        <button className="rounded-lg border border-[#8c716f] bg-white px-10 py-3 text-[14px] font-semibold leading-none tracking-[0.02em] text-[#4b0009] transition-all hover:bg-[#eeeeed]" type="button">
          Xem thêm khóa học
        </button>
      </div>
    </div>
  </section>
);

export default CourseCatalog;
