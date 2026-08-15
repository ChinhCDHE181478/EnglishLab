export const LESSON_POSITION_STATUS = {
  PASSED: 'PASSED',
  CURRENT: 'CURRENT',
  NOT_REACHED: 'NOT_REACHED',
  UNASSIGNED: 'UNASSIGNED',
};

export const LESSON_GRADING_STATUS = {
  NOT_STARTED: 'NOT_STARTED',
  PENDING: 'PENDING',
  GRADED: 'GRADED',
  INCOMPLETE: 'INCOMPLETE',
};

const getHomeworkSortValue = (homework) => {
  const deadline = homework?.deadline ? new Date(homework.deadline).getTime() : Number.NaN;
  return Number.isFinite(deadline) ? deadline : Number(homework?.id || 0);
};

const sortHomeworksByLearningOrder = (homeworks) => [...homeworks].sort((left, right) => (
  getHomeworkSortValue(left) - getHomeworkSortValue(right)
));

const buildLessonStats = (assignments, gradebook) => {
  const homeworkIds = new Set(assignments.map((assignment) => assignment.id));
  const statuses = gradebook.flatMap((entry) => (
    (entry.homeworks || [])
      .filter((homework) => homeworkIds.has(homework.id))
      .map((homework) => homework.status || 'NOT_SUBMITTED')
  ));
  const expectedSubmissionCount = assignments.length * gradebook.length;
  const knownSubmissionCount = statuses.length;
  const missingRecordCount = Math.max(expectedSubmissionCount - knownSubmissionCount, 0);
  const gradedCount = statuses.filter((status) => status === 'GRADED').length;
  const pendingCount = statuses.filter((status) => status === 'SUBMITTED').length;
  const returnedCount = statuses.filter((status) => status === 'RETURNED').length;
  const notSubmittedCount = statuses.filter((status) => status === 'NOT_SUBMITTED').length + missingRecordCount;

  return {
    assignmentCount: assignments.length,
    expectedSubmissionCount,
    gradedCount,
    pendingCount,
    returnedCount,
    notSubmittedCount,
    completionPercent: expectedSubmissionCount > 0
      ? Math.round((gradedCount / expectedSubmissionCount) * 100)
      : 0,
  };
};

const resolveGradingStatus = (stats) => {
  if (stats.pendingCount > 0) return LESSON_GRADING_STATUS.PENDING;
  if (stats.expectedSubmissionCount > 0 && stats.gradedCount === stats.expectedSubmissionCount) {
    return LESSON_GRADING_STATUS.GRADED;
  }
  if (stats.expectedSubmissionCount > 0) return LESSON_GRADING_STATUS.INCOMPLETE;
  return LESSON_GRADING_STATUS.NOT_STARTED;
};

const resolvePositionStatus = ({ displayOrder, currentDisplayOrder }) => {
  if (currentDisplayOrder != null && displayOrder > currentDisplayOrder) {
    return LESSON_POSITION_STATUS.NOT_REACHED;
  }
  if (displayOrder === currentDisplayOrder) return LESSON_POSITION_STATUS.CURRENT;
  return LESSON_POSITION_STATUS.PASSED;
};

export const buildGradebookLessons = ({
  curriculumUnits = [],
  gradebook = [],
  homeworks = [],
  sessions = [],
}) => {
  const sortedUnits = [...curriculumUnits].sort((left, right) => (
    Number(left.displayOrder || 0) - Number(right.displayOrder || 0)
  ));
  const completedSessionCount = sessions.filter((session) => session.status === 'COMPLETED').length;
  const lessonCount = sortedUnits.length || homeworks.length;
  const currentDisplayOrder = completedSessionCount < lessonCount
    ? completedSessionCount + 1
    : null;

  if (!sortedUnits.length) {
    return sortHomeworksByLearningOrder(homeworks).map((homework, index) => {
      const displayOrder = index + 1;
      const assignments = [homework];
      const stats = buildLessonStats(assignments, gradebook);
      return {
        id: `homework-${homework.id}`,
        curriculumUnitId: homework.curriculumUnitId || null,
        displayOrder,
        title: homework.curriculumUnitTitle || homework.title || `Bài ${displayOrder}`,
        description: homework.curriculumUnitTitle ? homework.title : '',
        assignments,
        stats,
        positionStatus: resolvePositionStatus({ displayOrder, currentDisplayOrder }),
        gradingStatus: resolveGradingStatus(stats),
      };
    });
  }

  const unitIds = new Set(sortedUnits.map((unit) => unit.id));
  const lessons = sortedUnits.map((unit, index) => {
    const displayOrder = Number(unit.displayOrder || index + 1);
    const assignments = sortHomeworksByLearningOrder(
      homeworks.filter((homework) => homework.curriculumUnitId === unit.id)
    );
    const stats = buildLessonStats(assignments, gradebook);
    return {
      id: `unit-${unit.id}`,
      curriculumUnitId: unit.id,
      displayOrder,
      title: unit.title || `Bài ${displayOrder}`,
      description: unit.description || unit.sessionPlan || '',
      assignments,
      stats,
      positionStatus: resolvePositionStatus({ displayOrder, currentDisplayOrder }),
      gradingStatus: resolveGradingStatus(stats),
    };
  });

  const unassignedHomeworks = sortHomeworksByLearningOrder(
    homeworks.filter((homework) => !homework.curriculumUnitId || !unitIds.has(homework.curriculumUnitId))
  );
  if (unassignedHomeworks.length) {
    const stats = buildLessonStats(unassignedHomeworks, gradebook);
    lessons.push({
      id: 'unassigned',
      curriculumUnitId: null,
      displayOrder: null,
      title: 'Bài tập chưa xếp bài học',
      description: 'Các bài tập này chưa được gắn với một bài trong giáo trình.',
      assignments: unassignedHomeworks,
      stats,
      positionStatus: LESSON_POSITION_STATUS.UNASSIGNED,
      gradingStatus: resolveGradingStatus(stats),
    });
  }

  return lessons;
};

export const getStudentLessonProgress = (entry, assignments) => {
  const homeworkById = new Map((entry.homeworks || []).map((homework) => [homework.id, homework]));
  const results = assignments.map((assignment) => ({
    ...assignment,
    ...(homeworkById.get(assignment.id) || {}),
    id: assignment.id,
    title: assignment.title,
    maxScore: assignment.maxScore ?? homeworkById.get(assignment.id)?.maxScore ?? 10,
    status: homeworkById.get(assignment.id)?.status || 'NOT_SUBMITTED',
    score: homeworkById.get(assignment.id)?.score ?? null,
  }));
  const gradedCount = results.filter((homework) => homework.status === 'GRADED').length;
  const pendingCount = results.filter((homework) => homework.status === 'SUBMITTED').length;

  return {
    results,
    gradedCount,
    pendingCount,
    isComplete: results.length > 0 && gradedCount === results.length,
  };
};

const HOMEWORK_GRADING_PRIORITY = {
  SUBMITTED: 0,
  RETURNED: 1,
  GRADED: 2,
  NOT_SUBMITTED: 3,
};

export const orderHomeworkGradingChoices = (homeworks = []) => (
  [...homeworks].sort((left, right) => (
    (HOMEWORK_GRADING_PRIORITY[left.status] ?? 4)
    - (HOMEWORK_GRADING_PRIORITY[right.status] ?? 4)
  ))
);
