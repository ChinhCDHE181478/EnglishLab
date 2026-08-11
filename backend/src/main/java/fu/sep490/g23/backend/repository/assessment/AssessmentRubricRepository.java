package fu.sep490.g23.backend.repository.assessment;

import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentRubricRepository extends JpaRepository<AssessmentRubric, Long> {
    Optional<AssessmentRubric> findByNameIgnoreCaseAndActiveTrue(String name);
    List<AssessmentRubric> findBySkillAndActiveTrueOrderByIdAsc(AssessmentSkill skill);
}
