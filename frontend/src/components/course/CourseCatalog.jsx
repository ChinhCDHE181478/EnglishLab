import CourseFilters from './CourseFilters';
import CatalogCourseCard from './CatalogCourseCard';

const CourseCatalog = ({
  courses,
  keyword,
  filters,
  onKeywordChange,
  onFilterChange,
  onClear,
  loading,
  currentBand = null,
  categories = [],
}) => (
  <section id="catalog" className="mb-[80px] grid grid-cols-1 gap-6 lg:grid-cols-4">
    <CourseFilters
      keyword={keyword}
      filters={filters}
      onKeywordChange={onKeywordChange}
      onFilterChange={onFilterChange}
      onClear={onClear}
      categories={categories}
    />
    <div className="lg:col-span-3">
      <div className="mb-6 flex flex-col gap-2 rounded-[24px] border border-[#dfbfbd]/25 bg-white px-6 py-5 shadow-sm">
        <p className="text-[12px] font-extrabold uppercase tracking-[0.16em] text-[#730014]">Danh mục học tập</p>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <h2 className="font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Toàn bộ khóa học</h2>
          <span className="text-sm font-semibold text-[#584140]">{courses.length} khóa học</span>
        </div>
      </div>

      {loading ? (
        <div className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-8 text-center text-[#584140]">Đang tải khóa học...</div>
      ) : courses.length === 0 ? (
        <div className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-8 text-center text-[#584140]">
          Không tìm thấy khóa học phù hợp. Hãy thử bỏ bớt bộ lọc để xem thêm khóa học.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
          {courses.map((course) => (
            <CatalogCourseCard key={course.id} course={course} currentBand={currentBand} />
          ))}
        </div>
      )}
    </div>
  </section>
);

export default CourseCatalog;
