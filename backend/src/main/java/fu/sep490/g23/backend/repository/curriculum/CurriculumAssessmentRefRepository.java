package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.curriculum.CurriculumAssessmentRef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumAssessmentRefRepository extends JpaRepository<CurriculumAssessmentRef, Long> {
    boolean existsByUnitIdAndAssessmentId(Long unitId, Long assessmentId);
}
