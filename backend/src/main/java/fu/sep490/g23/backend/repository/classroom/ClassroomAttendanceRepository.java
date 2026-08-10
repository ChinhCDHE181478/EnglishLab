package fu.sap490.g23.backend.repository.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomAttendanceRepository extends JpaRepository<ClassroomAttendance, Long> {
    List<ClassroomAttendance> findBySessionId(Long sessionId);

    Optional<ClassroomAttendance> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<ClassroomAttendance> findByStudentIdAndSession_ClassroomOfferingId(Long studentId, Long classroomOfferingId);
}
