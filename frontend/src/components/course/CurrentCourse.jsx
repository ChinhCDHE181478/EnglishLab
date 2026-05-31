import { useMemo, useState } from 'react';
import CourseActionButton from './CourseActionButton';

const tabs = [
  { id: 'current', label: 'Khóa học hiện tại' },
  { id: 'mine', label: 'Các khóa học của bạn' },
];

const getCourseButtonLabel = (enrollment) => ((enrollment.progressPercent || 0) >= 100 ? 'Đã hoàn thành' : 'Học tiếp');

const EnrollmentCard = ({ enrollment, compact = false }) => (
  <div className="flex flex-col items-center gap-6 rounded-2xl border border-[#dfbfbd]/30 bg-white p-6 shadow-sm md:flex-row">
    <div className={`${compact ? 'h-20 w-20' : 'h-24 w-24'} flex-shrink-0 overflow-hidden rounded-lg bg-[#eeeeed]`}>
      <img alt={enrollment.courseTitle} className="h-full w-full object-cover" src={enrollment.thumbnailUrl} />
    </div>
    <div className="w-full flex-grow">
      <div className="mb-2 flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <h3 className="font-headline-md text-[24px] font-semibold leading-[1.3]">{enrollment.courseTitle}</h3>
        <span className="font-bold text-[#4b0009]">{enrollment.progressPercent || 0}% hoàn thành</span>
      </div>
      <div className="mb-4 h-3 w-full rounded-full bg-[#eeeeed]">
        <div className="h-3 rounded-full bg-[#4b0009]" style={{ width: `${enrollment.progressPercent || 0}%` }} />
      </div>
      <p className="text-sm text-[#584140]">Tiếp tục tự học trong workspace, xem video và giữ streak mỗi ngày.</p>
    </div>
    <CourseActionButton
      className="whitespace-nowrap rounded-lg bg-[#4b0009] px-8 py-3 text-[14px] font-semibold tracking-[0.02em] text-white transition-all hover:bg-[#9E001F]"
      course={{ ...enrollment, id: enrollment.courseId, slug: enrollment.courseSlug, registered: true }}
    >
      {getCourseButtonLabel(enrollment)}
    </CourseActionButton>
  </div>
);

const CurrentCourse = ({ enrollments = [], isAuthenticated }) => {
  const [activeTab, setActiveTab] = useState('current');
  const currentEnrollment = useMemo(() => {
    if (!enrollments.length) return null;
    return enrollments.find((item) => (item.progressPercent || 0) < 100) || enrollments[0];
  }, [enrollments]);

  if (!isAuthenticated) {
    return (
      <section className="mb-12">
        <h2 className="font-headline-lg mb-6 text-[32px] font-bold leading-[1.2] text-[#4b0009]">Bắt đầu học online</h2>
        <div className="flex flex-col items-center justify-between gap-6 rounded-2xl border border-[#dfbfbd]/30 bg-white p-6 shadow-sm md:flex-row">
          <div className="w-full flex-grow">
            <div className="mb-2 flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
              <h3 className="font-headline-md text-[24px] font-semibold leading-[1.3]">Xem chi tiết khóa học trước khi mua</h3>
              <span className="font-bold text-[#4b0009]">Lộ trình tự học 100%</span>
            </div>
            <div className="mb-4 h-3 w-full rounded-full bg-[#eeeeed]">
              <div className="h-3 rounded-full bg-[#4b0009]" style={{ width: '35%' }} />
            </div>
            <p className="text-sm text-[#584140]">
              Khách có thể xem chi tiết khóa học, module và bài học preview. Khi đăng nhập, bạn có thể mua khóa học và truy cập workspace tự học ngay.
            </p>
          </div>
          <a className="whitespace-nowrap rounded-lg bg-[#4b0009] px-8 py-3 text-[14px] font-semibold tracking-[0.02em] text-white transition-all hover:bg-[#9E001F]" href="/courses">
            Khám phá khóa học
          </a>
        </div>
      </section>
    );
  }

  if (!enrollments.length) return null;

  return (
    <section className="mb-12">
      <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <h2 className="font-headline-lg text-[32px] font-bold leading-[1.2] text-[#4b0009]">Khóa học hiện tại</h2>
        <div className="inline-flex rounded-2xl border border-[#dfbfbd]/30 bg-white p-1 shadow-sm">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`cursor-pointer rounded-xl px-4 py-2 text-sm font-bold transition ${activeTab === tab.id ? 'bg-[#4b0009] text-white' : 'text-[#584140] hover:bg-[#fff0f1] hover:text-[#4b0009]'}`}
              type="button"
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {activeTab === 'current' ? (
        <EnrollmentCard enrollment={currentEnrollment} />
      ) : (
        <div className="grid gap-4">
          {enrollments.map((enrollment) => (
            <EnrollmentCard key={enrollment.id ?? enrollment.courseId ?? enrollment.courseSlug} compact enrollment={enrollment} />
          ))}
        </div>
      )}
    </section>
  );
};

export default CurrentCourse;
