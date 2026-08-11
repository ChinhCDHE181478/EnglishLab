package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;

@Repository
public interface PackageEnrollmentRepository extends JpaRepository<PackageEnrollment, Long> {
    boolean existsByStudentAndLearningPackage(User student, LearningPackage learningPackage);
    Optional<PackageEnrollment> findByStudentAndLearningPackage(User student, LearningPackage learningPackage);

    @EntityGraph(attributePaths = {"learningPackage"})
    List<PackageEnrollment> findByStudentOrderByRegisteredAtDesc(User student);

    List<PackageEnrollment> findByLearningPackage(LearningPackage learningPackage);

    long countByLearningPackage(LearningPackage learningPackage);
    long count();

    List<PackageEnrollment> findByStatusAndProgressPercentBetweenAndUpdatedAtBefore(
            EnrollmentStatus status,
            Integer minimumProgress,
            Integer maximumProgress,
            LocalDateTime updatedBefore
    );
}
