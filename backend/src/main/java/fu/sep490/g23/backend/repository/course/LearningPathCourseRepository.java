package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.LearningPathCourse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
/** Provides persistence access to path-course relationships and ordering. */
public interface LearningPathCourseRepository extends JpaRepository<LearningPathCourse, Long> {
    /** Returns path courses in display order with course data eagerly loaded. */
    @EntityGraph(attributePaths = {"onlineCourse"})
    List<LearningPathCourse> findByLearningPathIdOrderByDisplayOrderAscIdAsc(Long learningPathId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ref from LearningPathCourse ref join fetch ref.onlineCourse where ref.learningPath.id = :learningPathId order by ref.displayOrder, ref.id")
    List<LearningPathCourse> findByLearningPathIdForCheckout(@Param("learningPathId") Long learningPathId);

    /** Returns all relationships ordered by path code and course position. */
    @EntityGraph(attributePaths = {"learningPath", "onlineCourse"})
    List<LearningPathCourse> findAllByOrderByLearningPathCodeAscDisplayOrderAscIdAsc();

    /** Checks whether a course is already attached to a path. */
    boolean existsByLearningPathIdAndOnlineCourseId(Long learningPathId, Long onlineCourseId);
}
