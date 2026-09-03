package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.AssessmentRubricRequest;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AssessmentRubricService {
    List<AssessmentRubricResponse> list(String status, AssessmentSkill skill);

    Page<AssessmentRubricResponse> page(
            String status,
            AssessmentSkill skill,
            String keyword,
            Pageable pageable
    );

    Map<String, Long> stats(AssessmentSkill skill);

    AssessmentRubricResponse get(Long id);

    AssessmentRubricResponse create(AssessmentRubricRequest request);

    AssessmentRubricResponse update(Long id, AssessmentRubricRequest request);

    AssessmentRubricResponse deactivate(Long id);

    AssessmentRubricResponse reactivate(Long id);
}
