package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomAttendanceDispute;
import fu.sep490.g23.backend.entity.classroom.enums.AttendanceDisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomAttendanceDisputeRepository extends JpaRepository<ClassroomAttendanceDispute, Long> {
    List<ClassroomAttendanceDispute> findByStatusOrderByCreatedAtDesc(AttendanceDisputeStatus status);
    List<ClassroomAttendanceDispute> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    Optional<ClassroomAttendanceDispute> findByAttendanceIdAndStudentId(Long attendanceId, Long studentId);
    List<ClassroomAttendanceDispute> findByAttendanceSessionClassroomOfferingIdOrderByCreatedAtDesc(Long offeringId);
}
