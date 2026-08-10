package fu.sap490.g23.backend.dto.response.teacher;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ClassroomFeedbackAggregateResponse {
    private Long classroomId;
    private String classroomTitle;
    private LocalDate endDate;
    private long responseCount;
    private BigDecimal overallScore;
    private BigDecimal recommendationPercent;
}
