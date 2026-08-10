package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sap490.g23.backend.entity.classroom.enums.HomeworkSubmissionTiming;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClassroomHomeworkSubmissionResponse {
    private Long id;
    private Long homeworkId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatarUrl;
    private boolean submitted;
    private HomeworkSubmissionTiming submissionTiming;
    private String textAnswer;
    private String attachmentUrl;
    private LocalDateTime submittedAt;
    private HomeworkSubmissionStatus status;
    private BigDecimal score;
    private String teacherFeedback;
    private List<HomeworkTextAnnotationResponse> annotations;
    private LocalDateTime gradedAt;
}
