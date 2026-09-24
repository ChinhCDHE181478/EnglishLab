import { ChevronLeft, ChevronRight, Pause, Play, UserRound } from 'lucide-react';
import { useState } from 'react';
import useRotatingCarousel from './useRotatingCarousel';

const FALLBACK_TEACHER_IMAGES = [
  '/teachers/teacher-01.jpg',
  '/teachers/teacher-02.jpg',
  '/teachers/teacher-03.jpg',
  '/teachers/teacher-04.jpg',
];

const getFallbackTeacherImage = (teacherId) => {
  const numericId = Number(teacherId);
  const imageIndex = Number.isFinite(numericId)
    ? Math.abs(numericId) % FALLBACK_TEACHER_IMAGES.length
    : 0;
  return FALLBACK_TEACHER_IMAGES[imageIndex];
};

const TeacherImage = ({ teacher }) => {
  const [useFallback, setUseFallback] = useState(false);
  const [fallbackFailed, setFallbackFailed] = useState(false);
  const fallbackImage = getFallbackTeacherImage(teacher.id);
  const imageSource = useFallback || !teacher.avatarUrl ? fallbackImage : teacher.avatarUrl;

  if (fallbackFailed) {
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
      onError={() => {
        if (!useFallback && teacher.avatarUrl) {
          setUseFallback(true);
          return;
        }
        setFallbackFailed(true);
      }}
      src={imageSource}
    />
  );
};

const TeachersSection = ({ loading = false, teachers = [] }) => {
  const carousel = useRotatingCarousel(teachers);

  if (!loading && !teachers.length) return null;

  return (
    <section className="mx-auto max-w-7xl px-4 py-20 md:px-10">
      <div className="mb-12 text-center">
        <h2 className="mb-4 font-['Manrope'] text-3xl font-bold text-[#1a1c1c] md:text-4xl">
          Đội ngũ giảng viên tinh hoa
        </h2>
        <p className="mx-auto max-w-2xl text-lg leading-8 text-[#584140]">
          Các chuyên gia ngôn ngữ với trình độ học thuật xuất sắc và triết lý giảng dạy truyền cảm hứng.
        </p>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 gap-8 md:grid-cols-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <div className="aspect-[3/4] animate-pulse rounded-xl bg-[#eadfdd]" key={index} />
          ))}
        </div>
      ) : (
        <>
          <div aria-live="polite" className="grid grid-cols-1 gap-8 md:grid-cols-3">
            {carousel.visibleItems.map((teacher) => {
              const description = teacher.biography || teacher.specializations || '';
              return (
                <article
                  key={teacher.id}
                  className="animate-float-up group relative overflow-hidden rounded-xl border border-[#dfbfbd]/30 bg-white opacity-0 shadow-sm transition-all duration-300 hover:shadow-lg"
                  style={{ animationFillMode: 'forwards' }}
                >
                  <div className="aspect-[3/4] overflow-hidden bg-[#e2e2e2]">
                    <TeacherImage teacher={teacher} />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/20 to-transparent opacity-85 transition-opacity group-hover:opacity-95" />
                  </div>

                  <div className="absolute bottom-0 left-0 w-full translate-y-8 p-6 transition-transform duration-300 group-hover:translate-y-0">
                    <div className="mb-2 flex flex-wrap items-center gap-2">
                      {(teacher.badges || []).map((badge, index) => (
                        <span
                          key={badge}
                          className={`rounded px-2 py-1 text-[10px] font-semibold uppercase ${index === 0
                            ? 'bg-[#730014] text-white'
                            : 'border border-white/20 bg-white/20 text-white backdrop-blur-sm'
                          }`}
                        >
                          {badge}
                        </span>
                      ))}
                    </div>
                    <h3 className="font-['Manrope'] text-2xl font-semibold text-white">{teacher.name}</h3>
                    <p className="mb-4 text-white/80">{teacher.headline}</p>
                    {description ? (
                      <p className="line-clamp-4 border-t border-white/20 pt-4 text-sm leading-6 text-white/90 opacity-0 transition-opacity delay-100 duration-300 group-hover:opacity-100">
                        {description}
                      </p>
                    ) : null}
                  </div>
                </article>
              );
            })}
          </div>
          <CarouselControls label="giảng viên" {...carousel} />
        </>
      )}
    </section>
  );
};

function CarouselControls({ goNext, goPrevious, goToPage, label, page, pageCount, paused, togglePaused }) {
  if (pageCount <= 1) return null;
  return (
    <div className="mt-8 flex items-center justify-center gap-4">
      <button aria-label={`Xem ${label} trước`} className="rounded-full border border-[#dfbfbd] bg-white p-2 text-[#730014] transition hover:bg-[#fff2f3]" onClick={goPrevious} type="button">
        <ChevronLeft className="h-5 w-5" />
      </button>
      <div className="flex gap-2">
        {Array.from({ length: pageCount }).map((_, index) => (
          <button
            aria-label={`Trang ${label} ${index + 1}`}
            aria-pressed={page === index}
            className={`h-2.5 rounded-full transition-all ${page === index ? 'w-8 bg-[#730014]' : 'w-2.5 bg-[#dfbfbd]'}`}
            key={index}
            onClick={() => goToPage(index)}
            type="button"
          />
        ))}
      </div>
      <button aria-label={paused ? `Tiếp tục xoay ${label}` : `Tạm dừng xoay ${label}`} className="rounded-full p-2 text-[#730014] transition hover:bg-[#fff2f3]" onClick={togglePaused} type="button">
        {paused ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
      </button>
      <button aria-label={`Xem ${label} tiếp theo`} className="rounded-full border border-[#dfbfbd] bg-white p-2 text-[#730014] transition hover:bg-[#fff2f3]" onClick={goNext} type="button">
        <ChevronRight className="h-5 w-5" />
      </button>
    </div>
  );
}

export default TeachersSection;
