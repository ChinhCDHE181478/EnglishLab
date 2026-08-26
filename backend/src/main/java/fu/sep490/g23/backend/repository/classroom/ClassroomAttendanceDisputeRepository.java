package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomAttendanceDispute;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.enums.AttendanceDisputeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClassroomAttendanceDisputeRepository {
    private final ClassroomAttendanceRepository attendanceRepository;

    public List<ClassroomAttendanceDispute> findByStatusOrderByCreatedAtDesc(AttendanceDisputeStatus status) {
        return attendanceRepository.findByDisputeStatusOrderByCreatedAtDesc(status).stream().map(this::toDispute).toList();
    }

    public List<ClassroomAttendanceDispute> findByStudentIdOrderByCreatedAtDesc(Long studentId) {
        return attendanceRepository.findByStudentIdAndDisputeReasonIsNotNullOrderByCreatedAtDesc(studentId).stream()
                .map(this::toDispute).toList();
    }

    public Optional<ClassroomAttendanceDispute> findByAttendanceIdAndStudentId(Long attendanceId, Long studentId) {
        return attendanceRepository.findById(attendanceId)
                .filter(attendance -> attendance.getStudent().getId().equals(studentId))
                .filter(attendance -> attendance.getDisputeReason() != null)
                .map(this::toDispute);
    }

    public List<ClassroomAttendanceDispute> findByAttendanceSessionClassSectionIdOrderByCreatedAtDesc(Long classSectionId) {
        return attendanceRepository.findBySessionClassSectionIdAndDisputeReasonIsNotNullOrderByCreatedAtDesc(classSectionId)
                .stream().map(this::toDispute).toList();
    }

    public Optional<ClassroomAttendanceDispute> findById(Long id) {
        return attendanceRepository.findById(id).filter(attendance -> attendance.getDisputeReason() != null).map(this::toDispute);
    }

    public ClassroomAttendanceDispute save(ClassroomAttendanceDispute dispute) {
        ClassroomAttendance attendance = dispute.getAttendance();
        attendance.setDisputeReason(dispute.getReason());
        attendance.setDisputeStatus(dispute.getStatus());
        attendance.setDisputeReviewNote(dispute.getReviewNote());
        attendance.setDisputeReviewedBy(dispute.getReviewedBy());
        attendance.setDisputeReviewedAt(dispute.getReviewedAt());
        return toDispute(attendanceRepository.save(attendance));
    }

    private ClassroomAttendanceDispute toDispute(ClassroomAttendance attendance) {
        return ClassroomAttendanceDispute.builder()
                .id(attendance.getId())
                .attendance(attendance)
                .student(attendance.getStudent())
                .reason(attendance.getDisputeReason())
                .status(attendance.getDisputeStatus())
                .reviewNote(attendance.getDisputeReviewNote())
                .reviewedBy(attendance.getDisputeReviewedBy())
                .reviewedAt(attendance.getDisputeReviewedAt())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}
