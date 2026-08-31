package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomMaterialRepository extends JpaRepository<ClassroomMaterial, Long> {
    List<ClassroomMaterial> findByClassSectionIdOrderByCreatedAtDesc(Long classSectionId);
    boolean existsByClassSectionIdAndCenterMaterialIdAndSessionIsNull(Long classSectionId, Long centerMaterialId);
    boolean existsByClassSectionIdAndCenterMaterialIdAndSessionId(Long classSectionId, Long centerMaterialId, Long sessionId);
    Optional<ClassroomMaterial> findFirstByFileUrlEndingWith(String suffix);
    boolean existsByFileUrlEndingWith(String suffix);
}
