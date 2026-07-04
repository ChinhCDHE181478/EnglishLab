import AssessmentExamBuilder from '../AssessmentExamBuilder';
import SkillWorkspaceFrame from './SkillWorkspaceFrame';

export default function WritingPracticeWorkspace({ form, onChange }) {
  return (
    <SkillWorkspaceFrame
      description="Biên soạn đề viết với Task 1 hoặc Task 2, prompt, ảnh hoặc biểu đồ cho Task 1, số từ tối thiểu, thời gian gợi ý, rubric, sample answer và preview writing workspace."
      highlights={['Task 1 / Task 2', 'Prompt', 'Image/chart', 'Minimum words', 'Rubric', 'Sample answer']}
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
