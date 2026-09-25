package fu.sep490.g23.backend.repository.teacher;

import fu.sep490.g23.backend.entity.teacher.TeacherCredential;
import fu.sep490.g23.backend.entity.teacher.enums.CredentialVerificationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TeacherCredentialRepository extends JpaRepository<TeacherCredential, Long> {
    List<TeacherCredential> findByTeacherIdOrderByIssuedDateDescIdDesc(Long teacherId);

    @EntityGraph(attributePaths = {"teacher"})
    List<TeacherCredential> findByTeacherIdInAndVerificationStatusOrderByIssuedDateDescIdDesc(
            Collection<Long> teacherIds,
            CredentialVerificationStatus status
    );
}
