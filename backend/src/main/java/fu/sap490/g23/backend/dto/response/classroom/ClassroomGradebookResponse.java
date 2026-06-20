package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ClassroomGradebookResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private BigDecimal homeworkScore;
    private BigDecimal quizScore;
    private BigDecimal attendancePercent;
    private BigDecimal participationScore;
    private BigDecimal finalResult;
    private String teacherComment;
    private GradebookEntryStatus status;
}
