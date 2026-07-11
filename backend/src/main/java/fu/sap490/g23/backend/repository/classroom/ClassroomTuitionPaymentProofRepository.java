package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomTuitionPaymentProof;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionProofStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomTuitionPaymentProofRepository extends JpaRepository<ClassroomTuitionPaymentProof, Long> {

    List<ClassroomTuitionPaymentProof> findByEnrollmentIdOrderByCreatedAtDesc(Long enrollmentId);

    List<ClassroomTuitionPaymentProof> findByStatusOrderByCreatedAtAsc(TuitionProofStatus status);

    long countByEnrollmentIdAndStatus(Long enrollmentId, TuitionProofStatus status);
}
