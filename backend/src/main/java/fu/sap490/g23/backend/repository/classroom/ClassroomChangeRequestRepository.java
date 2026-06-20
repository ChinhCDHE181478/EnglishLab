package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomChangeRequestRepository extends JpaRepository<ClassroomChangeRequest, Long> {
    List<ClassroomChangeRequest> findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus status);

    List<ClassroomChangeRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    Optional<ClassroomChangeRequest> findByTargetSessionIdAndRequestTypeAndStatus(
            Long targetSessionId,
            ClassroomChangeRequestType requestType,
            ClassroomChangeRequestStatus status
    );
}
