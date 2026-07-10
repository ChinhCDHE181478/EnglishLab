export const HOMEWORK_GRADING_MODES = [
  {
    value: 'TEACHER',
    label: 'Giáo viên tự chấm',
    description: 'Học viên nộp bài, giáo viên chấm điểm và nhận xét thủ công.',
  },
  {
    value: 'AI',
    label: 'AI chấm theo rubric',
    description: 'Hệ thống chấm tự động theo bộ tiêu chí đã chọn. Giáo viên vẫn có thể xem lại và chỉnh điểm.',
  },
];

export const HOMEWORK_SKILLS = [
  {
    value: 'SPEAKING',
    label: 'Speaking',
    description: 'Bài nói / ghi âm. AI chấm theo 4 tiêu chí IELTS Speaking.',
    aiSupported: true,
  },
  {
    value: 'WRITING',
    label: 'Writing',
    description: 'Bài viết / luận. AI chấm theo 4 tiêu chí IELTS Writing Task 2.',
    aiSupported: true,
  },
  {
    value: 'LISTENING',
    label: 'Listening',
    description: 'Nộp đáp án Listening. AI kiểm tra độ chính xác và phân tích lỗi nghe.',
    aiSupported: true,
  },
  {
    value: 'READING',
    label: 'Reading',
    description: 'Nộp đáp án Reading. AI kiểm tra độ chính xác và phân tích lỗi đọc hiểu.',
    aiSupported: true,
  },
  {
    value: 'VOCABULARY',
    label: 'Vocabulary',
    description: 'Bài dùng từ vựng mục tiêu trong câu/đoạn. AI chấm nghĩa, collocation, câu.',
    aiSupported: true,
  },
];

export const getHomeworkSkillLabel = (skill) => (
  HOMEWORK_SKILLS.find((item) => item.value === skill)?.label || skill || '—'
);

export const getHomeworkGradingModeLabel = (mode) => (
  HOMEWORK_GRADING_MODES.find((item) => item.value === mode)?.label || 'Giáo viên tự chấm'
);

export const isAiGradedHomework = (homework) => homework?.gradingMode === 'AI';

export const getHomeworkFeedbackLabel = (homework) => (
  isAiGradedHomework(homework) ? 'Nhận xét AI' : 'Nhận xét từ giảng viên'
);

export const getHomeworkGradingHint = (homework) => {
  if (!isAiGradedHomework(homework)) return null;
  const skill = getHomeworkSkillLabel(homework.skill);
  return `Bài ${skill} này được AI chấm theo rubric ngay sau khi nộp. Giáo viên vẫn có thể xem lại và chỉnh điểm.`;
};
