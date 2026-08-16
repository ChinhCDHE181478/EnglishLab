import React from 'react';
import { UserRound } from 'lucide-react';

const teachers = [
  {
    name: 'Cô Minh Anh',
    role: 'Chuyên gia Writing & Speaking',
    badges: ['IELTS 8.5', 'CELTA'],
    image:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAov6XU0-Ek2X8QXH7IhOe0VnAeSDMp-olCkxodsEwgxIk3voBqMlVHF5uOp_rSX9WbpS7gmI8Vy93Zc5W5sw4eyuUbg3J07MYKSV9QD0RUNyfcMU74Ic9ER2Qe52ipFB6aT935d4x51GBH5U9IB-uJdzPYoE3Gerk88_uzPBM0g5sxKmjDY9EKGIRF5vtEOEdS9xV8L0tRCjag-Vt9wBzluJ7myczOnYQZz2XSa2Y6ZHeWdwyJQ_-LmDZtbaJCAjmaFkeuWikoycE',
    quote:
      'Học ngôn ngữ không chỉ là ghi nhớ, mà là cách bạn tư duy và diễn đạt thế giới quan của mình một cách tinh tế.',
  },
  {
    name: 'Thầy Quốc Huy',
    role: 'Chuyên gia Listening & Reading',
    badges: ['TOEIC 990', 'TESOL'],
    image:
      'https://lh3.googleusercontent.com/aida-public/AB6AXuAkS6XWnEjn8qM-_k2VEbkPXaenPWZW4-cY-F-OwjOkUVgfJbce2JtQgU339oaJRAtyKXPJ8qraTVAk-YD7Vx3xOmJfPenuN1KNi99PUNqUaIUDslP8MVMxFG_9EYX_H3OBacDQrmCDcdXlJ17fdrC854zuQtbnBgJI_29brUv7X2lVnaXPbYvYlLb8L00RJmh_E7vYlGs6Hf81gc6PHaUnXzMKRQ6nmq-u__ivRGkw88TZYuPeOcsLuoXpWPBIWgbq76uuqUBeeF8',
    quote:
      'Chiến thuật làm bài tốt bắt nguồn từ một nền tảng ngữ pháp và từ vựng vững chắc, không có đường tắt cho sự xuất sắc.',
  },
  {
    name: 'Cô Thu Hà',
    role: 'Chuyên gia Foundation',
    badges: ['IELTS 8.0', 'Master TESOL'],
    image:
      'https://images.openai.com/static-rsc-4/4EeNof_ty-AFdtn_x-ZLCAGUEpUE9NU1pll3Szub2y5bSZyuJuWC2R_zzXd4DVNvy_BOmI2xb1NamotgmXp7ksSA0Mjz3UNtVSWLnF7kLIm2l3SLAEAE0xAf4IIKj6LKA_a04mTRe8Og_GAr7eKINhULk-V7lg8XAyv2WcRJNT0x4oeleOt1K6aWhBUSzEMk?purpose=fullsize',
    quote:
      'Nhiệm vụ của tôi là giúp bạn vượt qua rào cản sợ hãi ban đầu, biến tiếng Anh thành công cụ giao tiếp tự nhiên.',
  },
];

const TeacherImage = ({ teacher }) => {
  const [hasError, setHasError] = React.useState(false);

  if (!teacher.image || hasError) {
    return (
      <div className="flex h-full items-center justify-center">
        <UserRound className="text-[#584140]/50" size={76} />
      </div>
    );
  }

  return (
    <img
      alt={teacher.name}
      className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
      onError={() => setHasError(true)}
      src={teacher.image}
    />
  );
};

const TeachersSection = () => (
  <section className="mx-auto max-w-7xl px-4 py-20 md:px-10">
    <div className="mb-16 text-center">
      <h2 className="mb-4 font-['Manrope'] text-3xl font-bold text-[#1a1c1c] md:text-4xl">
        Đội ngũ giảng viên tinh hoa
      </h2>
      <p className="mx-auto max-w-2xl text-lg leading-8 text-[#584140]">
        Các chuyên gia ngôn ngữ với trình độ học thuật xuất sắc và triết lý
        giảng dạy truyền cảm hứng.
      </p>
    </div>

    <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
      {teachers.map((teacher) => (
        <article
          key={teacher.name}
          className="group relative cursor-pointer overflow-hidden rounded-xl border border-[#dfbfbd]/30 bg-white shadow-sm transition-all duration-300 hover:shadow-lg"
        >
          <div className="aspect-[3/4] overflow-hidden bg-[#e2e2e2]">
            <TeacherImage teacher={teacher} />
            <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent opacity-80 transition-opacity group-hover:opacity-90" />
          </div>

          <div className="absolute bottom-0 left-0 w-full translate-y-8 p-6 transition-transform duration-300 group-hover:translate-y-0">
            <div className="mb-2 flex flex-wrap items-center gap-2">
              {teacher.badges.map((badge, index) => (
                <span
                  key={badge}
                  className={`rounded px-2 py-1 text-[10px] font-semibold uppercase ${
                    index === 0
                      ? 'bg-[#730014] text-white'
                      : 'border border-white/20 bg-white/20 text-white backdrop-blur-sm'
                  }`}
                >
                  {badge}
                </span>
              ))}
            </div>
            <h3 className="font-['Manrope'] text-2xl font-semibold text-white">{teacher.name}</h3>
            <p className="mb-4 text-white/80">{teacher.role}</p>
            <p className="border-t border-white/20 pt-4 text-sm leading-6 text-white/90 opacity-0 transition-opacity delay-100 duration-300 group-hover:opacity-100">
              "{teacher.quote}"
            </p>
          </div>
        </article>
      ))}
    </div>
  </section>
);

export default TeachersSection;
