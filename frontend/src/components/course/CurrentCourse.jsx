import { currentCourse } from './courseData';

const CurrentCourse = () => (
  <section className="mb-12">
    <h2 className="mb-6 font-['Manrope'] text-[32px] font-bold leading-[1.2] text-[#4b0009]">
      Khóa học hiện tại
    </h2>

    <div className="flex flex-col items-center gap-6 rounded-2xl border border-[#dfbfbd]/30 bg-white p-6 shadow-sm md:flex-row">
      <div className="h-24 w-24 flex-shrink-0 overflow-hidden rounded-lg bg-[#eeeeed]">
        <img src={currentCourse.image} alt="IELTS Intensive" className="h-full w-full object-cover" />
      </div>

      <div className="w-full flex-grow">
        <div className="mb-2 flex items-center justify-between gap-4">
          <h3 className="font-['Manrope'] text-2xl font-semibold leading-[1.3] text-[#1a1c1c]">
            {currentCourse.title}
          </h3>
          <span className="font-bold text-[#4b0009]">{currentCourse.progress}% hoàn thành</span>
        </div>

        <div className="mb-4 h-3 w-full rounded-full bg-[#eeeeed]">
          <div className="h-3 rounded-full bg-[#4b0009]" style={{ width: `${currentCourse.progress}%` }} />
        </div>

        <p className="font-['Inter'] text-sm text-[#584140]">Bài học tiếp theo: {currentCourse.nextLesson}</p>
      </div>

      <button className="whitespace-nowrap rounded-lg bg-[#4b0009] px-8 py-3 font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] text-white transition-all hover:bg-[#9E001F]">
        Học tiếp
      </button>
    </div>
  </section>
);

export default CurrentCourse;
