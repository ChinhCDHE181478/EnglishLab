package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.CreateAttendanceDisputeRequest;
import fu.sep490.g23.backend.dto.request.classroom.ReviewAttendanceDisputeRequest;
import fu.sep490.g23.backend.dto.response.classroom.AttendanceDisputeResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendance;
import fu.sep490.g23.backend.entity.classroom.ClassroomAttendanceDispute;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.classroom.enums.AttendanceDisputeStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceDisputeRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomAttendanceRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomTeacherAssignmentRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomAttendanceDisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomAttendanceDisputeServiceImpl implements ClassroomAttendanceDisputeService {

    private final ClassroomAttendanceDisputeRepository disputeRepository;
    private final ClassroomAttendanceRepository attendanceRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    private final ClassroomAccessHelper accessHelper;

    @Override
    public AttendanceDisputeResponse create(Long attendanceId, CreateAttendanceDisputeRequest request, String studentEmail) {
        User student = accessHelper.requireUser(studentEmail);
        ClassroomAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi điểm danh."));
        if (!attendance.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("Bạn chỉ có thể khiếu nại điểm danh của chính mình.");
        }
        disputeRepository.findByAttendanceIdAndStudentId(attendanceId, student.getId())
                .filter(dispute -> dispute.getStatus() == AttendanceDisputeStatus.PENDING)
                .ifPresent(ignored -> {
                    throw new RuntimeException("Bạn đã có khiếu nại đang chờ xử lý cho buổi học này.");
                });

        ClassroomAttendanceDispute dispute = disputeRepository.save(ClassroomAttendanceDispute.builder()
                .attendance(attendance)
                .student(student)
                .reason(request.getReason().trim())
                .status(AttendanceDisputeStatus.PENDING)
                .build());
        return toResponse(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDisputeResponse> listForStudent(String studentEmail) {
        User student = accessHelper.requireUser(studentEmail);
        return disputeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDisputeResponse> listForClass(Long offeringId, String teacherEmail) {
        User teacher = accessHelper.requireUser(teacherEmail);
        assertAssignedTeacher(teacher, offeringId);
        return disputeRepository.findByAttendanceSessionClassroomOfferingIdOrderByCreatedAtDesc(offeringId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDisputeResponse> listPending(String teacherEmail) {
        User teacher = accessHelper.requireUser(teacherEmail);
        return disputeRepository.findByStatusOrderByCreatedAtDesc(AttendanceDisputeStatus.PENDING).stream()
                .filter(dispute -> canReview(teacher, dispute.getAttendance().getSession().getClassroomOffering().getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AttendanceDisputeResponse review(Long disputeId, ReviewAttendanceDisputeRequest request, String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        ClassroomAttendanceDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại."));
        assertAssignedTeacher(reviewer, dispute.getAttendance().getSession().getClassroomOffering().getId());
        if (dispute.getStatus() != AttendanceDisputeStatus.PENDING) {
            throw new RuntimeException("Khiếu nại này đã được xử lý.");
        }
        if (request.getStatus() == AttendanceDisputeStatus.PENDING) {
            throw new RuntimeException("Trạng thái xử lý không hợp lệ.");
        }
        if (request.getStatus() == AttendanceDisputeStatus.REJECTED
                && (request.getReviewNote() == null || request.getReviewNote().isBlank())) {
            throw new RuntimeException("Vui lòng ghi chú lý do từ chối khiếu nại.");
        }

        dispute.setStatus(request.getStatus());
        dispute.setReviewNote(request.getReviewNote());
        dispute.setReviewedBy(reviewer);
        dispute.setReviewedAt(LocalDateTime.now());

        if (request.getStatus() == AttendanceDisputeStatus.APPROVED) {
            ClassroomAttendance attendance = dispute.getAttendance();
            if (request.getAttendanceStatus() != null && !request.getAttendanceStatus().isBlank()) {
                attendance.setStatus(ClassroomAttendanceStatus.valueOf(request.getAttendanceStatus()));
            } else {
                attendance.setStatus(ClassroomAttendanceStatus.PRESENT);
            }
            attendance.setTeacherConfirmed(true);
            attendance.setMarkedBy(reviewer);
            attendance.setNote("Cập nhật sau khiếu nại: " + dispute.getReason());
            attendanceRepository.save(attendance);
        }

        return toResponse(disputeRepository.save(dispute));
    }

    private void assertAssignedTeacher(User teacher, Long offeringId) {
        accessHelper.assertTeacher(teacher);
        if (!canReview(teacher, offeringId)) {
            throw new RuntimeException("Bạn không được phân công phụ trách lớp học này.");
        }
    }

    private boolean canReview(User teacher, Long offeringId) {
        boolean primaryTeacher = offeringRepository.findById(offeringId)
                .map(offering -> offering.getPrimaryTeacher() != null
                        && offering.getPrimaryTeacher().getId().equals(teacher.getId()))
                .orElse(false);
        if (primaryTeacher) {
            return true;
        }
        LocalDate today = LocalDate.now();
        return teacherAssignmentRepository.findByClassroomOfferingIdAndTeacherId(offeringId, teacher.getId())
                .filter(assignment -> assignment.getEffectiveFrom() == null || !assignment.getEffectiveFrom().isAfter(today))
                .filter(assignment -> assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today))
                .isPresent();
    }

    private AttendanceDisputeResponse toResponse(ClassroomAttendanceDispute dispute) {
        ClassroomAttendance attendance = dispute.getAttendance();
        return AttendanceDisputeResponse.builder()
                .id(dispute.getId())
                .attendanceId(attendance.getId())
                .sessionId(attendance.getSession().getId())
                .sessionTitle(formatSessionTitle(attendance.getSession()))
                .studentId(dispute.getStudent().getId())
                .studentName(dispute.getStudent().getFullName())
                .currentAttendanceStatus(attendance.getStatus().name())
                .reason(dispute.getReason())
                .status(dispute.getStatus().name())
                .reviewNote(dispute.getReviewNote())
                .reviewedByName(dispute.getReviewedBy() == null ? null : dispute.getReviewedBy().getFullName())
                .reviewedAt(dispute.getReviewedAt())
                .createdAt(dispute.getCreatedAt())
                .build();
    }

    private String formatSessionTitle(ClassroomSession session) {
        return session.getSessionDate() + " " + session.getStartTime() + "-" + session.getEndTime();
    }
}
