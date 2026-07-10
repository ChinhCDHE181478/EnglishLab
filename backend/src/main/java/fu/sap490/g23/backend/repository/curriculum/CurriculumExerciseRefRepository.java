package fu.sap490.g23.backend.repository.curriculum;

import fu.sap490.g23.backend.entity.curriculum.CurriculumExerciseRef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumExerciseRefRepository extends JpaRepository<CurriculumExerciseRef, Long> {
    boolean existsByUnitIdAndExerciseId(Long unitId, Long exerciseId);
}
