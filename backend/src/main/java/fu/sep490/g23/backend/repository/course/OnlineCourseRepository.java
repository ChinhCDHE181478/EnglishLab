package fu.sep490.g23.backend.repository.course;

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
    @EntityGraph(attributePaths = {"category", "versions", "versions.modules"})
    Optional<OnlineCourse> findWithModulesById(Long id);

    @EntityGraph(attributePaths = {"category", "versions", "versions.modules"})
    Optional<OnlineCourse> findWithModulesByIdAndDeletedFalseAndStatus(Long id, PackageStatus status);

    @EntityGraph(attributePaths = {"category", "versions", "versions.modules"})
    Optional<OnlineCourse> findBySlugAndDeletedFalseAndStatus(String slug, PackageStatus status);

    Optional<OnlineCourse> findBySlug(String slug);

    Optional<OnlineCourse> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"category"})
    List<OnlineCourse> findAllByCategoryIsNull();

    @Query("""
            select c from OnlineCourse c
            where c.deleted = false
              and c.status = :status
              and c.learningPathCode is not null
              and trim(c.learningPathCode) <> ''
            order by c.learningPathCode asc, c.learningPathName asc, c.learningPathOrder asc, c.id asc
            """)
    List<OnlineCourse> findPublishedLearningPathCourses(PackageStatus status);

    long countByCategoryAndDeletedFalse(CourseCategory category);

    long countByDeletedFalse();

    long countByDeletedFalseAndStatus(PackageStatus status);

    @Query("""
            select coalesce(category.name, 'Chưa phân loại'), count(course)
            from OnlineCourse course
            left join course.category category
            where course.deleted = false
            group by category.name
            order by count(course) desc
            """)
    List<Object[]> summarizeCategoryDistribution();
}
