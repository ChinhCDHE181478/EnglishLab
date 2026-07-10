package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGradebookRequest {

    @NotNull(message = "Học viên không được để trống")
    private Long studentId;

    private BigDecimal homeworkScore;
    private BigDecimal quizScore;
    private BigDecimal attendancePercent;
    private BigDecimal participationScore;
    private BigDecimal finalResult;
    private String teacherComment;
    private GradebookEntryStatus status;
}
