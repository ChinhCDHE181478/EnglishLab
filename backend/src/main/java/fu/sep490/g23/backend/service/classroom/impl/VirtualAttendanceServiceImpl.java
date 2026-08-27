package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.service.classroom.VirtualAttendanceService;
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

    @Override
    public void recordVirtualJoin(ClassSchedule session, User learner) {
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
    public void finalizeVirtualAttendance(ClassSchedule session) {
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
    }

    private ClassroomAttendanceStatus resolveJoinStatus(ClassSchedule session, LocalDateTime joinTime) {
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

}
