import React from 'react';
import { Clock, Laptop, MessageSquare, Target, Users } from 'lucide-react';

const courses = [
  {
    tag: 'Nền tảng',
    title: 'IELTS Foundation',
    description: 'Xây dựng nền tảng ngữ pháp, từ vựng và tư duy làm bài cho người mới bắt đầu.',
    meta: [
      [Clock, '10 tuần'],
      [Target, 'Mục tiêu: 5.5+'],
      [Laptop, 'Online / Offline'],
    ],
  },
  {
    tag: 'Tăng tốc',
    title: 'IELTS Intensive 6.5+',
    description: 'Luyện chuyên sâu 4 kỹ năng, tối ưu chiến lược xử lý đề và cải thiện band score.',
    meta: [
      [Clock, '12 tuần'],
      [Target, 'Mục tiêu: 6.5+'],
      [Users, 'Lớp nhỏ kèm sát'],
    ],
  },
  {
    tag: 'Chiến lược',
    title: 'TOEIC 750+',
    description: 'Nắm vững cấu trúc đề, chiến lược làm bài hiệu quả và luyện đề sát thực tế.',
    meta: [
      [Clock, '8 tuần'],
      [Target, 'Mục tiêu: 750+'],
      [Laptop, 'Online / Offline'],
    ],
  },
  {
    tag: 'Giao tiếp',
    title: 'English Communication',
    description: 'Tiếng Anh giao tiếp ứng dụng, tập trung vào phản xạ và tình huống thực tế.',
    meta: [
      [Clock, '8 tuần'],
      [MessageSquare, 'Thực hành nhóm'],
      [Laptop, 'Online 100%'],
    ],
  },
];

const CoursesSection = () => (
  <section id="courses" className="border-y border-[#dfbfbd]/30 bg-white py-20">
    <div className="mx-auto max-w-7xl px-4 md:px-10">
      <div className="mb-14 text-center">
        <span className="mb-4 inline-block rounded bg-[#730014]/10 px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-[#730014]">
          Chương trình nổi bật
        </span>
        <h2 className="font-['Manrope'] text-3xl font-bold text-[#1a1c1c] md:text-4xl">
          Khóa học phù hợp từng mục tiêu
        </h2>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        {courses.map((course) => (
          <article
            key={course.title}
            className="flex h-full flex-col rounded-lg border border-[#dfbfbd]/50 bg-[#f9f9f9] p-6 shadow-[0_4px_24px_rgba(26,28,28,0.04)] transition-shadow hover:shadow-md"
          >
            <span className="mb-4 w-fit rounded bg-[#e2e2e2] px-2 py-1 text-xs font-semibold uppercase tracking-wider text-[#1a1c1c]">
              {course.tag}
            </span>
            <h3 className="mb-2 font-['Manrope'] text-2xl font-semibold text-[#1a1c1c]">
              {course.title}
            </h3>
            <p className="mb-5 flex-grow leading-7 text-[#584140]">{course.description}</p>
            <div className="mb-6 space-y-2">
              {course.meta.map(([Icon, text]) => (
                <div key={text} className="flex items-center gap-2 text-sm text-[#584140]">
                  <Icon size={18} />
                  {text}
                </div>
              ))}
            </div>
            <a
              className="w-full rounded border border-[#730014] py-2.5 text-center text-sm font-semibold text-[#730014] transition-colors hover:bg-[#730014]/5"
              href="#"
            >
              Xem chi tiết
            </a>
          </article>
        ))}
      </div>
    </div>
  </section>
);

export default CoursesSection;
