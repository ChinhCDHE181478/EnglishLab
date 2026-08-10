package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sap490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import java.util.List;

public interface ClassroomHomeworkGradingCatalogService {

    List<AssessmentSkill> listHomeworkSkills();
    List<AssessmentRubricResponse> listRubricsBySkill(AssessmentSkill skill);
    List<AssessmentRubricResponse> listAllHomeworkRubrics();
    AssessmentRubric requireActiveRubric(Long rubricId);
    AssessmentRubricResponse mapRubric(AssessmentRubric rubric);
}
