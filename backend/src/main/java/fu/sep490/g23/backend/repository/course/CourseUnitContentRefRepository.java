package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.CourseUnitContentRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseUnitContentRefRepository extends JpaRepository<CourseUnitContentRef, Long> {
    List<CourseUnitContentRef> findByCourseUnitIdOrderBySequenceNumberAscIdAsc(Long courseUnitId);
}
