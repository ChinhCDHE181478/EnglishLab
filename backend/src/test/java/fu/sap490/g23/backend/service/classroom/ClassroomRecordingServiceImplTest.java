package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomRecordingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomRecordingServiceImplTest {

    @Mock
    private ClassroomOfferingRepository offeringRepository;

    @Mock
    private ClassroomSessionRepository sessionRepository;

    @Mock
    private ClassroomMapper mapper;

    @Mock
    private LarkMeetingService larkMeetingService;

    private ClassroomRecordingServiceImpl service;
    private ClassroomSession session;

    @BeforeEach
    void setUp() {
        service = new ClassroomRecordingServiceImpl(
                offeringRepository,
                sessionRepository,
                mapper,
                larkMeetingService,
                new LarkProperties()
        );
        session = ClassroomSession.builder()
                .id(11L)
                .classroomOffering(ClassroomOffering.builder().build())
                .larkMeetingId("meeting-11")
                .recordingSyncStatus(RecordingSyncStatus.PROCESSING)
                .recordingSyncAttempts(0)
                .build();
        when(sessionRepository.findById(11L)).thenReturn(Optional.of(session));
    }

    @Test
    void syncStoresReadyLarkRecordingAndKeepsItPrivateForReview() {
        stubSavedResponse();
        when(larkMeetingService.getRecording(session))
                .thenReturn(new LarkRecordingInfo("https://meetings.larksuite.com/minutes/abc", 180_000L));

        service.syncLarkRecording(11L);

        assertThat(session.getRecordingUrl()).isEqualTo("https://meetings.larksuite.com/minutes/abc");
        assertThat(session.getRecordingProvider()).isEqualTo("LARK");
        assertThat(session.getRecordingSyncStatus()).isEqualTo(RecordingSyncStatus.READY);
        assertThat(session.getRecordingDurationMs()).isEqualTo(180_000L);
        assertThat(session.getRecordingVisible()).isFalse();
        assertThat(session.getRecordingExpiresAt()).isNotNull();
    }

    @Test
    void processingResponseIsRetriedInsteadOfPublishedAsFailure() {
        stubSavedResponse();
        when(larkMeetingService.getRecording(session))
                .thenThrow(new RuntimeException("Lark API lỗi (124002): record processing"));

        service.syncLarkRecording(11L);

        assertThat(session.getRecordingSyncStatus()).isEqualTo(RecordingSyncStatus.PROCESSING);
        assertThat(session.getRecordingSyncAttempts()).isEqualTo(1);
        assertThat(session.getRecordingSyncError()).contains("tự thử lại");
    }

    @Test
    void cannotPublishSessionWithoutARecordingUrl() {
        UpdateRecordingRequest request = new UpdateRecordingRequest();
        request.setRecordingVisible(true);

        assertThatThrownBy(() -> service.updateSessionRecording(11L, request))
                .hasMessageContaining("Chưa có đường dẫn recording");
    }

    private void stubSavedResponse() {
        when(sessionRepository.save(session)).thenReturn(session);
        when(mapper.toManagerSessionResponse(session))
                .thenReturn(ClassroomSessionResponse.builder().id(11L).build());
    }
}
