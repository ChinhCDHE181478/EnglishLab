const normalizeText = (value, fallback = '') => (typeof value === 'string' && value.trim() ? value.trim() : fallback);

export const fallbackCourses = [];

export const normalizeCourse = (course = {}) => {
  const id = course.id ?? course.courseId ?? course.packageId ?? course.slug ?? null;
  const slug = course.slug ?? course.courseSlug ?? (id != null ? String(id) : '');
  const modules = Array.isArray(course.modules) ? course.modules : Array.isArray(course.sections) ? course.sections : [];

  const normalized = {
    ...course,
    id,
    slug,
    title: normalizeText(course.title ?? course.courseTitle ?? course.name, 'Khóa học EnglishLab'),
    shortDescription: normalizeText(course.shortDescription ?? course.summary ?? course.description, 'Khóa học trực tuyến tại EnglishLab.'),
    description: normalizeText(course.description ?? course.shortDescription ?? course.summary, 'Khóa học trực tuyến tại EnglishLab.'),
    thumbnailUrl: normalizeText(course.thumbnailUrl ?? course.imageUrl ?? course.coverImageUrl ?? course.thumbnail),
    category: course.category ?? course.categoryCode ?? course.type ?? 'ONLINE',
    categoryName: normalizeText(course.categoryName ?? course.category, 'Trực tuyến'),
    level: course.level ?? 'BEGINNER',
    duration: normalizeText(course.duration ?? course.durationText, 'Tự học linh hoạt'),
    recommendedCurrentBandMin: course.recommendedCurrentBandMin ?? course.currentBandMin ?? null,
    recommendedCurrentBandMax: course.recommendedCurrentBandMax ?? course.currentBandMax ?? null,
    targetBand: course.targetBand ?? null,
    learningPathCode: course.learningPathCode ?? null,
    learningPathName: course.learningPathName ?? null,
    learningPathOrder: course.learningPathOrder ?? null,
    targetOutcome: normalizeText(course.targetOutcome ?? course.outcome),
    recommendedNextCourseSlug: course.recommendedNextCourseSlug ?? null,
    focusSkills: Array.isArray(course.focusSkills) ? course.focusSkills : [],
    prerequisites: Array.isArray(course.prerequisites) ? course.prerequisites : [],
    price: Number(course.salePrice ?? course.price ?? course.tuitionFee ?? 0),
    salePrice: Number(course.salePrice ?? course.price ?? course.tuitionFee ?? 0),
    originalPrice: Number(course.originalPrice ?? course.listPrice ?? course.price ?? course.salePrice ?? course.tuitionFee ?? 0),
    discountPercent: Number(course.discountPercent ?? 0),
    totalLessons: Number(course.totalLessons ?? course.lessonCount ?? 0),
    totalHours: Number(course.totalHours ?? course.hours ?? 0),
    featured: Boolean(course.featured ?? course.isFeatured),
    registered: Boolean(course.registered ?? course.enrolled),
    status: course.status ?? (course.registered || course.enrolled ? 'ENROLLED' : 'AVAILABLE'),
    progressPercent: Number(course.progressPercent ?? course.progress ?? 0),
    enrollmentCount: Number(course.enrollmentCount ?? course.studentCount ?? course.totalEnrollments ?? 0),
    averageRating: Number(course.averageRating ?? course.rating ?? 0),
    reviewCount: Number(course.reviewCount ?? course.totalReviews ?? 0),
    modules,
  };

  if (!normalized.discountPercent && normalized.originalPrice > normalized.salePrice && normalized.originalPrice > 0) {
    normalized.discountPercent = Math.round(((normalized.originalPrice - normalized.salePrice) / normalized.originalPrice) * 100);
  }

  return normalized;
};

export const normalizeEnrollment = (enrollment = {}) => ({
  ...enrollment,
  id: enrollment.id ?? enrollment.packageId ?? enrollment.courseId,
  courseId: enrollment.courseId ?? enrollment.id ?? enrollment.packageId,
  courseSlug: enrollment.courseSlug ?? enrollment.slug ?? '',
  courseTitle: normalizeText(enrollment.courseTitle ?? enrollment.title, 'Khóa học EnglishLab'),
  thumbnailUrl: normalizeText(enrollment.thumbnailUrl),
  progressPercent: Number(enrollment.progressPercent ?? 0),
  streakDays: Number(enrollment.streakDays ?? 0),
  completedLessonIds: Array.isArray(enrollment.completedLessonIds) ? enrollment.completedLessonIds : [],
});

export const getCourseId = (course) => course?.id ?? course?.courseId ?? course?.packageId;

export const getCourseSlug = (course) => course?.slug ?? course?.courseSlug ?? String(getCourseId(course) ?? '');

export const buildCourseDetailPath = (course) => `/courses/${getCourseSlug(course)}`;

export const buildCourseHomePath = (course) => `/courses/${getCourseSlug(course)}/home`;

export const buildCourseWorkspacePath = (course) => `/courses/${getCourseSlug(course)}/learn`;

export const mergeCourseRegistrations = (courses, enrollments) => {
  const registeredIds = new Set(enrollments.map((item) => String(item.courseId ?? item.packageId)).filter(Boolean));
  const registeredSlugs = new Set(enrollments.map((item) => item.courseSlug).filter(Boolean));
  return courses.map((course) => ({
    ...course,
    registered:
      Boolean(course.registered)
      || registeredIds.has(String(getCourseId(course)))
      || registeredSlugs.has(getCourseSlug(course)),
  }));
};

export const findFallbackCourse = () => null;
