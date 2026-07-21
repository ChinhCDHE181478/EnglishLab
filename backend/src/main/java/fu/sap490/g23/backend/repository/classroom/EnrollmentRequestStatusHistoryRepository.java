package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.EnrollmentRequestStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRequestStatusHistoryRepository extends JpaRepository<EnrollmentRequestStatusHistory, Long> {
    List<EnrollmentRequestStatusHistory> findByEnrollmentRequestIdOrderByCreatedAtAscIdAsc(Long enrollmentRequestId);
}
