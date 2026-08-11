package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {
    Optional<TrainingProgram> findBySlug(String slug);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsBySlug(String slug);

    Optional<TrainingProgram> findFirstByCurriculumProgramIdAndDeliveryModeOrderByIdAsc(Long curriculumProgramId, ClassroomDeliveryMode deliveryMode);

    Optional<TrainingProgram> findFirstByCurriculumProgramIdOrderByIdAsc(Long curriculumProgramId);

    List<TrainingProgram> findByDeliveryModeOrderByDisplayOrderAscUpdatedAtDescIdDesc(ClassroomDeliveryMode deliveryMode);

    List<TrainingProgram> findAllByOrderByDisplayOrderAscUpdatedAtDescIdDesc();
}
