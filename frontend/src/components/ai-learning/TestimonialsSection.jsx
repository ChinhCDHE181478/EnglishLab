import React from 'react';
import { Star, StarHalf } from 'lucide-react';

const testimonials = [
  {
    name: 'Hoàng Nam',
    result: 'IELTS 7.5 Overall',
    initial: 'H',
    rating: 5,
    text:
      'Hệ thống AI chấm bài Writing thực sự là một bước đột phá. Mình có thể viết và nhận feedback bất cứ lúc nào, giúp mình tự tin hơn hẳn khi thi thật.',
  },
  {
    name: 'Linh Chi',
    result: 'TOEIC 850',
    initial: 'L',
    rating: 5,
    text:
      'Lộ trình học rất rõ ràng. Thầy cô vô cùng tâm huyết và theo sát từng học viên. Khóa TOEIC Intensive đã giúp mình đạt đủ điểm ra trường chỉ sau 2 tháng.',
  },
  {
    name: 'Trần Việt',
    result: 'IELTS 6.5 Overall',
    initial: 'T',
    rating: 4.5,
    text:
      'Môi trường học tập chuyên nghiệp, tài liệu được cập nhật liên tục. Mình thích nhất là các buổi mock test tổ chức y như thi thật.',
  },
];

const Rating = ({ value }) => (
  <div className="mb-4 flex items-center gap-1 text-yellow-500">
    {Array.from({ length: 5 }).map((_, index) => {
      if (value - index === 0.5) {
        return <StarHalf key={index} size={20} fill="currentColor" />;
      }

      return (
        <Star
          key={index}
          size={20}
          fill={index < value ? 'currentColor' : 'none'}
        />
      );
    })}
  </div>
);

const TestimonialsSection = () => (
  <section className="border-y border-[#dfbfbd]/30 bg-[#f9f9f9] py-20">
    <div className="mx-auto max-w-7xl px-4 md:px-10">
      <div className="mb-12 text-center">
        <h2 className="mb-4 font-['Manrope'] text-3xl font-bold text-[#1a1c1c] md:text-4xl">
          Học viên nói gì về EnglishLab
        </h2>
        <p className="mx-auto max-w-2xl text-lg leading-8 text-[#584140]">
          Những câu chuyện thành công thực tế từ cộng đồng học viên đã chinh
          phục mục tiêu.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
        {testimonials.map((item, index) => (
          <article
            key={item.name}
            className="animate-float-up flex flex-col rounded-xl border border-[#dfbfbd]/30 bg-white p-6 opacity-0 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg"
            style={{ animationFillMode: 'forwards', animationDelay: `${index * 100}ms` }}
          >
            <Rating value={item.rating} />
            <p className="mb-6 flex-grow italic leading-7 text-[#584140]">"{item.text}"</p>
            <div className="flex items-center gap-3 border-t border-[#dfbfbd]/30 pt-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#730014] font-bold text-white">
                {item.initial}
              </div>
              <div>
                <div className="font-semibold text-[#1a1c1c]">{item.name}</div>
                <div className="text-xs text-[#584140]">{item.result}</div>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  </section>
);

export default TestimonialsSection;
