package fu.sap490.g23.backend.dto.response.teacher;

import fu.sap490.g23.backend.entity.teacher.enums.TeacherEvaluationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherEvaluationResponse {
    private Long id;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal lessonDeliveryScore;
    private BigDecimal learnerSupportScore;
    private BigDecimal gradingTimelinessScore;
    private BigDecimal professionalismScore;
    private BigDecimal overallScore;
    private String strengths;
    private String improvementAreas;
    private String actionPlan;
    private TeacherEvaluationStatus status;
    private String evaluatorName;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
