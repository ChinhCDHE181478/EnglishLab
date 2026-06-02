const RecommendationBanner = () => (
  <section className="mb-[80px]">
    <div className="flex flex-col items-center justify-between gap-8 rounded-2xl border border-[#dfbfbd]/30 bg-[#f4f3f3] p-8 transition-shadow hover:shadow-md md:flex-row">
      <div className="flex items-center gap-8">
        <div className="flex h-32 w-32 flex-shrink-0 items-center justify-center rounded-2xl border bg-transparent p-2 shadow-sm">
          <img
            alt="EnglishLab Mascot"
            className="h-full w-full object-contain"
            src="https://lh3.googleusercontent.com/aida-public/AB6AXuAEyyOBVWYDtUkn-p2w5L5cYo_0GxWmYeMco1fQtOd7dPegolBmNeMhN5lYmltXDSbYL3ipHLSEAMGppFL12MlSdfO6SUjuBZX2r356VnZG4gw0rUN3NxGJzpqcKk-QMGE3ZJck741FbOxPL7CPSrLMV0a0SDjCHkzZUYn81wJ64mgiaEZmle5WbtIsjZoi-YeDbGGjLiNnq5fCz9f_uFZ_xHEXvMBLKlNsSKVmhVDIR9rS2fBB949U8OpkmXhsdcTeE5Ll2ktpQHQ"
          />
        </div>
        <div>
          <h3 className="font-headline-md text-[24px] font-semibold leading-[1.3] text-[#1a1c1c]">Chưa biết nên bắt đầu từ đâu?</h3>
          <p className="text-base leading-[1.6] text-[#584140]">Làm bài kiểm tra trình độ miễn phí để nhận lộ trình học phù hợp nhất cho riêng bạn.</p>
        </div>
      </div>
      <button className="whitespace-nowrap rounded-xl border-2 border-[#4b0009] bg-[#4b0009] px-10 py-4 text-[14px] font-semibold leading-none tracking-[0.02em] text-white shadow-lg transition-all hover:bg-transparent hover:text-[#4b0009]" type="button">
        Kiểm tra đầu vào
      </button>
    </div>
  </section>
);

export default RecommendationBanner;
