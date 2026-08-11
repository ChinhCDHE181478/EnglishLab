package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EnrollmentRequestRepository extends JpaRepository<EnrollmentRequest, Long> {
    List<EnrollmentRequest> findByLearnerOrderByCreatedAtDesc(User learner);

    List<EnrollmentRequest> findAllByOrderByCreatedAtDesc();

    List<EnrollmentRequest> findByStatusOrderByCreatedAtAsc(EnrollmentRequestStatus status);

    boolean existsByLearnerAndCourseOfferingAndStatusNotIn(
            User learner,
            TrainingProgram courseOffering,
            Collection<EnrollmentRequestStatus> terminalStatuses
    );

    boolean existsByLearnerAndRequestedClassroomAndStatusNotIn(
            User learner,
            ClassroomOffering requestedClassroom,
            Collection<EnrollmentRequestStatus> terminalStatuses
    );

}
