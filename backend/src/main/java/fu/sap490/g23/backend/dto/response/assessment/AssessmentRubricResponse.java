package fu.sap490.g23.backend.dto.response.assessment;

import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentRubricResponse {
    private Long id;
    private String name;
    private String examType;
    private AssessmentSkill skill;
    private String taskType;
    private String scoringScale;
    private String description;
    @Builder.Default
    private List<RubricCriterionResponse> criteria = new ArrayList<>();
}
