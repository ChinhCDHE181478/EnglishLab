package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackageEnrollmentRepository extends JpaRepository<PackageEnrollment, Long> {
    boolean existsByStudentAndLearningPackage(User student, LearningPackage learningPackage);
    Optional<PackageEnrollment> findByStudentAndLearningPackage(User student, LearningPackage learningPackage);

    @EntityGraph(attributePaths = {"learningPackage"})
    List<PackageEnrollment> findByStudentOrderByRegisteredAtDesc(User student);

    long countByLearningPackage(LearningPackage learningPackage);
    long count();
}
