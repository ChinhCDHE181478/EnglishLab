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
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomScheduleLockService;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.notification.ClassroomNotificationService;
import fu.sep490.g23.backend.service.classroom.ClassroomChangeRequestService;
import fu.sep490.g23.backend.service.classroom.ClassroomConflictService;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomChangeRequestServiceImpl implements ClassroomChangeRequestService {

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
                .build();

        changeRequest = changeRequestRepository.save(changeRequest);
        notificationService.notifyTrainingStaff(
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
    public List<ClassroomChangeRequestResponse> listPending() {
        return changeRequestRepository.findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus.PENDING).stream()
                .map(mapper::toChangeRequestResponse)
                .toList();
    }

    @Override
    public ClassroomChangeRequestResponse approve(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        accessHelper.assertStaffOperator(reviewer);

        ClassroomChangeRequest changeRequest = findPendingRequest(requestId);
        ConflictCheckRequest conflictRequest = buildConflictRequestFromEntity(changeRequest);
        configureSessionLockCheck(conflictRequest, changeRequest);

        scheduleLockService.lockDates(java.util.Arrays.asList(
                conflictRequest.getSessionDate(),
                changeRequest.getTargetClassSchedule() == null
                        ? conflictRequest.getSessionDate()
                        : changeRequest.getTargetClassSchedule().getSessionDate()
        ));

        boolean overrideConflict = request != null && Boolean.TRUE.equals(request.getOverrideConflict());

        if (!overrideConflict) {
            conflictService.assertNoBlockingConflict(conflictRequest);
        } else if (request.getReviewNote() == null || request.getReviewNote().isBlank()) {
            throw new RuntimeException("Cần ghi chú khi ghi đè xung đột lịch học.");
        }

        applyChangeRequest(changeRequest, overrideConflict);
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
    public ConflictCheckResultResponse checkPendingConflict(Long requestId) {
        ClassroomChangeRequest changeRequest = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu thay đổi."));
        if (changeRequest.getStatus() != ClassroomChangeRequestStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể kiểm tra trùng lịch cho yêu cầu đang chờ duyệt.");
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
        changeRequest.setStatus(ClassroomChangeRequestStatus.REJECTED);
        changeRequest.setReviewer(reviewer);
        changeRequest.setReviewedAt(LocalDateTime.now());
        String reviewNote = request == null ? null : request.getReviewNote();
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
            default -> {
            }
        }
    }

    private void requireNewValue(Map<String, Object> values, String key, String message) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new RuntimeException(message);
        }
    }
}
