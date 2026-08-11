package fu.sep490.g23.backend.dto.response.teacher;

import fu.sep490.g23.backend.entity.teacher.enums.TeacherFeedbackPace;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LearnerTeacherFeedbackResponse {
    private Long feedbackId;
    private Long classroomId;
    private String classroomTitle;
    private Long teacherId;
    private String teacherName;
    private LocalDate opensOn;
    private LocalDate closesOn;
    private boolean windowOpen;
    private boolean submitted;
    private boolean editable;
    private int clarityScore;
    private int engagementScore;
    private int learnerSupportScore;
    private int feedbackTimelinessScore;
    private int professionalismScore;
    private TeacherFeedbackPace pace;
    private Boolean wouldRecommend;
    private String strengths;
    private String improvementSuggestions;
    private String additionalComment;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
