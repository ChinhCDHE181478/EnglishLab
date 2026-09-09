package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<LearningPath> findByCodeIgnoreCase(String code);

    @Query(
            value = "select distinct path from LearningPath path join path.courseRefs ref join ref.onlineCourse course where course.status = fu.sep490.g23.backend.entity.course.enums.PackageStatus.PUBLISHED",
            countQuery = "select count(distinct path.id) from LearningPath path join path.courseRefs ref join ref.onlineCourse course where course.status = fu.sep490.g23.backend.entity.course.enums.PackageStatus.PUBLISHED"
    )
    Page<LearningPath> findPublicPaths(Pageable pageable);
}
