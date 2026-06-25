package fu.sap490.g23.backend.service.assessment;

import fu.sap490.g23.backend.dto.request.assessment.AssessmentRubricRequest;
import fu.sap490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;

import java.util.List;

public interface AssessmentRubricService {
    List<AssessmentRubricResponse> list(Boolean includeInactive, AssessmentSkill skill);

    AssessmentRubricResponse get(Long id);

    AssessmentRubricResponse create(AssessmentRubricRequest request);

    AssessmentRubricResponse update(Long id, AssessmentRubricRequest request);

    AssessmentRubricResponse deactivate(Long id);

    AssessmentRubricResponse reactivate(Long id);
}
