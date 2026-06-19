package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.HomeworkSubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomHomeworkSubmissionResponse {
    private Long id;
    private Long homeworkId;
    private Long studentId;
    private String studentName;
    private String textAnswer;
    private String attachmentUrl;
    private LocalDateTime submittedAt;
    private HomeworkSubmissionStatus status;
    private BigDecimal score;
    private String teacherFeedback;
    private LocalDateTime gradedAt;
}
