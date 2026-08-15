const STORAGE_KEY = 'englishlab.learningPathCheckout';

export const buildLearningPathCheckout = (path) => ({
  learningPathId: path.id,
  code: path.code,
  name: path.name,
  discountPercent: Number(path.discountPercent || 0),
  minimumCoursesForDiscount: Number(path.minimumCoursesForDiscount || 2),
  courses: (path.courses || [])
    .filter((course) => !course.owned)
    .map((course) => ({
      id: course.courseId,
      slug: course.slug,
      title: course.title,
      thumbnailUrl: course.thumbnailUrl,
      shortDescription: course.shortDescription || '',
      price: Number(course.currentPrice || 0),
      salePrice: Number(course.currentPrice || 0),
      originalPrice: Number(course.originalPrice ?? course.currentPrice ?? 0),
    })),
});

export const saveLearningPathCheckout = (path) => {
  const checkout = buildLearningPathCheckout(path);
  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(checkout));
  return checkout;
};

export const readLearningPathCheckout = () => {
  try {
    const parsed = JSON.parse(window.sessionStorage.getItem(STORAGE_KEY) || 'null');
    return parsed?.learningPathId && Array.isArray(parsed?.courses) ? parsed : null;
  } catch {
    return null;
  }
};

export const clearLearningPathCheckout = () => {
  window.sessionStorage.removeItem(STORAGE_KEY);
};
