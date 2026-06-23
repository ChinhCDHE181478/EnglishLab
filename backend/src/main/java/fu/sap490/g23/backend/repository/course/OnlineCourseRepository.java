package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.CourseCategory;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnlineCourseRepository extends JpaRepository<OnlineCourse, Long>, JpaSpecificationExecutor<OnlineCourse> {
    @EntityGraph(attributePaths = {"learningPackage", "category", "modules"})
    Optional<OnlineCourse> findWithModulesById(Long id);

    @EntityGraph(attributePaths = {"learningPackage", "category", "modules"})
    Optional<OnlineCourse> findWithModulesByIdAndLearningPackageDeletedFalseAndLearningPackageStatus(Long id, PackageStatus status);

    @EntityGraph(attributePaths = {"learningPackage", "category", "modules"})
    Optional<OnlineCourse> findByLearningPackage(LearningPackage learningPackage);

    @EntityGraph(attributePaths = {"learningPackage", "category"})
    List<OnlineCourse> findAllByCategoryIsNull();

    long countByCategoryAndLearningPackageDeletedFalse(CourseCategory category);

    long countByLearningPackageDeletedFalse();

    long countByLearningPackageDeletedFalseAndLearningPackageStatus(PackageStatus status);
}
