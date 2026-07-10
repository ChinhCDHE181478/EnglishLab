package fu.sap490.g23.backend.dto.request.assessment;

import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AssessmentRubricRequest {
    @NotBlank(message = "Tên rubric không được để trống.")
    @Size(max = 180, message = "Tên rubric không được vượt quá 180 ký tự.")
    private String name;

    @Size(max = 40, message = "Loại kỳ thi không được vượt quá 40 ký tự.")
    private String examType;

    @NotNull(message = "Kỹ năng không được để trống.")
    private AssessmentSkill skill;

    @Size(max = 80, message = "Loại task không được vượt quá 80 ký tự.")
    private String taskType;

    @Size(max = 60, message = "Thang điểm không được vượt quá 60 ký tự.")
    private String scoringScale;

    private String description;

    private Boolean active = true;

    @Valid
    @NotEmpty(message = "Rubric cần có ít nhất một tiêu chí chấm điểm.")
    private List<RubricCriterionRequest> criteria = new ArrayList<>();
}
