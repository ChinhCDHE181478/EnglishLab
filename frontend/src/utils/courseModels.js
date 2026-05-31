import { catalogCourses, popularCourses } from '../components/course/courseData';

const buildFallbackModules = (course) => {
  const totalLessons = Math.max(Number(course.totalLessons || 0), 6);
  const moduleCount = Math.max(2, Math.min(4, Math.ceil(totalLessons / 6)));

  return Array.from({ length: moduleCount }, (_, moduleIndex) => {
    const lessonCount = Math.max(2, Math.ceil(totalLessons / moduleCount));
    return {
      id: `${course.id}-module-${moduleIndex + 1}`,
      title:
        moduleIndex === 0
          ? 'Khoi dong va dinh huong'
          : moduleIndex === moduleCount - 1
            ? 'Tong hop va luyen tap'
            : `Module ${moduleIndex + 1}`,
      description:
        moduleIndex === 0
          ? 'Lam quen muc tieu khoa hoc, cach hoc va cac tai nguyen tu hoc.'
          : 'He thong bai giang video, tai lieu va bai tap tu luyen theo tung muc tieu.',
      displayOrder: moduleIndex + 1,
      lessons: Array.from({ length: lessonCount }, (_, lessonIndex) => ({
        id: `${course.id}-lesson-${moduleIndex + 1}-${lessonIndex + 1}`,
        title: `Bai hoc ${moduleIndex + 1}.${lessonIndex + 1}`,
        description: 'Noi dung video, tai lieu doc va bai tap thuc hanh.',
        durationMinutes: 18 + lessonIndex * 6,
        displayOrder: lessonIndex + 1,
        preview: moduleIndex === 0 && lessonIndex === 0,
      })),
    };
  });
};

const buildFallbackCourse = (course, index) => ({
  id: `demo-${index + 1}`,
  slug: course.title.toLowerCase().replace(/[^a-z0-9]+/gi, '-').replace(/^-|-$/g, ''),
  title: course.title,
  shortDescription: course.description,
  description: `${course.description} EnglishLab xay dung lo trinh tu hoc ro rang, gom video bai giang, tai lieu doc va bai tap thuc hanh theo tung module.`,
  thumbnailUrl: course.image,
  category:
    course.title.toLowerCase().includes('toeic')
      ? 'TOEIC'
      : course.title.toLowerCase().includes('communication') || course.title.toLowerCase().includes('speaking') || course.title.toLowerCase().includes('pronunciation')
        ? 'COMMUNICATION'
        : 'IELTS',
  level: index < 2 ? 'BEGINNER' : 'INTERMEDIATE',
  targetScore: course.level || course.category || 'IELTS / TOEIC',
  duration: course.duration || '8 Tuan',
  price: index % 3 === 0 ? 0 : 2500000,
  totalLessons: index < 4 ? 24 : 12,
  totalHours: index < 4 ? 36 : 18,
  featured: index < 4,
  registered: false,
});

export const fallbackCourses = [...popularCourses, ...catalogCourses].map(buildFallbackCourse).map((course) => ({
  ...course,
  modules: buildFallbackModules(course),
}));

export const normalizeCourse = (course) => {
  const normalized = {
    ...course,
    id: course.id ?? course.courseId ?? course.packageId ?? course.slug,
    slug: course.slug ?? course.courseSlug ?? course.packageSlug,
    title: course.title ?? course.courseTitle ?? course.name ?? 'Khóa học EnglishLab',
    shortDescription: course.shortDescription ?? course.description ?? course.summary ?? 'Khóa học online tại EnglishLab.',
    description: course.description ?? course.shortDescription ?? course.summary ?? 'Khóa học online tại EnglishLab.',
    thumbnailUrl: course.thumbnailUrl ?? course.imageUrl ?? course.coverImageUrl ?? course.thumbnail ?? fallbackCourses[0]?.thumbnailUrl,
    category: course.category ?? course.categoryCode ?? course.type ?? 'ONLINE',
    categoryName: course.categoryName ?? course.category ?? 'Online',
    level: course.level ?? 'BEGINNER',
    duration: course.duration ?? course.durationText ?? '8 Tuan',
    price: course.price ?? course.salePrice ?? course.tuitionFee ?? 0,
    totalLessons: course.totalLessons ?? course.lessonCount ?? 0,
    totalHours: course.totalHours ?? course.hours ?? 0,
    featured: Boolean(course.featured ?? course.isFeatured),
    registered: Boolean(course.registered ?? course.enrolled),
    progressPercent: course.progressPercent ?? course.progress ?? 0,
    enrollmentCount: course.enrollmentCount ?? course.studentCount ?? course.totalEnrollments ?? 0,
    modules: course.modules ?? course.sections ?? [],
  };

  if (!normalized.modules?.length) {
    normalized.modules = buildFallbackModules(normalized);
  }

  return normalized;
};

export const normalizeEnrollment = (enrollment) => ({
  ...enrollment,
  id: enrollment.id ?? enrollment.packageId ?? enrollment.courseId,
  courseId: enrollment.courseId ?? enrollment.id ?? enrollment.packageId,
  courseSlug: enrollment.courseSlug ?? enrollment.slug,
  courseTitle: enrollment.courseTitle ?? enrollment.title ?? 'Khóa học EnglishLab',
  thumbnailUrl: enrollment.thumbnailUrl ?? fallbackCourses[0]?.thumbnailUrl,
  progressPercent: enrollment.progressPercent ?? 0,
  streakDays: enrollment.streakDays ?? 0,
  completedLessonIds: enrollment.completedLessonIds ?? [],
});

export const getCourseId = (course) => course?.id ?? course?.courseId ?? course?.packageId;

export const getCourseSlug = (course) => course?.slug ?? course?.courseSlug ?? String(getCourseId(course));

export const buildCourseDetailPath = (course) => `/courses/${getCourseSlug(course)}`;

export const buildCourseWorkspacePath = (course) => `/courses/${getCourseSlug(course)}/learn`;

export const mergeCourseRegistrations = (courses, enrollments) => {
  const registeredIds = new Set(enrollments.map((item) => String(item.courseId ?? item.packageId)).filter(Boolean));
  const registeredSlugs = new Set(enrollments.map((item) => item.courseSlug).filter(Boolean));
  return courses.map((course) => ({
    ...course,
    registered:
      course.registered
      || registeredIds.has(String(getCourseId(course)))
      || registeredSlugs.has(getCourseSlug(course)),
  }));
};

export const findFallbackCourse = (slugOrId) =>
  fallbackCourses.find((course) => course.slug === slugOrId || String(course.id) === String(slugOrId));
