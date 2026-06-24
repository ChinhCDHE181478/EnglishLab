package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.LarkMeetingParticipant;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.repository.classroom.LarkMeetingParticipantRepository;
import fu.sap490.g23.backend.service.classroom.impl.LarkWebhookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LarkWebhookServiceTest {

    @Mock
    private ClassroomSessionRepository sessionRepository;

    @Mock
    private LarkMeetingParticipantRepository participantRepository;

    private LarkWebhookServiceImpl service;
    private ClassroomSession session;

    @BeforeEach
    void setUp() {
        service = new LarkWebhookServiceImpl(
                new LarkProperties(),
                sessionRepository,
                participantRepository
        );
        session = ClassroomSession.builder()
                .id(10L)
                .larkMeetingNo("235812466")
                .larkMeetingStatus(LarkMeetingStatus.SCHEDULED)
                .build();
        when(sessionRepository.findByLarkMeetingNo("235812466")).thenReturn(Optional.of(session));
    }

    @Test
    void joinMarksParticipantActiveAndCancelsEmptyCountdown() {
        when(participantRepository.findByClassroomSessionIdAndParticipantKey(
                10L,
                "open_id:ou_test"
        )).thenReturn(Optional.empty());

        service.handle(event("vc.meeting.join_meeting_v1"));

        verify(participantRepository).save(any(LarkMeetingParticipant.class));
        assertThat(session.getLarkMeetingStatus()).isEqualTo(LarkMeetingStatus.IN_PROGRESS);
        assertThat(session.getLarkEmptySince()).isNull();
        assertThat(session.getLarkMeetingId()).isEqualTo("meeting-test");
    }

    @Test
    void lastLeaveStartsFiveMinuteEmptyCountdown() {
        LarkMeetingParticipant participant = LarkMeetingParticipant.builder()
                .classroomSession(session)
                .participantKey("open_id:ou_test")
                .active(true)
                .build();
        when(participantRepository.findByClassroomSessionIdAndParticipantKey(
                10L,
                "open_id:ou_test"
        )).thenReturn(Optional.of(participant));
        when(participantRepository.countByClassroomSessionIdAndActiveTrue(10L)).thenReturn(0L);

        service.handle(event("vc.meeting.leave_meeting_v1"));

        assertThat(participant.isActive()).isFalse();
        assertThat(participant.getLeftAt()).isNotNull();
        assertThat(session.getLarkEmptySince()).isNotNull();
    }

    private Map<String, Object> event(String eventType) {
        return Map.of(
                "header", Map.of("event_type", eventType),
                "event", Map.of(
                        "meeting", Map.of(
                                "id", "meeting-test",
                                "meeting_no", "235812466"
                        ),
                        "operator", Map.of(
                                "id", Map.of("open_id", "ou_test")
                        )
                )
        );
    }
}
