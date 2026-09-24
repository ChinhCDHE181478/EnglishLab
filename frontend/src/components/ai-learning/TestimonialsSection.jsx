import { ChevronLeft, ChevronRight, Pause, Play, Star } from 'lucide-react';
import useRotatingCarousel from './useRotatingCarousel';

const Rating = ({ value }) => (
  <div aria-label={`${value} trên 5 sao`} className="mb-4 flex items-center gap-1 text-yellow-500">
    {Array.from({ length: 5 }).map((_, index) => (
      <Star key={index} size={20} fill={index < value ? 'currentColor' : 'none'} />
    ))}
  </div>
);

const TestimonialsSection = ({ loading = false, testimonials = [] }) => {
  const carousel = useRotatingCarousel(testimonials, 7000);

  if (!loading && !testimonials.length) return null;

  return (
    <section id="testimonials" className="scroll-mt-24 border-y border-[#dfbfbd]/30 bg-[#f9f9f9] py-20">
      <div className="mx-auto max-w-7xl px-4 md:px-10">
        <div className="mb-12 text-center">
          <h2 className="mb-4 font-['Manrope'] text-3xl font-bold text-[#1a1c1c] md:text-4xl">
            Học viên nói gì về EnglishLab
          </h2>
          <p className="mx-auto max-w-2xl text-lg leading-8 text-[#584140]">
            Những trải nghiệm thực tế từ cộng đồng học viên đã hoàn thành khóa học.
          </p>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
            {Array.from({ length: 3 }).map((_, index) => (
              <div className="h-64 animate-pulse rounded-xl bg-[#eadfdd]" key={index} />
            ))}
          </div>
        ) : (
          <>
            <div aria-live="polite" className="grid grid-cols-1 gap-6 md:grid-cols-3">
              {carousel.visibleItems.map((item, index) => (
                <article
                  key={item.id}
                  className="animate-float-up flex flex-col rounded-xl border border-[#dfbfbd]/30 bg-white p-6 opacity-0 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg"
                  style={{ animationFillMode: 'forwards', animationDelay: `${index * 100}ms` }}
                >
                  <Rating value={item.rating} />
                  <p className="mb-6 flex-grow italic leading-7 text-[#584140]">“{item.comment}”</p>
                  <div className="flex items-center gap-3 border-t border-[#dfbfbd]/30 pt-4">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-[#730014] font-bold text-white">
                      {item.learnerName?.charAt(0)?.toUpperCase() || 'E'}
                    </div>
                    <div>
                      <div className="font-semibold text-[#1a1c1c]">{item.learnerName}</div>
                      <div className="line-clamp-1 text-xs text-[#584140]">{item.courseTitle}</div>
                    </div>
                  </div>
                </article>
              ))}
            </div>
            <CarouselControls {...carousel} />
          </>
        )}
      </div>
    </section>
  );
};

function CarouselControls({ goNext, goPrevious, goToPage, page, pageCount, paused, togglePaused }) {
  if (pageCount <= 1) return null;
  return (
    <div className="mt-8 flex items-center justify-center gap-4">
      <button aria-label="Xem đánh giá trước" className="rounded-full border border-[#dfbfbd] bg-white p-2 text-[#730014] transition hover:bg-[#fff2f3]" onClick={goPrevious} type="button">
        <ChevronLeft className="h-5 w-5" />
      </button>
      <div className="flex gap-2">
        {Array.from({ length: pageCount }).map((_, index) => (
          <button
            aria-label={`Trang đánh giá ${index + 1}`}
            aria-pressed={page === index}
            className={`h-2.5 rounded-full transition-all ${page === index ? 'w-8 bg-[#730014]' : 'w-2.5 bg-[#dfbfbd]'}`}
            key={index}
            onClick={() => goToPage(index)}
            type="button"
          />
        ))}
      </div>
      <button aria-label={paused ? 'Tiếp tục xoay đánh giá' : 'Tạm dừng xoay đánh giá'} className="rounded-full p-2 text-[#730014] transition hover:bg-[#fff2f3]" onClick={togglePaused} type="button">
        {paused ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
      </button>
      <button aria-label="Xem đánh giá tiếp theo" className="rounded-full border border-[#dfbfbd] bg-white p-2 text-[#730014] transition hover:bg-[#fff2f3]" onClick={goNext} type="button">
        <ChevronRight className="h-5 w-5" />
      </button>
    </div>
  );
}

export default TestimonialsSection;
