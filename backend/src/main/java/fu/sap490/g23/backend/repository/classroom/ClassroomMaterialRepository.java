package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomMaterialRepository extends JpaRepository<ClassroomMaterial, Long> {
    List<ClassroomMaterial> findByClassroomOfferingIdOrderByCreatedAtDesc(Long classroomOfferingId);
    boolean existsByClassroomOfferingIdAndCenterMaterialIdAndSessionIsNull(Long classroomOfferingId, Long centerMaterialId);
    boolean existsByClassroomOfferingIdAndCenterMaterialIdAndSessionId(Long classroomOfferingId, Long centerMaterialId, Long sessionId);
}
