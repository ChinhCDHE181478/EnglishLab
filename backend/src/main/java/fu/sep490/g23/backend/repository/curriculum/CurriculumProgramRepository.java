package fu.sep490.g23.backend.repository.curriculum;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurriculumProgramRepository extends JpaRepository<CurriculumProgram, Long> {
    Optional<CurriculumProgram> findBySlug(String slug);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsBySlug(String slug);
    List<CurriculumProgram> findByDeliveryModeOrderByDisplayOrderAscUpdatedAtDescIdDesc(ClassroomDeliveryMode deliveryMode);
    List<CurriculumProgram> findAllByOrderByDisplayOrderAscUpdatedAtDescIdDesc();
}
