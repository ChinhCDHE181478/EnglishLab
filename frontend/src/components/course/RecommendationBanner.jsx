import { recommendation } from './courseData';

const RecommendationBanner = () => (
  <section className="mb-20">
    <div className="flex flex-col items-center justify-between gap-8 rounded-2xl border border-[#dfbfbd]/30 bg-[#f4f3f3] p-8 transition-shadow hover:shadow-md md:flex-row">
      <div className="flex items-center gap-8">
        <div className="flex h-32 w-32 flex-shrink-0 items-center justify-center rounded-2xl border bg-transparent p-2 shadow-sm">
          <img src={recommendation.image} alt="EnglishLab Mascot" className="h-full w-full object-contain" />
        </div>
        <div>
          <h3 className="font-['Manrope'] text-2xl font-semibold leading-[1.3] text-[#1a1c1c]">
            {recommendation.title}
          </h3>
          <p className="font-['Inter'] leading-[1.6] text-[#584140]">{recommendation.description}</p>
        </div>
      </div>

      <button className="whitespace-nowrap rounded-xl border-2 border-[#4b0009] bg-[#4b0009] px-10 py-4 font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] text-white shadow-lg transition-all hover:bg-transparent hover:text-[#4b0009]">
        Kiểm tra đầu vào
      </button>
    </div>
  </section>
);

export default RecommendationBanner;
