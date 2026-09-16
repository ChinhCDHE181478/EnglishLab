import AssessmentExamBuilder from '../AssessmentExamBuilder';
import SkillWorkspaceFrame from './SkillWorkspaceFrame';

export default function ListeningPracticeWorkspace({ form, inline = false, onChange }) {
  return (
    <SkillWorkspaceFrame
      compact={inline}
      description="Biên soạn bài nghe với audio, transcript, từng phần nghe, nhóm câu hỏi, đáp án, giải thích và phần xem trước audio cùng câu hỏi."
      highlights={['Audio', 'Transcript', 'Listening parts', 'Nhóm câu hỏi', 'Đáp án', 'Preview audio + questions']}
      title="Listening editor"
    >
      <AssessmentExamBuilder
        assessment={{ ...form, skill: 'LISTENING', type: 'LESSON_PRACTICE' }}
        inline={inline}
        onChange={(field, value) => {
          if (field === 'skill' || field === 'type') return;
          onChange(field, value);
        }}
      />
    </SkillWorkspaceFrame>
  );
}
