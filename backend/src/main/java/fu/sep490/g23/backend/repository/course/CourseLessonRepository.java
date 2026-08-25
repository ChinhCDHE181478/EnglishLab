package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseLessonRepository extends JpaRepository<CourseLesson, Long> {
    List<CourseLesson> findByCourseUnitIdOrderBySequenceNumberAscIdAsc(Long courseUnitId);
}
