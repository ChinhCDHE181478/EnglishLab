package fu.sep490.g23.backend.repository.classroom;

import fu.sep490.g23.backend.entity.classroom.LarkMeetingParticipant;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LarkMeetingParticipantRepository {
    private final ClassroomAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    public Optional<LarkMeetingParticipant> findByClassScheduleIdAndParticipantKey(Long scheduleId, String key) {
        return attendanceRepository.findBySessionIdAndProviderParticipantKey(scheduleId, key).map(this::toParticipant);
    }

    public long countByClassScheduleIdAndActiveTrue(Long scheduleId) {
        return attendanceRepository.countBySessionIdAndProviderParticipantActiveTrue(scheduleId);
    }

    public java.util.List<LarkMeetingParticipant> findByClassScheduleId(Long scheduleId) {
        return attendanceRepository.findBySessionIdAndProviderParticipantKeyIsNotNull(scheduleId).stream()
                .map(this::toParticipant).toList();
    }

    public LarkMeetingParticipant save(LarkMeetingParticipant participant) {
        if (participant.getUserId() == null) {
            return participant;
        }
        ClassroomAttendance attendance = attendanceRepository
                .findBySessionIdAndStudentId(participant.getClassSchedule().getId(), participant.getUserId())
                .orElseGet(() -> ClassroomAttendance.builder()
                        .session(participant.getClassSchedule())
                        .student(userRepository.findById(participant.getUserId())
                                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người tham dự.")))
                        .status(ClassroomAttendanceStatus.ABSENT)
                        .build());
        attendance.setProviderParticipantKey(participant.getParticipantKey());
        attendance.setProviderParticipantActive(participant.isActive());
        attendance.setJoinTime(participant.getJoinedAt());
        attendance.setLeaveTime(participant.getLeftAt());
        return toParticipant(attendanceRepository.save(attendance));
    }

    public void deleteByClassScheduleId(Long scheduleId) {
        attendanceRepository.findBySessionIdAndProviderParticipantKeyIsNotNull(scheduleId).forEach(attendance -> {
            attendance.setProviderParticipantKey(null);
            attendance.setProviderParticipantActive(false);
            attendanceRepository.save(attendance);
        });
    }

    private LarkMeetingParticipant toParticipant(ClassroomAttendance attendance) {
        return LarkMeetingParticipant.builder()
                .id(attendance.getId())
                .classSchedule(attendance.getSession())
                .participantKey(attendance.getProviderParticipantKey())
                .userId(attendance.getStudent().getId())
                .active(attendance.isProviderParticipantActive())
                .joinedAt(attendance.getJoinTime())
                .leftAt(attendance.getLeaveTime())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();
    }
}
