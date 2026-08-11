package fu.sep490.g23.backend.repository.teacher;

import fu.sep490.g23.backend.entity.teacher.TeacherCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherCredentialRepository extends JpaRepository<TeacherCredential, Long> {
    List<TeacherCredential> findByTeacherIdOrderByIssuedDateDescIdDesc(Long teacherId);
}
