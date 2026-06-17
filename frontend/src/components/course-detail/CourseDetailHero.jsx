import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import LearnerCourseActions from '../learner/LearnerCourseActions';
import CourseCommerceActions from '../learner/CourseCommerceActions';
import { formatCoursePrice } from '../course/courseFormatters';
import { formatBandRangeText, formatBandValue, getBandFitInfo } from '../../utils/selfPacedHelpers';

const formatLevelLabel = (level) => {
  const normalized = String(level || '').trim().toUpperCase();
  switch (normalized) {
    case 'BEGINNER': return 'Sơ cấp';
    case 'ELEMENTARY': return 'Cơ bản';
    case 'PRE_INTERMEDIATE': return 'Tiền trung cấp';
    case 'INTERMEDIATE': return 'Trung cấp';
    case 'UPPER_INTERMEDIATE': return 'Trung cao cấp';
    case 'ADVANCED': return 'Nâng cao';
    default: return 'Tự học theo lộ trình';
  }
};

const formatRatingSummary = (course) => {
  const averageRating = Number(course.averageRating || 0);
  const reviewCount = Number(course.reviewCount || 0);
  if (averageRating > 0 && reviewCount > 0) {
    return `${averageRating.toFixed(1)} ⭐ (${reviewCount.toLocaleString('vi-VN')} đánh giá)`;
  }
  if (averageRating > 0) {
    return `${averageRating.toFixed(1)} ⭐`;
  }
  return 'Chưa có đánh giá';
};

const formatBandCompactText = (course) => {
  const min = Number(course?.recommendedCurrentBandMin);
  const max = Number(course?.recommendedCurrentBandMax);

  if (Number.isFinite(min) && Number.isFinite(max) && min > 0 && max > 0) {
    return `Band ${formatBandValue(min)} - ${formatBandValue(max)}`;
  }

  return 'Đang cập nhật';
};

const buildStatItems = (course, bandFit) => {
  const averageRating = Number(course.averageRating || 0);
  const reviewCount = Number(course.reviewCount || 0);
  const totalLessons = Number(course.totalLessons || 0);
  const totalHours = Number(course.totalHours || 0);
  const enrollmentCount = Number(course.enrollmentCount || 0);

  return [
    {
      title: 'Band phù hợp',
      value: formatBandCompactText(course),
      description: bandFit.message || 'Phù hợp với trình độ hiện tại',
    },
    {
      title: 'Đánh giá học viên',
      value: averageRating > 0 ? `${averageRating.toFixed(1)} ★` : 'Chưa có',
      description:
        reviewCount > 0
          ? `${reviewCount.toLocaleString('vi-VN')} lượt đánh giá`
          : 'Chưa có đánh giá',
    },
    {
      title: 'Trình độ đề xuất',
      value: formatLevelLabel(course.level),
      description: course.targetBand ? `Mục tiêu Band ${formatBandValue(course.targetBand)}` : 'Theo lộ trình hiện tại',
    },
    {
      title: 'Số bài học',
      value: totalLessons > 0 ? `${totalLessons} bài học` : 'Đang cập nhật',
      description: focusSkillsText(course.focusSkills),
    },
    {
      title: 'Thời lượng học',
      value: course.duration || 'Tự học linh hoạt',
      description:
        totalHours > 0
          ? `${totalHours} giờ học`
          : enrollmentCount > 0
            ? `${enrollmentCount.toLocaleString('vi-VN')} học viên tham gia`
            : 'Theo tiến độ riêng',
    },
  ];
};

const focusSkillsText = (skills = []) => {
  if (!Array.isArray(skills) || !skills.length) {
    return 'Nội dung theo lộ trình hiện tại';
  }

  return skills.slice(0, 2).join(', ');
};

const buildTabItems = ({ course, bandFit, focusSkills, prerequisites }) => [
  {
    id: 'overview',
    label: 'Giới thiệu',
    title: 'Những gì bạn sẽ học',
    content: (
      <div className="grid gap-4 md:grid-cols-2">
        <div className="flex gap-3 text-base leading-8 text-[#584140]">
          <span className="mt-1 text-lg font-black text-[#8a0018]">✓</span>
          <p>{course.targetOutcome || 'Đang cập nhật mục tiêu đầu ra cho khóa học này.'}</p>
        </div>
        <div className="flex gap-3 text-base leading-8 text-[#584140]">
          <span className="mt-1 text-lg font-black text-[#8a0018]">✓</span>
          <p>{course.description || course.shortDescription || 'Khóa học đang được cập nhật thêm mô tả chi tiết.'}</p>
        </div>
      </div>
    ),
  },
  {
    id: 'results',
    label: 'Kết quả',
    title: 'Kết quả sau khi học',
    content: (
      <div className="grid gap-4 md:grid-cols-2">
        <div className="flex gap-3 text-base leading-8 text-[#584140]">
          <span className="mt-1 text-lg font-black text-[#8a0018]">✓</span>
          <p>
            {course.targetBand
              ? `Mục tiêu đầu ra hướng tới Band ${formatBandValue(course.targetBand)}.`
              : 'Khóa học tập trung giúp bạn cải thiện kết quả học tập thực tế.'}
          </p>
        </div>
        <div className="flex gap-3 text-base leading-8 text-[#584140]">
          <span className="mt-1 text-lg font-black text-[#8a0018]">✓</span>
          <p>{bandFit.message || 'Lộ trình sẽ được cá nhân hóa tốt hơn khi có thêm dữ liệu trình độ của bạn.'}</p>
        </div>
      </div>
    ),
  },
  {
    id: 'skills',
    label: 'Kỹ năng',
    title: 'Kỹ năng bạn sẽ đạt được',
    content: focusSkills.length ? (
      <div className="flex flex-wrap gap-3">
        {focusSkills.map((skill) => (
          <span key={skill} className="rounded-full bg-[#fff1f3] px-4 py-2 text-sm font-semibold text-[#6d2230]">
            {skill}
          </span>
        ))}
      </div>
    ) : (
      <p className="text-base leading-8 text-[#584140]">Đang cập nhật kỹ năng trọng tâm cho khóa học này.</p>
    ),
  },
  {
    id: 'requirements',
    label: 'Điều kiện',
    title: 'Điều kiện nên học trước',
    content: (
      <div className="grid gap-4 md:grid-cols-2">
        {prerequisites.map((item) => (
          <div key={item} className="flex gap-3 text-base leading-8 text-[#584140]">
            <span className="mt-1 text-lg font-black text-[#8a0018]">✓</span>
            <p>{item}</p>
          </div>
        ))}
      </div>
    ),
  },
];

const CourseDetailHero = ({ course, currentBand = null }) => {
  const bandFit = getBandFitInfo(course, currentBand);
  const priceLabel = formatCoursePrice(course.salePrice || course.price);
  const statusLabel = course.registered ? 'Đã đăng ký' : Number(course.salePrice || course.price) > 0 ? 'Học phí' : '';
  const heroBackgroundStyle = course.thumbnailUrl ? { backgroundImage: `url(${course.thumbnailUrl})` } : undefined;
  const focusSkills = course.focusSkills?.length ? course.focusSkills : [];
  const prerequisites = course.prerequisites?.length ? course.prerequisites : ['Không có yêu cầu học trước bắt buộc.'];
  const tabs = useMemo(
    () => buildTabItems({ course, bandFit, focusSkills, prerequisites }),
    [bandFit, course, focusSkills, prerequisites],
  );
  const statItems = useMemo(() => buildStatItems(course, bandFit), [bandFit, course]);
  const [activeTab, setActiveTab] = useState(() => tabs[0]?.id || 'overview');
  const activeSection = tabs.find((item) => item.id === activeTab) || tabs[0];

  return (
    <section className="w-full text-[#201f24]">
      <div className="relative overflow-hidden bg-[linear-gradient(135deg,_#fff8f6,_#fffefe)]">
        <div
          aria-hidden="true"
          className="absolute inset-0 bg-cover bg-right-center bg-no-repeat"
          style={heroBackgroundStyle}
        />
        <div
          aria-hidden="true"
          className="absolute inset-0 bg-[linear-gradient(90deg,rgba(255,248,246,0.985)_0%,rgba(255,248,246,0.96)_44%,rgba(255,248,246,0.86)_68%,rgba(255,248,246,0.76)_100%)]"
        />

        {/* 1. Phần Background Phía Phải (Bán nguyệt công nghệ kiểu Coursera) */}
        <div className="absolute top-0 right-0 hidden h-full w-1/2 pointer-events-none md:block">
          <div className="absolute -right-16 top-1/2 -translate-y-1/2 h-[500px] w-[500px] rounded-full border-[32px] border-[#8a0018]/5" />
          <div className="absolute right-12 top-1/2 -translate-y-1/2 h-[360px] w-[360px] rounded-full border-[16px] border-[#8a0018]/10 border-t-transparent" />
          <div className="absolute -right-24 -top-24 h-80 w-80 rounded-full bg-[#8a0018]/5 blur-3xl" />
        </div>

        <div className="relative mx-auto max-w-[1280px] px-6 py-12 md:px-12 md:py-16 lg:px-16">
        {/* Breadcrumb & Logo */}
        <div className="mb-8 flex flex-wrap items-center gap-2 text-xs font-bold uppercase tracking-[0.14em] text-[#7c6a68]">
          <Link className="hover:text-[#4b0009] transition" to="/courses">Khóa học</Link>
          <span>/</span>
          <span className="text-[#6e0012]">{course.categoryName || course.category || 'Trực tuyến'}</span>
        </div>

        {/* Khối Thông tin chính bên trái */}
        <div className="max-w-[800px]">
          {/* Tên Chuyên ngành / Tên khóa học */}
          <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-tight text-[#201f24] sm:text-4xl md:text-5xl md:leading-[1.15]">
            {course.title}
          </h1>

          {/* Mô tả ngắn */}
          <p className="mt-4 max-w-[720px] text-base leading-7 text-[#584140] opacity-90 md:text-lg md:leading-8">
            {course.description || course.shortDescription}
          </p>

          {/* Giá và Ưu đãi */}
          <div className="mt-6 flex flex-wrap items-center gap-4 text-sm">
            {statusLabel ? (
              <span className="rounded-md border border-[#d9c3c0] bg-white/90 px-3 py-1.5 font-bold text-[#6d2230]">
                {statusLabel}
              </span>
            ) : null}
            <span className="font-['Manrope'] text-2xl font-black text-[#4b0009]">{priceLabel}</span>
            {Number(course.discountPercent || 0) > 0 && (
              <span className="rounded-md bg-[#fff1f3] px-2.5 py-1 text-xs font-extrabold text-[#730014]">
                Giảm {course.discountPercent}%
              </span>
            )}
          </div>

          {/* Nút Kêu gọi hành động (CTA) */}
          <div className="mt-8 flex flex-col gap-3.5 sm:flex-row sm:flex-wrap sm:items-center">
            <div className="inline-flex flex-col gap-1">
              <LearnerCourseActions course={course} onDetailPage />
            </div>
            {!course.registered && <CourseCommerceActions course={course} />}
            <span aria-hidden="true" className="hidden h-px w-8 bg-[#d9c3c0] sm:block" />
            <span className="inline-flex items-center gap-2 text-sm font-medium text-[#6e5957]">
              <span className="material-symbols-outlined text-[18px] text-[#8a0018]">groups</span>
              <strong className="text-[#6e5957]">{Number(course.enrollmentCount || 0).toLocaleString('vi-VN')}</strong>
              <span>học viên đã đăng ký</span>
            </span>
          </div>
        </div>
        </div>
      </div>

      {/* 2. Thanh Stats Bar nằm ngang phía dưới (Bám sát layout Coursera) */}
      <div className="w-full border-t border-[#e4d5d1] bg-white">
        <div className="mx-auto max-w-[1280px] px-6 md:px-12 lg:px-16">
          <div className="grid grid-cols-2 gap-y-6 py-6 sm:grid-cols-3 md:grid-cols-5 md:gap-x-4 md:divide-x md:divide-[#eadfdd] md:py-8">
            {statItems.map((item, index) => (
              <div key={item.title} className={index === 0 ? 'pr-4' : index === statItems.length - 1 ? 'pl-2 md:pl-4' : 'px-2 md:px-4'}>
                <p className="text-lg font-bold text-[#201f24] md:text-xl">{item.value}</p>
                <p className="mt-1 text-xs leading-5 text-[#6e5957]">{item.description}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="w-full py-8">
        <div className="rounded-[28px] border border-[#e4d5d1] bg-white p-6 shadow-[0_14px_30px_rgba(94,28,36,0.06)] md:p-8">
          <div className="border-b border-[#eee2df]">
            <div className="flex flex-wrap gap-7 pb-4">
              {tabs.map((tab) => {
                const active = tab.id === activeSection.id;
                return (
                  <button
                    key={tab.id}
                    className={`border-b-2 pb-3 text-sm font-bold transition ${
                      active
                        ? 'border-[#8a0018] text-[#4b0009]'
                        : 'border-transparent text-[#7b6563] hover:border-[#e5c2bf] hover:text-[#4b0009]'
                    }`}
                    onClick={() => setActiveTab(tab.id)}
                    type="button"
                  >
                    {tab.label}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="mt-7">
            <h2 className="font-['Manrope'] text-2xl font-extrabold text-[#201f24]">{activeSection.title}</h2>
            <div className="mt-5">{activeSection.content}</div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default CourseDetailHero;
