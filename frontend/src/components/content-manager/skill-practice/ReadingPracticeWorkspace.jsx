import AssessmentExamBuilder from '../AssessmentExamBuilder';
import SkillWorkspaceFrame from './SkillWorkspaceFrame';

export default function ReadingPracticeWorkspace({ form, onChange }) {
  return (
    <SkillWorkspaceFrame
      description="Biên soạn bài đọc với passage, đánh số đoạn văn, nhóm câu hỏi, đáp án, evidence theo đoạn hoặc dòng, giải thích và phần xem trước passage cùng câu hỏi."
      highlights={['Passage', 'Đoạn văn', 'Nhóm câu hỏi', 'Đáp án', 'Evidence', 'Preview passage + questions']}
      title="Reading editor"
    >
      <AssessmentExamBuilder
        assessment={{ ...form, skill: 'READING', type: 'LESSON_PRACTICE' }}
        onChange={(field, value) => {
          if (field === 'skill' || field === 'type') return;
          onChange(field, value);
        }}
      />
    </SkillWorkspaceFrame>
  );
}
