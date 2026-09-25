package fu.sep490.g23.backend.service.classroom.impl;
import lombok.extern.slf4j.Slf4j;

import fu.sep490.g23.backend.dto.request.classroom.CompleteEnrollmentTestRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCenterEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.RejectEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.ScheduleEnrollmentTestRequest;
import fu.sep490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sep490.g23.backend.dto.request.classroom.EnrollStudentRequest;
import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestSummaryResponse;
import fu.sep490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sep490.g23.backend.dto.response.classroom.CenterEnrollmentLearnerResponse;
import fu.sep490.g23.backend.dto.response.classroom.EnrollmentDemandReportResponse;
import fu.sep490.g23.backend.dto.response.classroom.EnrollmentRequestHistoryResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.EnrollmentRequestStatusHistory;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.classroom.CourseRegistrationRequestRepository;
import fu.sep490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.security.TrainingRolePolicy;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.assessment.PlacementEligibilityService;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.auth.AuthTokenService;
import fu.sep490.g23.backend.service.classroom.EnrollmentRequestService;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.service.classroom.ClassroomConflictService;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.service.mail.AuthMailService;
import fu.sep490.g23.backend.service.mail.EnrollmentRequestMailService;
import fu.sep490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentRequestServiceImpl implements EnrollmentRequestService {
    private static final int ESTIMATED_CLASS_CAPACITY = 16;

    private static final Set<EnrollmentRequestStatus> TERMINAL_STATUSES = Set.of(
            EnrollmentRequestStatus.REJECTED,
            EnrollmentRequestStatus.CANCELLED,
            EnrollmentRequestStatus.CLASS_ASSIGNED
    );
    private static final Set<ClassroomSessionStatus> ACTIVE_SESSION_STATUSES = Set.of(
            ClassroomSessionStatus.SCHEDULED,
            ClassroomSessionStatus.OPEN,
            ClassroomSessionStatus.IN_PROGRESS
    );

    private final CourseRegistrationRequestRepository enrollmentRequestRepository;
    private final EnrollmentRequestStatusHistoryRepository historyRepository;
    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;
    private final PlacementEligibilityService placementEligibilityService;
    private final ClassroomOfferingService classSectionService;
    private final ClassroomMapper classroomMapper;
    private final ClassroomConflictService classroomConflictService;
    private final EnrollmentRequestMailService enrollmentRequestMailService;
    private final AuthTokenService authTokenService;
    private final AuthMailService authMailService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public CourseEnrollmentRequestResponse submit(CreateCourseEnrollmentRequest request, String learnerEmail) {
        User learner = requireUser(learnerEmail);
        if (!learner.hasRole(RoleCodes.LEARNER)) {
            throw new IllegalArgumentException("Chỉ học viên mới có thể gửi yêu cầu đăng ký khóa học.");
        }
        if (request.getClassroomId() != null) {
            throw new IllegalArgumentException(
                    "Form đăng ký chỉ chọn khóa học quan tâm; Staff sẽ xếp lớp phù hợp sau khi tư vấn và test đầu vào."
            );
        }
        ClassSection preferredClassSection = null;
        InstructorLedCourse offering = requirePublishedProgram(request.getCourseOfferingId());
        if (enrollmentRequestRepository.existsByLearnerAndCourseOfferingAndStatusNotIn(
                learner,
                offering,
                TERMINAL_STATUSES
        )) {
            throw new IllegalArgumentException("Bạn đã có một hồ sơ đang được xử lý cho khóa học này.");
        }

        CourseRegistrationRequest courseRegistrationRequest = CourseRegistrationRequest.builder()
                .learner(learner)
                .courseOffering(offering)
                .preferredClassSection(preferredClassSection)
                .placementAttempt(null)
                .status(EnrollmentRequestStatus.SUBMITTED)
                .requestSource(EnrollmentRequestSource.ONLINE)
                .contactName(request.getContactName().trim())
                .contactEmail(request.getContactEmail().trim().toLowerCase())
                .contactPhone(request.getContactPhone().trim())
                .facebookUrl(trimOrNull(request.getFacebookUrl()))
                .consultationTrack(request.getConsultationTrack().trim())
                .studyWorkGoal(trimOrNull(request.getStudyWorkGoal()))
                .preferredSchedule(trimOrNull(request.getPreferredSchedule()))
                .learnerNote(trimOrNull(request.getNote()))
                .reviewedBy(nextEnrollmentOwner())
                .reviewedAt(LocalDateTime.now())
                .build();
        enrollmentRequestRepository.save(courseRegistrationRequest);
        recordTransition(
                courseRegistrationRequest,
                null,
                EnrollmentRequestStatus.SUBMITTED,
                learner,
                "Học viên đăng ký tư vấn khóa học " + offering.getTitle() + "."
        );
        return toResponse(courseRegistrationRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public CenterEnrollmentLearnerResponse findCenterEnrollmentLearner(
            String email,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Vui lòng nhập email học viên.");
        }

        User learner = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (learner == null) {
            return CenterEnrollmentLearnerResponse.builder()
                    .existingAccount(false)
                    .unavailableCourseIds(List.of())
                    .unavailableClassroomIds(List.of())
                    .build();
        }
        if (!learner.hasRole(RoleCodes.LEARNER)) {
            throw new IllegalArgumentException(
                    "Email này đang thuộc tài khoản nội bộ và không thể dùng để ghi danh học viên."
            );
        }
        return CenterEnrollmentLearnerResponse.builder()
                .existingAccount(true)
                .fullName(learner.getFullName())
                .phoneNumber(learner.getPhoneNumber())
                .unavailableCourseIds(findUnavailableCourseIds(learner))
                .unavailableClassroomIds(findUnavailableClassroomIds(learner))
                .build();
    }

    @Override
    public CourseEnrollmentRequestResponse createAtCenter(
            CreateCenterEnrollmentRequest payload,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        ClassSection target = requireAssignableClassroom(payload.getClassroomId());

        String normalizedEmail = payload.getEmail().trim().toLowerCase();
        User learner = userRepository.findByEmail(normalizedEmail).orElse(null);
        boolean accountCreated = learner == null;
        boolean setupEmailRequired = accountCreated || !learner.isEmailVerified();

        if (accountCreated) {
            if (!StringUtils.hasText(payload.getFullName())) {
                throw new IllegalArgumentException("Vui lòng nhập họ và tên cho tài khoản học viên mới.");
            }
            if (!StringUtils.hasText(payload.getPhoneNumber())) {
                throw new IllegalArgumentException("Vui lòng nhập số điện thoại cho tài khoản học viên mới.");
            }
            learner = User.builder()
                    .fullName(payload.getFullName().trim())
                    .email(normalizedEmail)
                    .phoneNumber(payload.getPhoneNumber().trim())
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .emailVerified(true)
                    .profileCompleted(false)
                    .build();
            userRoleService.assignRole(learner, RoleCodes.LEARNER);
            learner = userRepository.save(learner);
        } else {
            if (!learner.hasRole(RoleCodes.LEARNER)) {
                throw new IllegalArgumentException(
                        "Email này đang thuộc tài khoản nội bộ và không thể dùng để mở hồ sơ học viên."
                );
            }
            if (!learner.isEmailVerified()) {
                learner.setEmailVerified(true);
                learner.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            }
            if (!StringUtils.hasText(learner.getPhoneNumber())) {
                if (!StringUtils.hasText(payload.getPhoneNumber())) {
                    throw new IllegalArgumentException("Vui lòng bổ sung số điện thoại cho tài khoản học viên.");
                }
                learner.setPhoneNumber(payload.getPhoneNumber().trim());
            }
            learner = userRepository.save(learner);
        }

        if (hasActiveRequestOrEnrollmentForCourse(learner, target.getInstructorLedCourse().getId())) {
            throw new IllegalArgumentException(
                    "Học viên đã có hồ sơ đăng ký hoặc lớp học còn hiệu lực cho khóa học này."
            );
        }

        ClassroomEnrollmentResponse enrollment = classSectionService.enrollStudent(
                target.getId(),
                EnrollStudentRequest.builder()
                        .studentId(learner.getId())
                        .note(trimOrNull(payload.getNote()))
                        .build()
        );
        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST) {
            throw new IllegalArgumentException("Lớp đã đủ chỗ; hãy chọn lớp khác cho học viên.");
        }

        LocalDateTime now = LocalDateTime.now();
        CourseRegistrationRequest request = CourseRegistrationRequest.builder()
                .learner(learner)
                .courseOffering(target.getInstructorLedCourse())
                .assignedClassSection(target)
                .contactName(learner.getFullName())
                .contactEmail(learner.getEmail())
                .contactPhone(learner.getPhoneNumber())
                .consultationTrack(target.getInstructorLedCourse() == null
                        ? null
                        : target.getInstructorLedCourse().getCode())
                .status(EnrollmentRequestStatus.CLASS_ASSIGNED)
                .requestSource(EnrollmentRequestSource.CENTER)
                .staffNote(trimOrNull(payload.getNote()))
                .reviewedBy(staff)
                .reviewedAt(now)
                .build();
        request = enrollmentRequestRepository.save(request);
        recordTransition(
                request,
                null,
                EnrollmentRequestStatus.CLASS_ASSIGNED,
                staff,
                "Đã ghi danh trực tiếp tại trung tâm và xếp vào lớp "
                        + target.getTitle() + "."
        );

        if (setupEmailRequired) {
            AuthToken setupToken = authTokenService.issuePasswordResetToken(learner);
            authMailService.sendStaffCreatedAccountEmail(learner, setupToken.getToken());
        }
        enrollmentRequestMailService.sendClassAssignment(request, target, enrollment);

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
    public CourseEnrollmentRequestResponse respondToCourseRecommendation(
            Long requestId,
            boolean accepted,
            String learnerEmail
    ) {
        User learner = requireUser(learnerEmail);
        CourseRegistrationRequest request = enrollmentRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đăng ký."));
        if (request.getLearner() == null || !request.getLearner().getId().equals(learner.getId())) {
            throw new AccessDeniedException("Bạn không có quyền phản hồi đề xuất này.");
        }
        if (request.getStatus() != EnrollmentRequestStatus.CLASS_PROPOSED) {
            throw new IllegalArgumentException("Đề xuất khóa học này không còn chờ xác nhận.");
        }
        if (accepted) {
            request.setRejectionReason(null);
            transition(
                    request,
                    EnrollmentRequestStatus.WAITING_FOR_CLASS,
                    learner,
                    "Học viên đã đồng ý với khóa học được đề xuất."
            );
        } else {
            request.setCancelledAt(LocalDateTime.now());
            request.setRejectionReason("Học viên không đồng ý với khóa học được đề xuất.");
            transition(
                    request,
                    EnrollmentRequestStatus.CANCELLED,
                    learner,
                    request.getRejectionReason()
            );
        }
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseEnrollmentRequestResponse> listForStaff(
            EnrollmentRequestStatus status,
            String staffEmail
    ) {
        User staff = requireUser(staffEmail);
        assertStaffViewer(staff);
        List<CourseRegistrationRequest> requests;
        if (staff.hasRole(RoleCodes.ADMIN)) {
            requests = status == null
                    ? enrollmentRequestRepository.findAllByOrderByCreatedAtDesc()
                    : enrollmentRequestRepository.findByStatusOrderByCreatedAtAsc(status);
        } else {
            requests = status == null
                    ? enrollmentRequestRepository.findByReviewedByOrderByCreatedAtDesc(staff)
                    : enrollmentRequestRepository.findByReviewedByAndStatusOrderByCreatedAtAsc(staff, status);
        }
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
        CourseRegistrationRequest request = requireOwnedRequestForUpdate(requestId, staff);
        if (request.getStatus() != EnrollmentRequestStatus.SUBMITTED
                && request.getStatus() != EnrollmentRequestStatus.INVITATION_SENT
                && request.getStatus() != EnrollmentRequestStatus.TEST_SCHEDULED) {
            throw new IllegalArgumentException("Chỉ hồ sơ mới đăng ký hoặc đã hẹn test mới có thể xếp lịch.");
        }
        if (!payload.getAppointmentAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ngày giờ đến test phải ở trong tương lai.");
        }
        request.setInvitationSentAt(LocalDateTime.now());
        request.setTestAppointmentAt(payload.getAppointmentAt());
        request.setTestLocation(payload.getLocation().trim());
        request.setStaffNote(trimOrNull(payload.getNote()));
        transition(
                request,
                EnrollmentRequestStatus.TEST_SCHEDULED,
                staff,
                "Đã hẹn học viên đến test lúc "
                        + payload.getAppointmentAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                        + " tại " + payload.getLocation().trim() + "."
        );
        enrollmentRequestMailService.sendTestAppointment(
                request,
                payload.getAppointmentAt(),
                payload.getLocation().trim()
        );
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
        CourseRegistrationRequest request = requireOwnedRequestForUpdate(requestId, staff);
        if (request.getStatus() != EnrollmentRequestStatus.TEST_SCHEDULED) {
            throw new IllegalArgumentException("Chỉ có thể ghi kết quả sau khi hồ sơ đã được xếp lịch test.");
        }
        request.setTestCompletedAt(LocalDateTime.now());
        String evaluatedCourseTitle = request.getCourseOffering() == null
                ? null
                : request.getCourseOffering().getTitle();
        request.setStaffNote(trimOrNull(payload.getNote()));
        if (Boolean.TRUE.equals(payload.getEligible())) {
            request.setConfirmedLevel(null);
            request.setRejectionReason(null);
            transition(
                    request,
                    EnrollmentRequestStatus.WAITING_FOR_CLASS,
                    staff,
                    "Học viên đã hoàn thành đánh giá và phù hợp với khóa học đã đăng ký."
            );
        } else {
            if (!StringUtils.hasText(payload.getNote())) {
                throw new IllegalArgumentException("Vui lòng ghi rõ lý do học viên chưa đủ điều kiện.");
            }
            request.setConfirmedLevel(null);
            if (payload.getRecommendedCourseOfferingId() != null) {
                InstructorLedCourse recommendation = requirePublishedProgram(payload.getRecommendedCourseOfferingId());
                if (request.getCourseOffering() != null
                        && request.getCourseOffering().getId().equals(recommendation.getId())) {
                    throw new IllegalArgumentException("Khóa học đề xuất phải khác khóa học đang được đánh giá.");
                }
                request.setCourseOffering(recommendation);
                request.setRejectionReason(null);
                transition(
                        request,
                        EnrollmentRequestStatus.CLASS_PROPOSED,
                        staff,
                        "Khóa học ban đầu chưa phù hợp. Đã đề xuất khóa "
                                + recommendation.getTitle() + " và chờ học viên xác nhận."
                );
            } else {
                request.setRejectionReason(payload.getNote().trim());
                transition(
                        request,
                        EnrollmentRequestStatus.REJECTED,
                        staff,
                        "Học viên đã test nhưng chưa đủ điều kiện: " + payload.getNote().trim()
                );
            }
        }
        enrollmentRequestMailService.sendTestResult(
                request,
                Boolean.TRUE.equals(payload.getEligible()),
                evaluatedCourseTitle
        );
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
        CourseRegistrationRequest request = requireOwnedRequestForUpdate(requestId, staff);
        if (TERMINAL_STATUSES.contains(request.getStatus())
                || request.getStatus() == EnrollmentRequestStatus.CLASS_PROPOSED) {
            throw new IllegalArgumentException("Không thể từ chối yêu cầu ở trạng thái hiện tại.");
        }
        request.setRejectionReason(payload.getReason().trim());
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
        CourseRegistrationRequest request = requireOwnedRequestForUpdate(requestId, staff);
        if (!isReadyForClassAssignment(request)) {
            throw new IllegalArgumentException(
                    "Chỉ có thể xếp lớp sau khi học viên đã test, đủ điều kiện và hồ sơ đang chờ xếp lớp."
            );
        }
        ClassSection target = requireAssignableClassroomForUpdate(payload.getClassroomId());
        if (!matchesRequestedCourse(target, request)) {
            throw new IllegalArgumentException("Lớp đã chọn không thuộc khóa học đã được học viên xác nhận.");
        }
        User learner = userRepository.findByIdForUpdate(request.getLearner().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        if (!isAvailableForLearner(target, learner.getId())) {
            throw new IllegalArgumentException(
                    "Lớp không còn phù hợp: có thể đã đủ chỗ, học viên đã được ghi danh hoặc lịch học bị trùng. Vui lòng chọn lại."
            );
        }
        ClassroomEnrollmentResponse enrollment = classSectionService.enrollStudent(
                target.getId(),
                EnrollStudentRequest.builder()
                        .studentId(learner.getId())
                        .note(trimOrNull(payload.getNote()))
                        .build()
        );
        if (enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST) {
            throw new IllegalArgumentException("Lớp đã đủ chỗ; hãy chọn lớp khác cho học viên.");
        }
        request.setAssignedClassSection(target);
        request.setStaffNote(trimOrNull(payload.getNote()));
        transition(request, EnrollmentRequestStatus.CLASS_ASSIGNED, staff,
                "Đã chọn lớp " + target.getTitle() + " và chuyển học viên sang bước thanh toán học phí.");
        enrollmentRequestMailService.sendClassAssignment(request, target, enrollment);
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listAvailableClassroomIds(Long requestId, String staffEmail) {
        User staff = requireUser(staffEmail);
        assertStaff(staff);
        CourseRegistrationRequest request = requireOwnedRequest(requestId, staff);
        if (!isReadyForClassAssignment(request)) {
            return List.of();
        }
        Long learnerId = request.getLearner().getId();
        return classSectionRepository.findAll().stream()
                .filter(this::isAssignableClassroom)
                .filter(offering -> matchesRequestedCourse(offering, request))
                .filter(offering -> isAvailableForLearner(offering, learnerId))
                .map(ClassSection::getId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDemandReportResponse> getDemandReport(String managerEmail) {
        User manager = requireUser(managerEmail);
        if (!TrainingRolePolicy.canApprove(manager)) {
            throw new IllegalArgumentException("Chỉ Manager mới có quyền xem báo cáo nhu cầu mở lớp.");
        }
        Map<InstructorLedCourse, List<CourseRegistrationRequest>> grouped = enrollmentRequestRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(request -> request.getCourseOffering() != null)
                .filter(this::isRelevantForClassOpeningDemand)
                .collect(Collectors.groupingBy(CourseRegistrationRequest::getCourseOffering));

        // Include every published instructor-led course so manager can request opening
        // even when there are currently zero registration requests.
        return instructorLedCourseRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(course -> course.getPublicationStatus() == PackageStatus.PUBLISHED)
                .map(course -> toDemandReport(course, grouped.getOrDefault(course, List.of())))
                .sorted((left, right) -> {
                    int byDemand = Long.compare(
                            right.getTotalRegistrations() == null ? 0L : right.getTotalRegistrations(),
                            left.getTotalRegistrations() == null ? 0L : left.getTotalRegistrations()
                    );
                    if (byDemand != 0) {
                        return byDemand;
                    }
                    return String.valueOf(left.getCourseOfferingTitle())
                            .compareToIgnoreCase(String.valueOf(right.getCourseOfferingTitle()));
                })
                .toList();
    }

    private void transition(
            CourseRegistrationRequest request,
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
            CourseRegistrationRequest request,
            EnrollmentRequestStatus source,
            EnrollmentRequestStatus target,
            User actor,
            String reason
    ) {
        historyRepository.save(EnrollmentRequestStatusHistory.builder()
                .courseRegistrationRequest(request)
                .fromStatus(source)
                .toStatus(target)
                .actor(actor)
                .reason(reason)
                .build());
    }

    private CourseEnrollmentRequestResponse toResponse(CourseRegistrationRequest request) {
        PlacementEligibilityResult eligibility = request.getPlacementAttempt() == null
                ? null
                : placementEligibilityService.evaluateEligibility(
                        request.getLearner().getId(),
                        request.getPlacementAttempt().getId()
                );
        InstructorLedCourse offering = request.getCourseOffering();
        ClassSection preferredClassSection = request.getPreferredClassSection();
        ClassSection assignedClassSection = request.getAssignedClassSection();
        ClassroomEnrollmentResponse assignedEnrollment = assignedClassSection == null
                ? null
                : classEnrollmentRepository.findByStudentIdAndClassSectionId(
                        request.getLearner().getId(), assignedClassSection.getId())
                .map(classroomMapper::toEnrollmentResponse)
                .orElse(null);
        return CourseEnrollmentRequestResponse.builder()
                .id(request.getId())
                .learnerId(request.getLearner().getId())
                .learnerName(request.getLearner().getFullName())
                .learnerEmail(request.getLearner().getEmail())
                .contactName(request.getContactName() == null ? request.getLearner().getFullName() : request.getContactName())
                .contactEmail(request.getContactEmail() == null ? request.getLearner().getEmail() : request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .facebookUrl(request.getFacebookUrl())
                .consultationTrack(request.getConsultationTrack())
                .studyWorkGoal(request.getStudyWorkGoal())
                .courseOfferingId(offering == null ? null : offering.getId())
                .courseOfferingTitle(offering == null ? null : offering.getTitle())
                .requestedClassroomId(preferredClassSection == null ? null : preferredClassSection.getId())
                .requestedClassroomTitle(preferredClassSection == null ? null : preferredClassSection.getTitle())
                .requestedClassroomCode(preferredClassSection == null ? null : preferredClassSection.getSlug())
                .requestedClassroomStartDate(preferredClassSection == null ? null : preferredClassSection.getStartDate())
                .requestedClassroomSchedule(preferredClassSection == null || preferredClassSection.getDeliveryMode() == null
                        ? null
                        : preferredClassSection.getDeliveryMode().name())
                .requestedClassroomTeacherName(preferredClassSection == null || preferredClassSection.getPrimaryTeacher() == null
                        ? null
                        : preferredClassSection.getPrimaryTeacher().getFullName())
                .requestedClassroomLocation(preferredClassSection == null
                        ? null
                        : preferredClassSection.getRoom() == null
                        ? null
                        : preferredClassSection.getRoom().getLocationAddress())
                .deliveryType(preferredClassSection == null ? null : preferredClassSection.getDeliveryMode())
                .status(request.getStatus())
                .statusLabel(statusLabel(request.getStatus(), assignedEnrollment))
                .requestSource(request.getRequestSource() == null
                        ? EnrollmentRequestSource.ONLINE
                        : request.getRequestSource())
                .confirmedLevel(request.getConfirmedLevel())
                .preferredSchedule(request.getPreferredSchedule())
                .learnerNote(request.getLearnerNote())
                .staffNote(request.getStaffNote())
                .rejectionReason(request.getRejectionReason())
                .invitationSentAt(request.getInvitationSentAt())
                .testAppointmentAt(request.getTestAppointmentAt())
                .testLocation(request.getTestLocation())
                .testCompletedAt(request.getTestCompletedAt())
                .placementAttemptId(request.getPlacementAttempt() == null ? null : request.getPlacementAttempt().getId())
                .placementEligibility(eligibility)
                .latestPlacementResult(latestPlacementResult(request.getLearner()))
                .assignedClassroomId(assignedClassSection == null ? null : assignedClassSection.getId())
                .assignedClassroomTitle(assignedClassSection == null ? null : assignedClassSection.getTitle())
                .assignedEnrollment(assignedEnrollment)
                .history(historyRepository.findByCourseRegistrationRequestIdOrderByCreatedAtAscIdAsc(request.getId()).stream()
                        .map(this::toHistoryResponse)
                        .toList())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private PlacementTestSummaryResponse latestPlacementResult(User learner) {
        return placementTestAttemptRepository
                .findByStudentAndTestCodeOrderBySubmittedAtDesc(
                        learner,
                        PlacementTestDefinitionService.TEST_CODE
                ).stream()
                .filter(attempt -> !isSkillAssessment(attempt))
                .findFirst()
                .map(attempt -> PlacementTestSummaryResponse.builder()
                        .attemptId(attempt.getId())
                        .examType(placementExamType(attempt))
                        .listeningScore(attempt.getListeningScore())
                        .readingScore(attempt.getReadingScore())
                        .writingScore(attempt.getWritingScore())
                        .speakingScore(attempt.getSpeakingScore())
                        .overallScore(attempt.getOverallScore())
                        .evaluationStatus(attempt.getEvaluationStatus())
                        .recommendedLevel(attempt.getRecommendedLevel())
                        .submittedAt(attempt.getSubmittedAt())
                        .reviewedAt(attempt.getReviewedAt())
                        .build())
                .orElse(null);
    }

    private boolean isSkillAssessment(PlacementTestAttempt attempt) {
        return String.valueOf(attempt.getAiFeedbackJson()).contains("\"examType\":\"SKILL\"");
    }

    private String placementExamType(PlacementTestAttempt attempt) {
        String feedback = String.valueOf(attempt.getAiFeedbackJson());
        if (feedback.contains("\"examType\":\"TOEIC\"")
                || attempt.getOverallScore() != null
                && attempt.getOverallScore().compareTo(BigDecimal.valueOf(9)) > 0) {
            return "TOEIC";
        }
        return "IELTS";
    }

    private EnrollmentDemandReportResponse toDemandReport(
            InstructorLedCourse program,
            List<CourseRegistrationRequest> requests
    ) {
        // requests already filtered to opening-demand scope
        long awaitingContact = countStatus(requests, EnrollmentRequestStatus.SUBMITTED);
        long invitationsSent = countStatus(requests, EnrollmentRequestStatus.INVITATION_SENT);
        long testsScheduled = countStatus(requests, EnrollmentRequestStatus.TEST_SCHEDULED)
                + countStatus(requests, EnrollmentRequestStatus.AWAITING_PLACEMENT_TEST);
        long qualified = countStatus(requests, EnrollmentRequestStatus.WAITING_FOR_CLASS)
                + countStatus(requests, EnrollmentRequestStatus.PLACEMENT_TEST_COMPLETED)
                + countStatus(requests, EnrollmentRequestStatus.UNDER_STAFF_REVIEW)
                + countStatus(requests, EnrollmentRequestStatus.CLASS_PROPOSED);
        long assignedUpcoming = requests.stream()
                .filter(request -> request.getStatus() == EnrollmentRequestStatus.CLASS_ASSIGNED)
                .count();
        long activePipeline = awaitingContact + invitationsSent + testsScheduled + qualified;

        List<ClassSection> upcomingClasses = classSectionRepository.findAll().stream()
                .filter(section -> section.getInstructorLedCourse() != null
                        && program.getId().equals(section.getInstructorLedCourse().getId()))
                .filter(section -> section.getStatus() == ClassroomOfferingStatus.UPCOMING)
                .toList();
        int capacityHint = upcomingClasses.stream()
                .map(ClassSection::getCapacity)
                .filter(java.util.Objects::nonNull)
                .filter(capacity -> capacity > 0)
                .findFirst()
                .orElse(ESTIMATED_CLASS_CAPACITY);
        long openSeatRemaining = upcomingClasses.stream()
                .mapToLong(section -> {
                    int capacity = section.getCapacity() == null ? 0 : Math.max(0, section.getCapacity());
                    long occupied = classEnrollmentRepository.countByOfferingAndRegistrationStatuses(
                            section.getId(),
                            ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT
                    );
                    return Math.max(0, capacity - occupied);
                })
                .sum();
        long overflow = Math.max(0, activePipeline - openSeatRemaining);
        int suggested = overflow == 0 ? 0 : (int) Math.ceil((double) overflow / capacityHint);

        return EnrollmentDemandReportResponse.builder()
                .courseOfferingId(program.getId())
                .courseOfferingCode(program.getCode())
                .courseOfferingTitle(program.getTitle())
                .deliveryMode(requests.stream()
                        .map(CourseRegistrationRequest::getPreferredClassSection)
                        .filter(java.util.Objects::nonNull)
                        .map(ClassSection::getDeliveryMode)
                        .findFirst()
                        .orElseGet(() -> upcomingClasses.stream()
                                .map(ClassSection::getDeliveryMode)
                                .filter(java.util.Objects::nonNull)
                                .findFirst()
                                .orElse(null)))
                .classCapacity(capacityHint)
                .totalRegistrations((long) requests.size())
                .awaitingContact(awaitingContact)
                .invitationsSent(invitationsSent)
                .testsScheduled(testsScheduled)
                .qualifiedForClass(qualified)
                .assigned(assignedUpcoming)
                .rejected(0L)
                .existingOpenClassCount(upcomingClasses.size())
                .openSeatRemaining(openSeatRemaining)
                .suggestedClassCount(suggested)
                .build();
    }

    /**
     * Nhu cầu mở lớp chỉ gồm hồ sơ còn phải xử lý hoặc đã xếp vào lớp chưa khai giảng.
     * Không tính đơn đã xếp vào lớp đang học / đã kết thúc, cũng không tính từ chối / hủy.
     */
    private boolean isRelevantForClassOpeningDemand(CourseRegistrationRequest request) {
        if (request == null || request.getStatus() == null) {
            return false;
        }
        return switch (request.getStatus()) {
            case REJECTED, CANCELLED -> false;
            case CLASS_ASSIGNED -> isAssignedToUpcomingClass(request);
            case SUBMITTED, INVITATION_SENT, TEST_SCHEDULED, AWAITING_PLACEMENT_TEST,
                    PLACEMENT_TEST_COMPLETED, UNDER_STAFF_REVIEW, WAITING_FOR_CLASS, CLASS_PROPOSED -> true;
        };
    }

    private boolean isAssignedToUpcomingClass(CourseRegistrationRequest request) {
        ClassSection assigned = request.getAssignedClassSection();
        if (assigned != null) {
            return assigned.getStatus() == ClassroomOfferingStatus.UPCOMING;
        }
        ClassSection preferred = request.getPreferredClassSection();
        return preferred != null && preferred.getStatus() == ClassroomOfferingStatus.UPCOMING;
    }

    private long countStatus(List<CourseRegistrationRequest> requests, EnrollmentRequestStatus status) {
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

    private InstructorLedCourse requirePublishedProgram(Long programId) {
        InstructorLedCourse program = instructorLedCourseRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học cần tư vấn."));
        if (program.getPublicationStatus() != PackageStatus.PUBLISHED) {
            throw new IllegalArgumentException("Khóa học này chưa mở nhận đăng ký tư vấn.");
        }
        return program;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private CourseRegistrationRequest requireRequest(Long id) {
        return enrollmentRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đăng ký."));
    }

    private CourseRegistrationRequest requireOwnedRequest(Long id, User staff) {
        CourseRegistrationRequest request = requireRequest(id);
        if (staff.hasRole(RoleCodes.ADMIN)) {
            return request;
        }
        if (request.getReviewedBy() == null
                || !request.getReviewedBy().getId().equals(staff.getId())) {
            throw new AccessDeniedException("Hồ sơ này do nhân viên khác phụ trách.");
        }
        return request;
    }

    private CourseRegistrationRequest requireOwnedRequestForUpdate(Long id, User staff) {
        CourseRegistrationRequest request = enrollmentRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đăng ký."));
        if (!staff.hasRole(RoleCodes.ADMIN)
                && (request.getReviewedBy() == null
                || !request.getReviewedBy().getId().equals(staff.getId()))) {
            throw new AccessDeniedException("Hồ sơ này do nhân viên khác phụ trách.");
        }
        return request;
    }

    private User nextEnrollmentOwner() {
        List<User> staffMembers = userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF);
        if (staffMembers.isEmpty()) {
            throw new IllegalStateException("Hiện chưa có nhân viên phụ trách đăng ký đang hoạt động.");
        }
        User lastOwner = enrollmentRequestRepository
                .findFirstByReviewedByInAndReviewedAtIsNotNullAndRequestSourceOrderByReviewedAtDescIdDesc(
                        staffMembers,
                        EnrollmentRequestSource.ONLINE
                )
                .map(CourseRegistrationRequest::getReviewedBy)
                .orElse(null);
        if (lastOwner == null) {
            return staffMembers.get(0);
        }
        for (int index = 0; index < staffMembers.size(); index++) {
            if (staffMembers.get(index).getId().equals(lastOwner.getId())) {
                return staffMembers.get((index + 1) % staffMembers.size());
            }
        }
        return staffMembers.get(0);
    }

    private void assertStaff(User user) {
        if (!TrainingRolePolicy.canPerformStaffAction(user)) {
            throw new IllegalArgumentException("Bạn không có quyền xử lý yêu cầu đăng ký.");
        }
    }

    private void assertStaffViewer(User user) {
        if (!TrainingRolePolicy.canPerformStaffAction(user)) {
            throw new IllegalArgumentException("Bạn không có quyền xem yêu cầu đăng ký.");
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
            case CLASS_PROPOSED -> "Chờ học viên xác nhận khóa đề xuất";
            case CLASS_ASSIGNED -> "Đã chọn lớp - chờ hoàn tất học phí";
            case REJECTED -> "Đã từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String statusLabel(
            EnrollmentRequestStatus status,
            ClassroomEnrollmentResponse assignedEnrollment
    ) {
        if (status != EnrollmentRequestStatus.CLASS_ASSIGNED || assignedEnrollment == null) {
            return statusLabel(status);
        }
        if (assignedEnrollment.isHasPendingTuitionProof()) {
            return "Đã nộp học phí - chờ xác nhận";
        }
        return switch (assignedEnrollment.getRegistrationStatus()) {
            case DEPOSIT_PAID -> "Đã đặt cọc học phí";
            case PARTIALLY_PAID -> "Đã thanh toán một phần học phí";
            case FULLY_PAID -> "Đã thanh toán học phí";
            case ASSIGNED -> "Đã thanh toán học phí - đã vào lớp";
            default -> statusLabel(status);
        };
    }

    private ClassSection requireAssignableClassroom(Long classroomId) {
        ClassSection target = classSectionRepository.findById(classroomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp cần xếp."));
        if (!isAssignableClassroom(target)) {
            throw new IllegalArgumentException(
                    "Chỉ có thể xếp vào lớp đã công bố, chưa bắt đầu và còn chỗ."
            );
        }
        return target;
    }

    private ClassSection requireAssignableClassroomForUpdate(Long classroomId) {
        ClassSection target = classSectionRepository.findByIdForUpdate(classroomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp cần xếp."));
        if (!isAssignableClassroom(target)) {
            throw new IllegalArgumentException(
                    "Chỉ có thể xếp vào lớp đã công bố, chưa bắt đầu và còn chỗ."
            );
        }
        return target;
    }

    private boolean isAssignableClassroom(ClassSection target) {
        boolean hasCapacity = target.getCapacity() == null
                || target.getCapacity() <= 0
                || classEnrollmentRepository.countByOfferingAndRegistrationStatuses(
                        target.getId(),
                        ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT
                ) < target.getCapacity();

        return hasCapacity
                && target.getInstructorLedCourse() != null
                && target.getInstructorLedCourse().getPublicationStatus() == PackageStatus.PUBLISHED
                && target.getStatus() == ClassroomOfferingStatus.UPCOMING
                && target.getStartDate() != null
                && !target.getStartDate().isBefore(LocalDate.now())
                && (target.getPlannedEndDate() == null || !target.getPlannedEndDate().isBefore(LocalDate.now()));
    }

    private boolean isReadyForClassAssignment(CourseRegistrationRequest request) {
        if (request.getLearner() == null) {
            return false;
        }
        if (request.getStatus() == EnrollmentRequestStatus.WAITING_FOR_CLASS) {
            return true;
        }
        if (request.getStatus() != EnrollmentRequestStatus.CLASS_ASSIGNED
                || request.getAssignedClassSection() == null) {
            return false;
        }
        return classEnrollmentRepository.findByStudentIdAndClassSectionId(
                        request.getLearner().getId(), request.getAssignedClassSection().getId())
                .map(enrollment -> enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.REJECTED)
                .orElse(false);
    }

    private List<Long> findUnavailableCourseIds(User learner) {
        Set<Long> courseIds = enrollmentRequestRepository.findByLearnerOrderByCreatedAtDesc(learner).stream()
                .filter(request -> !TERMINAL_STATUSES.contains(request.getStatus()))
                .map(CourseRegistrationRequest::getCourseOffering)
                .filter(java.util.Objects::nonNull)
                .map(InstructorLedCourse::getId)
                .collect(Collectors.toSet());
        classEnrollmentRepository.findByStudentIdAndRegistrationStatusIn(
                        learner.getId(),
                        ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS
                ).stream()
                .map(enrollment -> enrollment.getClassSection().getInstructorLedCourse())
                .filter(java.util.Objects::nonNull)
                .map(InstructorLedCourse::getId)
                .forEach(courseIds::add);
        return courseIds.stream().sorted().toList();
    }

    private List<Long> findUnavailableClassroomIds(User learner) {
        return classEnrollmentRepository.findByStudentIdAndRegistrationStatusIn(
                        learner.getId(),
                        ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS
                ).stream()
                .map(enrollment -> enrollment.getClassSection().getId())
                .distinct()
                .sorted()
                .toList();
    }

    private boolean hasActiveRequestOrEnrollmentForCourse(User learner, Long courseId) {
        return findUnavailableCourseIds(learner).contains(courseId);
    }

    private boolean isAvailableForLearner(ClassSection offering, Long learnerId) {
        var classResult = classroomConflictService.check(ConflictCheckRequest.builder()
                .classSectionId(offering.getId())
                .learnerIds(List.of(learnerId))
                .checkCapacity(true)
                .build());
        if (classResult.isHasBlockingConflict()) {
            return false;
        }

        return classScheduleRepository
                .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId())
                .stream()
                .filter(session -> ACTIVE_SESSION_STATUSES.contains(session.getStatus()))
                .allMatch(session -> !classroomConflictService.check(ConflictCheckRequest.builder()
                        .sessionDate(session.getSessionDate())
                        .startTime(session.getStartTime())
                        .endTime(session.getEndTime())
                        .learnerIds(List.of(learnerId))
                        .checkCapacity(false)
                        .build()).isHasBlockingConflict());
    }

    private boolean matchesRequestedCourse(
            ClassSection classroom,
            CourseRegistrationRequest request
    ) {
        return classroom.getInstructorLedCourse() != null
                && request.getCourseOffering() != null
                && classroom.getInstructorLedCourse().getId().equals(request.getCourseOffering().getId());
    }

}
