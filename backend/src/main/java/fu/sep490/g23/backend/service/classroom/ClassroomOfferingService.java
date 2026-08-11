package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.request.classroom.UpdateLarkLinkRequest;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomTeacherSummaryResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;

import fu.sep490.g23.backend.dto.request.classroom.TransferEnrollmentRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomTuitionPaymentResponse;

import fu.sep490.g23.backend.dto.request.classroom.ReorderWaitlistRequest;
import fu.sep490.g23.backend.dto.request.classroom.AssignToClassRequest;

import fu.sep490.g23.backend.dto.request.classroom.ResolveTuitionSettlementRequest;
import fu.sep490.g23.backend.dto.request.classroom.RecordTuitionPaymentRequest;

import fu.sep490.g23.backend.dto.request.classroom.RejectRegistrationRequest;
import fu.sep490.g23.backend.dto.request.classroom.TransferStudentRequest;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.dto.request.classroom.EnrollStudentRequest;

import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;

import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClassroomOfferingService {

    Page<ClassroomOfferingResponse> getPublicOfferings(ClassroomDeliveryMode mode, Pageable pageable);

    ClassroomOfferingResponse getPublicOffering(String slugOrId);

    List<ClassroomOfferingResponse> getMyClasses(String learnerEmail);

    List<ClassroomOfferingResponse> getAssignedClasses(String teacherEmail);

    List<ClassroomOfferingResponse> getStaffOfferings();

    ClassroomOfferingResponse getStaffOffering(Long id);

    ClassroomOfferingResponse getOffering(Long id, boolean full);

    ClassroomOfferingResponse getLearnerOffering(Long id, String learnerEmail);

    ClassroomOfferingResponse createOffering(CreateClassroomOfferingRequest request, String creatorEmail);

    ClassroomOfferingResponse updateOffering(Long id, CreateClassroomOfferingRequest request, String actorEmail);

    ClassroomOfferingResponse closeOffering(Long id, String actorEmail);

    List<ClassroomSessionResponse> getSessions(Long offeringId);

    List<ClassroomSessionResponse> getLearnerSessions(Long offeringId, String learnerEmail);

    ClassroomSessionResponse createSession(Long offeringId, CreateClassroomSessionRequest request);

    /**
     * Tạo buổi học sau khi Nhân viên đào tạo đã duyệt yêu cầu thay đổi.
     * Khi {@code enforceConflictCheck} = false, bỏ qua kiểm tra xung đột vì TM đã ghi đè.
     */
    ClassroomSessionResponse createSession(
            Long offeringId,
            CreateClassroomSessionRequest request,
            boolean enforceConflictCheck
    );

    ClassroomSessionResponse syncVirtualSessionMeeting(Long sessionId, String actorEmail);

    ClassroomSessionResponse updateSession(Long sessionId, CreateClassroomSessionRequest request);

    void deleteSession(Long sessionId);

    ClassroomEnrollmentResponse enrollStudent(Long offeringId, EnrollStudentRequest request);

    ClassroomEnrollmentResponse enrollStudentByEmail(Long offeringId, String studentEmail);

    void removeStudent(Long offeringId, Long studentId);

    ClassroomEnrollmentResponse transferStudent(Long offeringId, TransferStudentRequest request);

    ClassroomEnrollmentResponse confirmRegistration(Long enrollmentId, String actorEmail);

    ClassroomEnrollmentResponse rejectRegistration(Long enrollmentId, RejectRegistrationRequest request, String actorEmail);

    ClassroomEnrollmentResponse recordTuitionPayment(Long enrollmentId, RecordTuitionPaymentRequest request, String actorEmail);

    ClassroomEnrollmentResponse resolveTuitionSettlement(
            Long enrollmentId,
            ResolveTuitionSettlementRequest request,
            String actorEmail
    );

    /**
     * Ghi nhận học phí tự động sau khi PayOS xác nhận thanh toán thành công.
     * Idempotent theo {@code note} (thường chứa mã đơn PayOS).
     */
    ClassroomEnrollmentResponse applyPayosTuitionPayment(Long enrollmentId, java.math.BigDecimal amount, String note);

    ClassroomEnrollmentResponse assignToClass(Long enrollmentId, AssignToClassRequest request, String actorEmail);

    List<ClassroomEnrollmentResponse> listRegistrations(
            ClassroomRegistrationStatus status,
            Long classroomOfferingId,
            Boolean needsAction,
            Boolean settlementPending
    );

    List<ClassroomEnrollmentResponse> reorderWaitlist(
            Long classroomOfferingId,
            ReorderWaitlistRequest request,
            String actorEmail
    );

    ClassroomSessionResponse applyApprovedSessionScheduleChange(Long sessionId, CreateClassroomSessionRequest request);

    ClassroomEnrollmentResponse getRegistration(Long enrollmentId);

    List<ClassroomTuitionPaymentResponse> getTuitionHistory(Long enrollmentId);

    ClassroomEnrollmentResponse transferEnrollment(Long enrollmentId, TransferEnrollmentRequest request, String actorEmail);

    ConflictCheckResultResponse checkEnrollmentConflict(Long enrollmentId);

    ClassroomTeacherSummaryResponse assignTeacher(Long offeringId, Long teacherId, ClassroomTeacherRole role);

    ClassroomTeacherSummaryResponse replaceTeacher(Long offeringId, Long oldTeacherId, Long newTeacherId);

    ClassroomSessionResponse openVirtualSession(Long sessionId, String actorEmail);

    ClassroomSessionResponse joinVirtualSession(Long sessionId, String learnerEmail);

    ClassroomSessionResponse joinVirtualClass(Long offeringId, Long sessionId, String learnerEmail);

    ClassroomSessionResponse closeVirtualSession(Long sessionId, String actorEmail);

    ClassroomSessionResponse updateSessionLarkLink(Long sessionId, UpdateLarkLinkRequest request);

    ConflictCheckResultResponse checkConflict(ConflictCheckRequest request);
}
