package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstructorLedCourseRepository extends JpaRepository<InstructorLedCourse, Long> {
    Optional<InstructorLedCourse> findByCodeIgnoreCase(String code);

    Optional<InstructorLedCourse> findBySlug(String slug);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsBySlug(String slug);
}
