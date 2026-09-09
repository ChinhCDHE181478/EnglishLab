package fu.sep490.g23.backend.repository.assessment;

import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AssessmentRubricRepository extends JpaRepository<AssessmentRubric, Long>, JpaSpecificationExecutor<AssessmentRubric> {
    Optional<AssessmentRubric> findByNameIgnoreCaseAndStatus(String name, String status);
    List<AssessmentRubric> findBySkillAndStatusOrderByIdAsc(AssessmentSkill skill, String status);
}
