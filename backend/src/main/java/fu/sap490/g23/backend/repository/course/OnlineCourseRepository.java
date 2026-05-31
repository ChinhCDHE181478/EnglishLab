package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
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
    Optional<OnlineCourse> findByLearningPackage(LearningPackage learningPackage);

    @EntityGraph(attributePaths = {"learningPackage", "category"})
    List<OnlineCourse> findAllByCategoryIsNull();
}
