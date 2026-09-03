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
    // Fetch only one List collection per query. Modules are initialized separately
    // inside the service transaction to avoid Hibernate's MultipleBagFetchException.
    @EntityGraph(attributePaths = {"category", "versions"})
    Optional<OnlineCourse> findWithModulesById(Long id);

    @EntityGraph(attributePaths = {"category", "versions"})
    Optional<OnlineCourse> findWithModulesByIdAndStatus(Long id, PackageStatus status);

    @EntityGraph(attributePaths = {"category", "versions"})
    Optional<OnlineCourse> findBySlugAndStatus(String slug, PackageStatus status);

    Optional<OnlineCourse> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"category"})
    List<OnlineCourse> findAllByCategoryIsNull();

    long countByCategoryAndStatusNot(CourseCategory category, PackageStatus status);

    long countByStatusNot(PackageStatus status);

    long countByStatus(PackageStatus status);

    @Query("""
            select coalesce(category.name, 'Chưa phân loại'), count(course)
            from OnlineCourse course
            left join course.category category
            where course.status <> fu.sep490.g23.backend.entity.course.enums.PackageStatus.ARCHIVED
            group by category.name
            order by count(course) desc
            """)
    List<Object[]> summarizeCategoryDistribution();
}
