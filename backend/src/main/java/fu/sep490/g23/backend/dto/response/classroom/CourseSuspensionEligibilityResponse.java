package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CourseSuspensionEligibilityResponse {
    private Long enrollmentId;
    private Long classSectionId;
    private String classroomTitle;
    private String courseTitle;
    private BigDecimal tuitionAmountDue;
    private BigDecimal tuitionAmountPaid;
    private long completedSessions;
    private long totalSessions;
    private int progressPercent;
    private boolean eligible;
    private String eligibilityMessage;
}
