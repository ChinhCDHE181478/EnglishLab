package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.HomeworkStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHomeworkRequest {

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    @Size(max = 220)
    private String title;

    private String instruction;
    private LocalDateTime deadline;
    private BigDecimal maxScore;
    private Boolean allowResubmission;

    @Size(max = 700)
    private String attachmentUrl;

    private HomeworkStatus status;
    private Long sessionId;
}
