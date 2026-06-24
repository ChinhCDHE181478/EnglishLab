package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sap490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.notification.ClassroomNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomChangeRequestServiceImpl implements ClassroomChangeRequestService {

    private final ClassroomChangeRequestRepository changeRequestRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomRoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper mapper;
    private final ClassroomConflictService conflictService;
    private final ClassroomOfferingService offeringService;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomNotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public ConflictCheckResultResponse checkConflict(CreateChangeRequestRequest request, String requesterEmail) {
        User requester = accessHelper.requireUser(requesterEmail);
        accessHelper.assertTeacher(requester);

        ClassroomOffering offering = offeringRepository.findById(request.getClassroomOfferingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        ClassroomSession targetSession = resolveTargetSession(request);
        ConflictCheckRequest conflictRequest = buildConflictRequest(request, offering, targetSession);
        return conflictService.check(conflictRequest);
    }

    @Override
    public ClassroomChangeRequestResponse create(CreateChangeRequestRequest request, String requesterEmail) {
        User requester = accessHelper.requireUser(requesterEmail);
        accessHelper.assertTeacher(requester);

        ClassroomOffering offering = offeringRepository.findById(request.getClassroomOfferingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        ClassroomSession targetSession = resolveTargetSession(request);
        validateChangeRequest(request, targetSession);

        if (request.getTargetSessionId() != null) {
            changeRequestRepository.findByTargetSessionIdAndRequestTypeAndStatus(
                    request.getTargetSessionId(),
                    request.getRequestType(),
                    ClassroomChangeRequestStatus.PENDING
            ).ifPresent(existing -> {
                throw new RuntimeException("Đã có yêu cầu cùng loại đang chờ duyệt cho buổi học này.");
            });
        }

        String oldValuesJson = buildOldValuesJson(request.getRequestType(), offering, targetSession);
        ConflictCheckRequest conflictRequest = buildConflictRequest(request, offering, targetSession);
        // Block submission when there is a real schedule conflict (teacher/room/learner).
        // Training Manager can still override at approval time.
        conflictService.assertNoBlockingConflict(conflictRequest);

        ClassroomChangeRequest changeRequest = ClassroomChangeRequest.builder()
                .requestType(request.getRequestType())
                .requester(requester)
                .requesterRole(requester.getRole())
                .classroomOffering(offering)
                .targetSession(targetSession)
                .oldValuesJson(oldValuesJson)
                .newValuesJson(request.getNewValuesJson())
                .reason(request.getReason())
                .status(ClassroomChangeRequestStatus.PENDING)
                .build();

        changeRequest = changeRequestRepository.save(changeRequest);
        notificationService.notifyTrainingManagers(
                "CLASSROOM_CHANGE_REQUEST_PENDING",
                "Yêu cầu thay đổi lớp học",
                requester.getFullName() + " gửi yêu cầu " + mapper.changeRequestTypeLabel(request.getRequestType()) + ".",
                Map.of("requestId", changeRequest.getId(), "classroomId", offering.getId())
        );
        notificationService.notifyUser(
                requester,
                "CLASSROOM_CHANGE_REQUEST_CREATED",
                "Yêu cầu thay đổi đã được gửi",
                "Yêu cầu " + mapper.changeRequestTypeLabel(request.getRequestType()) + " đang chờ Training Manager duyệt.",
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
    public List<ClassroomChangeRequestResponse> listPending() {
        return changeRequestRepository.findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus.PENDING).stream()
                .map(mapper::toChangeRequestResponse)
                .toList();
    }

    @Override
    public ClassroomChangeRequestResponse approve(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        accessHelper.assertTrainingManager(reviewer);

        ClassroomChangeRequest changeRequest = findPendingRequest(requestId);
        ConflictCheckRequest conflictRequest = buildConflictRequestFromEntity(changeRequest);
        conflictRequest.setCheckSessionLocked(true);

        if (!request.isOverrideConflict()) {
            conflictService.assertNoBlockingConflict(conflictRequest);
        } else if (request.getReviewNote() == null || request.getReviewNote().isBlank()) {
            throw new RuntimeException("Cần ghi chú khi ghi đè xung đột lịch học.");
        }

        applyChangeRequest(changeRequest);
        changeRequest.setStatus(ClassroomChangeRequestStatus.APPLIED);
        changeRequest.setReviewer(reviewer);
        changeRequest.setReviewedAt(LocalDateTime.now());
        changeRequest.setReviewNote(request.getReviewNote());
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
    public ClassroomChangeRequestResponse reject(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail) {
        User reviewer = accessHelper.requireUser(reviewerEmail);
        accessHelper.assertTrainingManager(reviewer);

        ClassroomChangeRequest changeRequest = findPendingRequest(requestId);
        changeRequest.setStatus(ClassroomChangeRequestStatus.REJECTED);
        changeRequest.setReviewer(reviewer);
        changeRequest.setReviewedAt(LocalDateTime.now());
        changeRequest.setReviewNote(request.getReviewNote());
        changeRequest = changeRequestRepository.save(changeRequest);

        notificationService.notifyUser(
                changeRequest.getRequester(),
                "CLASSROOM_CHANGE_REQUEST_REJECTED",
                "Yêu cầu thay đổi bị từ chối",
                request.getReviewNote() == null || request.getReviewNote().isBlank()
                        ? "Yêu cầu của bạn đã bị từ chối."
                        : request.getReviewNote(),
                Map.of("requestId", changeRequest.getId())
        );
        return mapper.toChangeRequestResponse(changeRequest);
    }

    private ClassroomChangeRequest findPendingRequest(Long requestId) {
        ClassroomChangeRequest changeRequest = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu thay đổi."));
        if (changeRequest.getStatus() != ClassroomChangeRequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu không còn ở trạng thái chờ duyệt.");
        }
        return changeRequest;
    }

    private ClassroomSession resolveTargetSession(CreateChangeRequestRequest request) {
        if (request.getTargetSessionId() == null) {
            return null;
        }
        return sessionRepository.findById(request.getTargetSessionId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học mục tiêu."));
    }

    private String buildOldValuesJson(ClassroomChangeRequestType type, ClassroomOffering offering, ClassroomSession session) {
        try {
            Map<String, Object> values = switch (type) {
                case RESCHEDULE_SESSION, CANCEL_SESSION, CHANGE_ROOM, CHANGE_TEACHER, UPDATE_LARK_LINK -> sessionValues(session);
                case TRANSFER_STUDENT, TRANSFER_CLASS -> Map.of("classroomOfferingId", offering.getId());
                case CREATE_MAKEUP_SESSION -> Map.of();
            };
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> sessionValues(ClassroomSession session) {
        if (session == null) {
            return Map.of();
        }
        return Map.of(
                "sessionDate", session.getSessionDate().toString(),
                "startTime", session.getStartTime().toString(),
                "endTime", session.getEndTime().toString(),
                "teacherId", session.getTeacher() == null ? null : session.getTeacher().getId(),
                "roomId", session.getRoom() == null ? null : session.getRoom().getId(),
                "larkMeetingUrl", session.getLarkMeetingUrl(),
                "status", session.getStatus().name()
        );
    }

    private ConflictCheckRequest buildConflictRequest(CreateChangeRequestRequest request, ClassroomOffering offering, ClassroomSession session) {
        ConflictCheckRequest.ConflictCheckRequestBuilder builder = ConflictCheckRequest.builder()
                .classroomOfferingId(offering.getId())
                .requestType(request.getRequestType())
                .checkSessionLocked(true);

        if (session != null) {
            builder.sessionId(session.getId())
                    .excludeSessionId(session.getId())
                    .teacherId(session.getTeacher() == null ? null : session.getTeacher().getId())
                    .roomId(session.getRoom() == null ? null : session.getRoom().getId())
                    .sessionDate(session.getSessionDate())
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime());
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
        }
        if (newValues.containsKey("roomId") && newValues.get("roomId") != null) {
            builder.roomId(Long.valueOf(String.valueOf(newValues.get("roomId"))));
        }
        if (newValues.containsKey("targetClassroomOfferingId") && newValues.get("targetClassroomOfferingId") != null) {
            builder.targetClassroomOfferingId(Long.valueOf(String.valueOf(newValues.get("targetClassroomOfferingId"))));
        }
        if (newValues.containsKey("studentId") && newValues.get("studentId") != null) {
            builder.learnerIds(List.of(Long.valueOf(String.valueOf(newValues.get("studentId")))));
        }
        if (newValues.containsKey("larkMeetingUrl")) {
            builder.larkMeetingUrl(String.valueOf(newValues.get("larkMeetingUrl")));
        }

        return builder.build();
    }

    private ConflictCheckRequest buildConflictRequestFromEntity(ClassroomChangeRequest changeRequest) {
        CreateChangeRequestRequest request = CreateChangeRequestRequest.builder()
                .requestType(changeRequest.getRequestType())
                .classroomOfferingId(changeRequest.getClassroomOffering().getId())
                .targetSessionId(changeRequest.getTargetSession() == null ? null : changeRequest.getTargetSession().getId())
                .newValuesJson(changeRequest.getNewValuesJson())
                .reason(changeRequest.getReason())
                .build();
        return buildConflictRequest(request, changeRequest.getClassroomOffering(), changeRequest.getTargetSession());
    }

    private void applyChangeRequest(ClassroomChangeRequest changeRequest) {
        Map<String, Object> newValues = parseJsonMap(changeRequest.getNewValuesJson());
        ClassroomSession session = changeRequest.getTargetSession();

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
                offeringService.updateSession(session.getId(), sessionRequest);
                session.setStatus(ClassroomSessionStatus.RESCHEDULED);
                sessionRepository.save(session);
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
                session.setLocked(true);
                sessionRepository.save(session);
            }
            case CREATE_MAKEUP_SESSION -> {
                CreateClassroomSessionRequest sessionRequest = CreateClassroomSessionRequest.builder()
                        .sessionDate(LocalDate.parse(String.valueOf(newValues.get("sessionDate"))))
                        .startTime(LocalTime.parse(String.valueOf(newValues.get("startTime"))))
                        .endTime(LocalTime.parse(String.valueOf(newValues.get("endTime"))))
                        .teacherId(newValues.get("teacherId") == null ? null : Long.valueOf(String.valueOf(newValues.get("teacherId"))))
                        .roomId(newValues.get("roomId") == null ? null : Long.valueOf(String.valueOf(newValues.get("roomId"))))
                        .status(ClassroomSessionStatus.MAKEUP)
                        .build();
                offeringService.createSession(changeRequest.getClassroomOffering().getId(), sessionRequest);
            }
            case TRANSFER_STUDENT -> offeringService.transferStudent(
                    changeRequest.getClassroomOffering().getId(),
                    TransferStudentRequest.builder()
                            .studentId(Long.valueOf(String.valueOf(newValues.get("studentId"))))
                            .targetClassroomOfferingId(Long.valueOf(String.valueOf(newValues.get("targetClassroomOfferingId"))))
                            .note(changeRequest.getReason())
                            .build()
            );
            case UPDATE_LARK_LINK -> {
                if (session == null) {
                    throw new RuntimeException("Thiếu buổi học mục tiêu.");
                }
                offeringService.updateSessionLarkLink(session.getId(), UpdateLarkLinkRequest.builder()
                        .larkMeetingUrl(String.valueOf(newValues.get("larkMeetingUrl")))
                        .build());
            }
            case TRANSFER_CLASS -> throw new RuntimeException("Chuyển cả lớp chưa được hỗ trợ trong phiên bản hiện tại.");
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

    private void validateChangeRequest(CreateChangeRequestRequest request, ClassroomSession session) {
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
