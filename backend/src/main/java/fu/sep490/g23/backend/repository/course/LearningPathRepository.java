package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
/** Provides persistence access to learning-path metadata. */
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    /** Checks whether a path code exists, ignoring case. */
    boolean existsByCodeIgnoreCase(String code);

    /** Finds a path by code, ignoring case. */
    Optional<LearningPath> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select path from LearningPath path where path.id = :id")
    Optional<LearningPath> findByIdForCheckout(@Param("id") Long id);

    /** Pages only paths containing at least one published course. */
    @Query(
            value = "select distinct path from LearningPath path join path.courseRefs ref join ref.onlineCourse course where course.status = fu.sep490.g23.backend.entity.course.enums.PackageStatus.PUBLISHED",
            countQuery = "select count(distinct path.id) from LearningPath path join path.courseRefs ref join ref.onlineCourse course where course.status = fu.sep490.g23.backend.entity.course.enums.PackageStatus.PUBLISHED"
    )
    Page<LearningPath> findPublicPaths(Pageable pageable);
}
