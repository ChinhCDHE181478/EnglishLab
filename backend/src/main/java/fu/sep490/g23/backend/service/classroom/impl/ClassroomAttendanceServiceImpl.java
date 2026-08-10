package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;


import fu.sap490.g23.backend.dto.request.classroom.SaveAttendanceRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomAttendanceResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
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
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomAccessHelper accessHelper;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAttendanceResponse> getBySession(Long sessionId) {
        return attendanceRepository.findBySessionId(sessionId).stream()
                .map(mapper::toAttendanceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAttendanceResponse> getByClass(Long offeringId) {
        return attendanceRepository.findAll().stream()
                .filter(record -> record.getSession().getClassroomOffering().getId().equals(offeringId))
                .map(mapper::toAttendanceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomAttendanceResponse> getByClassForStudent(Long offeringId, String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId)
                .filter(enrollment -> ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS.contains(enrollment.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền truy cập lớp học này."));
        return attendanceRepository.findByStudentIdAndSession_ClassroomOfferingId(learner.getId(), offeringId).stream()
                .map(mapper::toAttendanceResponse)
                .toList();
    }

    @Override
    public List<ClassroomAttendanceResponse> saveBulk(SaveAttendanceRequest request, String markerEmail) {
        User marker = accessHelper.requireUser(markerEmail);
        accessHelper.assertTeacher(marker);

        ClassroomSession session = sessionRepository.findById(request.getSessionId())
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
