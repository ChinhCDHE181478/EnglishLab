package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurriculumUnitRepository extends JpaRepository<CurriculumUnit, Long> {
    List<CurriculumUnit> findByProgramIdOrderByDisplayOrderAscIdAsc(Long programId);
}
