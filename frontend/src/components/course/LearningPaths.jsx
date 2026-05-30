import { learningPaths } from './courseData';
import CourseSectionHeading from './CourseSectionHeading';
import MaterialIcon from './MaterialIcon';

const LearningPaths = () => (
  <section className="mb-20">
    <CourseSectionHeading title="Lộ trình học tập toàn diện" centered className="mb-8" />

    <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
      {learningPaths.map((path) => (
        <article
          key={path.title}
          className="group relative overflow-hidden rounded-2xl border border-[#dfbfbd]/30 bg-white p-8 shadow-sm transition-all duration-300 hover:-translate-y-2 hover:shadow-xl"
        >
          <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-[#4b0009]/10">
            <MaterialIcon name={path.icon} className="text-4xl text-[#4b0009]" />
          </div>
          <h3 className="mb-4 font-['Manrope'] text-2xl font-semibold leading-[1.3] text-[#1a1c1c]">
            {path.title}
          </h3>
          <p className="mb-6 font-['Inter'] leading-[1.6] text-[#584140]">{path.description}</p>
          <a href="#" className="group/link inline-flex items-center gap-2 font-['Inter'] font-bold text-[#4b0009]">
            Chi tiết lộ trình
            <MaterialIcon name="arrow_forward" className="text-sm transition-transform group-hover/link:translate-x-1" />
          </a>
        </article>
      ))}
    </div>
  </section>
);

export default LearningPaths;
