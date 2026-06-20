package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sap490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomHomeworkResponse {
    private Long id;
    private Long classroomOfferingId;
    private Long sessionId;
    private String title;
    private String instruction;
    private LocalDateTime deadline;
    private BigDecimal maxScore;
    private boolean allowResubmission;
    private String attachmentUrl;
    private HomeworkStatus status;
    private boolean overdue;
    private ClassroomHomeworkSubmissionResponse mySubmission;
}
