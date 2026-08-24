package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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

    @EntityGraph(attributePaths = {"learningPackage"})
    @Query("""
            select c from OnlineCourse c
            where c.learningPackage.deleted = false
              and c.learningPackage.status = :status
              and c.learningPathCode is not null
              and trim(c.learningPathCode) <> ''
            order by c.learningPathCode asc, c.learningPathName asc, c.learningPathOrder asc, c.id asc
            """)
    List<OnlineCourse> findPublishedLearningPathCourses(PackageStatus status);

    long countByCategoryAndLearningPackageDeletedFalse(CourseCategory category);

    long countByLearningPackageDeletedFalse();

    long countByLearningPackageDeletedFalseAndLearningPackageStatus(PackageStatus status);

    @Query("""
            select coalesce(category.name, 'Chưa phân loại'), count(course)
            from OnlineCourse course
            left join course.category category
            where course.learningPackage.deleted = false
            group by category.name
            order by count(course) desc
            """)
    List<Object[]> summarizeCategoryDistribution();
}
