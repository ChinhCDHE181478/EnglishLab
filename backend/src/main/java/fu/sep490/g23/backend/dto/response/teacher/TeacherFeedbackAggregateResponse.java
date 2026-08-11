package fu.sep490.g23.backend.dto.response.teacher;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TeacherFeedbackAggregateResponse {
    private Long teacherId;
    private String teacherName;
    private long responseCount;
    private int anonymityThreshold;
    private boolean protectedByAnonymity;
    private BigDecimal overallScore;
    private BigDecimal clarityScore;
    private BigDecimal engagementScore;
    private BigDecimal learnerSupportScore;
    private BigDecimal feedbackTimelinessScore;
    private BigDecimal professionalismScore;
    private BigDecimal recommendationPercent;
    private Map<String, Long> paceDistribution;
    private Map<Integer, Long> overallRatingDistribution;
    private List<ClassroomFeedbackAggregateResponse> classrooms;
}
