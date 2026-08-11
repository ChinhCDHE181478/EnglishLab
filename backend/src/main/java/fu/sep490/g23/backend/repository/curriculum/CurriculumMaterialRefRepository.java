package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.curriculum.CurriculumMaterialRef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumMaterialRefRepository extends JpaRepository<CurriculumMaterialRef, Long> {
    boolean existsByUnitIdAndMaterialId(Long unitId, Long materialId);
    boolean existsByMaterialId(Long materialId);
}
