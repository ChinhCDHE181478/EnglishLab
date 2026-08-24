const hasSubmission = (assessment) => Boolean(assessment?.latestSubmission?.id);

export const findFurthestReachedModuleIndex = ({
  modules = [],
  completedLessonIds = new Set(),
  assessmentsByModule = new Map(),
  getLessonId,
}) => {
  let furthestIndex = -1;

  modules.forEach((module, moduleIndex) => {
    const hasLessonProgress = (module.lessons || []).some((lesson, lessonIndex) => (
      completedLessonIds.has(getLessonId(module, lesson, lessonIndex))
    ));
    const moduleAssessments = assessmentsByModule.get(String(module.id)) || [];
    const courseAssessments = moduleIndex === modules.length - 1
      ? (assessmentsByModule.get('course') || [])
      : [];
    const hasAssessmentProgress = [...moduleAssessments, ...courseAssessments].some(hasSubmission);

    if (hasLessonProgress || hasAssessmentProgress) {
      furthestIndex = moduleIndex;
    }
  });

  return furthestIndex;
};

export const isReachedModuleUnlocked = ({
  sequentiallyUnlocked,
  moduleIndex,
  furthestReachedModuleIndex,
  previousAssessmentsReady = true,
}) => sequentiallyUnlocked
  || (previousAssessmentsReady && moduleIndex <= furthestReachedModuleIndex);

