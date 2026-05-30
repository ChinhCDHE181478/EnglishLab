import React from 'react';
import { Globe2, Share2 } from 'lucide-react';

const footerColumns = [
  {
    title: 'Khóa học',
    links: ['IELTS Foundation', 'IELTS Intensive 6.5+', 'TOEIC Mastery', 'English Communication'],
  },
  {
    title: 'EnglishLab',
    links: ['Giáo viên', 'Lịch khai giảng', 'Về trung tâm', 'Liên hệ'],
  },
  {
    title: 'Pháp lý',
    links: ['Privacy Policy', 'Terms of Service', 'Cookie Policy'],
  },
];

const Footer = () => (
  <footer className="border-t border-black/5 bg-white py-20 text-[#1a1c1c]">
    <div className="mx-auto grid max-w-7xl grid-cols-1 gap-12 px-4 md:grid-cols-4 md:px-10">
      <div className="space-y-6 md:col-span-1">
        <div className="font-['Manrope'] text-3xl font-black tracking-tight">EnglishLab</div>
        <p className="max-w-sm text-sm font-light leading-7 text-[#5f5e5e]">
          Trung tâm luyện thi học thuật chuẩn quốc tế, kết hợp chuyên gia và AI
          để cá nhân hóa lộ trình học.
        </p>
        <div className="flex gap-4">
          <a
            className="btn-hover flex h-10 w-10 items-center justify-center rounded-full border border-black/10 text-[#5f5e5e] transition-all hover:border-[#730014] hover:bg-[#730014] hover:text-white"
            href="#"
            aria-label="Website"
          >
            <Globe2 size={16} />
          </a>
          <a
            className="btn-hover flex h-10 w-10 items-center justify-center rounded-full border border-black/10 text-[#5f5e5e] transition-all hover:border-[#730014] hover:bg-[#730014] hover:text-white"
            href="#"
            aria-label="Share"
          >
            <Share2 size={16} />
          </a>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-8 md:col-span-3 md:grid-cols-3">
        {footerColumns.map((column) => (
          <div key={column.title} className="space-y-5">
            <h4 className="text-[10px] font-bold uppercase tracking-widest text-[#1a1c1c]">
              {column.title}
            </h4>
            <nav className="flex flex-col gap-4 text-sm font-medium text-[#5f5e5e]">
              {column.links.map((link) => (
                <a
                  key={link}
                  className="transition-all hover:translate-x-1 hover:text-[#730014]"
                  href="#"
                >
                  {link}
                </a>
              ))}
            </nav>
          </div>
        ))}
      </div>
    </div>

    <div className="mx-auto mt-16 flex max-w-7xl flex-col items-center justify-between gap-4 border-t border-black/5 px-4 pt-8 text-[9px] font-bold uppercase tracking-widest text-[#5f5e5e]/50 md:flex-row md:px-10">
      <span>© 2024 EnglishLab Academic Center. All rights reserved.</span>
      <span className="font-black text-[#730014]">Scholarly Precision.</span>
    </div>
  </footer>
);

export default Footer;
