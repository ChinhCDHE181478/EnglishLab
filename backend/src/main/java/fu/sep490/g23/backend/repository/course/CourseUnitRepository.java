package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseUnitRepository extends JpaRepository<CourseUnit, Long> {
    List<CourseUnit> findByInstructorLedCourseIdOrderBySequenceNumberAscIdAsc(Long instructorLedCourseId);

    long countByInstructorLedCourseId(Long instructorLedCourseId);
}
