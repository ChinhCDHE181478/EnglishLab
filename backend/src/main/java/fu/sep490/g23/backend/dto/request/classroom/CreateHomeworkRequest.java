package fu.sep490.g23.backend.dto.request.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.HomeworkStatus;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
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
    private Long curriculumUnitId;
    private HomeworkActivityType activityType;
    private String activityConfigJson;
    private Boolean aiReviewEnabled;
    private HomeworkGradingMode gradingMode;
    private AssessmentSkill skill;
    private Long rubricId;
    private Long assessmentBankItemId;
}
