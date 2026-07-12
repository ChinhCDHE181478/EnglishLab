package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClassroomOfferingService {

    Page<ClassroomOfferingResponse> getPublicOfferings(ClassroomDeliveryMode mode, Pageable pageable);

    ClassroomOfferingResponse getPublicOffering(String slugOrId);

    List<ClassroomOfferingResponse> getMyClasses(String learnerEmail);

    List<ClassroomOfferingResponse> getAssignedClasses(String teacherEmail);

    List<ClassroomOfferingResponse> getManagerOfferings();

    ClassroomOfferingResponse getManagerOffering(Long id);

    ClassroomOfferingResponse getOffering(Long id, boolean full);

    ClassroomOfferingResponse getLearnerOffering(Long id, String learnerEmail);

    ClassroomOfferingResponse createOffering(CreateClassroomOfferingRequest request, String creatorEmail);

    ClassroomOfferingResponse updateOffering(Long id, CreateClassroomOfferingRequest request);

    ClassroomOfferingResponse publishOffering(Long id);

    ClassroomOfferingResponse closeOffering(Long id, String actorEmail);

    List<ClassroomSessionResponse> getSessions(Long offeringId);

    List<ClassroomSessionResponse> getLearnerSessions(Long offeringId, String learnerEmail);

    ClassroomSessionResponse createSession(Long offeringId, CreateClassroomSessionRequest request);

    /**
     * Tạo buổi học sau khi Training Manager đã duyệt yêu cầu thay đổi.
     * Khi {@code enforceConflictCheck} = false, bỏ qua kiểm tra xung đột vì TM đã ghi đè.
     */
    ClassroomSessionResponse createSession(
            Long offeringId,
            CreateClassroomSessionRequest request,
            boolean enforceConflictCheck
    );

    ClassroomSessionResponse updateSession(Long sessionId, CreateClassroomSessionRequest request);

    void deleteSession(Long sessionId);

    ClassroomEnrollmentResponse enrollStudent(Long offeringId, EnrollStudentRequest request);

    ClassroomEnrollmentResponse enrollStudentByEmail(Long offeringId, String studentEmail);

    void removeStudent(Long offeringId, Long studentId);

    ClassroomEnrollmentResponse transferStudent(Long offeringId, TransferStudentRequest request);

    ClassroomEnrollmentResponse registerForClass(Long offeringId, RegisterClassRequest request, String learnerEmail);

    ClassroomEnrollmentResponse getMyRegistration(Long offeringId, String learnerEmail);

    ClassroomEnrollmentResponse cancelMyRegistration(Long offeringId, String learnerEmail);

    List<ClassroomEnrollmentResponse> getMyRegistrations(String learnerEmail);

    ClassroomEnrollmentResponse confirmRegistration(Long enrollmentId, String actorEmail);

    ClassroomEnrollmentResponse rejectRegistration(Long enrollmentId, RejectRegistrationRequest request, String actorEmail);

    ClassroomEnrollmentResponse recordTuitionPayment(Long enrollmentId, RecordTuitionPaymentRequest request, String actorEmail);

    ClassroomEnrollmentResponse assignToClass(Long enrollmentId, AssignToClassRequest request, String actorEmail);

    List<ClassroomEnrollmentResponse> listRegistrations(
            ClassroomRegistrationStatus status,
            Long classroomOfferingId,
            Boolean needsAction
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
