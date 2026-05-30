import { teachers } from './courseData';
import CourseSectionHeading from './CourseSectionHeading';

const FeaturedTeachers = () => (
  <section className="mb-20">
    <CourseSectionHeading title="Đội ngũ giảng viên tâm huyết" centered className="mb-8" />

    <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
      {teachers.map((teacher) => (
        <article
          key={teacher.name}
          className="rounded-2xl border border-[#dfbfbd]/20 bg-white p-6 text-center shadow-sm transition-shadow hover:shadow-md"
        >
          <div className="mx-auto mb-6 h-32 w-32 overflow-hidden rounded-full border-4 border-[#4b0009]/10">
            <img src={teacher.image} alt={teacher.name} className="h-full w-full object-cover" />
          </div>
          <h3 className="mb-1 font-['Manrope'] text-2xl font-semibold leading-[1.3] text-[#1a1c1c]">{teacher.name}</h3>
          <p className="mb-4 font-bold text-[#4b0009]">{teacher.score}</p>
          <p className="mb-6 px-4 text-sm text-[#584140]">{teacher.description}</p>
          <button className="font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] text-[#4b0009] underline">
            Xem hồ sơ giảng viên
          </button>
        </article>
      ))}
    </div>
  </section>
);

export default FeaturedTeachers;
