import { useEffect, useState } from 'react';

const DESKTOP_QUERY = '(min-width: 768px)';
const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)';

const useRotatingCarousel = (items, intervalMs = 6000) => {
  const [visibleCount, setVisibleCount] = useState(() => (
    typeof window !== 'undefined' && window.matchMedia(DESKTOP_QUERY).matches ? 3 : 1
  ));
  const [page, setPage] = useState(0);
  const [paused, setPaused] = useState(false);

  const pageCount = Math.max(1, Math.ceil(items.length / visibleCount));
  const safePage = page % pageCount;
  const visibleItems = Array.from(
    { length: Math.min(visibleCount, items.length) },
    (_, index) => items[((safePage * visibleCount) + index) % items.length],
  );

  useEffect(() => {
    const media = window.matchMedia(DESKTOP_QUERY);
    const updateVisibleCount = () => setVisibleCount(media.matches ? 3 : 1);
    updateVisibleCount();
    media.addEventListener('change', updateVisibleCount);
    return () => media.removeEventListener('change', updateVisibleCount);
  }, []);

  useEffect(() => {
    setPage(0);
  }, [items.length, visibleCount]);

  useEffect(() => {
    if (paused || pageCount <= 1 || window.matchMedia(REDUCED_MOTION_QUERY).matches) return undefined;
    const timer = window.setInterval(() => {
      setPage((current) => (current + 1) % pageCount);
    }, intervalMs);
    return () => window.clearInterval(timer);
  }, [intervalMs, pageCount, paused]);

  return {
    page: safePage,
    pageCount,
    paused,
    visibleItems,
    goNext: () => setPage((current) => (current + 1) % pageCount),
    goPrevious: () => setPage((current) => (current - 1 + pageCount) % pageCount),
    goToPage: setPage,
    togglePaused: () => setPaused((current) => !current),
  };
};

export default useRotatingCarousel;
