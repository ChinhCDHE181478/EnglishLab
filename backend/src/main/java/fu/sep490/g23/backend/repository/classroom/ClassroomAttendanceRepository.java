package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomAttendanceRepository extends JpaRepository<ClassroomAttendance, Long> {
    List<ClassroomAttendance> findBySessionId(Long sessionId);

    Optional<ClassroomAttendance> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<ClassroomAttendance> findByStudentIdAndSession_ClassSectionId(Long studentId, Long classSectionId);

    List<ClassroomAttendance> findByDisputeStatusOrderByCreatedAtDesc(
            fu.sep490.g23.backend.entity.classroom.enums.AttendanceDisputeStatus status);

    List<ClassroomAttendance> findByStudentIdAndDisputeReasonIsNotNullOrderByCreatedAtDesc(Long studentId);

    List<ClassroomAttendance> findBySessionClassSectionIdAndDisputeReasonIsNotNullOrderByCreatedAtDesc(Long classSectionId);

    List<ClassroomAttendance> findBySessionIdAndProviderParticipantKeyIsNotNull(Long sessionId);

    Optional<ClassroomAttendance> findBySessionIdAndProviderParticipantKey(Long sessionId, String participantKey);

    long countBySessionIdAndProviderParticipantActiveTrue(Long sessionId);
}
