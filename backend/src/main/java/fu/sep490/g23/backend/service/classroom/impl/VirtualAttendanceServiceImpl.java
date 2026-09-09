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
        // No-op: virtual tracking columns removed from schema
    }

    @Override
    public void finalizeVirtualAttendance(ClassSchedule session) {
        // No-op: virtual tracking columns removed from schema
    }
}
