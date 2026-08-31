package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomChangeRequestRepository extends JpaRepository<ClassroomChangeRequest, Long>, JpaSpecificationExecutor<ClassroomChangeRequest> {
    @Override
    @EntityGraph(attributePaths = {"requester", "classSection", "targetClassSchedule", "reviewer"})
    Page<ClassroomChangeRequest> findAll(Specification<ClassroomChangeRequest> specification, Pageable pageable);

    long countByRequesterId(Long requesterId);

    long countByRequesterIdAndStatus(Long requesterId, ClassroomChangeRequestStatus status);

    long countByRequesterIdAndStatusIn(Long requesterId, List<ClassroomChangeRequestStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ClassroomChangeRequest request where request.id = :id")
    Optional<ClassroomChangeRequest> findByIdForUpdate(@Param("id") Long id);

    List<ClassroomChangeRequest> findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus status);

    List<ClassroomChangeRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    Optional<ClassroomChangeRequest> findByTargetClassScheduleIdAndRequestTypeAndStatus(
            Long targetClassScheduleId,
            ClassroomChangeRequestType requestType,
            ClassroomChangeRequestStatus status
    );
}
