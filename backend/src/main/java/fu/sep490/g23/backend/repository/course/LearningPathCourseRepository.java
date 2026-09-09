package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.LearningPathCourse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/** Provides persistence access to path-course relationships and ordering. */
public interface LearningPathCourseRepository extends JpaRepository<LearningPathCourse, Long> {
    /** Returns path courses in display order with course data eagerly loaded. */
    @EntityGraph(attributePaths = {"onlineCourse"})
    List<LearningPathCourse> findByLearningPathIdOrderByDisplayOrderAscIdAsc(Long learningPathId);

    /** Returns all relationships ordered by path code and course position. */
    @EntityGraph(attributePaths = {"learningPath", "onlineCourse"})
    List<LearningPathCourse> findAllByOrderByLearningPathCodeAscDisplayOrderAscIdAsc();

    /** Checks whether a course is already attached to a path. */
    boolean existsByLearningPathIdAndOnlineCourseId(Long learningPathId, Long onlineCourseId);
}
