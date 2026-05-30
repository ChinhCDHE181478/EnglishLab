import { journeyImage } from './courseData';

const LearningJourney = () => (
  <section className="mb-12">
    <h2 className="mb-8 font-['Manrope'] text-[32px] font-bold leading-[1.2] text-[#4b0009]">
      Hành trình chinh phục 100 ngày
    </h2>

    <div className="relative overflow-hidden rounded-3xl border border-[#dfbfbd]/30 bg-white p-12 shadow-sm">
      <div className="relative mx-auto max-w-5xl py-8">
        <div className="overflow-hidden rounded-xl border border-[#dfbfbd]/20 bg-white p-6 shadow-sm">
          <img
            src={journeyImage}
            alt="100 Day Streak Journey"
            className="h-auto w-full object-contain mix-blend-multiply"
          />
        </div>
      </div>

      <div className="mt-12 text-center">
        <p className="font-['Inter'] text-lg italic leading-[1.6] text-[#584140] opacity-70">
          'Kiên trì mỗi ngày, làm chủ tương lai học thuật.'
        </p>
      </div>
    </div>
  </section>
);

export default LearningJourney;
