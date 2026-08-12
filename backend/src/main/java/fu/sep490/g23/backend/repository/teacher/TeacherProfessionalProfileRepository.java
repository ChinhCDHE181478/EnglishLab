package fu.sep490.g23.backend.repository.teacher;

import fu.sep490.g23.backend.entity.teacher.TeacherProfessionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherProfessionalProfileRepository extends JpaRepository<TeacherProfessionalProfile, Long> {
    Optional<TeacherProfessionalProfile> findByTeacherId(Long teacherId);
}
