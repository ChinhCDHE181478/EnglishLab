package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseRegistrationRequestRepository extends JpaRepository<CourseRegistrationRequest, Long> {
    List<CourseRegistrationRequest> findByLearnerOrderByCreatedAtDesc(User learner);

    List<CourseRegistrationRequest> findAllByOrderByCreatedAtDesc();

    List<CourseRegistrationRequest> findByStatusOrderByCreatedAtAsc(EnrollmentRequestStatus status);

    List<CourseRegistrationRequest> findByReviewedByOrderByCreatedAtDesc(User reviewedBy);

    List<CourseRegistrationRequest> findByReviewedByAndStatusOrderByCreatedAtAsc(
            User reviewedBy,
            EnrollmentRequestStatus status
    );

    Optional<CourseRegistrationRequest> findFirstByReviewedByInAndReviewedAtIsNotNullAndRequestSourceOrderByReviewedAtDescIdDesc(
            Collection<User> reviewedBy,
            EnrollmentRequestSource requestSource
    );

    boolean existsByLearnerAndCourseOfferingAndStatusNotIn(
            User learner,
            InstructorLedCourse courseOffering,
            Collection<EnrollmentRequestStatus> terminalStatuses
    );

    boolean existsByLearnerAndPreferredClassSectionAndStatusNotIn(
            User learner,
            ClassSection preferredClassSection,
            Collection<EnrollmentRequestStatus> terminalStatuses
    );

}
