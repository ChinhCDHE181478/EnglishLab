import AssessmentExamBuilder from '../AssessmentExamBuilder';
import SkillWorkspaceFrame from './SkillWorkspaceFrame';

export default function WritingPracticeWorkspace({ form, onChange }) {
  return (
    <SkillWorkspaceFrame
      description="Biên soạn đề viết với Task 1 hoặc Task 2, đề bài, ảnh hoặc biểu đồ cho Task 1, số từ tối thiểu, thời gian gợi ý, tiêu chí chấm, bài mẫu và phần xem trước."
      highlights={['Task 1 / Task 2', 'Đề bài', 'Ảnh/biểu đồ', 'Số từ tối thiểu', 'Tiêu chí chấm', 'Bài mẫu']}
      title="Writing editor"
    >
      <AssessmentExamBuilder
        assessment={{ ...form, skill: 'WRITING', type: 'WRITING_TASK' }}
        onChange={(field, value) => {
          if (field === 'skill' || field === 'type') return;
          onChange(field, value);
        }}
      />
    </SkillWorkspaceFrame>
  );
}
