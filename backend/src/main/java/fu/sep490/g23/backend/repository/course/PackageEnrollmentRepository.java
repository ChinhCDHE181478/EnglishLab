package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;

@Repository
public interface PackageEnrollmentRepository extends JpaRepository<PackageEnrollment, Long>, JpaSpecificationExecutor<PackageEnrollment> {
    @Override
    @EntityGraph(attributePaths = {"student", "learningPackage", "learningPackage.packageType"})
    Page<PackageEnrollment> findAll(Specification<PackageEnrollment> specification, Pageable pageable);
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
