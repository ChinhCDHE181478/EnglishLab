package fu.sap490.g23.backend.dto.response.teacher;

import fu.sap490.g23.backend.entity.teacher.enums.TeacherFeedbackPace;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AnonymizedTeacherFeedbackResponse {
    private Long feedbackId;
    private Long classroomId;
    private String classroomTitle;
    private BigDecimal overallScore;
    private int clarityScore;
    private int engagementScore;
    private int learnerSupportScore;
    private int feedbackTimelinessScore;
    private int professionalismScore;
    private TeacherFeedbackPace pace;
    private boolean wouldRecommend;
    private String strengths;
    private String improvementSuggestions;
    private String additionalComment;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
