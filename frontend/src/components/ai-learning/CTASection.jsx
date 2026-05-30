import React, { useState } from 'react';

const CTASection = () => {
  const [glow, setGlow] = useState({ x: '50%', y: '50%', visible: false });

  const handleMouseMove = (event) => {
    const rect = event.currentTarget.getBoundingClientRect();
    setGlow({
      x: `${event.clientX - rect.left}px`,
      y: `${event.clientY - rect.top}px`,
      visible: true,
    });
  };

  return (
    <section
      className="px-4 py-24 md:px-8 md:py-36 lg:py-48"
      onMouseMove={handleMouseMove}
      onMouseLeave={() => setGlow((current) => ({ ...current, visible: false }))}
    >
      <div className="group relative mx-auto max-w-[1400px] overflow-hidden">
        <div
          className="cta-cursor-glow"
          style={{
            '--mouse-x': glow.x,
            '--mouse-y': glow.y,
            opacity: glow.visible ? 1 : 0,
          }}
        />

        <div className="relative flex flex-col items-center overflow-hidden rounded-sm bg-[#1a1c1d] p-10 text-center text-white shadow-2xl sm:p-16 lg:p-40">
          <div className="academic-pattern absolute inset-0 opacity-10" />
          <div className="absolute -right-32 -top-32 h-[600px] w-[600px] rounded-full bg-[#9e001f]/20 blur-[120px] transition-transform duration-1000 group-hover:scale-110" />
          <div className="absolute -bottom-32 -left-32 h-[400px] w-[400px] rounded-full bg-[#7a0018]/30 blur-[100px] transition-transform duration-1000 group-hover:scale-110" />

          <h2 className="relative z-10 mb-10 max-w-5xl font-['Manrope'] text-4xl font-extrabold leading-[1.1] tracking-tight text-white sm:text-5xl lg:mb-14 lg:text-7xl">
            Sẵn sàng bứt phá điểm số cùng lộ trình dành riêng cho bạn.
          </h2>
          <p className="relative z-10 mb-12 max-w-2xl text-lg font-light leading-8 text-white/60 lg:mb-16 lg:text-xl">
            Bắt đầu bằng bài đánh giá năng lực, nhận kế hoạch học tập rõ ràng
            và feedback AI chi tiết để cải thiện từng kỹ năng qua mỗi buổi học.
          </p>
          <div className="relative z-10 flex flex-col gap-5 sm:flex-row">
            <a
              className="btn-hover scholar-gradient px-10 py-5 text-[10px] font-bold uppercase tracking-[0.3em] text-white shadow-[0_15px_30px_rgba(158,0,31,0.4)] sm:px-14 sm:py-6"
              href="#"
            >
              Thi thử miễn phí
            </a>
            <a
              className="btn-hover border border-white/20 bg-white/5 px-10 py-5 text-[10px] font-bold uppercase tracking-[0.3em] text-white backdrop-blur-sm transition-all hover:bg-white hover:text-[#1a1c1d] sm:px-14 sm:py-6"
              href="#"
            >
              Tư vấn lộ trình
            </a>
          </div>
        </div>
      </div>
    </section>
  );
};

export default CTASection;
