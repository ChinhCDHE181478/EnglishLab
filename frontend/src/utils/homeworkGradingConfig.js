export const HOMEWORK_GRADING_MODES = [
  {
    value: 'AUTO',
    label: 'Hệ thống tự chấm',
    description: 'Đáp án được đối chiếu với answer key ngay khi học viên nộp và giáo viên chỉ xem lại kết quả.',
  },
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

export const HOMEWORK_ACTIVITY_TYPES = [
  {
    value: 'TEXT_RESPONSE',
    label: 'Làm bài trực tiếp',
    description: 'Học viên viết hoặc ghi âm câu trả lời ngay trên hệ thống, không cần nộp tệp.',
  },
  {
    value: 'FILE_RESPONSE',
    label: 'Nộp file',
    description: 'Giáo viên giao đề/tài liệu, học viên tải file bài làm lên để giáo viên nhận lại file.',
  },
  {
    value: 'SKILL_PRACTICE',
    label: 'Bài trắc nghiệm',
    description: 'Học viên trả lời từng câu; hệ thống đối chiếu đáp án và tính điểm ngay khi nộp.',
  },
  {
    value: 'FLASHCARD_REVIEW',
    label: 'Ôn flashcard theo unit',
    description: 'Bài giao yêu cầu học viên ôn bộ flashcard của unit/chương trình học.',
  },
  {
    value: 'MIXED',
    label: 'Bài tổng hợp',
    description: 'Học viên làm bài trực tiếp trên hệ thống và nhận thêm tệp đề hoặc tài liệu giáo viên đính kèm.',
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
    description: 'Nộp đáp án Listening. Giáo viên review theo answer key có sẵn.',
    aiSupported: false,
  },
  {
    value: 'READING',
    label: 'Reading',
    description: 'Nộp đáp án Reading. Giáo viên review theo answer key có sẵn.',
    aiSupported: false,
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

export const getHomeworkActivityTypeLabel = (type) => (
  HOMEWORK_ACTIVITY_TYPES.find((item) => item.value === type)?.label || 'Làm bài trực tiếp'
);

export const isAiGradedHomework = (homework) => homework?.gradingMode === 'AI';

export const isAutoGradedHomework = (homework) => homework?.gradingMode === 'AUTO';

export const getHomeworkFeedbackLabel = (homework) => (
  isAiGradedHomework(homework) ? 'Nhận xét AI' : 'Nhận xét từ giảng viên'
);

export const getHomeworkGradingHint = (homework) => {
  if (!isAiGradedHomework(homework)) return null;
  const skill = getHomeworkSkillLabel(homework.skill);
  return `Bài ${skill} này được AI chấm theo rubric ngay sau khi nộp. Giáo viên vẫn có thể xem lại và chỉnh điểm.`;
};
