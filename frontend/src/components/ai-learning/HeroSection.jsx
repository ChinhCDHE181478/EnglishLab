import React from 'react';
import mascotImage from '../../assets/englishlab-mascot.png';

const HeroSection = () => (
  <section className="mx-auto grid max-w-7xl grid-cols-1 items-center gap-12 px-4 py-16 md:px-10 md:py-20 lg:grid-cols-2">
    <div className="space-y-6">
      <span className="inline-block rounded bg-[#730014]/10 px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-[#730014]">
        Trung tâm luyện thi IELTS & TOEIC
      </span>
      <h1 className="font-['Manrope'] text-4xl font-bold leading-tight tracking-tight text-[#1a1c1c] md:text-5xl">
        Chinh phục IELTS/TOEIC cùng chuyên gia và AI.
      </h1>
      <p className="max-w-xl text-lg leading-8 text-[#584140]">
        Lộ trình học cá nhân hóa kết hợp công nghệ AI chấm chữa chi tiết. Cam
        kết đầu ra bằng văn bản.
      </p>
      <div className="flex flex-col gap-4 pt-4 sm:flex-row">
        <a
          className="rounded bg-[#730014] px-8 py-4 text-center text-sm font-semibold text-white shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:bg-[#4b0009]"
          href="#"
        >
          Thi thử ngay
        </a>
        <a
          className="rounded border border-[#8c716f] px-8 py-4 text-center text-sm font-semibold text-[#1a1c1c] transition-colors hover:bg-[#e2e2e2]"
          href="#courses"
        >
          Khám phá khóa học
        </a>
      </div>
    </div>

    <div className="relative flex h-[500px] items-center justify-center overflow-visible md:h-[620px]">
      <div className="absolute inset-0 origin-center scale-[0.85] overflow-hidden rounded-full bg-[#e8e8e8] opacity-80 shadow-xl mix-blend-multiply" />
      <div className="absolute inset-x-8 bottom-10 h-24 rounded-full bg-[#730014]/10 blur-3xl" />
      <img
        alt="EnglishLab mascot"
        className="animate-float-up relative z-10 max-h-full w-[112%] max-w-none translate-y-5 object-contain drop-shadow-2xl md:w-[118%]"
        src={mascotImage}
      />
    </div>
  </section>
);

export default HeroSection;
