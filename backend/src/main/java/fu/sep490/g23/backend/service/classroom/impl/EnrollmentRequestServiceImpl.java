package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.CompleteEnrollmentTestRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCenterEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.RejectEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.ScheduleEnrollmentTestRequest;
import fu.sep490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sep490.g23.backend.dto.request.classroom.EnrollStudentRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sep490.g23.backend.dto.response.classroom.EnrollmentDemandReportResponse;
import fu.sep490.g23.backend.dto.response.classroom.EnrollmentRequestHistoryResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sep490.g23.backend.entity.classroom.EnrollmentRequestStatusHistory;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.EnrollmentRequestRepository;
import fu.sep490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.security.TrainingRolePolicy;
import fu.sep490.g23.backend.service.assessment.PlacementEligibilityService;
import fu.sep490.g23.backend.service.auth.AuthTokenService;
import fu.sep490.g23.backend.service.classroom.EnrollmentRequestService;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.service.mail.AuthMailService;
import fu.sep490.g23.backend.service.mail.EnrollmentRequestMailService;
import fu.sep490.g23.backend.service.user.UserRoleService;
import fu.sep490.g23.backend.service.classroom.ClassroomConflictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentRequestServiceImpl implements EnrollmentRequestService {
    private static final Set<EnrollmentRequestStatus> TERMINAL_STATUSES = Set.of(
            EnrollmentRequestStatus.REJECTED,
            EnrollmentRequestStatus.CANCELLED,
            EnrollmentRequestStatus.CLASS_ASSIGNED
    );

    private static final Set<EnrollmentRequestStatus> READ_ONLY_STATUSES = Set.of(
            EnrollmentRequestStatus.REJECTED,
            EnrollmentRequestStatus.CANCELLED,
            EnrollmentRequestStatus.CLASS_ASSIGNED
    );

    private static final Set<ClassroomSessionStatus> ACTIVE_SESSION_STATUSES = Set.of(
            ClassroomSessionStatus.SCHEDULED,
            ClassroomSessionStatus.OPEN,
            ClassroomSessionStatus.IN_PROGRESS,
            ClassroomSessionStatus.RESCHEDULED,
            ClassroomSessionStatus.MAKEUP
    );

    private final EnrollmentRequestRepository enrollmentRequestRepository;
    private final EnrollmentRequestStatusHistoryRepository historyRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassroomOfferingRepository classroomOfferingRepository;
    private final ClassroomSessionRepository classroomSessionRepository;
    private final ClassroomEnrollmentRepository classroomEnrollmentRepository;
    private final UserRepository userRepository;
    private final PlacementEligibilityService placementEligibilityService;
    private final ClassroomOfferingService classroomOfferingService;
    private final ClassroomConflictService classroomConflictService;
    private final EnrollmentRequestMailService enrollmentRequestMailService;
    private final AuthTokenService authTokenService;
    private final AuthMailService authMailService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public CourseEnrollmentRequestResponse submit(CreateCourseEnrollmentRequest request, String learnerEmail) {
        User learner = requireUser(learnerEmail);
        if (!learner.hasRole(RoleEnum.LEARNER)) {
            throw new IllegalArgumentException("Chỉ học viên mới có thể gửi yêu cầu đăng ký khóa học.");
        }
        if (request.getClassroomId() != null) {
            throw new IllegalArgumentException(
                    "Form đăng ký chỉ chọn khóa học quan tâm; Staff sẽ xếp lớp phù hợp sau khi tư vấn và test đầu vào."
            );
        }
        ClassroomOffering requestedClassroom = null;
        TrainingProgram offering = requirePublishedProgram(request.getCourseOfferingId());
        if (enrollmentRequestRepository.existsByLearnerAndCourseOfferingAndStatusNotIn(
                learner,
                offering,
                TERMINAL_STATUSES
        )) {
            throw new IllegalArgumentException("Bạn đã có một hồ sơ đang được xử lý cho khóa học này.");
        }

        EnrollmentRequest enrollmentRequest = EnrollmentRequest.builder()
                .learner(learner)
                .courseOffering(offering)
                .requestedClassroom(requestedClassroom)
                .placementAttempt(null)
                .status(EnrollmentRequestStatus.SUBMITTED)
                .requestSource(EnrollmentRequestSource.ONLINE)
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
        recordTransition(
                enrollmentRequest,
                null,
                EnrollmentRequestStatus.SUBMITTED,
                learner,
                "Học viên đăng ký tư vấn khóa học " + offering.getTitle() + "."
        );
        return toResponse(enrollmentRequest);
    }

    @Override
    public CourseEnrollmentRequestResponse createAtCenter(
            CreateCenterEnrollmentRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        ClassroomOffering target = requireAssignableClassroom(payload.getClassroomId());

        String normalizedEmail = payload.getEmail().trim().toLowerCase();
        User learner = userRepository.findByEmail(normalizedEmail).orElse(null);
        boolean accountCreated = learner == null;
        boolean setupEmailRequired = accountCreated || !learner.isEmailVerified();

        if (accountCreated) {
            learner = User.builder()
                    .fullName(payload.getFullName().trim())
                    .email(normalizedEmail)
                    .phoneNumber(payload.getPhoneNumber().trim())
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .emailVerified(true)
                    .profileCompleted(false)
                    .build();
            userRoleService.assignRole(learner, RoleEnum.LEARNER);
            learner = userRepository.save(learner);
        } else {
            if (!learner.hasRole(RoleEnum.LEARNER)) {
                throw new IllegalArgumentException(
                        "Email này đang thuộc tài khoản nội bộ và không thể dùng để mở hồ sơ học viên."
                );
            }
            if (!learner.isEmailVerified()) {
                learner.setEmailVerified(true);
                learner.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            }
            if (!StringUtils.hasText(learner.getPhoneNumber())) {
                learner.setPhoneNumber(payload.getPhoneNumber().trim());
            }
            learner = userRepository.save(learner);
        }

        if (classroomEnrollmentRepository
                .existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                        learner.getId(),
                        target.getId(),
                        ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS
                )) {
            throw new IllegalArgumentException("Học viên đã có hồ sơ còn hiệu lực trong lớp này.");
        }

        ClassroomEnrollmentResponse enrollment = classroomOfferingService.enrollStudent(
                target.getId(),
                EnrollStudentRequest.builder()
                        .studentId(learner.getId())
                        .note(trimOrNull(payload.getNote()))
                        .build()
        );
        if (!enrollment.isHasClassAccess()) {
            throw new IllegalArgumentException("Lớp đã đủ chỗ; hãy chọn lớp khác cho học viên.");
        }

        LocalDateTime now = LocalDateTime.now();
        EnrollmentRequest request = EnrollmentRequest.builder()
                .learner(learner)
                .courseOffering(target.getTrainingProgram())
                .assignedClassroom(target)
                .contactName(learner.getFullName())
                .contactEmail(learner.getEmail())
                .contactPhone(payload.getPhoneNumber().trim())
                .consultationTrack(target.getTrainingProgram() == null
                        ? null
                        : target.getTrainingProgram().getCode())
                .confirmedLevel(payload.getConfirmedLevel())
                .status(EnrollmentRequestStatus.CLASS_ASSIGNED)
                .requestSource(EnrollmentRequestSource.CENTER)
                .staffNote(trimOrNull(payload.getNote()))
                .reviewedBy(staff)
                .reviewedAt(now)
                .testCompletedAt(now)
                .build();
        request = enrollmentRequestRepository.save(request);
        recordTransition(
                request,
                null,
                EnrollmentRequestStatus.CLASS_ASSIGNED,
                staff,
                "Đã ghi danh trực tiếp tại trung tâm và xếp vào lớp "
                        + target.getLearningPackage().getTitle() + "."
        );

        if (setupEmailRequired) {
            AuthToken setupToken = authTokenService.issuePasswordResetToken(learner);
            authMailService.sendStaffCreatedAccountEmail(learner, setupToken.getToken());
        }
        enrollmentRequestMailService.sendClassAssignment(request, target);

        CourseEnrollmentRequestResponse response = toResponse(request);
        response.setLearnerAccountCreated(accountCreated);
        response.setAccountSetupEmailSent(setupEmailRequired);
        return response;
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
    public CourseEnrollmentRequestResponse scheduleTest(
            Long requestId,
            ScheduleEnrollmentTestRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        EnrollmentRequest request = requireRequest(requestId);
        if (request.getStatus() != EnrollmentRequestStatus.SUBMITTED
                && request.getStatus() != EnrollmentRequestStatus.INVITATION_SENT
                && request.getStatus() != EnrollmentRequestStatus.TEST_SCHEDULED) {
            throw new IllegalArgumentException("Chỉ hồ sơ mới đăng ký hoặc đã hẹn test mới có thể xếp lịch.");
        }
        if (!payload.getAppointmentAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ngày giờ đến test phải ở trong tương lai.");
        }
        request.setTestAppointmentAt(payload.getAppointmentAt());
        request.setTestLocation(payload.getLocation().trim());
        request.setInvitationSentAt(LocalDateTime.now());
        request.setStaffNote(trimOrNull(payload.getNote()));
        request.setReviewedBy(staff);
        request.setReviewedAt(LocalDateTime.now());
        transition(
                request,
                EnrollmentRequestStatus.TEST_SCHEDULED,
                staff,
                "Đã hẹn học viên đến test lúc "
                        + payload.getAppointmentAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                        + " tại " + payload.getLocation().trim() + "."
        );
        enrollmentRequestMailService.sendTestAppointment(request);
        return toResponse(request);
    }

    @Override
    public CourseEnrollmentRequestResponse completeTest(
            Long requestId,
            CompleteEnrollmentTestRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        EnrollmentRequest request = requireRequest(requestId);
        if (request.getStatus() != EnrollmentRequestStatus.TEST_SCHEDULED) {
            throw new IllegalArgumentException("Chỉ có thể ghi kết quả sau khi hồ sơ đã được xếp lịch test.");
        }
        request.setTestCompletedAt(LocalDateTime.now());
        request.setStaffNote(trimOrNull(payload.getNote()));
        request.setReviewedBy(staff);
        request.setReviewedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(payload.getEligible())) {
            if (payload.getPlacementLevel() == null) {
                throw new IllegalArgumentException("Vui lòng chọn trình độ phù hợp trước khi chuyển hồ sơ sang chờ xếp lớp.");
            }
            request.setConfirmedLevel(payload.getPlacementLevel());
            request.setRejectionReason(null);
            transition(
                    request,
                    EnrollmentRequestStatus.WAITING_FOR_CLASS,
                    staff,
                    "Học viên đã hoàn thành test, đủ điều kiện học và phù hợp trình độ "
                            + placementLevelLabel(payload.getPlacementLevel()) + "."
            );
        } else {
            if (!StringUtils.hasText(payload.getNote())) {
                throw new IllegalArgumentException("Vui lòng ghi rõ lý do học viên chưa đủ điều kiện.");
            }
            request.setConfirmedLevel(null);
            request.setRejectionReason(payload.getNote().trim());
            transition(
                    request,
                    EnrollmentRequestStatus.REJECTED,
                    staff,
                    "Học viên đã test nhưng chưa đủ điều kiện: " + payload.getNote().trim()
            );
        }
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
    public CourseEnrollmentRequestResponse assignClass(
            Long requestId,
            AssignEnrollmentClassRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        EnrollmentRequest request = requireRequest(requestId);
        if (request.getStatus() != EnrollmentRequestStatus.WAITING_FOR_CLASS) {
            throw new IllegalArgumentException(
                    "Chỉ có thể xếp lớp sau khi học viên đã test, đủ điều kiện và hồ sơ đang chờ xếp lớp."
            );
        }
        ClassroomOffering target = requireAssignableClassroom(payload.getClassroomId());
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
        enrollmentRequestMailService.sendClassAssignment(request, target);
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listAvailableClassroomIds(Long requestId, String staffEmail) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        EnrollmentRequest request = requireRequest(requestId);
        if (request.getStatus() != EnrollmentRequestStatus.WAITING_FOR_CLASS || request.getLearner() == null) {
            return List.of();
        }
        Long learnerId = request.getLearner().getId();
        return classroomOfferingRepository.findAll().stream()
                .filter(this::isAssignableClassroom)
                .filter(offering -> isAvailableForLearner(offering, learnerId))
                .map(ClassroomOffering::getId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDemandReportResponse> getDemandReport(String managerEmail) {
        User manager = requireUser(managerEmail);
        if (!TrainingRolePolicy.canApprove(manager)) {
            throw new IllegalArgumentException("Chỉ Manager mới có quyền xem báo cáo nhu cầu mở lớp.");
        }
        Map<TrainingProgram, List<EnrollmentRequest>> grouped = enrollmentRequestRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(request -> request.getCourseOffering() != null)
                .collect(Collectors.groupingBy(EnrollmentRequest::getCourseOffering));
        return grouped.entrySet().stream()
                .map(entry -> toDemandReport(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> Long.compare(right.getTotalRegistrations(), left.getTotalRegistrations()))
                .toList();
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
                .requestSource(request.getRequestSource() == null
                        ? EnrollmentRequestSource.ONLINE
                        : request.getRequestSource())
                .confirmedLevel(request.getConfirmedLevel())
                .preferredSchedule(request.getPreferredSchedule())
                .campusPreference(request.getCampusPreference())
                .learnerNote(request.getLearnerNote())
                .staffNote(request.getStaffNote())
                .rejectionReason(request.getRejectionReason())
                .invitationSentAt(request.getInvitationSentAt())
                .testAppointmentAt(request.getTestAppointmentAt())
                .testLocation(request.getTestLocation())
                .testCompletedAt(request.getTestCompletedAt())
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

    private EnrollmentDemandReportResponse toDemandReport(
            TrainingProgram program,
            List<EnrollmentRequest> requests
    ) {
        long awaitingContact = countStatus(requests, EnrollmentRequestStatus.SUBMITTED);
        long invitationsSent = countStatus(requests, EnrollmentRequestStatus.INVITATION_SENT);
        long testsScheduled = countStatus(requests, EnrollmentRequestStatus.TEST_SCHEDULED);
        long qualified = countStatus(requests, EnrollmentRequestStatus.WAITING_FOR_CLASS);
        long assigned = countStatus(requests, EnrollmentRequestStatus.CLASS_ASSIGNED);
        long rejected = countStatus(requests, EnrollmentRequestStatus.REJECTED);
        int capacity = program.getMaxCapacity() == null || program.getMaxCapacity() <= 0
                ? 30
                : program.getMaxCapacity();
        long activePipeline = awaitingContact + invitationsSent + testsScheduled + qualified;
        return EnrollmentDemandReportResponse.builder()
                .courseOfferingId(program.getId())
                .courseOfferingCode(program.getCode())
                .courseOfferingTitle(program.getTitle())
                .deliveryMode(program.getDeliveryMode())
                .classCapacity(capacity)
                .totalRegistrations((long) requests.size())
                .awaitingContact(awaitingContact)
                .invitationsSent(invitationsSent)
                .testsScheduled(testsScheduled)
                .qualifiedForClass(qualified)
                .assigned(assigned)
                .rejected(rejected)
                .suggestedClassCount(activePipeline == 0 ? 0 : (int) Math.ceil((double) activePipeline / capacity))
                .build();
    }

    private long countStatus(List<EnrollmentRequest> requests, EnrollmentRequestStatus status) {
        return requests.stream().filter(request -> request.getStatus() == status).count();
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

    private TrainingProgram requirePublishedProgram(Long programId) {
        TrainingProgram program = trainingProgramRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học cần tư vấn."));
        if (program.getStatus() != PackageStatus.PUBLISHED) {
            throw new IllegalArgumentException("Khóa học này chưa mở nhận đăng ký tư vấn.");
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
            case SUBMITTED -> "Mới đăng ký - chờ nhân viên liên hệ";
            case INVITATION_SENT -> "Đã gửi lời mời - chờ chốt lịch";
            case TEST_SCHEDULED -> "Đã hẹn lịch test";
            case AWAITING_PLACEMENT_TEST -> "Chờ placement test";
            case PLACEMENT_TEST_COMPLETED -> "Đã hoàn thành placement test";
            case UNDER_STAFF_REVIEW -> "Nhân viên đang rà soát";
            case WAITING_FOR_CLASS -> "Đủ điều kiện - chờ xếp lớp";
            case CLASS_PROPOSED -> "Đã có đề xuất lớp";
            case CLASS_ASSIGNED -> "Hoàn tất - Đã xếp lớp";
            case REJECTED -> "Đã từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }

    private ClassroomOffering requireAssignableClassroom(Long classroomId) {
        ClassroomOffering target = classroomOfferingRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp cần xếp."));
        if (!isAssignableClassroom(target)) {
            throw new IllegalArgumentException(
                    "Chỉ có thể xếp vào lớp đã công bố, còn chỗ và có ngày khai giảng trong tương lai."
            );
        }
        return target;
    }

    private String placementLevelLabel(fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel level) {
        return switch (level) {
            case BEGINNER -> "Cơ bản";
            case INTERMEDIATE -> "Trung cấp";
            case ADVANCED -> "Nâng cao";
        };
    }

    private boolean isAssignableClassroom(ClassroomOffering target) {
        return target.getLearningPackage() != null
                && target.getStatus() == fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus.UPCOMING
                && target.getStartDate() != null
                && target.getStartDate().isAfter(java.time.LocalDate.now())
                && target.getLearningPackage().getStatus() == fu.sep490.g23.backend.entity.course.enums.PackageStatus.PUBLISHED;
    }

    private boolean isAvailableForLearner(ClassroomOffering offering, Long learnerId) {
        var classResult = classroomConflictService.check(fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest.builder()
                .classroomOfferingId(offering.getId())
                .learnerIds(java.util.List.of(learnerId))
                .checkCapacity(true)
                .build());
        if (classResult.isHasBlockingConflict()) {
            return false;
        }

        return classroomSessionRepository
                .findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offering.getId())
                .stream()
                .filter(session -> ACTIVE_SESSION_STATUSES.contains(session.getStatus()))
                .allMatch(session -> !classroomConflictService.check(fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest.builder()
                        .sessionDate(session.getSessionDate())
                        .startTime(session.getStartTime())
                        .endTime(session.getEndTime())
                        .learnerIds(java.util.List.of(learnerId))
                        .checkCapacity(false)
                        .build()).isHasBlockingConflict());
    }
}
