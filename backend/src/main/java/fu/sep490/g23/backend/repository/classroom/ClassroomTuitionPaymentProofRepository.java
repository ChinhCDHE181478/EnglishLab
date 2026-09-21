package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomTuitionPaymentProof;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionProofStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ClassroomTuitionPaymentProofRepository extends JpaRepository<ClassroomTuitionPaymentProof, Long> {

    List<ClassroomTuitionPaymentProof> findByEnrollmentIdOrderByCreatedAtDesc(Long enrollmentId);

    List<ClassroomTuitionPaymentProof> findByStatusOrderByCreatedAtAsc(TuitionProofStatus status);

    long countByEnrollmentIdAndStatus(Long enrollmentId, TuitionProofStatus status);

    Optional<ClassroomTuitionPaymentProof> findFirstByFileUrlEndingWith(String suffix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select proof from ClassroomTuitionPaymentProof proof where proof.id = :id")
    Optional<ClassroomTuitionPaymentProof> findByIdForUpdate(@Param("id") Long id);

    boolean existsByFileUrlEndingWith(String suffix);
}
