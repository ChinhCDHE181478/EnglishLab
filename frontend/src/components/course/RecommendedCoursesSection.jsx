import CatalogCourseCard from './CatalogCourseCard';
import BrandLoadingState from '../ui/BrandLoadingState';

const RecommendedCoursesSection = ({ courses = [], loading = false, error = '', hasCurrentBand = false, currentBand = null, onRetry }) => {
  const daCoBandHienTai = hasCurrentBand || Number(currentBand) > 0;

  return (
    <section className="mb-[88px]">
      <div className="mb-6 flex flex-col gap-2">
        <h2 className="font-['Manrope'] text-[32px] font-extrabold leading-[1.2] text-[#4b0009]">Khóa học phù hợp với bạn</h2>
        <p className="text-sm leading-7 text-[#584140]">
          {daCoBandHienTai
            ? 'Gợi ý dựa trên trình độ hiện tại, mục tiêu đầu ra và dữ liệu khóa học hiện có.'
            : 'Hãy cập nhật trình độ hiện tại để nhận gợi ý khóa học chính xác hơn. Trong lúc này, EnglishLab vẫn hiển thị các khóa học nổi bật.'}
        </p>
      </div>

      {loading ? (
        <BrandLoadingState compact className="rounded-[28px]" message="Đang tải danh sách gợi ý..." />
      ) : error ? (
        <div className="rounded-[28px] border border-[#f0d4d7] bg-white p-8 text-center text-[#93000a]">
          <p>{error}</p>
          <button className="mt-4 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]" onClick={onRetry} type="button">
            Thử lại
          </button>
        </div>
      ) : courses.length ? (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
          {courses.map((course) => (
            <div key={course.id} className="max-w-[380px] space-y-4">
              <CatalogCourseCard compact course={course} />
              <div className="rounded-2xl border border-[#dfbfbd]/20 bg-white px-4 py-3 text-center text-sm font-semibold text-[#584140]">
                {course.recommendationReason}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-8 text-center text-[#584140]">
          Hiện chưa có khóa học phù hợp với trình độ của bạn.
        </div>
      )}
    </section>
  );
};

export default RecommendedCoursesSection;
