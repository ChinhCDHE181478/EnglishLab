package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;

import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.*;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.repository.classroom.LarkMeetingParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LarkWebhookServiceImpl implements LarkWebhookService {

    private final LarkProperties properties;
    private final ClassroomSessionRepository sessionRepository;
    private final LarkMeetingParticipantRepository participantRepository;

    @Transactional
    public void handle(Map<String, Object> payload) {
        Map<String, Object> header = asMap(payload.get("header"));
        String eventType = stringValue(header.get("event_type"));
        if (!"vc.meeting.join_meeting_v1".equals(eventType)
                && !"vc.meeting.leave_meeting_v1".equals(eventType)) {
            return;
        }
        verifyToken(header);

        Map<String, Object> event = asMap(payload.get("event"));
        Map<String, Object> meeting = asMap(event.get("meeting"));
        String meetingId = stringValue(meeting.get("id"));
        String meetingNo = stringValue(meeting.get("meeting_no"));
        ClassroomSession session = findSession(meetingId, meetingNo);
        if (session == null) {
            log.warn("Không tìm thấy buổi học cho webhook Lark meetingId={}, meetingNo={}.", meetingId, meetingNo);
            return;
        }

        session.setLarkMeetingId(blankToNull(meetingId));
        session.setLarkMeetingNo(blankToNull(meetingNo));
        String participantKey = resolveParticipantKey(event);
        if (participantKey.isBlank()) {
            log.warn("Webhook Lark {} không có định danh người tham gia.", eventType);
            return;
        }

        if ("vc.meeting.join_meeting_v1".equals(eventType)) {
            markJoined(session, participantKey);
        } else {
            markLeft(session, participantKey);
        }
        sessionRepository.save(session);
    }

    private void markJoined(ClassroomSession session, String participantKey) {
        LocalDateTime now = LocalDateTime.now();
        LarkMeetingParticipant participant = participantRepository
                .findByClassroomSessionIdAndParticipantKey(session.getId(), participantKey)
                .orElseGet(() -> LarkMeetingParticipant.builder()
                        .classroomSession(session)
                        .participantKey(participantKey)
                        .build());
        participant.setActive(true);
        participant.setJoinedAt(now);
        participant.setLeftAt(null);
        participantRepository.save(participant);

        session.setLarkEmptySince(null);
        session.setLarkMeetingStatus(LarkMeetingStatus.IN_PROGRESS);
    }

    private void markLeft(ClassroomSession session, String participantKey) {
        participantRepository.findByClassroomSessionIdAndParticipantKey(session.getId(), participantKey)
                .ifPresent(participant -> {
                    participant.setActive(false);
                    participant.setLeftAt(LocalDateTime.now());
                    participantRepository.save(participant);
                });
        if (participantRepository.countByClassroomSessionIdAndActiveTrue(session.getId()) == 0) {
            session.setLarkEmptySince(LocalDateTime.now());
        }
    }

    private ClassroomSession findSession(String meetingId, String meetingNo) {
        if (!meetingId.isBlank()) {
            ClassroomSession session = sessionRepository.findByLarkMeetingId(meetingId).orElse(null);
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

    private void verifyToken(Map<String, Object> header) {
        String expected = properties.getVerificationToken();
        if (expected != null && !expected.isBlank()
                && !expected.equals(stringValue(header.get("token")))) {
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
}
