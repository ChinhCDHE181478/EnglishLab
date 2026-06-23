import { clampBand, normalizeBandThreshold } from './ieltsBandScale';

const toNumber = (value) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

export const formatBandValue = (value) => {
  const parsed = clampBand(value) ?? toNumber(value);
  if (parsed == null) return '';
  return Number.isInteger(parsed) ? parsed.toFixed(1) : String(parsed);
};

const isUnavailableCourse = (course) => ['PAUSED', 'UNAVAILABLE'].includes(String(course?.status || '').toUpperCase());

const isCourseTooEasy = (course, currentBand) => {
  const current = toNumber(currentBand);
  const max = toNumber(course?.recommendedCurrentBandMax);
  const target = toNumber(course?.targetBand);

  if (current == null || max == null) return false;
  if (current <= max) return false;
  if (target != null && target > current) return false;
  return true;
};

export const formatBandRangeText = (course) => {
  const min = toNumber(course?.recommendedCurrentBandMin);
  const max = toNumber(course?.recommendedCurrentBandMax);
  if (min == null || max == null) {
    return 'Khóa học này chưa có thông tin trình độ khuyến nghị.';
  }
  return `Phù hợp với học viên Band ${formatBandValue(min)} đến ${formatBandValue(max)}`;
};

export const getBandFitInfo = (course, currentBand) => {
  const min = toNumber(course?.recommendedCurrentBandMin);
  const max = toNumber(course?.recommendedCurrentBandMax);
  const current = toNumber(currentBand);

  if (current == null || current <= 0) {
    return {
      tone: 'neutral',
      message: 'Cập nhật trình độ hiện tại để nhận gợi ý khóa học chính xác hơn.',
    };
  }

  if (min == null || max == null) {
    return {
      tone: 'neutral',
      message: 'Khóa học này chưa có thông tin trình độ khuyến nghị.',
    };
  }

  if (current < min) {
    return {
      tone: 'warning',
      message: 'Bạn nên học khóa nền tảng trước khi bắt đầu khóa này.',
    };
  }

  if (current > max) {
    return {
      tone: 'soft',
      message: 'Khóa học này có thể hơi dễ so với trình độ hiện tại của bạn.',
    };
  }

  return {
    tone: 'good',
    message: 'Phù hợp với trình độ hiện tại của bạn.',
  };
};

export const resolveAssessmentPassingThreshold = (assessment, course = null) => {
  if (assessment?.resolvedPassingThreshold != null) {
    return normalizeBandThreshold(assessment, assessment.resolvedPassingThreshold);
  }
  if (toNumber(assessment?.passingScore) != null) {
    return normalizeBandThreshold(assessment, assessment.passingScore);
  }

  if (String(assessment?.type || '').toUpperCase() === 'MODULE_TEST' && course?.targetBand != null) {
    return normalizeBandThreshold(assessment, Number(course.targetBand) - 0.5);
  }

  return null;
};

const formatThresholdValue = (value) => {
  const parsed = toNumber(value);
  if (parsed == null) return '';
  return Number.isInteger(parsed) ? parsed.toFixed(1) : String(parsed);
};

export const formatPassingThresholdLabel = (assessment, course = null) => {
  if (assessment?.passingThresholdLabel) {
    return assessment.passingThresholdLabel;
  }

  const threshold = resolveAssessmentPassingThreshold(assessment, course);
  if (threshold == null) {
    return null;
  }

  const formatted = formatThresholdValue(threshold);
  if (toNumber(assessment?.passingScore) != null) {
    return `Ngưỡng đạt (cấu hình CMS): ${formatted}`;
  }
  if (String(assessment?.type || '').toUpperCase() === 'MODULE_TEST' && course?.targetBand != null) {
    return `Ngưỡng đạt (band mục tiêu khóa - 0.5): ${formatted}`;
  }
  return null;
};

export const isAssessmentPassed = (assessment, course = null) => {
  const latestStatus = String(assessment?.latestSubmission?.status || '');
  if (latestStatus === 'PASSED') return true;
  if (latestStatus === 'NEEDS_IMPROVEMENT') return false;

  const score = toNumber(assessment?.latestSubmission?.aiScore);
  const threshold = resolveAssessmentPassingThreshold(assessment, course);
  if (score != null && threshold != null) {
    return score >= threshold;
  }

  if (latestStatus === 'AI_EVALUATED' && threshold == null) {
    return true;
  }
  return false;
};

const isAssessmentSubmitted = (assessment) => Boolean(assessment?.latestSubmission?.id);

const resolveCompletionStatusLabel = ({
  lessonPercent,
  requiredAssessments,
  submittedAssessments,
  failedAssessments,
  passedAssessments,
}) => {
  if (lessonPercent <= 0) return 'Chưa bắt đầu';
  if (lessonPercent < 100) return 'Đang học';
  if (failedAssessments.length) return 'Bạn cần cải thiện kết quả để hoàn thành khóa học';
  if (requiredAssessments.length && submittedAssessments.length < requiredAssessments.length) return 'Cần hoàn thành bài đánh giá';
  if (requiredAssessments.length && passedAssessments.length < requiredAssessments.length) return 'Cần hoàn thành bài đánh giá';
  return 'Đủ điều kiện nhận chứng nhận';
};

export const calculateCompletionStatus = ({ course, enrollment, assessments = [] }) => {
  const totalLessons = Math.max(0, Number(course?.totalLessons || 0));
  const completedLessons = Array.isArray(enrollment?.completedLessonIds) ? enrollment.completedLessonIds.length : 0;
  const lessonPercent = totalLessons ? Math.min(100, Math.round((completedLessons / totalLessons) * 100)) : 0;
  const totalModules = Math.max(0, Number(course?.modules?.length || 0));
  const completedModules = Math.min(totalModules, Math.floor((lessonPercent / 100) * totalModules) || 0);
  const modulePercent = totalModules ? Math.min(100, Math.round((completedModules / totalModules) * 100)) : 0;

  const requiredAssessments = (assessments || []).filter((item) => item?.active !== false);
  const submittedAssessments = requiredAssessments.filter(isAssessmentSubmitted);
  const passedAssessments = requiredAssessments.filter((item) => isAssessmentPassed(item, course));
  const failedAssessments = requiredAssessments.filter((item) => String(item?.latestSubmission?.status || '') === 'NEEDS_IMPROVEMENT');
  const assessmentPercent = requiredAssessments.length ? Math.round((passedAssessments.length / requiredAssessments.length) * 100) : 100;

  let statusLabel = resolveCompletionStatusLabel({
    lessonPercent,
    requiredAssessments,
    submittedAssessments,
    failedAssessments,
    passedAssessments,
  });

  const eligibleForCertificate = lessonPercent >= 100 && failedAssessments.length === 0 && passedAssessments.length >= requiredAssessments.length;
  if (Number(enrollment?.progressPercent || 0) >= 100 && eligibleForCertificate) {
    statusLabel = 'Đã hoàn thành';
  }

  const latestPassedSubmission = [...passedAssessments]
    .map((item) => item?.latestSubmission)
    .filter(Boolean)
    .sort((left, right) => new Date(right.submittedAt || 0).getTime() - new Date(left.submittedAt || 0).getTime())[0];

  return {
    lessonPercent,
    modulePercent,
    assessmentPercent,
    completedLessons,
    totalLessons,
    completedModules,
    totalModules,
    totalAssessments: requiredAssessments.length,
    submittedAssessments: submittedAssessments.length,
    passedAssessments: passedAssessments.length,
    failedAssessments: failedAssessments.length,
    statusLabel,
    eligibleForCertificate,
    completedAt: latestPassedSubmission?.submittedAt || enrollment?.updatedAt || enrollment?.enrolledAt || null,
    reasons: {
      missingAssessment: requiredAssessments.length > submittedAssessments.length,
      needsImprovement: failedAssessments.length > 0,
      missingLessonProgress: lessonPercent < 100,
    },
  };
};

export const buildRecommendationReason = ({ course, currentBand, targetBand = null }) => {
  const min = toNumber(course?.recommendedCurrentBandMin);
  const max = toNumber(course?.recommendedCurrentBandMax);
  const courseTargetBand = toNumber(course?.targetBand);
  const current = toNumber(currentBand);
  const learnerTargetBand = toNumber(targetBand);

  if (current != null && min != null && max != null && current >= min && current <= max) {
    return 'Phù hợp với band hiện tại của bạn.';
  }

  if (learnerTargetBand != null && courseTargetBand != null && Math.abs(courseTargetBand - learnerTargetBand) <= 0.5) {
    return `Khớp sát với mục tiêu band ${formatBandValue(learnerTargetBand)} của bạn.`;
  }

  if (current != null && courseTargetBand != null && courseTargetBand > current) {
    return `Giúp bạn tiến tới mục tiêu band ${formatBandValue(courseTargetBand)}`;
  }

  return 'Khóa học phù hợp với mục tiêu học hiện tại của bạn.';
};

export const recommendCoursesForLearner = ({ courses, enrollments, currentBand, targetBand = null }) => {
  const registeredIds = new Set((enrollments || []).map((item) => String(item.courseId || item.id)));
  const learnerTargetBand = toNumber(targetBand);

  return (courses || [])
    .filter((course) => !registeredIds.has(String(course.id)) && !isUnavailableCourse(course) && !isCourseTooEasy(course, currentBand))
    .map((course) => {
      const fit = getBandFitInfo(course, currentBand);
      const courseTargetBand = toNumber(course?.targetBand);

      let score = 0;
      if (fit.tone === 'good') score += 5;
      if (fit.tone === 'warning') score -= 2;
      if (courseTargetBand != null && toNumber(currentBand) != null && courseTargetBand > toNumber(currentBand)) score += 3;
      if (learnerTargetBand != null && courseTargetBand != null) {
        const distance = Math.abs(courseTargetBand - learnerTargetBand);
        if (distance <= 0.25) score += 4;
        else if (distance <= 0.5) score += 3;
        else if (distance <= 1) score += 1;
        if (courseTargetBand > learnerTargetBand + 1) score -= 2;
      }
      if (course.discountPercent) score += 1;

      return {
        ...course,
        recommendationReason: buildRecommendationReason({ course, currentBand, targetBand: learnerTargetBand }),
        recommendationScore: score,
      };
    })
    .sort((left, right) => right.recommendationScore - left.recommendationScore)
    .slice(0, 4);
};

export const summarizeCourseReviews = (reviews, courseId) => {
  const filtered = (reviews || []).filter((item) => String(item.courseId) === String(courseId));
  const total = filtered.length;
  const average = total
    ? Number((filtered.reduce((sum, item) => sum + Number(item.rating || 0), 0) / total).toFixed(1))
    : 0;

  return {
    total,
    average,
    items: filtered.sort((left, right) => new Date(right.updatedAt || right.createdAt).getTime() - new Date(left.updatedAt || left.createdAt).getTime()),
  };
};
