package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.LarkMeetingParticipant;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sap490.g23.backend.repository.classroom.LarkMeetingParticipantRepository;
import fu.sap490.g23.backend.service.classroom.VirtualAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VirtualAttendanceServiceImpl implements VirtualAttendanceService {

    private static final int MIN_ATTENDANCE_MINUTES = 15;

    private final ClassroomAttendanceRepository attendanceRepository;
    private final LarkMeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Override
    public void recordVirtualJoin(ClassroomSession session, User learner) {
        LocalDateTime now = LocalDateTime.now();
        ClassroomAttendance attendance = attendanceRepository
                .findBySessionIdAndStudentId(session.getId(), learner.getId())
                .orElseGet(() -> ClassroomAttendance.builder()
                        .session(session)
                        .student(learner)
                        .status(ClassroomAttendanceStatus.ABSENT)
                        .build());

        if (attendance.getJoinTime() == null) {
            attendance.setJoinTime(now);
        }
        attendance.setStatus(resolveJoinStatus(session, now));
        attendance.setTeacherConfirmed(false);
        attendanceRepository.save(attendance);
    }

    @Override
    public void finalizeVirtualAttendance(ClassroomSession session) {
        LocalDateTime now = LocalDateTime.now();
        List<ClassroomAttendance> records = attendanceRepository.findBySessionId(session.getId());
        for (ClassroomAttendance attendance : records) {
            if (attendance.getJoinTime() == null) {
                continue;
            }
            if (attendance.getLeaveTime() == null) {
                attendance.setLeaveTime(now);
            }
            int minutes = computeDurationMinutes(attendance.getJoinTime(), attendance.getLeaveTime());
            attendance.setDurationMinutes(minutes);
            if (!attendance.isTeacherConfirmed()) {
                attendance.setStatus(minutes >= MIN_ATTENDANCE_MINUTES
                        ? ClassroomAttendanceStatus.PRESENT
                        : ClassroomAttendanceStatus.LATE);
            }
            attendanceRepository.save(attendance);
        }
        syncLarkParticipantAttendance(session);
    }

    @Override
    public void syncLarkParticipantAttendance(ClassroomSession session) {
        List<LarkMeetingParticipant> participants = participantRepository.findByClassroomSessionId(session.getId());
        for (LarkMeetingParticipant participant : participants) {
            Long userId = participant.getUserId();
            if (userId == null) {
                userId = resolveUserIdFromParticipantKey(participant.getParticipantKey());
                if (userId != null) {
                    participant.setUserId(userId);
                    participantRepository.save(participant);
                }
            }
            if (userId == null || participant.getJoinedAt() == null) {
                continue;
            }
            User student = userRepository.findById(userId).orElse(null);
            if (student == null) {
                continue;
            }
            ClassroomAttendance attendance = attendanceRepository
                    .findBySessionIdAndStudentId(session.getId(), student.getId())
                    .orElseGet(() -> ClassroomAttendance.builder()
                            .session(session)
                            .student(student)
                            .status(ClassroomAttendanceStatus.ABSENT)
                            .build());

            LocalDateTime joinTime = participant.getJoinedAt();
            LocalDateTime leaveTime = participant.getLeftAt() == null ? LocalDateTime.now() : participant.getLeftAt();
            if (attendance.getJoinTime() == null || joinTime.isBefore(attendance.getJoinTime())) {
                attendance.setJoinTime(joinTime);
            }
            if (attendance.getLeaveTime() == null || leaveTime.isAfter(attendance.getLeaveTime())) {
                attendance.setLeaveTime(leaveTime);
            }
            int minutes = computeDurationMinutes(attendance.getJoinTime(), attendance.getLeaveTime());
            attendance.setDurationMinutes(minutes);
            if (!attendance.isTeacherConfirmed()) {
                attendance.setStatus(minutes >= MIN_ATTENDANCE_MINUTES
                        ? ClassroomAttendanceStatus.PRESENT
                        : ClassroomAttendanceStatus.LATE);
            }
            attendanceRepository.save(attendance);
        }
    }

    private ClassroomAttendanceStatus resolveJoinStatus(ClassroomSession session, LocalDateTime joinTime) {
        LocalDateTime start = session.getStartDateTime();
        if (joinTime.isAfter(start.plusMinutes(10))) {
            return ClassroomAttendanceStatus.LATE;
        }
        return ClassroomAttendanceStatus.PRESENT;
    }

    private int computeDurationMinutes(LocalDateTime joinTime, LocalDateTime leaveTime) {
        if (joinTime == null || leaveTime == null || leaveTime.isBefore(joinTime)) {
            return 0;
        }
        return (int) Duration.between(joinTime, leaveTime).toMinutes();
    }

    private Long resolveUserIdFromParticipantKey(String participantKey) {
        if (participantKey == null || !participantKey.startsWith("open_id:")) {
            return null;
        }
        String openId = participantKey.substring("open_id:".length());
        return userRepository.findByLarkOpenId(openId).map(User::getId).orElse(null);
    }
}
