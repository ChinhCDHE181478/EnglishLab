import { describe, expect, it } from 'vitest';
import {
  countActuallyCompletedSteps,
  getLearningPathStepLabel,
  getPlacementScoreLabel,
  groupPlacementRecommendations,
  shouldShowPlacementRecommendations,
} from './placementRecommendation';

describe('placement recommendation helpers', () => {
  it('uses Band for IELTS and Điểm for TOEIC', () => {
    expect(getPlacementScoreLabel('IELTS')).toBe('Band');
    expect(getPlacementScoreLabel('TOEIC')).toBe('Điểm');
  });

  it('labels waived steps without counting them as completed', () => {
    expect(getLearningPathStepLabel('PLACEMENT_WAIVED')).toBe('Bỏ qua theo kết quả đầu vào');
    expect(countActuallyCompletedSteps([
      { stepStatus: 'PLACEMENT_WAIVED' },
      { stepStatus: 'COMPLETED', completed: true },
    ])).toBe(1);
  });

  it('groups instructor-led and online recommendations', () => {
    const grouped = groupPlacementRecommendations({
      recommendedInstructorLedCourses: [{ id: 1 }, { id: 2 }],
      recommendedOnlineCourses: [{ id: 3 }],
    });
    expect(grouped.instructorLed).toHaveLength(2);
    expect(grouped.online).toHaveLength(1);
  });

  it('hides final recommendations while manual review is pending', () => {
    expect(shouldShowPlacementRecommendations({ recommendationReady: false })).toBe(false);
    expect(shouldShowPlacementRecommendations({ recommendationReady: true })).toBe(true);
  });
});
