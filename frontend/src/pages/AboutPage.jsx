import React from 'react';
import { Link } from 'react-router-dom';
import {
  BookOpen,
  Calendar,
  CheckCircle2,
  ChevronRight,
  Clock,
  FileText,
  GraduationCap,
  Layers,
  Layout,
  MessageSquare,
  Target,
  UserCheck,
  Video,
} from 'lucide-react';
import LearnerPageShell from '../components/learner/LearnerPageShell';

export default function AboutPage() {
  const platformFeatures = [
    {
      title: 'Giáo trình & Unit bài học',
      description: 'Mỗi khóa học được thiết kế thành các Unit bài học rõ ràng, bao gồm tài nguyên giảng dạy, bài tập luyện tập, bài đánh giá và bộ thẻ Flashcards học từ vựng.',
      icon: BookOpen,
      badge: 'Nội dung chuẩn hóa',
    },
    {
      title: 'Lớp học Virtual & Tại cơ sở',
      description: 'Hỗ trợ hai hình thức học linh hoạt: Lớp học trực tuyến Virtual (tham gia dễ dàng qua Lark / Google Meet) và lớp học trực tiếp tại cơ sở với lịch học đồng bộ.',
      icon: Video,
      badge: 'Linh hoạt thời gian',
    },
    {
      title: 'Theo dõi tiến độ thực tế',
      description: 'Học viên dễ dàng theo dõi tỷ lệ hoàn thành bài học, lịch sử điểm danh, kết quả bài làm và phản hồi trực tiếp từ giảng viên chuyên môn.',
      icon: Target,
      badge: 'Minh bạch kết quả',
    },
    {
      title: 'Kho học liệu & Ngân hàng đề',
      description: 'Thư viện tài liệu tham khảo, tài liệu luyện nghe, bài đọc và đề thi thử phong phú được phân loại theo kỹ năng và cấp độ.',
      icon: Layers,
      badge: 'Tài nguyên phong phú',
    },
  ];

  const steps = [
    {
      step: '01',
      title: 'Kiểm tra trình độ',
      desc: 'Tham gia bài Placement Test để xác định chính xác band điểm đầu vào và nhận gợi ý lộ trình học phù hợp.',
    },
    {
      step: '02',
      title: 'Đăng ký lớp & Khóa học',
      desc: 'Lựa chọn khóa học online, lớp học tại cơ sở hoặc lớp Virtual theo lịch khai giảng phù hợp với nhu cầu.',
    },
    {
      step: '03',
      title: 'Học tập & Luyện tập',
      desc: 'Theo dõi lịch học, tham gia các buổi học, ôn tập qua Flashcards và làm bài tập theo từng Unit bài học.',
    },
    {
      step: '04',
      title: 'Đánh giá & Nhận chứng nhận',
      desc: 'Hoàn thành các bài kiểm tra định kỳ, nhận nhận xét từ giáo viên và cấp chứng nhận hoàn thành khóa học.',
    },
  ];

  return (
    <LearnerPageShell
      title="Về EnglishLab"
      description="Hệ thống quản lý và hỗ trợ học tập Anh ngữ toàn diện dành cho Học viên, Giảng viên và Quản lý nội dung."
      eyebrow="TỔNG QUAN NỀN TẢNG"
    >
      <div className="space-y-10 py-2">
        {/* ── 1. Header Overview Banner ── */}
        <section className="rounded-2xl border border-[#dcc0bf]/30 bg-[#fffafb] p-6 md:p-8">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="space-y-3 max-w-2xl">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-[#fff0f1] px-3 py-1 text-xs font-bold text-[#730014] border border-[#dfbfbd]/40">
                <GraduationCap className="h-3.5 w-3.5" />
                Hệ thống Quản lý & Đào tạo EnglishLab
              </span>
              <h1 className="font-['Manrope'] text-2xl md:text-3xl font-extrabold text-[#1a1c1c] leading-snug">
                Nền Tảng Học Tập Anh Ngữ Hiện Đại & Đồng Bộ
              </h1>
              <p className="text-xs md:text-sm text-[#584140] leading-relaxed">
                EnglishLab được phát triển nhằm mang lại trải nghiệm học tập hiệu quả, rõ ràng cho học viên qua từng giai đoạn. Từ việc quản lý giáo trình, giao bài tập, theo dõi điểm danh đến tổ chức lớp học trực tuyến — tất cả đều được tích hợp trên một hệ thống duy nhất.
              </p>
            </div>

            <div className="flex shrink-0 flex-col sm:flex-row gap-3">
              <Link
                to="/courses"
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-[#4b0009] px-5 py-2.5 text-xs font-bold text-white shadow-sm transition hover:bg-[#730014] active:scale-95"
              >
                Khám phá khóa học
                <ChevronRight className="h-4 w-4" />
              </Link>
              <Link
                to="/placement-test"
                className="inline-flex items-center justify-center gap-2 rounded-xl border border-[#dfbfbd] bg-white px-5 py-2.5 text-xs font-bold text-[#4b0009] transition hover:bg-[#fff7f7] active:scale-95"
              >
                Kiểm tra trình độ
              </Link>
            </div>
          </div>
        </section>

        {/* ── 2. Platform Core Features ── */}
        <section className="space-y-4">
          <div className="flex items-center justify-between border-b border-[#dcc0bf]/20 pb-3">
            <div>
              <h2 className="font-['Manrope'] text-lg font-extrabold text-[#1a1c1c]">Các Tính Năng Nổi Bật</h2>
              <p className="text-xs text-[#584140]">Nền tảng đáp ứng đầy đủ quy trình học tập và giảng dạy</p>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {platformFeatures.map((item) => {
              const Icon = item.icon;
              return (
                <div
                  key={item.title}
                  className="rounded-2xl border border-[#dcc0bf]/30 bg-white p-5 shadow-sm space-y-3 transition hover:border-[#730014]/25 flex flex-col justify-between"
                >
                  <div className="space-y-2.5">
                    <div className="flex items-center justify-between">
                      <div className="h-9 w-9 rounded-xl bg-[#fff0f1] flex items-center justify-center text-[#730014]">
                        <Icon className="h-4.5 w-4.5" />
                      </div>
                      <span className="text-[9px] font-extrabold uppercase tracking-wider text-[#8b706e] bg-[#fffafb] px-2 py-0.5 rounded-md border border-gray-100">
                        {item.badge}
                      </span>
                    </div>
                    <h3 className="font-bold text-sm text-[#1a1c1c]">{item.title}</h3>
                    <p className="text-xs text-[#584140] leading-relaxed">{item.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        {/* ── 3. Learner Journey Steps ── */}
        <section className="rounded-2xl border border-[#dcc0bf]/30 bg-white p-6 md:p-8 space-y-6">
          <div className="space-y-1">
            <span className="text-[10px] font-extrabold uppercase tracking-widest text-[#730014]">QUY TRÌNH HỌC TẬP</span>
            <h2 className="font-['Manrope'] text-xl font-extrabold text-[#1a1c1c]">4 Bước Bắt Đầu Cùng EnglishLab</h2>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {steps.map((s) => (
              <div key={s.step} className="rounded-xl bg-[#fffafb]/80 border border-gray-100 p-4 space-y-2 relative">
                <span className="text-xs font-extrabold text-[#730014] bg-[#fff0f1] px-2 py-0.5 rounded-md">
                  Bước {s.step}
                </span>
                <h4 className="font-bold text-xs text-[#1a1c1c] pt-1">{s.title}</h4>
                <p className="text-[11px] text-[#584140] leading-relaxed">{s.desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* ── 4. Quick Portal Navigation Cards ── */}
        <section className="grid gap-4 md:grid-cols-3">
          <div className="rounded-2xl border border-[#dcc0bf]/30 bg-white p-5 space-y-3">
            <div className="flex items-center gap-2 text-[#730014]">
              <Layout className="h-4.5 w-4.5" />
              <h3 className="font-bold text-sm text-[#1a1c1c]">Khóa Học Online</h3>
            </div>
            <p className="text-xs text-[#584140] leading-relaxed">
              Các khóa học bài bản từ TOEIC, IELTS đến Giao tiếp công sở với học liệu chi tiết.
            </p>
            <Link to="/courses" className="inline-flex items-center gap-1 text-xs font-bold text-[#730014] hover:underline pt-1">
              Xem các khóa học <ChevronRight className="h-3.5 w-3.5" />
            </Link>
          </div>

          <div className="rounded-2xl border border-[#dcc0bf]/30 bg-white p-5 space-y-3">
            <div className="flex items-center gap-2 text-[#730014]">
              <Calendar className="h-4.5 w-4.5" />
              <h3 className="font-bold text-sm text-[#1a1c1c]">Lịch Khai Giảng</h3>
            </div>
            <p className="text-xs text-[#584140] leading-relaxed">
              Theo dõi danh sách các lớp học sắp khai giảng tại cơ sở hoặc lớp trực tuyến Virtual.
            </p>
            <Link to="/opening-schedule" className="inline-flex items-center gap-1 text-xs font-bold text-[#730014] hover:underline pt-1">
              Xem lịch khai giảng <ChevronRight className="h-3.5 w-3.5" />
            </Link>
          </div>

          <div className="rounded-2xl border border-[#dcc0bf]/30 bg-white p-5 space-y-3">
            <div className="flex items-center gap-2 text-[#730014]">
              <Layers className="h-4.5 w-4.5" />
              <h3 className="font-bold text-sm text-[#1a1c1c]">Lộ Trình Đào Tạo</h3>
            </div>
            <p className="text-xs text-[#584140] leading-relaxed">
              Các chương trình tổng thể kết hợp nhiều khóa học giúp học viên đạt mục tiêu dài hạn.
            </p>
            <Link to="/learning-paths" className="inline-flex items-center gap-1 text-xs font-bold text-[#730014] hover:underline pt-1">
              Xem các lộ trình <ChevronRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        </section>
      </div>
    </LearnerPageShell>
  );
}
