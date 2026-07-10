import AssessmentExamBuilder from '../AssessmentExamBuilder';
import SkillWorkspaceFrame from './SkillWorkspaceFrame';

export default function SpeakingPracticeWorkspace({ form, onChange }) {
  return (
    <SkillWorkspaceFrame
      description="Biên soạn đề nói với Part 1, Part 2 cue card, Part 3, thời gian chuẩn bị, thời gian trả lời, rubric và recording flow preview."
      highlights={['Part 1', 'Part 2 cue card', 'Part 3', 'Thời gian chuẩn bị', 'Rubric', 'Recording flow preview']}
      title="Speaking editor"
    >
      <AssessmentExamBuilder
        assessment={{ ...form, skill: 'SPEAKING', type: 'SPEAKING_TASK' }}
        onChange={(field, value) => {
          if (field === 'skill' || field === 'type') return;
          onChange(field, value);
        }}
      />
    </SkillWorkspaceFrame>
  );
}
