package fu.sap490.g23.backend.repository.teacher;

import fu.sap490.g23.backend.entity.teacher.TeacherGoogleMeetConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherGoogleMeetConnectionRepository extends JpaRepository<TeacherGoogleMeetConnection, Long> {
    Optional<TeacherGoogleMeetConnection> findByTeacherId(Long teacherId);
}
