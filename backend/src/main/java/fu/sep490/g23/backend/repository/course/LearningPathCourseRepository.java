package fu.sap490.g23.backend.repository.course;

import fu.sap490.g23.backend.entity.course.LearningPathCourse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningPathCourseRepository extends JpaRepository<LearningPathCourse, Long> {
    @EntityGraph(attributePaths = {"onlineCourse", "onlineCourse.learningPackage"})
    List<LearningPathCourse> findByLearningPathIdOrderByDisplayOrderAscIdAsc(Long learningPathId);

    @EntityGraph(attributePaths = {"learningPath", "onlineCourse", "onlineCourse.learningPackage"})
    List<LearningPathCourse> findAllByOrderByLearningPathCodeAscDisplayOrderAscIdAsc();

    boolean existsByLearningPathIdAndOnlineCourseId(Long learningPathId, Long onlineCourseId);
}
