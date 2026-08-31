package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;


import fu.sep490.g23.backend.dto.request.classroom.SaveAttendanceRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomAttendanceResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomAttendanceServiceImpl implements ClassroomAttendanceService {

    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassScheduleRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAttendanceResponse> getBySession(Long sessionId) {
        ClassSchedule session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));

        Long offeringId = session.getClassSection().getId();

        // 1. Existing attendance records for this session
        List<ClassroomAttendance> existingRecords = attendanceRepository.findBySessionId(sessionId);
        java.util.Map<Long, ClassroomAttendance> byStudentId = new java.util.LinkedHashMap<>();
        for (ClassroomAttendance record : existingRecords) {
            byStudentId.put(record.getStudent().getId(), record);
        }

        // 2. All enrolled students with class access (ASSIGNED)
        List<fu.sep490.g23.backend.entity.classroom.ClassEnrollment> enrollments =
                enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(
                        offeringId,
                        java.util.Set.of(fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus.ASSIGNED)
                );

        // 3. Merge: existing records first, then placeholders for students without records
        List<ClassroomAttendanceResponse> responses = new ArrayList<>();
        java.util.Set<Long> includedStudentIds = new java.util.HashSet<>(byStudentId.keySet());

        // Add existing attendance records
        for (ClassroomAttendance record : existingRecords) {
            responses.add(mapper.toAttendanceResponse(record));
        }

        // Add placeholder responses for enrolled students who don't have a record yet
        for (var enrollment : enrollments) {
            Long studentId = enrollment.getStudent().getId();
            if (!includedStudentIds.contains(studentId)) {
                responses.add(mapper.toPlaceholderAttendanceResponse(session, enrollment.getStudent()));
                includedStudentIds.add(studentId);
            }
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAttendanceResponse> getByClass(Long offeringId) {
        return attendanceRepository.findAll().stream()
                .filter(record -> record.getSession().getClassSection().getId().equals(offeringId))
                .map(mapper::toAttendanceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAttendanceResponse> getByClassForStudent(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        enrollmentRepository.findByStudentIdAndClassSectionId(learner.getId(), offeringId)
                .filter(enrollment -> ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS.contains(enrollment.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền truy cập lớp học này."));
        return attendanceRepository.findByStudentIdAndSession_ClassSectionId(learner.getId(), offeringId).stream()
                .map(mapper::toAttendanceResponse)
                .toList();
    }

    @Override
    public List<ClassroomAttendanceResponse> saveBulk(SaveAttendanceRequest request, String markerEmail) {
        User marker = accessHelper.requireUser(markerEmail);
        accessHelper.assertTeacher(marker);

        ClassSchedule session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học."));

        List<ClassroomAttendanceResponse> responses = new ArrayList<>();
        for (var record : request.getRecords()) {
            User student = userRepository.findById(record.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));

            ClassroomAttendance attendance = attendanceRepository
                    .findBySessionIdAndStudentId(session.getId(), student.getId())
                    .orElseGet(() -> ClassroomAttendance.builder()
                            .session(session)
                            .student(student)
                            .build());

            attendance.setStatus(record.getStatus());
            attendance.setNote(record.getNote());
            attendance.setJoinTime(record.getJoinTime());
            attendance.setLeaveTime(record.getLeaveTime());
            attendance.setDurationMinutes(record.getDurationMinutes());
            attendance.setTeacherConfirmed(true);
            attendance.setMarkedBy(marker);

            responses.add(mapper.toAttendanceResponse(attendanceRepository.save(attendance)));
        }
        return responses;
    }
}
