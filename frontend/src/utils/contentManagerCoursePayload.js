export function buildManagedCoursePayload(course, overrides = {}) {
  const source = { ...course, ...overrides };
  const modules = Array.isArray(source.modules) ? source.modules : [];

  return {
    title: source.title || '',
    shortDescription: source.shortDescription || '',
    description: source.description || '',
    category: source.category || 'ONLINE',
    level: source.level || 'BEGINNER',
    status: source.status || 'DRAFT',
    targetScore: source.targetScore || '',
    recommendedCurrentBandMin: nullableNumber(source.recommendedCurrentBandMin),
    targetBand: nullableNumber(source.targetBand),
    learningPathCode: cleanNullable(source.learningPathCode),
    learningPathName: cleanNullable(source.learningPathName),
    learningPathOrder: nullableNumber(source.learningPathOrder),
    targetOutcome: cleanNullable(source.targetOutcome),
    recommendedNextCourseSlug: cleanNullable(source.recommendedNextCourseSlug),
    duration: source.duration || '',
    price: Number(source.price || 0),
    salePrice: source.salePrice == null || source.salePrice === '' ? null : Number(source.salePrice),
    thumbnailUrl: source.thumbnailUrl || '',
    featured: Boolean(source.featured),
    modules: modules.map((module, moduleIndex) => ({
      id: module.id,
      title: module.title || `Mô-đun ${moduleIndex + 1}`,
      description: module.description || '',
      displayOrder: Number(module.displayOrder || moduleIndex + 1),
      lessons: (module.lessons || []).map((lesson, lessonIndex) => ({
        id: lesson.id,
        title: lesson.title || `Bài học ${lessonIndex + 1}`,
        description: lesson.description || '',
        contentType: String(lesson.contentType || 'VIDEO').toUpperCase(),
        contentText: lesson.contentText || '',
        videoUrl: lesson.videoUrl || '',
        materialUrl: lesson.materialUrl || '',
        transcriptSegments: lesson.transcriptSegments || [],
        flashcardSetIds: (lesson.flashcardSets || []).length
          ? lesson.flashcardSets.map((set) => Number(set.id)).filter(Boolean)
          : (lesson.flashcardSetIds || []).map((id) => Number(id)).filter(Boolean),
        durationMinutes: Number(lesson.durationMinutes || 0),
        displayOrder: Number(lesson.displayOrder || lessonIndex + 1),
        preview: Boolean(lesson.preview),
      })),
    })),
  };
}

function nullableNumber(value) {
  if (value == null || value === '') return null;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function cleanNullable(value) {
  const cleaned = String(value ?? '').trim();
  return cleaned || null;
}
