import AssessmentExamBuilder from '../AssessmentExamBuilder';
import SkillWorkspaceFrame from './SkillWorkspaceFrame';

export default function SpeakingPracticeWorkspace({ form, inline = false, onChange }) {
  return (
    <SkillWorkspaceFrame
      compact={inline}
      description="Biên soạn đề nói với Part 1, thẻ chủ đề Part 2, Part 3, thời gian chuẩn bị, thời gian trả lời, tiêu chí chấm và phần xem trước luồng ghi âm."
      highlights={['Part 1', 'Thẻ chủ đề Part 2', 'Part 3', 'Thời gian chuẩn bị', 'Tiêu chí chấm', 'Xem trước ghi âm']}
      title="Speaking editor"
    >
      <AssessmentExamBuilder
        assessment={{ ...form, skill: 'SPEAKING', type: 'SPEAKING_TASK' }}
        inline={inline}
        onChange={(field, value) => {
          if (field === 'skill' || field === 'type') return;
          onChange(field, value);
        }}
      />
    </SkillWorkspaceFrame>
  );
}
