package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.dto.request.classroom.TransferStudentRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.dto.request.classroom.CreateChangeRequestRequest;
import fu.sep490.g23.backend.dto.request.classroom.ReviewChangeRequestRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCourseReturnRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCourseSuspensionRequest;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomScheduleLockService;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.CourseSuspensionEligibilityResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.notification.ClassroomNotificationService;
import fu.sep490.g23.backend.service.classroom.ClassroomChangeRequestService;
import fu.sep490.g23.backend.service.classroom.ClassroomConflictService;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomChangeRequestServiceImpl implements ClassroomChangeRequestService {

    private static final Collection<ClassroomChangeRequestType> SUSPENSION_REQUEST_TYPES = EnumSet.of(
            ClassroomChangeRequestType.SUSPEND_STUDENT,
            ClassroomChangeRequestType.RESUME_STUDENT
    );

    private final ClassroomChangeRequestRepository changeRequestRepository;
    private final ClassSectionRepository offeringRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassScheduleRepository sessionRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomConflictService conflictService;
    private final ClassroomScheduleLockService scheduleLockService;
    private final ClassroomOfferingService offeringService;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomNotificationService notificationService;
    private final HomeworkAttachmentStorageService attachmentStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public ConflictCheckResultResponse checkConflict(CreateChangeRequestRequest request, String requesterEmail) {
        User requester = accessHelper.requireUser(requesterEmail);
        accessHelper.assertTeacher(requester);

        ClassSection offering = offeringRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        ClassSchedule targetClassSchedule = resolveTargetSession(request);
        ConflictCheckRequest conflictRequest = buildConflictRequest(request, offering, targetClassSchedule);
        return conflictService.check(conflictRequest);
    }

    @Override
    public ClassroomChangeRequestResponse create(CreateChangeRequestRequest request, String requesterEmail) {
        User requester = accessHelper.requireUser(requesterEmail);
        accessHelper.assertTeacher(requester);

        ClassSection offering = offeringRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        ClassSchedule targetClassSchedule = resolveTargetSession(request);
        validateChangeRequest(request, targetClassSchedule);

        if (request.getTargetSessionId() != null) {
            changeRequestRepository.findByTargetClassScheduleIdAndRequestTypeAndStatus(
                    request.getTargetSessionId(),
                    request.getRequestType(),
                    ClassroomChangeRequestStatus.PENDING
            ).ifPresent(existing -> {
                throw new RuntimeException("Đã có yêu cầu cùng loại đang chờ duyệt cho buổi học này.");
            });
        }

        String oldValuesJson = buildOldValuesJson(request.getRequestType(), offering, targetClassSchedule);
        ConflictCheckRequest conflictRequest = buildConflictRequest(request, offering, targetClassSchedule);
        // Block submission when there is a real schedule conflict (teacher/room/learner).
        // Nhân viên đào tạo vẫn có thể xử lý xung đột theo quyền vận hành khi duyệt.
        conflictService.assertNoBlockingConflict(conflictRequest);

        ClassroomChangeRequest changeRequest = ClassroomChangeRequest.builder()
                .requestType(request.getRequestType())
                .requester(requester)
                .requesterRole(requester.getPrimaryRoleCode())
                .classSection(offering)
                .targetClassSchedule(targetClassSchedule)
                .oldValuesJson(oldValuesJson)
                .newValuesJson(request.getNewValuesJson())
                .reason(request.getReason())
                .status(ClassroomChangeRequestStatus.PENDING)
                .reviewer(nextRequestOwner())
                .build();

        changeRequest = changeRequestRepository.save(changeRequest);
        notificationService.notifyUser(
                changeRequest.getReviewer(),
                "CLASSROOM_CHANGE_REQUEST_PENDING",
                "Yêu cầu thay đổi lớp học",
                requester.getFullName() + " gửi yêu cầu " + mapper.changeRequestTypeLabel(request.getRequestType()) + ".",
                Map.of("requestId", changeRequest.getId(), "classroomId", offering.getId())
        );
        notificationService.notifyUser(
                requester,
                "CLASSROOM_CHANGE_REQUEST_CREATED",
                "Yêu cầu thay đổi đã được gửi",
                "Yêu cầu " + mapper.changeRequestTypeLabel(request.getRequestType()) + " đang chờ Nhân viên đào tạo duyệt.",
                Map.of("requestId", changeRequest.getId(), "classroomId", offering.getId())
        );
        return mapper.toChangeRequestResponse(changeRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomChangeRequestResponse> listMine(String requesterEmail) {
        User requester = accessHelper.requireUser(requesterEmail);
        return changeRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId()).stream()
                .map(mapper::toChangeRequestResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassroomChangeRequestResponse> pageMine(
            String requesterEmail,
            String statusGroup,
            String keyword,
            Pageable pageable
    ) {
        User requester = accessHelper.requireUser(requesterEmail);
        Specification<ClassroomChangeRequest> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("requester").get("id"), requester.getId());
        String normalizedStatus = statusGroup == null ? "" : statusGroup.trim().toUpperCase(java.util.Locale.ROOT);
        if ("PENDING".equals(normalizedStatus) || "REJECTED".equals(normalizedStatus)) {
            ClassroomChangeRequestStatus status = ClassroomChangeRequestStatus.valueOf(normalizedStatus);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        } else if ("APPROVED".equals(normalizedStatus)) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    root.get("status").in(ClassroomChangeRequestStatus.APPROVED, ClassroomChangeRequestStatus.APPLIED));
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalizedKeyword.isBlank()) {
            String pattern = "%" + normalizedKeyword + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("classSection").get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("reason")), pattern)
            ));
        }
        return changeRequestRepository.findAll(specification, pageable)
                .map(mapper::toChangeRequestResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getMyStats(String requesterEmail) {
        User requester = accessHelper.requireUser(requesterEmail);
        Long requesterId = requester.getId();
        return Map.of(
                "total", changeRequestRepository.countByRequesterId(requesterId),
                "pending", changeRequestRepository.countByRequesterIdAndStatus(requesterId, ClassroomChangeRequestStatus.PENDING),
                "approved", changeRequestRepository.countByRequesterIdAndStatusIn(
                        requesterId, List.of(ClassroomChangeRequestStatus.APPROVED, ClassroomChangeRequestStatus.APPLIED)),
                "rejected", changeRequestRepository.countByRequesterIdAndStatus(requesterId, ClassroomChangeRequestStatus.REJECTED)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseSuspensionEligibilityResponse> listSuspensionEligibility(String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        return enrollmentRepository.findByStudentIdAndRegistrationStatusIn(
                        learner.getId(),
                        List.of(ClassroomRegistrationStatus.ASSIGNED)
                ).stream()
                .map(enrollment -> suspensionEligibility(enrollment, learner, true))
                .toList();
    }

    @Override
    public ClassroomChangeRequestResponse createSuspensionRequest(
            CreateCourseSuspensionRequest request,
            String learnerEmail
    ) {
        User authenticatedLearner = accessHelper.requireUser(learnerEmail);
        User learner = userRepository.findByIdForUpdate(authenticatedLearner.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        ClassEnrollment enrollment = enrollmentRepository.findByIdForUpdate(request.getEnrollmentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học cần bảo lưu."));
        assertEnrollmentOwner(enrollment, learner);

        CourseSuspensionEligibilityResponse eligibility = suspensionEligibility(enrollment, learner, true);
        if (!eligibility.isEligible()) {
            throw new RuntimeException(eligibility.getEligibilityMessage());
        }
        validateSuspensionDates(request.getRequestedStartDate(), request.getRequestedReturnDate(), true);
        String proofUrl = normalizeSuspensionProofUrl(request.getProofUrl());
        if (changeRequestRepository.existsByRequesterIdAndClassSectionIdAndRequestTypeAndStatus(
                learner.getId(),
                enrollment.getClassSection().getId(),
                ClassroomChangeRequestType.SUSPEND_STUDENT,
                ClassroomChangeRequestStatus.PENDING
        )) {
            throw new RuntimeException("Lớp học này đã có yêu cầu bảo lưu đang chờ xử lý.");
        }

        ClassSection classroom = enrollment.getClassSection();
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("enrollmentId", enrollment.getId());
        oldValues.put("studentId", learner.getId());
        oldValues.put("studentName", learner.getFullName());
        oldValues.put("courseId", classroom.getInstructorLedCourse().getId());
        oldValues.put("courseTitle", classroom.getInstructorLedCourse().getTitle());
        oldValues.put("registrationStatus", ClassroomRegistrationStatus.ASSIGNED.name());
        oldValues.put("tuitionAmountDue", enrollment.getTuitionAmountDue());
        oldValues.put("tuitionAmountPaid", enrollment.getTuitionAmountPaid());
        oldValues.put("completedSessions", eligibility.getCompletedSessions());
        oldValues.put("totalSessions", eligibility.getTotalSessions());
        oldValues.put("progressPercent", eligibility.getProgressPercent());

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("enrollmentId", enrollment.getId());
        newValues.put("registrationStatus", ClassroomRegistrationStatus.SUSPENDED.name());
        newValues.put("requestedStartDate", request.getRequestedStartDate().toString());
        newValues.put("requestedReturnDate", request.getRequestedReturnDate().toString());
        newValues.put("proofUrl", proofUrl);

        ClassroomChangeRequest changeRequest = ClassroomChangeRequest.builder()
                .requestType(ClassroomChangeRequestType.SUSPEND_STUDENT)
                .requester(learner)
                .requesterRole(learner.getPrimaryRoleCode())
                .classSection(classroom)
                .oldValuesJson(writeJson(oldValues))
                .newValuesJson(writeJson(newValues))
                .reason(request.getReason().trim())
                .status(ClassroomChangeRequestStatus.PENDING)
                .reviewer(nextRequestOwner())
                .build();
        changeRequest = changeRequestRepository.save(changeRequest);
        notifyRequestCreated(changeRequest, "Học viên gửi yêu cầu bảo lưu khóa học.");
        return mapper.toChangeRequestResponse(changeRequest);
    }

    @Override
    public ClassroomChangeRequestResponse createReturnRequest(
            CreateCourseReturnRequest request,
            String learnerEmail
    ) {
        User authenticatedLearner = accessHelper.requireUser(learnerEmail);
        User learner = userRepository.findByIdForUpdate(authenticatedLearner.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        ClassroomChangeRequest suspension = changeRequestRepository.findById(request.getSuspensionRequestId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu bảo lưu."));
        if (!suspension.getRequester().getId().equals(learner.getId())
                || suspension.getRequestType() != ClassroomChangeRequestType.SUSPEND_STUDENT
                || suspension.getStatus() != ClassroomChangeRequestStatus.APPLIED) {
            throw new AccessDeniedException("Yêu cầu bảo lưu không hợp lệ.");
        }

        Map<String, Object> suspensionValues = parseJsonMap(suspension.getNewValuesJson());
        LocalDate returnDeadline = LocalDate.parse(String.valueOf(suspensionValues.get("requestedReturnDate")));
        if (LocalDate.now().isAfter(returnDeadline)) {
            throw new RuntimeException("Đã quá thời hạn đăng ký học lại của yêu cầu bảo lưu này.");
        }
        Long enrollmentId = longValue(suspensionValues, "enrollmentId", "Thiếu hồ sơ lớp học đã bảo lưu.");
        ClassEnrollment enrollment = enrollmentRepository.findByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ lớp học đã bảo lưu."));
        assertEnrollmentOwner(enrollment, learner);
        if (enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.SUSPENDED) {
            throw new RuntimeException("Hồ sơ này không còn ở trạng thái bảo lưu.");
        }
        if (changeRequestRepository.existsByRequesterIdAndClassSectionIdAndRequestTypeAndStatus(
                learner.getId(),
                enrollment.getClassSection().getId(),
                ClassroomChangeRequestType.RESUME_STUDENT,
                ClassroomChangeRequestStatus.PENDING
        )) {
            throw new RuntimeException("Yêu cầu xếp lớp học lại đang được xử lý.");
        }

        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("enrollmentId", enrollment.getId());
        oldValues.put("studentId", learner.getId());
        oldValues.put("studentName", learner.getFullName());
        oldValues.put("courseId", enrollment.getClassSection().getInstructorLedCourse().getId());
        oldValues.put("courseTitle", enrollment.getClassSection().getInstructorLedCourse().getTitle());
        oldValues.put("registrationStatus", ClassroomRegistrationStatus.SUSPENDED.name());
        oldValues.put("suspensionRequestId", suspension.getId());
        oldValues.put("returnDeadline", returnDeadline.toString());

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("enrollmentId", enrollment.getId());
        newValues.put("registrationStatus", ClassroomRegistrationStatus.ASSIGNED.name());

        ClassroomChangeRequest changeRequest = ClassroomChangeRequest.builder()
                .requestType(ClassroomChangeRequestType.RESUME_STUDENT)
                .requester(learner)
                .requesterRole(learner.getPrimaryRoleCode())
                .classSection(enrollment.getClassSection())
                .oldValuesJson(writeJson(oldValues))
                .newValuesJson(writeJson(newValues))
                .reason("Đề nghị xếp lớp để tiếp tục khóa học đã bảo lưu.")
                .status(ClassroomChangeRequestStatus.PENDING)
                .reviewer(nextRequestOwner())
                .build();
        changeRequest = changeRequestRepository.save(changeRequest);
        notifyRequestCreated(changeRequest, "Học viên đã sẵn sàng quay lại học.");
        return mapper.toChangeRequestResponse(changeRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomChangeRequestResponse> listMySuspensionRequests(String learnerEmail) {
        User learner = accessHelper.requireUser(learnerEmail);
        return changeRequestRepository.findByRequesterIdAndRequestTypeInOrderByCreatedAtDesc(
                        learner.getId(), SUSPENSION_REQUEST_TYPES
                ).stream()
                .map(mapper::toChangeRequestResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomOfferingResponse> listReturnOptions(Long requestId, String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        accessHelper.assertStaffOperator(reviewer);
        ClassroomChangeRequest changeRequest = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu học lại."));
        assertAssignedReviewer(changeRequest, reviewer);
        if (changeRequest.getRequestType() != ClassroomChangeRequestType.RESUME_STUDENT
                || changeRequest.getStatus() != ClassroomChangeRequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu học lại không còn hiệu lực.");
        }

        Map<String, Object> oldValues = parseJsonMap(changeRequest.getOldValuesJson());
        Long enrollmentId = longValue(oldValues, "enrollmentId", "Thiếu hồ sơ bảo lưu.");
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ bảo lưu."));
        Long courseId = enrollment.getClassSection().getInstructorLedCourse().getId();
        LocalDate today = LocalDate.now();

        return offeringRepository.findByStatusIn(List.of(ClassroomOfferingStatus.UPCOMING, ClassroomOfferingStatus.ACTIVE))
                .stream()
                .filter(classroom -> classroom.getInstructorLedCourse().getId().equals(courseId))
                .filter(classroom -> hasFutureSession(classroom, today))
                .filter(classroom -> canReceiveReturningLearner(classroom, enrollment))
                .map(classroom -> mapper.toOfferingResponse(classroom, false, null, null, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomChangeRequestResponse> listPending(String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        accessHelper.assertStaffOperator(reviewer);
        List<ClassroomChangeRequest> requests = canReviewEveryRequest(reviewer)
                ? changeRequestRepository.findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus.PENDING)
                : changeRequestRepository.findByReviewerAndStatusOrderByCreatedAtDesc(
                        reviewer,
                        ClassroomChangeRequestStatus.PENDING
                );
        return requests.stream()
                .map(mapper::toChangeRequestResponse)
                .toList();
    }

    @Override
    public ClassroomChangeRequestResponse approve(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        accessHelper.assertStaffOperator(reviewer);

        ClassroomChangeRequest changeRequest = findPendingRequest(requestId);
        assertAssignedReviewer(changeRequest, reviewer);
        boolean overrideConflict = request != null && Boolean.TRUE.equals(request.getOverrideConflict());

        if (changeRequest.getRequestType() == ClassroomChangeRequestType.SUSPEND_STUDENT) {
            applyCourseSuspension(changeRequest);
        } else if (changeRequest.getRequestType() == ClassroomChangeRequestType.RESUME_STUDENT) {
            if (request == null || request.getTargetClassSectionId() == null) {
                throw new RuntimeException("Vui lòng chọn lớp học phù hợp để xếp lại học viên.");
            }
            applyCourseReturn(changeRequest, request.getTargetClassSectionId(), reviewer);
        } else {
            ConflictCheckRequest conflictRequest = buildConflictRequestFromEntity(changeRequest);
            configureSessionLockCheck(conflictRequest, changeRequest);
            scheduleLockService.lockDates(java.util.Arrays.asList(
                    conflictRequest.getSessionDate(),
                    changeRequest.getTargetClassSchedule() == null
                            ? conflictRequest.getSessionDate()
                            : changeRequest.getTargetClassSchedule().getSessionDate()
            ));
            if (!overrideConflict) {
                conflictService.assertNoBlockingConflict(conflictRequest);
            } else if (request.getReviewNote() == null || request.getReviewNote().isBlank()) {
                throw new RuntimeException("Cần ghi chú khi ghi đè xung đột lịch học.");
            }
            applyChangeRequest(changeRequest, overrideConflict);
        }
        changeRequest.setStatus(ClassroomChangeRequestStatus.APPLIED);
        changeRequest.setReviewer(reviewer);
        changeRequest.setReviewedAt(LocalDateTime.now());
        changeRequest.setReviewNote(request == null ? null : request.getReviewNote());
        changeRequest = changeRequestRepository.save(changeRequest);

        notificationService.notifyUser(
                changeRequest.getRequester(),
                "CLASSROOM_CHANGE_REQUEST_APPROVED",
                "Yêu cầu thay đổi đã được duyệt",
                "Yêu cầu " + mapper.changeRequestTypeLabel(changeRequest.getRequestType()) + " đã được áp dụng.",
                Map.of("requestId", changeRequest.getId())
        );
        return mapper.toChangeRequestResponse(changeRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ConflictCheckResultResponse checkPendingConflict(Long requestId, String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        accessHelper.assertStaffOperator(reviewer);
        ClassroomChangeRequest changeRequest = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu thay đổi."));
        if (changeRequest.getStatus() != ClassroomChangeRequestStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể kiểm tra trùng lịch cho yêu cầu đang chờ duyệt.");
        }
        assertAssignedReviewer(changeRequest, reviewer);
        if (SUSPENSION_REQUEST_TYPES.contains(changeRequest.getRequestType())) {
            return ConflictCheckResultResponse.builder().build();
        }
        ConflictCheckRequest conflictRequest = buildConflictRequestFromEntity(changeRequest);
        configureSessionLockCheck(conflictRequest, changeRequest);
        return conflictService.check(conflictRequest);
    }

    @Override
    public ClassroomChangeRequestResponse reject(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        accessHelper.assertStaffOperator(reviewer);

        ClassroomChangeRequest changeRequest = findPendingRequest(requestId);
        assertAssignedReviewer(changeRequest, reviewer);
        changeRequest.setStatus(ClassroomChangeRequestStatus.REJECTED);
        changeRequest.setReviewer(reviewer);
        changeRequest.setReviewedAt(LocalDateTime.now());
        String reviewNote = request == null ? null : request.getReviewNote();
        if (SUSPENSION_REQUEST_TYPES.contains(changeRequest.getRequestType())
                && (reviewNote == null || reviewNote.isBlank())) {
            throw new RuntimeException("Vui lòng nhập lý do từ chối để học viên có thể bổ sung hồ sơ.");
        }
        changeRequest.setReviewNote(reviewNote);
        changeRequest = changeRequestRepository.save(changeRequest);

        notificationService.notifyUser(
                changeRequest.getRequester(),
                "CLASSROOM_CHANGE_REQUEST_REJECTED",
                "Yêu cầu thay đổi bị từ chối",
                reviewNote == null || reviewNote.isBlank()
                        ? "Yêu cầu của bạn đã bị từ chối."
                        : reviewNote,
                Map.of("requestId", changeRequest.getId())
        );
        return mapper.toChangeRequestResponse(changeRequest);
    }

    private ClassroomChangeRequest findPendingRequest(Long requestId) {
        ClassroomChangeRequest changeRequest = changeRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu thay đổi."));
        if (changeRequest.getStatus() != ClassroomChangeRequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu không còn ở trạng thái chờ duyệt.");
        }
        return changeRequest;
    }

    private User nextRequestOwner() {
        List<User> staffMembers = userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF);
        if (staffMembers.isEmpty()) {
            throw new IllegalStateException("Hiện chưa có nhân viên phụ trách yêu cầu đang hoạt động.");
        }
        User lastOwner = changeRequestRepository
                .findFirstByReviewerInOrderByCreatedAtDescIdDesc(staffMembers)
                .map(ClassroomChangeRequest::getReviewer)
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

    private void assertAssignedReviewer(ClassroomChangeRequest request, User reviewer) {
        if (canReviewEveryRequest(reviewer)) {
            return;
        }
        if (request.getReviewer() == null || !request.getReviewer().getId().equals(reviewer.getId())) {
            throw new AccessDeniedException("Yêu cầu này do nhân viên khác phụ trách.");
        }
    }

    private boolean canReviewEveryRequest(User reviewer) {
        return reviewer.hasRole(RoleCodes.MANAGER) || reviewer.hasRole(RoleCodes.ADMIN);
    }

    /**
     * Makeup uses a source session only as context; completed/locked sources must not block approval.
     * Other change types still enforce session-lock conflict checks.
     */
    private void configureSessionLockCheck(
            ConflictCheckRequest conflictRequest,
            ClassroomChangeRequest changeRequest
    ) {
        if (changeRequest.getRequestType() != ClassroomChangeRequestType.CREATE_MAKEUP_SESSION) {
            conflictRequest.setCheckSessionLocked(true);
        }
    }

    private ClassSchedule resolveTargetSession(CreateChangeRequestRequest request) {
        if (request.getTargetSessionId() == null) {
            return null;
        }
        return sessionRepository.findById(request.getTargetSessionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học mục tiêu."));
    }

    private String buildOldValuesJson(ClassroomChangeRequestType type, ClassSection offering, ClassSchedule session) {
        try {
            Map<String, Object> values = switch (type) {
                case RESCHEDULE_SESSION, CANCEL_SESSION, CHANGE_ROOM, CHANGE_TEACHER, RECREATE_GOOGLE_MEET -> sessionValues(session);
                case TRANSFER_STUDENT, TRANSFER_CLASS -> Map.of("classSectionId", offering.getId());
                case CREATE_MAKEUP_SESSION -> sessionValues(session);
                case SUSPEND_STUDENT, RESUME_STUDENT -> Map.of("classSectionId", offering.getId());
            };
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể lưu lịch hiện tại của buổi học vào yêu cầu thay đổi.", ex);
        }
    }

    private Map<String, Object> sessionValues(ClassSchedule session) {
        if (session == null) {
            return Map.of();
        }
        // LinkedHashMap cho phép value = null; Map.of() thì không
        // (roomId / teacherId / larkMeetingUrl thường null với lớp online hoặc chưa gán phòng).
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sessionDate", session.getSessionDate() == null ? null : session.getSessionDate().toString());
        values.put("startTime", session.getStartTime() == null ? null : session.getStartTime().toString());
        values.put("endTime", session.getEndTime() == null ? null : session.getEndTime().toString());
        values.put("teacherId", session.getTeacher() == null ? null : session.getTeacher().getId());
        values.put("roomId", session.getRoom() == null ? null : session.getRoom().getId());
        values.put("status", session.getStatus() == null ? null : session.getStatus().name());
        return values;
    }

    private ConflictCheckRequest buildConflictRequest(CreateChangeRequestRequest request, ClassSection offering, ClassSchedule session) {
        boolean makeup = request.getRequestType() == ClassroomChangeRequestType.CREATE_MAKEUP_SESSION;
        ConflictCheckRequest.ConflictCheckRequestBuilder builder = ConflictCheckRequest.builder()
                .classSectionId(offering.getId())
                .requestType(request.getRequestType())
                // A completed or cancelled source session is valid context for a makeup request.
                // Only the proposed makeup schedule should participate in conflict detection.
                .checkSessionLocked(!makeup);

        if (session != null) {
            builder.sessionId(session.getId())
                    .excludeSessionId(session.getId());
            // Makeup chỉ lấy buổi gốc làm ngữ cảnh; lịch đề xuất đến từ newValues (+ mặc định lớp).
            if (!makeup) {
                builder.teacherId(session.getTeacher() == null ? null : session.getTeacher().getId())
                        .roomId(session.getRoom() == null ? null : session.getRoom().getId())
                        .sessionDate(session.getSessionDate())
                        .startTime(session.getStartTime())
                        .endTime(session.getEndTime());
            }
        }

        Map<String, Object> newValues = parseJsonMap(request.getNewValuesJson());
        if (newValues.containsKey("sessionDate")) {
            builder.sessionDate(LocalDate.parse(String.valueOf(newValues.get("sessionDate"))));
        }
        if (newValues.containsKey("startTime")) {
            builder.startTime(LocalTime.parse(String.valueOf(newValues.get("startTime"))));
        }
        if (newValues.containsKey("endTime")) {
            builder.endTime(LocalTime.parse(String.valueOf(newValues.get("endTime"))));
        }
        if (newValues.containsKey("teacherId") && newValues.get("teacherId") != null) {
            builder.teacherId(Long.valueOf(String.valueOf(newValues.get("teacherId"))));
        } else if (makeup && offering.getPrimaryTeacher() != null) {
            builder.teacherId(offering.getPrimaryTeacher().getId());
        }
        if (newValues.containsKey("roomId") && newValues.get("roomId") != null) {
            builder.roomId(Long.valueOf(String.valueOf(newValues.get("roomId"))));
        } else if (makeup && offering.getRoom() != null) {
            // Khớp createSession: roomId null → dùng phòng mặc định của lớp.
            builder.roomId(offering.getRoom().getId());
        }
        if (newValues.containsKey("targetClassSectionId") && newValues.get("targetClassSectionId") != null) {
            builder.targetClassSectionId(Long.valueOf(String.valueOf(newValues.get("targetClassSectionId"))));
        }
        if (newValues.containsKey("studentId") && newValues.get("studentId") != null) {
            builder.learnerIds(List.of(Long.valueOf(String.valueOf(newValues.get("studentId")))));
        }
        if (newValues.containsKey("larkMeetingUrl")) {
        }

        return builder.build();
    }

    private ConflictCheckRequest buildConflictRequestFromEntity(ClassroomChangeRequest changeRequest) {
        CreateChangeRequestRequest request = CreateChangeRequestRequest.builder()
                .requestType(changeRequest.getRequestType())
                .classSectionId(changeRequest.getClassSection().getId())
                .targetSessionId(changeRequest.getTargetClassSchedule() == null ? null : changeRequest.getTargetClassSchedule().getId())
                .newValuesJson(changeRequest.getNewValuesJson())
                .reason(changeRequest.getReason())
                .build();
        return buildConflictRequest(request, changeRequest.getClassSection(), changeRequest.getTargetClassSchedule());
    }

    private void applyChangeRequest(ClassroomChangeRequest changeRequest) {
        applyChangeRequest(changeRequest, false);
    }

    private void applyChangeRequest(ClassroomChangeRequest changeRequest, boolean overrideConflict) {
        Map<String, Object> newValues = parseJsonMap(changeRequest.getNewValuesJson());
        ClassSchedule session = changeRequest.getTargetClassSchedule();

        switch (changeRequest.getRequestType()) {
            case RESCHEDULE_SESSION -> {
                if (session == null) {
                    throw new RuntimeException("Thiếu buổi học mục tiêu.");
                }
                CreateClassroomSessionRequest sessionRequest = CreateClassroomSessionRequest.builder()
                        .sessionDate(LocalDate.parse(String.valueOf(newValues.get("sessionDate"))))
                        .startTime(LocalTime.parse(String.valueOf(newValues.get("startTime"))))
                        .endTime(LocalTime.parse(String.valueOf(newValues.get("endTime"))))
                        .teacherId(newValues.get("teacherId") == null ? null : Long.valueOf(String.valueOf(newValues.get("teacherId"))))
                        .roomId(newValues.get("roomId") == null ? null : Long.valueOf(String.valueOf(newValues.get("roomId"))))
                        .build();
                offeringService.applyApprovedSessionScheduleChange(session.getId(), sessionRequest);
            }
            case CHANGE_ROOM -> {
                if (session == null) {
                    throw new RuntimeException("Thiếu buổi học mục tiêu.");
                }
                Long roomId = Long.valueOf(String.valueOf(newValues.get("roomId")));
                session.setRoom(roomRepository.findById(roomId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học.")));
                sessionRepository.save(session);
            }
            case CHANGE_TEACHER -> {
                if (session == null) {
                    throw new RuntimeException("Thiếu buổi học mục tiêu.");
                }
                Long teacherId = Long.valueOf(String.valueOf(newValues.get("teacherId")));
                User teacher = userRepository.findById(teacherId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên."));
                session.setTeacher(teacher);
                sessionRepository.save(session);
            }
            case CANCEL_SESSION -> {
                if (session == null) {
                    throw new RuntimeException("Thiếu buổi học mục tiêu.");
                }
                session.setStatus(ClassroomSessionStatus.CANCELLED);
                sessionRepository.save(session);
            }
            case CREATE_MAKEUP_SESSION -> {
                CreateClassroomSessionRequest sessionRequest = CreateClassroomSessionRequest.builder()
                        .sessionDate(LocalDate.parse(String.valueOf(newValues.get("sessionDate"))))
                        .startTime(LocalTime.parse(String.valueOf(newValues.get("startTime"))))
                        .endTime(LocalTime.parse(String.valueOf(newValues.get("endTime"))))
                        .teacherId(newValues.get("teacherId") == null ? null : Long.valueOf(String.valueOf(newValues.get("teacherId"))))
                        .roomId(newValues.get("roomId") == null ? null : Long.valueOf(String.valueOf(newValues.get("roomId"))))
                        .status(ClassroomSessionStatus.SCHEDULED)
                        .build();
                offeringService.createSession(
                        changeRequest.getClassSection().getId(),
                        sessionRequest,
                        !overrideConflict
                );
            }
            case TRANSFER_STUDENT -> offeringService.transferStudent(
                    changeRequest.getClassSection().getId(),
                    TransferStudentRequest.builder()
                            .studentId(Long.valueOf(String.valueOf(newValues.get("studentId"))))
                            .targetClassSectionId(Long.valueOf(String.valueOf(newValues.get("targetClassSectionId"))))
                            .note(changeRequest.getReason())
                            .build()
            );
            case RECREATE_GOOGLE_MEET -> {
                if (session == null) {
                    throw new RuntimeException("Thiếu buổi học mục tiêu.");
                }
                offeringService.syncVirtualSessionMeeting(session.getId(), changeRequest.getRequester() != null ? changeRequest.getRequester().getEmail() : "system@englishlab.edu.vn");
            }
            case TRANSFER_CLASS -> {
                ClassSection sourceOffering = changeRequest.getClassSection();
                Long targetOfferingId = Long.valueOf(String.valueOf(newValues.get("targetClassSectionId")));
                List<ClassEnrollment> activeEnrollments = enrollmentRepository
                        .findByClassSectionIdAndRegistrationStatusIn(
                                sourceOffering.getId(),
                                ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT
                        );
                if (activeEnrollments.isEmpty()) {
                    throw new RuntimeException("Lớp nguồn không có học viên để chuyển.");
                }
                for (ClassEnrollment enrollment : activeEnrollments) {
                    offeringService.transferStudent(
                            sourceOffering.getId(),
                            TransferStudentRequest.builder()
                                    .studentId(enrollment.getStudent().getId())
                                    .targetClassSectionId(targetOfferingId)
                                    .note(changeRequest.getReason())
                                    .build()
                    );
                }
            }
            case SUSPEND_STUDENT, RESUME_STUDENT ->
                    throw new RuntimeException("Yêu cầu bảo lưu phải được xử lý qua luồng dành cho học viên.");
            default -> throw new RuntimeException("Loại yêu cầu không được hỗ trợ.");
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new RuntimeException("Dữ liệu newValuesJson không hợp lệ.");
        }
    }

    private void validateChangeRequest(CreateChangeRequestRequest request, ClassSchedule session) {
        if (request.getRequestType() == ClassroomChangeRequestType.CANCEL_SESSION) {
            throw new RuntimeException("Loại yêu cầu hủy buổi học không còn được hỗ trợ.");
        }

        Map<String, Object> newValues = parseJsonMap(request.getNewValuesJson());
        switch (request.getRequestType()) {
            case RESCHEDULE_SESSION -> {
                if (session == null) {
                    throw new RuntimeException("Vui lòng chọn buổi học cần đổi lịch.");
                }
                requireNewValue(newValues, "sessionDate", "Vui lòng chọn ngày học mới.");
                requireNewValue(newValues, "startTime", "Vui lòng chọn khung giờ mới.");
                requireNewValue(newValues, "endTime", "Vui lòng chọn khung giờ mới.");
            }
            case CHANGE_ROOM -> {
                if (session == null) {
                    throw new RuntimeException("Vui lòng chọn buổi học cần đổi phòng.");
                }
                requireNewValue(newValues, "roomId", "Vui lòng chọn phòng học mới.");
            }
            case CHANGE_TEACHER -> {
                if (session == null) {
                    throw new RuntimeException("Vui lòng chọn buổi học cần đổi giáo viên.");
                }
                requireNewValue(newValues, "teacherId", "Vui lòng chọn giáo viên thay thế.");
            }
            case CREATE_MAKEUP_SESSION -> {
                if (session == null) {
                    throw new RuntimeException("Vui lòng chọn buổi học cần học bù.");
                }
                requireNewValue(newValues, "sessionDate", "Vui lòng chọn ngày học bù.");
                requireNewValue(newValues, "startTime", "Vui lòng chọn khung giờ học bù.");
                requireNewValue(newValues, "endTime", "Vui lòng chọn khung giờ học bù.");
            }
            case SUSPEND_STUDENT, RESUME_STUDENT ->
                    throw new RuntimeException("Học viên phải gửi yêu cầu này từ Trung tâm hỗ trợ.");
            default -> {
            }
        }
    }

    private CourseSuspensionEligibilityResponse suspensionEligibility(
            ClassEnrollment enrollment,
            User learner,
            boolean checkPendingRequest
    ) {
        List<ClassSchedule> sessions = sessionRepository
                .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(enrollment.getClassSection().getId()).stream()
                .filter(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED)
                .toList();
        LocalDateTime now = LocalDateTime.now();
        long completedSessions = sessions.stream()
                .filter(session -> !session.getEndDateTime().isAfter(now))
                .count();
        long totalSessions = sessions.size();
        int progressPercent = totalSessions == 0
                ? 0
                : (int) Math.round(completedSessions * 100.0 / totalSessions);

        String message = null;
        if (enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.ASSIGNED) {
            message = "Chỉ lớp đang học mới có thể gửi yêu cầu bảo lưu.";
        } else if (!isTuitionFullyPaid(enrollment)) {
            message = "Khóa học cần được thanh toán đủ trước khi bảo lưu.";
        } else if (totalSessions == 0) {
            message = "Lớp học chưa có lịch học để xác định điều kiện bảo lưu.";
        } else if (completedSessions * 2 > totalSessions) {
            message = "Khóa học đã vượt quá 50% số buổi.";
        } else if (hasUsedSuspension(learner.getId(), enrollment.getClassSection().getInstructorLedCourse().getId())) {
            message = "Khóa học này đã sử dụng quyền bảo lưu một lần.";
        } else if (checkPendingRequest && hasPendingSuspension(
                learner.getId(), enrollment.getClassSection().getInstructorLedCourse().getId())) {
            message = "Yêu cầu bảo lưu của lớp này đang được xử lý.";
        }

        return CourseSuspensionEligibilityResponse.builder()
                .enrollmentId(enrollment.getId())
                .classSectionId(enrollment.getClassSection().getId())
                .classroomTitle(enrollment.getClassSection().getTitle())
                .courseTitle(enrollment.getClassSection().getInstructorLedCourse().getTitle())
                .tuitionAmountDue(enrollment.getTuitionAmountDue())
                .tuitionAmountPaid(enrollment.getTuitionAmountPaid())
                .completedSessions(completedSessions)
                .totalSessions(totalSessions)
                .progressPercent(progressPercent)
                .eligible(message == null)
                .eligibilityMessage(message == null ? "Đủ điều kiện gửi yêu cầu bảo lưu." : message)
                .build();
    }

    private boolean isTuitionFullyPaid(ClassEnrollment enrollment) {
        if (enrollment.getTuitionAmountDue() == null) {
            return false;
        }
        BigDecimal due = enrollment.getTuitionAmountDue();
        BigDecimal paid = enrollment.getTuitionAmountPaid() == null ? BigDecimal.ZERO : enrollment.getTuitionAmountPaid();
        return paid.compareTo(due) >= 0;
    }

    private boolean hasUsedSuspension(Long learnerId, Long courseId) {
        return changeRequestRepository.findByRequesterIdAndRequestTypeInOrderByCreatedAtDesc(
                        learnerId,
                        List.of(ClassroomChangeRequestType.SUSPEND_STUDENT)
                ).stream()
                .filter(request -> request.getStatus() == ClassroomChangeRequestStatus.APPLIED)
                .map(request -> parseJsonMap(request.getOldValuesJson()).get("courseId"))
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .anyMatch(value -> value.equals(String.valueOf(courseId)));
    }

    private boolean hasPendingSuspension(Long learnerId, Long courseId) {
        return changeRequestRepository.findByRequesterIdAndRequestTypeInOrderByCreatedAtDesc(
                        learnerId,
                        List.of(ClassroomChangeRequestType.SUSPEND_STUDENT)
                ).stream()
                .filter(request -> request.getStatus() == ClassroomChangeRequestStatus.PENDING)
                .map(request -> parseJsonMap(request.getOldValuesJson()).get("courseId"))
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .anyMatch(value -> value.equals(String.valueOf(courseId)));
    }

    private void validateSuspensionDates(LocalDate startDate, LocalDate returnDate, boolean acceptingNewRequest) {
        LocalDate today = LocalDate.now();
        if (acceptingNewRequest && !startDate.equals(today)) {
            throw new RuntimeException("Yêu cầu bảo lưu bắt đầu từ ngày gửi yêu cầu.");
        }
        if (returnDate.isBefore(startDate)) {
            throw new RuntimeException("Ngày dự kiến quay lại phải từ ngày bắt đầu bảo lưu trở đi.");
        }
        if (returnDate.isAfter(startDate.plusMonths(3))) {
            throw new RuntimeException("Thời gian bảo lưu không được vượt quá 3 tháng.");
        }
        if (!acceptingNewRequest && returnDate.isBefore(today)) {
            throw new RuntimeException("Yêu cầu đã quá thời hạn bảo lưu.");
        }
    }

    private void applyCourseSuspension(ClassroomChangeRequest changeRequest) {
        Map<String, Object> values = parseJsonMap(changeRequest.getNewValuesJson());
        Long enrollmentId = longValue(values, "enrollmentId", "Thiếu hồ sơ lớp học cần bảo lưu.");
        ClassEnrollment enrollment = enrollmentRepository.findByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ lớp học cần bảo lưu."));
        assertEnrollmentOwner(enrollment, changeRequest.getRequester());
        CourseSuspensionEligibilityResponse eligibility = suspensionEligibility(
                enrollment, changeRequest.getRequester(), false);
        if (!eligibility.isEligible()) {
            throw new RuntimeException(eligibility.getEligibilityMessage());
        }
        validateSuspensionDates(
                LocalDate.parse(String.valueOf(values.get("requestedStartDate"))),
                LocalDate.parse(String.valueOf(values.get("requestedReturnDate"))),
                false
        );
        offeringService.suspendEnrollment(
                enrollment.getId(),
                "Bảo lưu theo yêu cầu #" + changeRequest.getId()
        );
    }

    private void applyCourseReturn(
            ClassroomChangeRequest changeRequest,
            Long targetClassSectionId,
            User reviewer
    ) {
        Map<String, Object> oldValues = parseJsonMap(changeRequest.getOldValuesJson());
        Long enrollmentId = longValue(oldValues, "enrollmentId", "Thiếu hồ sơ bảo lưu.");
        ClassEnrollment enrollment = enrollmentRepository.findByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ bảo lưu."));
        assertEnrollmentOwner(enrollment, changeRequest.getRequester());
        if (enrollment.getRegistrationStatus() != ClassroomRegistrationStatus.SUSPENDED) {
            throw new RuntimeException("Hồ sơ này không còn ở trạng thái bảo lưu.");
        }

        ClassSection target = offeringRepository.findByIdForUpdate(targetClassSectionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học được chọn."));
        if (!target.getInstructorLedCourse().getId()
                .equals(enrollment.getClassSection().getInstructorLedCourse().getId())) {
            throw new RuntimeException("Lớp được chọn không thuộc khóa học đã bảo lưu.");
        }
        if (!List.of(ClassroomOfferingStatus.UPCOMING, ClassroomOfferingStatus.ACTIVE).contains(target.getStatus())
                || !hasFutureSession(target, LocalDate.now())) {
            throw new RuntimeException("Lớp được chọn không còn nhận học viên học lại.");
        }
        if (!canReceiveReturningLearner(target, enrollment)) {
            throw new RuntimeException("Lớp được chọn đã hết chỗ hoặc trùng lịch học của học viên.");
        }

        Map<String, Object> newValues = new LinkedHashMap<>(parseJsonMap(changeRequest.getNewValuesJson()));
        newValues.put("targetClassSectionId", target.getId());
        newValues.put("targetClassroomTitle", target.getTitle());
        changeRequest.setNewValuesJson(writeJson(newValues));

        if (target.getId().equals(enrollment.getClassSection().getId())) {
            enrollment.setRegistrationStatus(ClassroomRegistrationStatus.ASSIGNED);
            enrollment.setAssignedAt(LocalDateTime.now());
            enrollment.setAssignedBy(reviewer);
            enrollment.setAssignmentNote("Tiếp tục học sau bảo lưu");
            enrollmentRepository.save(enrollment);
            return;
        }

        offeringService.transferStudent(
                enrollment.getClassSection().getId(),
                TransferStudentRequest.builder()
                        .studentId(enrollment.getStudent().getId())
                        .targetClassSectionId(target.getId())
                        .note("Xếp lớp lại sau bảo lưu theo yêu cầu #" + changeRequest.getId())
                        .build()
        );
    }

    private boolean canReceiveReturningLearner(ClassSection classroom, ClassEnrollment enrollment) {
        if (!classroom.getId().equals(enrollment.getClassSection().getId())
                && enrollmentRepository.findByStudentIdAndClassSectionId(
                        enrollment.getStudent().getId(), classroom.getId()).isPresent()) {
            return false;
        }
        Integer capacity = classroom.getCapacity();
        if (capacity != null && capacity > 0
                && enrollmentRepository.countByOfferingAndRegistrationStatuses(
                        classroom.getId(), ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT) >= capacity) {
            return false;
        }
        List<ClassSchedule> schedules = sessionRepository
                .findByClassSectionIdOrderBySessionDateAscStartTimeAsc(classroom.getId());
        for (ClassSchedule session : schedules) {
            if (session.getStatus() == ClassroomSessionStatus.CANCELLED
                    || session.getStatus() == ClassroomSessionStatus.COMPLETED
                    || session.getEndDateTime().isBefore(LocalDateTime.now())) {
                continue;
            }
            ConflictCheckResultResponse result = conflictService.check(ConflictCheckRequest.builder()
                    .learnerIds(List.of(enrollment.getStudent().getId()))
                    .sessionDate(session.getSessionDate())
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .excludeSessionId(session.getId())
                    .checkCapacity(false)
                    .build());
            if (result.isHasBlockingConflict()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasFutureSession(ClassSection classroom, LocalDate today) {
        return sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(classroom.getId()).stream()
                .anyMatch(session -> session.getStatus() != ClassroomSessionStatus.CANCELLED
                        && !session.getSessionDate().isBefore(today));
    }

    private void assertEnrollmentOwner(ClassEnrollment enrollment, User learner) {
        if (!enrollment.getStudent().getId().equals(learner.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác với hồ sơ lớp học này.");
        }
    }

    private void notifyRequestCreated(ClassroomChangeRequest changeRequest, String staffMessage) {
        notificationService.notifyUser(
                changeRequest.getReviewer(),
                "CLASSROOM_CHANGE_REQUEST_PENDING",
                "Yêu cầu bảo lưu khóa học",
                staffMessage,
                Map.of("requestId", changeRequest.getId(), "classroomId", changeRequest.getClassSection().getId())
        );
        notificationService.notifyUser(
                changeRequest.getRequester(),
                "CLASSROOM_CHANGE_REQUEST_CREATED",
                "Yêu cầu đã được gửi",
                "Yêu cầu đang chờ Nhân viên đào tạo xử lý.",
                Map.of("requestId", changeRequest.getId(), "classroomId", changeRequest.getClassSection().getId())
        );
    }

    private Long longValue(Map<String, Object> values, String key, String message) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new RuntimeException(message);
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String normalizeSuspensionProofUrl(String proofUrl) {
        return attachmentStorageService.loadStoredAttachmentFromUrl(proofUrl.trim())
                .map(attachment -> "/api/classroom-homework/attachments/" + attachment.fileName())
                .orElseThrow(() -> new RuntimeException("Giấy tờ minh chứng phải được tải lên EnglishLab."));
    }

    private String writeJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception exception) {
            throw new RuntimeException("Không thể lưu thông tin yêu cầu bảo lưu.", exception);
        }
    }

    private String appendNote(String existing, String addition) {
        if (existing == null || existing.isBlank()) {
            return addition;
        }
        return existing + " | " + addition;
    }

    private void requireNewValue(Map<String, Object> values, String key, String message) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new RuntimeException(message);
        }
    }
}
