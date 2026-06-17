const FinalCourseCta = () => (
  <section id="enrollment" className="mb-12">
    <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-[#4b0009] to-[#9E001F] p-12 text-center text-white shadow-xl">
      <div className="pointer-events-none absolute inset-0 opacity-10">
        <div className="absolute right-0 top-0 h-96 w-96 -translate-y-1/2 translate-x-1/2 rounded-full bg-white" />
        <div className="absolute bottom-0 left-0 h-64 w-64 -translate-x-1/2 translate-y-1/2 rounded-full bg-white" />
      </div>
      <h2 className="font-headline-lg relative z-10 mb-6 text-[36px] font-bold leading-[1.2] tracking-[-0.02em] md:text-[32px]">Bạn chưa biết nên chọn khóa nào?</h2>
      <p className="relative z-10 mx-auto mb-10 max-w-2xl text-lg leading-[1.6] text-white opacity-90">
        Hãy để chuyên gia của EnglishLab hỗ trợ bạn chọn kế hoạch học cá nhân hóa hoàn toàn miễn phí.
      </p>
      <div className="relative z-10 flex flex-col justify-center gap-4 sm:flex-row">
        <button className="rounded-lg bg-white px-10 py-4 font-bold text-[#4b0009] shadow-lg transition-all hover:-translate-y-1 hover:bg-[#eeeeed]" type="button">
          Nhận tư vấn miễn phí
        </button>
        <button className="rounded-lg border-2 border-white bg-transparent px-10 py-4 font-bold text-white transition-all hover:bg-white hover:text-[#4b0009]" type="button">
          Làm bài kiểm tra đầu vào
        </button>
      </div>
    </div>
  </section>
);

export default FinalCourseCta;
