package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.service.classroom.LarkProperties;
import fu.sep490.g23.backend.service.classroom.VirtualAttendanceService;
import fu.sep490.g23.backend.service.classroom.LarkWebhookService;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.LarkMeetingParticipant;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.LarkMeetingParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LarkWebhookServiceImpl implements LarkWebhookService {

    private static final String MEETING_STARTED = "vc.meeting.meeting_started_v1";
    private static final String MEETING_ENDED = "vc.meeting.meeting_ended_v1";
    private static final String PARTICIPANT_JOINED = "vc.meeting.join_meeting_v1";
    private static final String PARTICIPANT_LEFT = "vc.meeting.leave_meeting_v1";
    private static final String RECORDING_STARTED = "vc.meeting.recording_started_v1";
    private static final String RECORDING_ENDED = "vc.meeting.recording_ended_v1";
    private static final String RECORDING_READY = "vc.meeting.recording_ready_v1";
    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            MEETING_STARTED,
            MEETING_ENDED,
            PARTICIPANT_JOINED,
            PARTICIPANT_LEFT,
            RECORDING_STARTED,
            RECORDING_ENDED,
            RECORDING_READY
    );

    private final LarkProperties properties;
    private final ClassScheduleRepository sessionRepository;
    private final LarkMeetingParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final VirtualAttendanceService virtualAttendanceService;

    @Override
    public void verifyChallenge(Map<String, Object> payload) {
        verifyToken(stringValue(payload.get("token")));
    }

    @Override
    @Transactional
    public void handle(Map<String, Object> payload) {
        Map<String, Object> header = asMap(payload.get("header"));
        verifyToken(stringValue(header.get("token")));
        String eventType = stringValue(header.get("event_type"));
        if (!SUPPORTED_EVENTS.contains(eventType)) {
            return;
        }

        Map<String, Object> event = asMap(payload.get("event"));
        Map<String, Object> meeting = asMap(event.get("meeting"));
        String meetingId = stringValue(meeting.get("id"));
        String meetingNo = stringValue(meeting.get("meeting_no"));
        ClassSchedule session = findSession(meetingId, meetingNo);
        if (session == null) {
            log.warn("Không tìm thấy buổi học cho webhook Lark meetingId={}, meetingNo={}.", meetingId, meetingNo);
            return;
        }

        session.setLarkMeetingId(blankToNull(meetingId));
        session.setLarkMeetingNo(blankToNull(meetingNo));
        if (!PARTICIPANT_JOINED.equals(eventType) && !PARTICIPANT_LEFT.equals(eventType)) {
            updateMeetingState(session, eventType);
            sessionRepository.save(session);
            return;
        }

        String participantKey = resolveParticipantKey(event);
        if (participantKey.isBlank()) {
            log.warn("Webhook Lark {} không có định danh người tham gia.", eventType);
            return;
        }

        if (PARTICIPANT_JOINED.equals(eventType)) {
            markJoined(session, participantKey);
        } else {
            markLeft(session, participantKey);
        }
        virtualAttendanceService.syncLarkParticipantAttendance(session);
        sessionRepository.save(session);
    }

    private void updateMeetingState(ClassSchedule session, String eventType) {
        switch (eventType) {
            case MEETING_STARTED -> {
                session.setLarkMeetingStatus(LarkMeetingStatus.IN_PROGRESS);
                if (session.getStatus() == ClassroomSessionStatus.SCHEDULED
                        || session.getStatus() == ClassroomSessionStatus.OPEN) {
                    session.setStatus(ClassroomSessionStatus.IN_PROGRESS);
                }
            }
            case MEETING_ENDED -> {
                session.setLarkMeetingStatus(LarkMeetingStatus.ENDED);
                if (session.getStatus() == ClassroomSessionStatus.IN_PROGRESS
                        || session.getStatus() == ClassroomSessionStatus.OPEN
                        || session.getStatus() == ClassroomSessionStatus.SCHEDULED) {
                    session.setStatus(ClassroomSessionStatus.COMPLETED);
                }
                if (session.getRecordingSyncStatus() == RecordingSyncStatus.RECORDING) {
                    markRecordingProcessing(session);
                }
            }
            case RECORDING_STARTED -> {
                session.setRecordingProvider("LARK");
                session.setRecordingSyncStatus(RecordingSyncStatus.RECORDING);
                session.setRecordingSyncError(null);
            }
            case RECORDING_ENDED -> markRecordingProcessing(session);
            case RECORDING_READY -> {
                markRecordingProcessing(session);
                session.setRecordingSyncAttempts(0);
                session.setRecordingLastAttemptAt(null);
            }
            default -> log.debug("Bỏ qua trạng thái webhook Lark {}.", eventType);
        }
    }

    private void markRecordingProcessing(ClassSchedule session) {
        session.setRecordingProvider("LARK");
        session.setRecordingSyncStatus(RecordingSyncStatus.PROCESSING);
        session.setRecordingSyncError(null);
    }

    private void markJoined(ClassSchedule session, String participantKey) {
        LocalDateTime now = LocalDateTime.now();
        LarkMeetingParticipant participant = participantRepository
                .findByClassScheduleIdAndParticipantKey(session.getId(), participantKey)
                .orElseGet(() -> LarkMeetingParticipant.builder()
                        .classSchedule(session)
                        .participantKey(participantKey)
                        .build());
        participant.setActive(true);
        participant.setJoinedAt(now);
        participant.setLeftAt(null);
        participant.setUserId(resolveUserId(participantKey));
        participantRepository.save(participant);

        session.setLarkEmptySince(null);
        session.setLarkMeetingStatus(LarkMeetingStatus.IN_PROGRESS);
    }

    private void markLeft(ClassSchedule session, String participantKey) {
        participantRepository.findByClassScheduleIdAndParticipantKey(session.getId(), participantKey)
                .ifPresent(participant -> {
                    participant.setActive(false);
                    participant.setLeftAt(LocalDateTime.now());
                    participantRepository.save(participant);
                });
        if (participantRepository.countByClassScheduleIdAndActiveTrue(session.getId()) == 0) {
            session.setLarkEmptySince(LocalDateTime.now());
        }
    }

    private ClassSchedule findSession(String meetingId, String meetingNo) {
        if (!meetingId.isBlank()) {
            ClassSchedule session = sessionRepository.findByLarkMeetingId(meetingId).orElse(null);
            if (session != null) return session;
        }
        return meetingNo.isBlank() ? null : sessionRepository.findByLarkMeetingNo(meetingNo).orElse(null);
    }

    private String resolveParticipantKey(Map<String, Object> event) {
        Map<String, Object> id = asMap(asMap(event.get("operator")).get("id"));
        String openId = stringValue(id.get("open_id"));
        if (!openId.isBlank()) return "open_id:" + openId;
        String userId = stringValue(id.get("user_id"));
        if (!userId.isBlank()) return "user_id:" + userId;
        String unionId = stringValue(id.get("union_id"));
        return unionId.isBlank() ? "" : "union_id:" + unionId;
    }

    private void verifyToken(String actual) {
        String expected = properties.getVerificationToken();
        if (expected != null && !expected.isBlank()
                && !expected.equals(actual)) {
            throw new RuntimeException("Webhook Lark không hợp lệ.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Long resolveUserId(String participantKey) {
        if (participantKey == null || !participantKey.startsWith("open_id:")) {
            return null;
        }
        String openId = participantKey.substring("open_id:".length());
        return userRepository.findByLarkOpenId(openId).map(User::getId).orElse(null);
    }
}
