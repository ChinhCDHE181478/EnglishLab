package fu.sep490.g23.backend.repository.course;

import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.List;

public interface InstructorLedCourseRepository extends JpaRepository<InstructorLedCourse, Long>, JpaSpecificationExecutor<InstructorLedCourse> {
    Optional<InstructorLedCourse> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<InstructorLedCourse> findAllByOrderByUpdatedAtDescIdDesc();
}
