export const formatCoursePrice = (price) => {
  const amount = Number(price || 0);
  if (!amount) return 'Miễn phí';

  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
};

export const isPaidCourse = (course) => Number(course?.price || 0) > 0;
