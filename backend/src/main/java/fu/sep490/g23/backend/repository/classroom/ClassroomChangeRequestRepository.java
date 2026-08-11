package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomChangeRequestRepository extends JpaRepository<ClassroomChangeRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ClassroomChangeRequest request where request.id = :id")
    Optional<ClassroomChangeRequest> findByIdForUpdate(@Param("id") Long id);

    List<ClassroomChangeRequest> findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus status);

    List<ClassroomChangeRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    Optional<ClassroomChangeRequest> findByTargetSessionIdAndRequestTypeAndStatus(
            Long targetSessionId,
            ClassroomChangeRequestType requestType,
            ClassroomChangeRequestStatus status
    );
}
