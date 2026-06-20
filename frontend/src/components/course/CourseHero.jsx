import { useRef } from 'react';
import StatCard from './StatCard';

const CourseHero = ({ user, registeredCount = 0 }) => {
  const sectionRef = useRef(null);
  const isAuthenticated = Boolean(user);
  const displayName = user?.fullName?.split(' ')?.slice(-1)?.[0] || user?.fullName || user?.email || 'bạn';
  const targetExam = user?.targetExam || 'IELTS / TOEIC';
  const targetScore = user?.targetScore || '7.5+';
  const studyGoal = isAuthenticated
    ? user?.studyGoal || `Hôm nay là một ngày phù hợp để tiến gần hơn tới mục tiêu ${targetExam} của bạn.`
    : 'Khám phá các khóa học IELTS, TOEIC và giao tiếp được thiết kế rõ ràng cho người học tự chủ.';

  const handleMouseMove = (event) => {
    const section = sectionRef.current;
    if (!section) return;
    const rect = section.getBoundingClientRect();
    section.style.setProperty('--mouse-x', `${event.clientX - rect.left}px`);
    section.style.setProperty('--mouse-y', `${event.clientY - rect.top}px`);
  };

  return (
    <section
      ref={sectionRef}
      onMouseMove={handleMouseMove}
      className="glow-card relative mb-10 overflow-hidden rounded-3xl bg-[#4b0009] p-10 text-white shadow-2xl"
    >
      <div className="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(600px_circle_at_var(--mouse-x,50%)_var(--mouse-y,50%),rgba(255,255,255,0.05)_0%,rgba(255,114,116,0.03)_40%,transparent_80%)]" />
      <div className="pointer-events-none absolute inset-0 opacity-10" style={{ backgroundImage: "url('https://www.transparenttextures.com/patterns/cubes.png')" }} />
      <div className="relative z-10 flex flex-col items-center justify-between gap-10 lg:flex-row">
        <div className="flex-1 space-y-6 text-center lg:text-left">
          <div className="space-y-2">
            <h1 className="text-4xl font-bold tracking-tight text-white md:text-5xl lg:text-6xl">
              {isAuthenticated ? `Chào mừng trở lại, ${displayName}!` : 'Khám phá khóa học tại EnglishLab'}
            </h1>
            <p className="max-w-xl text-lg leading-[1.6] text-white/80">{studyGoal}</p>
          </div>
          <div className="flex flex-wrap justify-center gap-4 lg:justify-start">
            <a className="flex items-center gap-2 rounded-xl bg-white px-6 py-3 text-[14px] font-semibold tracking-[0.02em] text-[#4b0009] shadow-lg transition-all hover:bg-[#eeeeed]" href="#popular-courses">
              {isAuthenticated ? 'Bắt đầu bài học mới' : 'Xem khóa học nổi bật'}
            </a>
            <a className="flex items-center gap-2 rounded-xl border border-white/20 bg-white/10 px-6 py-3 text-[14px] font-semibold tracking-[0.02em] text-white backdrop-blur-md transition-all hover:bg-white/20" href="#catalog">
              Khám phá khóa học phù hợp
            </a>
          </div>
        </div>
        <div className="flex flex-wrap justify-center gap-6">
          {isAuthenticated ? (
            <>
              <StatCard icon="school" value={registeredCount} label="Khóa đã đăng ký" />
              <StatCard icon="flag" value={targetExam} label="Mục tiêu học" />
              <StatCard icon="target" value={targetScore} label="Điểm mục tiêu" />
            </>
          ) : (
            <>
              <StatCard icon="menu_book" value="12+" label="Khóa học trực tuyến" />
              <StatCard icon="verified" value="IELTS" label="Trọng tâm chính" />
              <StatCard icon="school" value="0đ" label="Có khóa miễn phí" />
            </>
          )}
        </div>
      </div>
    </section>
  );
};

export default CourseHero;
