export const PLACEMENT_LEVEL_OPTIONS = [
  { value: 'BEGINNER', label: 'Cơ bản' },
  { value: 'INTERMEDIATE', label: 'Trung cấp' },
  { value: 'ADVANCED', label: 'Nâng cao' },
];

export const getPlacementLevelLabel = (value) => (
  PLACEMENT_LEVEL_OPTIONS.find((option) => option.value === value)?.label || value || 'Chưa xác định'
);

export const getDeliveryModeLabel = (value) => (
  value === 'VIRTUAL' ? 'Virtual' : value === 'OFFLINE' ? 'Tại trung tâm' : value || 'Online'
);

export const getPlacementScoreLabel = (examType) => (
  String(examType || '').toUpperCase() === 'TOEIC' ? 'Điểm' : 'Band'
);

export const getLearningPathStepLabel = (status) => ({
  PLACEMENT_WAIVED: 'Bỏ qua theo kết quả đầu vào',
  CURRENT: 'Khuyến nghị hiện tại',
  COMPLETED: 'Đã hoàn thành',
  NEXT: 'Tiếp theo',
  LOCKED: 'Chưa mở',
}[status] || 'Chưa mở');

export const countActuallyCompletedSteps = (courses = []) => (
  courses.filter((course) => course.stepStatus === 'COMPLETED' || course.completed).length
);

export const groupPlacementRecommendations = (recommendation = {}) => ({
  offline: (recommendation.recommendedTrainingPrograms || []).filter((item) => item.deliveryMode === 'OFFLINE'),
  virtual: (recommendation.recommendedTrainingPrograms || []).filter((item) => item.deliveryMode === 'VIRTUAL'),
  online: recommendation.recommendedOnlineCourses || [],
});

export const shouldShowPlacementRecommendations = (recommendation) => (
  Boolean(recommendation?.recommendationReady)
);
