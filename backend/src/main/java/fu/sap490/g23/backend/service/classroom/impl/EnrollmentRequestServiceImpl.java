package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.dto.request.classroom.ConfirmEnrollmentPlacementRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.RejectEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.CompleteEnrollmentConsultationRequest;
import fu.sap490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sap490.g23.backend.dto.request.classroom.EnrollStudentRequest;
import fu.sap490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sap490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sap490.g23.backend.dto.response.classroom.EnrollmentRequestHistoryResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sap490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sap490.g23.backend.entity.classroom.EnrollmentRequestStatusHistory;
import fu.sap490.g23.backend.entity.classroom.TrainingProgram;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sap490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.repository.classroom.EnrollmentRequestRepository;
import fu.sap490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sap490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.security.TrainingRolePolicy;
import fu.sap490.g23.backend.service.assessment.PlacementEligibilityService;
import fu.sap490.g23.backend.service.classroom.EnrollmentRequestService;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentRequestServiceImpl implements EnrollmentRequestService {
    private static final Set<EnrollmentRequestStatus> TERMINAL_STATUSES = Set.of(
            EnrollmentRequestStatus.REJECTED,
            EnrollmentRequestStatus.CANCELLED,
            EnrollmentRequestStatus.CLASS_ASSIGNED
    );

    private final EnrollmentRequestRepository enrollmentRequestRepository;
    private final EnrollmentRequestStatusHistoryRepository historyRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassroomOfferingRepository classroomOfferingRepository;
    private final PlacementTestAttemptRepository placementAttemptRepository;
    private final UserRepository userRepository;
    private final PlacementEligibilityService placementEligibilityService;
    private final ClassroomOfferingService classroomOfferingService;

    @Override
    public CourseEnrollmentRequestResponse submit(CreateCourseEnrollmentRequest request, String learnerEmail) {
        User learner = requireUser(learnerEmail);
        if (!learner.hasRole(RoleEnum.LEARNER)) {
            throw new IllegalArgumentException("Chỉ học viên mới có thể gửi yêu cầu đăng ký khóa học.");
        }
        ClassroomOffering requestedClassroom = resolveRequestedClassroom(request.getClassroomId());
        TrainingProgram offering = requestedClassroom == null
                ? resolveOptionalProgram(request.getCourseOfferingId())
                : requestedClassroom.getTrainingProgram();
        if (enrollmentRequestRepository.existsByLearnerAndStatusNotIn(learner, TERMINAL_STATUSES)) {
            throw new IllegalArgumentException("Bạn đã có một form đang được Staff xử lý.");
        }

        EnrollmentRequest enrollmentRequest = EnrollmentRequest.builder()
                .learner(learner)
                .courseOffering(offering)
                .requestedClassroom(requestedClassroom)
                .placementAttempt(null)
                .status(EnrollmentRequestStatus.SUBMITTED)
                .contactName(request.getContactName().trim())
                .contactEmail(request.getContactEmail().trim().toLowerCase())
                .contactPhone(request.getContactPhone().trim())
                .facebookUrl(trimOrNull(request.getFacebookUrl()))
                .desiredClassCode(trimOrNull(request.getDesiredClassCode()))
                .consultationTrack(request.getConsultationTrack().trim())
                .studyWorkGoal(trimOrNull(request.getStudyWorkGoal()))
                .preferredSchedule(trimOrNull(request.getPreferredSchedule()))
                .campusPreference(trimOrNull(request.getCampusPreference()))
                .learnerNote(trimOrNull(request.getNote()))
                .build();
        enrollmentRequestRepository.save(enrollmentRequest);
        recordTransition(enrollmentRequest, null, EnrollmentRequestStatus.SUBMITTED, learner,
                requestedClassroom == null
                        ? "Học viên gửi form đăng ký học và nhận tư vấn chung."
                        : "Học viên gửi form tư vấn, có ghi mã lớp " + requestedClassroom.getLearningPackage().getTitle() + ".");
        return toResponse(enrollmentRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseEnrollmentRequestResponse> listMine(String learnerEmail) {
        User learner = requireUser(learnerEmail);
        return enrollmentRequestRepository.findByLearnerOrderByCreatedAtDesc(learner).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CourseEnrollmentRequestResponse refreshPlacement(Long requestId, String learnerEmail) {
        User learner = requireUser(learnerEmail);
        EnrollmentRequest request = requireRequest(requestId);
        assertOwner(request, learner);
        if (request.getStatus() != EnrollmentRequestStatus.AWAITING_PLACEMENT_TEST) {
            return toResponse(request);
        }
        PlacementTestAttempt latestAttempt = placementAttemptRepository
                .findTopByStudentOrderBySubmittedAtDesc(learner)
                .orElse(null);
        if (latestAttempt != null) {
            request.setPlacementAttempt(latestAttempt);
        }
        if (isEligible(learner, request.getPlacementAttempt())) {
            transition(
                    request,
                    EnrollmentRequestStatus.UNDER_STAFF_REVIEW,
                    learner,
                    "Placement test đã đủ điều kiện; yêu cầu được chuyển cho Staff."
            );
        }
        return toResponse(request);
    }

    @Override
    public CourseEnrollmentRequestResponse cancel(Long requestId, String learnerEmail) {
        User learner = requireUser(learnerEmail);
        EnrollmentRequest request = requireRequest(requestId);
        assertOwner(request, learner);
        if (request.getStatus() == EnrollmentRequestStatus.CLASS_PROPOSED
                || request.getStatus() == EnrollmentRequestStatus.CLASS_ASSIGNED
                || TERMINAL_STATUSES.contains(request.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu không còn có thể hủy ở trạng thái hiện tại.");
        }
        request.setCancelledAt(LocalDateTime.now());
        transition(request, EnrollmentRequestStatus.CANCELLED, learner, "Học viên hủy yêu cầu.");
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseEnrollmentRequestResponse> listForStaff(
            EnrollmentRequestStatus status,
            String staffEmail
    ) {
        assertStaff(requireUser(staffEmail));
        List<EnrollmentRequest> requests = status == null
                ? enrollmentRequestRepository.findAllByOrderByCreatedAtDesc()
                : enrollmentRequestRepository.findByStatusOrderByCreatedAtAsc(status);
        return requests.stream().map(this::toResponse).toList();
    }

    @Override
    public CourseEnrollmentRequestResponse confirmPlacementLevel(
            Long requestId,
            ConfirmEnrollmentPlacementRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        EnrollmentRequest request = requireRequest(requestId);
        if (request.getStatus() == EnrollmentRequestStatus.AWAITING_PLACEMENT_TEST
                && isEligible(request.getLearner(), request.getPlacementAttempt())) {
            transition(request, EnrollmentRequestStatus.UNDER_STAFF_REVIEW, staff, "Placement test đã đủ điều kiện.");
        }
        if (request.getStatus() != EnrollmentRequestStatus.UNDER_STAFF_REVIEW) {
            throw new IllegalArgumentException("Yêu cầu chưa sẵn sàng để xác nhận trình độ.");
        }
        if (!isEligible(request.getLearner(), request.getPlacementAttempt())) {
            throw new IllegalArgumentException("Placement test chưa đủ điều kiện để phân lớp.");
        }
        request.setConfirmedLevel(payload.getPlacementLevel());
        request.setStaffNote(trimOrNull(payload.getNote()));
        request.setReviewedBy(staff);
        request.setReviewedAt(LocalDateTime.now());
        transition(request, EnrollmentRequestStatus.WAITING_FOR_CLASS, staff, "Staff đã xác nhận trình độ phân lớp.");
        return toResponse(request);
    }

    @Override
    public CourseEnrollmentRequestResponse reject(
            Long requestId,
            RejectEnrollmentRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        EnrollmentRequest request = requireRequest(requestId);
        if (TERMINAL_STATUSES.contains(request.getStatus())
                || request.getStatus() == EnrollmentRequestStatus.CLASS_PROPOSED) {
            throw new IllegalArgumentException("Không thể từ chối yêu cầu ở trạng thái hiện tại.");
        }
        request.setRejectionReason(payload.getReason().trim());
        request.setReviewedBy(staff);
        request.setReviewedAt(LocalDateTime.now());
        transition(request, EnrollmentRequestStatus.REJECTED, staff, payload.getReason().trim());
        return toResponse(request);
    }

    @Override
    public CourseEnrollmentRequestResponse completeConsultation(
            Long requestId,
            CompleteEnrollmentConsultationRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        EnrollmentRequest request = requireRequest(requestId);
        if (request.getStatus() != EnrollmentRequestStatus.SUBMITTED
                && request.getStatus() != EnrollmentRequestStatus.UNDER_STAFF_REVIEW) {
            throw new IllegalArgumentException("Chỉ form mới hoặc đang xử lý mới có thể đánh dấu đã tư vấn.");
        }
        request.setStaffNote(trimOrNull(payload.getNote()));
        request.setReviewedBy(staff);
        request.setReviewedAt(LocalDateTime.now());
        transition(request, EnrollmentRequestStatus.WAITING_FOR_CLASS, staff,
                "Staff đã hoàn tất tư vấn/test bên ngoài và xác nhận sẵn sàng xếp lớp.");
        return toResponse(request);
    }

    @Override
    public CourseEnrollmentRequestResponse assignClass(
            Long requestId,
            AssignEnrollmentClassRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        EnrollmentRequest request = requireRequest(requestId);
        if (TERMINAL_STATUSES.contains(request.getStatus())
                || request.getStatus() == EnrollmentRequestStatus.CLASS_PROPOSED) {
            throw new IllegalArgumentException("Form không còn có thể xếp lớp ở trạng thái hiện tại.");
        }
        ClassroomOffering target = classroomOfferingRepository.findById(payload.getClassroomId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp cần xếp."));
        if (target.getStatus() != ClassroomOfferingStatus.UPCOMING
                && target.getStatus() != ClassroomOfferingStatus.ACTIVE) {
            throw new IllegalArgumentException("Chỉ có thể xếp vào lớp sắp khai giảng hoặc đang hoạt động.");
        }
        ClassroomEnrollmentResponse enrollment = classroomOfferingService.enrollStudent(
                target.getId(),
                EnrollStudentRequest.builder()
                        .studentId(request.getLearner().getId())
                        .note(trimOrNull(payload.getNote()))
                        .build()
        );
        if (!enrollment.isHasClassAccess()) {
            throw new IllegalArgumentException("Lớp đã đủ chỗ; hãy chọn lớp khác cho học viên.");
        }
        request.setAssignedClassroom(target);
        request.setStaffNote(trimOrNull(payload.getNote()));
        request.setReviewedBy(staff);
        request.setReviewedAt(LocalDateTime.now());
        transition(request, EnrollmentRequestStatus.CLASS_ASSIGNED, staff,
                "Staff đã xếp học viên vào lớp " + target.getLearningPackage().getTitle() + ".");
        return toResponse(request);
    }

    private void transition(
            EnrollmentRequest request,
            EnrollmentRequestStatus target,
            User actor,
            String reason
    ) {
        EnrollmentRequestStatus source = request.getStatus();
        if (source == target) return;
        request.setStatus(target);
        enrollmentRequestRepository.save(request);
        recordTransition(request, source, target, actor, reason);
    }

    private void recordTransition(
            EnrollmentRequest request,
            EnrollmentRequestStatus source,
            EnrollmentRequestStatus target,
            User actor,
            String reason
    ) {
        historyRepository.save(EnrollmentRequestStatusHistory.builder()
                .enrollmentRequest(request)
                .fromStatus(source)
                .toStatus(target)
                .actor(actor)
                .reason(reason)
                .build());
    }

    private CourseEnrollmentRequestResponse toResponse(EnrollmentRequest request) {
        PlacementEligibilityResult eligibility = request.getPlacementAttempt() == null
                ? null
                : placementEligibilityService.evaluateEligibility(
                        request.getLearner().getId(),
                        request.getPlacementAttempt().getId()
                );
        TrainingProgram offering = request.getCourseOffering();
        ClassroomOffering requestedClassroom = request.getRequestedClassroom();
        return CourseEnrollmentRequestResponse.builder()
                .id(request.getId())
                .learnerId(request.getLearner().getId())
                .learnerName(request.getLearner().getFullName())
                .learnerEmail(request.getLearner().getEmail())
                .contactName(request.getContactName() == null ? request.getLearner().getFullName() : request.getContactName())
                .contactEmail(request.getContactEmail() == null ? request.getLearner().getEmail() : request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .facebookUrl(request.getFacebookUrl())
                .desiredClassCode(request.getDesiredClassCode())
                .consultationTrack(request.getConsultationTrack())
                .studyWorkGoal(request.getStudyWorkGoal())
                .courseOfferingId(offering == null ? null : offering.getId())
                .courseOfferingTitle(offering == null ? null : offering.getTitle())
                .requestedClassroomId(requestedClassroom == null ? null : requestedClassroom.getId())
                .requestedClassroomTitle(requestedClassroom == null ? null : requestedClassroom.getLearningPackage().getTitle())
                .requestedClassroomCode(requestedClassroom == null ? null : requestedClassroom.getLearningPackage().getSlug())
                .requestedClassroomStartDate(requestedClassroom == null ? null : requestedClassroom.getStartDate())
                .requestedClassroomSchedule(requestedClassroom == null ? null : requestedClassroom.getLearningPackage().getStudyMode())
                .requestedClassroomTeacherName(requestedClassroom == null || requestedClassroom.getPrimaryTeacher() == null
                        ? null
                        : requestedClassroom.getPrimaryTeacher().getFullName())
                .requestedClassroomLocation(requestedClassroom == null
                        ? null
                        : requestedClassroom.getOfflineAddress())
                .deliveryType(offering == null ? null : offering.getDeliveryMode())
                .plannedStartDate(offering == null ? null : offering.getPlannedStartDate())
                .plannedSchedule(offering == null ? null : offering.getPlannedSchedule())
                .capacity(offering == null ? null : offering.getMaxCapacity())
                .status(request.getStatus())
                .statusLabel(statusLabel(request.getStatus()))
                .confirmedLevel(request.getConfirmedLevel())
                .preferredSchedule(request.getPreferredSchedule())
                .campusPreference(request.getCampusPreference())
                .learnerNote(request.getLearnerNote())
                .staffNote(request.getStaffNote())
                .rejectionReason(request.getRejectionReason())
                .placementAttemptId(request.getPlacementAttempt() == null ? null : request.getPlacementAttempt().getId())
                .placementEligibility(eligibility)
                .assignedClassroomId(request.getAssignedClassroom() == null ? null : request.getAssignedClassroom().getId())
                .history(historyRepository.findByEnrollmentRequestIdOrderByCreatedAtAscIdAsc(request.getId()).stream()
                        .map(this::toHistoryResponse)
                        .toList())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private EnrollmentRequestHistoryResponse toHistoryResponse(EnrollmentRequestStatusHistory history) {
        return EnrollmentRequestHistoryResponse.builder()
                .id(history.getId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .statusLabel(statusLabel(history.getToStatus()))
                .actorId(history.getActor() == null ? null : history.getActor().getId())
                .actorName(history.getActor() == null ? "Hệ thống" : history.getActor().getFullName())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private boolean isEligible(User learner, PlacementTestAttempt attempt) {
        return attempt != null && placementEligibilityService
                .evaluateEligibility(learner.getId(), attempt.getId())
                .isEligible();
    }

    private PlacementTestAttempt resolvePlacementAttempt(User learner, Long attemptId) {
        if (attemptId == null) {
            return placementAttemptRepository.findTopByStudentOrderBySubmittedAtDesc(learner).orElse(null);
        }
        PlacementTestAttempt attempt = placementAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả placement test."));
        if (!attempt.getStudent().getId().equals(learner.getId())) {
            throw new IllegalArgumentException("Kết quả placement test không thuộc học viên hiện tại.");
        }
        return attempt;
    }

    private ClassroomOffering resolveRequestedClassroom(Long classroomId) {
        if (classroomId == null) return null;
        ClassroomOffering classroom = classroomOfferingRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp đăng ký."));
        if (classroom.getStatus() != ClassroomOfferingStatus.UPCOMING
                || classroom.getLearningPackage() == null
                || !classroom.getLearningPackage().isPublished()
                || classroom.getStartDate() == null
                || !classroom.getStartDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Lớp này không còn mở nhận form tư vấn.");
        }
        return classroom;
    }

    private TrainingProgram resolveOptionalProgram(Long programId) {
        if (programId == null) return null;
        TrainingProgram program = trainingProgramRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình cần tư vấn."));
        if (program.getStatus() != PackageStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chương trình này chưa mở nhận tư vấn.");
        }
        return program;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private EnrollmentRequest requireRequest(Long id) {
        return enrollmentRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đăng ký."));
    }

    private void assertOwner(EnrollmentRequest request, User learner) {
        if (!request.getLearner().getId().equals(learner.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền truy cập yêu cầu này.");
        }
    }

    private void assertStaff(User user) {
        if (!TrainingRolePolicy.canPerformStaffAction(user)) {
            throw new IllegalArgumentException("Bạn không có quyền xử lý yêu cầu đăng ký.");
        }
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String statusLabel(EnrollmentRequestStatus status) {
        return switch (status) {
            case SUBMITTED -> "Mới đăng ký - chờ Staff liên hệ";
            case AWAITING_PLACEMENT_TEST -> "Chờ placement test";
            case PLACEMENT_TEST_COMPLETED -> "Đã hoàn thành placement test";
            case UNDER_STAFF_REVIEW -> "Nhân viên đang rà soát";
            case WAITING_FOR_CLASS -> "Đã tư vấn - chờ xếp lớp";
            case CLASS_PROPOSED -> "Đã có đề xuất lớp";
            case CLASS_ASSIGNED -> "Đã xếp lớp";
            case REJECTED -> "Đã từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }
}
