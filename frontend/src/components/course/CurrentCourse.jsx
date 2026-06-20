import { useMemo, useState } from 'react';
import mascotHoi from '../../assets/sticker-hoi.png';
import CourseActionButton from './CourseActionButton';

const PAGE_SIZE = 5;

const tabs = [
  { id: 'current', label: 'Khóa học đang học' },
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
        <h3 className="text-[24px] font-semibold leading-[1.3]">{enrollment.courseTitle}</h3>
        <span className="font-bold text-[#4b0009]">{enrollment.progressPercent || 0}% hoàn thành</span>
      </div>
      <div className="mb-4 h-3 w-full rounded-full bg-[#eeeeed]">
        <div className="h-3 rounded-full bg-[#4b0009]" style={{ width: `${enrollment.progressPercent || 0}%` }} />
      </div>
      <p className="text-sm text-[#584140]">Tiếp tục tự học trong không gian học tập, xem video và duy trì nhịp học mỗi ngày.</p>
    </div>
    <CourseActionButton
      className="whitespace-nowrap rounded-lg bg-[#4b0009] px-8 py-3 text-[14px] font-semibold tracking-[0.02em] text-white transition-all hover:bg-[#9E001F]"
      course={{ ...enrollment, id: enrollment.courseId, slug: enrollment.courseSlug, registered: true }}
    >
      {getCourseButtonLabel(enrollment)}
    </CourseActionButton>
  </div>
);

const EmptyCurrentCourse = ({ isAuthenticated }) => (
  <section className="mb-12">
    <div className="flex flex-col items-start gap-5 rounded-[28px] border border-[#dfbfbd]/25 bg-white px-6 py-7 shadow-sm md:flex-row md:items-center md:justify-between md:px-8">
      <div className="flex items-center gap-5">
        <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-[22px] border border-[#efe3e4] bg-[#fcf8f8] p-3">
          <img alt="EnglishLab" className="h-full w-full object-contain" src={mascotHoi} />
        </div>
        <div>
          <h2 className="text-[20px] font-extrabold leading-[1.3] text-[#2b2828]">Chưa biết nên bắt đầu từ đâu?</h2>
          <p className="mt-2 text-sm leading-7 text-[#584140]">
            {isAuthenticated
              ? 'Làm bài kiểm tra trình độ miễn phí để nhận gợi ý khóa học phù hợp nhất cho riêng bạn.'
              : 'Đăng nhập và làm bài kiểm tra trình độ miễn phí để nhận gợi ý khóa học phù hợp nhất cho riêng bạn.'}
          </p>
        </div>
      </div>
      <a
        className="inline-flex min-w-[160px] items-center justify-center rounded-xl bg-[#4b0009] px-6 py-3 text-sm font-extrabold text-white shadow-[0_10px_24px_rgba(75,0,9,0.18)] transition hover:bg-[#730014]"
        href="/placement-test"
      >
        Kiểm tra đầu vào
      </a>
    </div>
  </section>
);

const CurrentCourse = ({ enrollments = [], isAuthenticated }) => {
  const [activeTab, setActiveTab] = useState('current');
  const [currentPage, setCurrentPage] = useState(1);
  const totalPages = Math.max(1, Math.ceil(enrollments.length / PAGE_SIZE));
  const pagedEnrollments = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return enrollments.slice(start, start + PAGE_SIZE);
  }, [currentPage, enrollments]);
  const currentEnrollment = useMemo(() => {
    if (!enrollments.length) return null;
    return enrollments.find((item) => (item.progressPercent || 0) < 100) || enrollments[0];
  }, [enrollments]);

  if (!isAuthenticated || !enrollments.length) {
    return <EmptyCurrentCourse isAuthenticated={isAuthenticated} />;
  }

  return (
    <section className="mb-12">
      <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <h2 className="text-[32px] font-bold leading-[1.2] text-[#4b0009]">Khóa học đang học</h2>
        <div className="inline-flex rounded-2xl border border-[#dfbfbd]/30 bg-white p-1 shadow-sm">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              className={`rounded-xl px-4 py-2 text-sm font-bold transition ${activeTab === tab.id ? 'bg-[#4b0009] text-white' : 'text-[#584140] hover:bg-[#fff0f1] hover:text-[#4b0009]'}`}
              type="button"
              onClick={() => {
                setActiveTab(tab.id);
                setCurrentPage(1);
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {activeTab === 'current' ? (
        <EnrollmentCard enrollment={currentEnrollment} />
      ) : (
        <>
          <div className="grid gap-4">
            {pagedEnrollments.map((enrollment) => (
              <EnrollmentCard key={enrollment.id ?? enrollment.courseId ?? enrollment.courseSlug} compact enrollment={enrollment} />
            ))}
          </div>
          {totalPages > 1 ? (
            <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-sm font-semibold text-[#584140]">
                Trang {currentPage} / {totalPages} • {enrollments.length} khóa học
              </p>
              <div className="flex gap-2">
                <button
                  className="rounded-xl border border-[#dfbfbd]/40 bg-white px-4 py-2 text-sm font-bold text-[#4b0009] transition hover:bg-[#fff0f1] disabled:cursor-not-allowed disabled:opacity-40"
                  type="button"
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                >
                  Trước
                </button>
                <button
                  className="rounded-xl bg-[#4b0009] px-4 py-2 text-sm font-bold text-white transition hover:bg-[#9E001F] disabled:cursor-not-allowed disabled:opacity-40"
                  type="button"
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                >
                  Sau
                </button>
              </div>
            </div>
          ) : null}
        </>
      )}
    </section>
  );
};

export default CurrentCourse;
