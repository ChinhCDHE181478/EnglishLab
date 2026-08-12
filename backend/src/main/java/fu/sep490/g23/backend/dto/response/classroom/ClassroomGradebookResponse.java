package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ClassroomGradebookResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private BigDecimal homeworkAverage;
    private BigDecimal attendancePercent;
    private BigDecimal finalResult;
    private String teacherComment;
    private GradebookEntryStatus status;
    private List<ClassroomGradebookHomeworkResponse> homeworks;
}
