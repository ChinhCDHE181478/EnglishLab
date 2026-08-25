package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseRegistrationRequestRepository extends JpaRepository<CourseRegistrationRequest, Long> {
    List<CourseRegistrationRequest> findByLearnerOrderByCreatedAtDesc(User learner);

    List<CourseRegistrationRequest> findAllByOrderByCreatedAtDesc();

    List<CourseRegistrationRequest> findByStatusOrderByCreatedAtAsc(EnrollmentRequestStatus status);

    boolean existsByLearnerAndCourseOfferingAndStatusNotIn(
            User learner,
            TrainingProgram courseOffering,
            Collection<EnrollmentRequestStatus> terminalStatuses
    );

    boolean existsByLearnerAndPreferredClassSectionAndStatusNotIn(
            User learner,
            ClassSection preferredClassSection,
            Collection<EnrollmentRequestStatus> terminalStatuses
    );

}
