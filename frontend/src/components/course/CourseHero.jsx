import { useRef } from 'react';
import MaterialIcon from './MaterialIcon';

const buildDashboardStats = (user) => [
  {
    icon: 'school',
    value: user?.targetExam || 'Chưa chọn',
    label: 'Mục tiêu học',
  },
  {
    icon: 'target',
    value: user?.targetScore || 'Chưa đặt',
    label: 'Điểm mục tiêu',
  },
  {
    icon: 'verified',
    value: user?.profileCompleted ? 'Đã đủ' : 'Cần bổ sung',
    label: 'Hồ sơ',
  },
];

const CourseHero = ({ user }) => {
  const heroRef = useRef(null);
  const displayName = user?.fullName || user?.email || 'bạn';
  const learningTarget = user?.targetExam || 'tiếng Anh';
  const studyGoal =
    user?.studyGoal ||
    `Hôm nay là một ngày tuyệt vời để tiến thêm một bước trên lộ trình ${learningTarget} của bạn.`;
  const dashboardStats = buildDashboardStats(user);

  const handleMouseMove = (event) => {
    const rect = heroRef.current?.getBoundingClientRect();
    if (!rect || !heroRef.current) return;

    heroRef.current.style.setProperty('--mouse-x', `${event.clientX - rect.left}px`);
    heroRef.current.style.setProperty('--mouse-y', `${event.clientY - rect.top}px`);
  };

  return (
    <section
      ref={heroRef}
      onMouseMove={handleMouseMove}
      className="glow-card relative mb-10 overflow-hidden rounded-3xl bg-[#4b0009] p-10 text-white shadow-2xl"
    >
      <div
        className="pointer-events-none absolute inset-0 z-0"
        style={{
          background:
            'radial-gradient(600px circle at var(--mouse-x, 50%) var(--mouse-y, 50%), rgba(255, 255, 255, 0.05) 0%, rgba(255, 114, 116, 0.03) 40%, transparent 80%)',
        }}
      />
      <div
        className="pointer-events-none absolute inset-0 opacity-10"
        style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cubes.png')" }}
      />

      <div className="relative z-10 flex flex-col items-center justify-between gap-10 lg:flex-row">
        <div className="flex-1 space-y-6 text-center lg:text-left">
          <div className="space-y-2">
            <h1 className="font-['Manrope'] text-4xl font-bold tracking-tight text-white md:text-5xl lg:text-6xl">
              Chào mừng trở lại, {displayName}!
            </h1>
            <p className="max-w-xl font-['Inter'] text-lg leading-[1.6] text-white/80">
              {studyGoal}
            </p>
          </div>

          <div className="flex flex-wrap justify-center gap-4 lg:justify-start">
            <button className="flex cursor-pointer items-center gap-2 rounded-xl bg-white px-6 py-3 font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] text-[#4b0009] shadow-lg transition-all hover:bg-[#eeeeed]">
              Bắt đầu bài học mới
            </button>
            <button className="flex cursor-pointer items-center gap-2 rounded-xl border border-white/20 bg-white/10 px-6 py-3 font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] text-white backdrop-blur-md transition-all hover:bg-white/20">
              Cập nhật mục tiêu học
            </button>
          </div>
        </div>

        <div className="flex flex-wrap justify-center gap-6">
          {dashboardStats.map((stat) => (
            <div
              key={stat.label}
              className="group flex w-40 cursor-default flex-col items-center rounded-2xl border border-white/20 bg-white/10 p-6 text-center backdrop-blur-xl transition-all hover:bg-white/20"
            >
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-[#730014]/50 transition-transform group-hover:scale-110">
                <MaterialIcon name={stat.icon} className="text-white" />
              </div>
              <p className="mb-1 max-w-full truncate text-2xl font-bold text-white">{stat.value}</p>
              <p className="font-['Inter'] text-[10px] font-semibold uppercase leading-none tracking-[0.1em] text-white/60">
                {stat.label}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default CourseHero;
