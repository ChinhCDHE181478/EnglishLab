package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomTuitionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomTuitionPaymentRepository extends JpaRepository<ClassroomTuitionPayment, Long> {
    List<ClassroomTuitionPayment> findByEnrollmentIdOrderByCreatedAtDesc(Long enrollmentId);
}
