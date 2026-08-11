package fu.sep490.g23.backend.service.classroom;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.service.classroom.impl.GoogleMeetServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleMeetServiceImplTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createsRealGoogleMeetSpaceAndPersistsCompatibilityFields() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> respond(
                exchange,
                200,
                """
                        {"access_token":"access-token","expires_in":3600}
                        """
        ));
        server.createContext("/v2/spaces", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization"))
                    .isEqualTo("Bearer access-token");
            respond(
                    exchange,
                    200,
                    """
                            {
                              "name":"spaces/space-resource-id",
                              "meetingUri":"https://meet.google.com/abc-defg-hij",
                              "meetingCode":"abc-defg-hij"
                            }
                            """
            );
        });
        server.start();

        GoogleMeetProperties properties = configuredProperties();
        TeacherGoogleMeetConnectionService connectionService = connectedTeacherService();
        VirtualMeetingService service = new GoogleMeetServiceImpl(properties, connectionService);
        ClassroomSession session = sessionWithTeacher();

        service.syncMeeting(session);

        assertThat(session.getLarkMeetingId()).isEqualTo("spaces/space-resource-id");
        assertThat(session.getLarkMeetingNo()).isEqualTo("abc-defg-hij");
        assertThat(session.getLarkMeetingUrl()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(session.getLarkMeetingStatus()).isEqualTo(LarkMeetingStatus.SCHEDULED);
        assertThat(session.getLarkSyncStatus()).isEqualTo("SYNCED");
        assertThat(service.getPlatformName()).isEqualTo("Google Meet");
    }

    @Test
    void usesTheClassroomTeacherInsteadOfAnObsoleteStaffMeetingOwner() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> respond(exchange, 200, "{\"access_token\":\"access-token\",\"expires_in\":3600}"));
        server.createContext("/v2/spaces", exchange -> respond(
                exchange,
                200,
                "{\"name\":\"spaces/teacher-owned-room\",\"meetingUri\":\"https://meet.google.com/teacher-owned-room\",\"meetingCode\":\"teacher-owned-room\"}"
        ));
        server.start();

        User classroomTeacher = User.builder().id(88L).fullName("Giáo viên chủ lớp").email("owner@example.com").build();
        User obsoleteStaffOwner = User.builder().id(99L).fullName("Nhân viên cũ").email("staff@example.com").build();
        ClassroomOffering offering = ClassroomOffering.builder()
                .id(9L)
                .primaryTeacher(classroomTeacher)
                .virtualMeetingOwner(obsoleteStaffOwner)
                .build();
        ClassroomSession session = sessionWithTeacher();
        session.setClassroomOffering(offering);
        TeacherGoogleMeetConnectionService connectionService = connectedTeacherService();

        new GoogleMeetServiceImpl(configuredProperties(), connectionService).syncMeeting(session);

        verify(connectionService).requireRefreshToken(classroomTeacher);
    }

    @Test
    void rejectsLegacyLarkAndPlaceholderUrls() {
        VirtualMeetingService service = new GoogleMeetServiceImpl(
                new GoogleMeetProperties(),
                mock(TeacherGoogleMeetConnectionService.class)
        );

        assertThat(service.isLegacyOrPlaceholderUrl(
                "https://meet.larksuite.com/s/englishlab-toeic-650-showcase"
        )).isTrue();
        assertThat(service.isLegacyOrPlaceholderUrl("https://example.com/fake-room")).isTrue();
        assertThat(service.resolveStatus("https://meet.google.com/abc-defg-hij"))
                .isEqualTo(LarkMeetingStatus.SCHEDULED);
    }

    @Test
    void reportsMissingOAuthConfigurationInsteadOfCreatingFakeLink() {
        GoogleMeetProperties properties = new GoogleMeetProperties();
        properties.setEnabled(true);
        VirtualMeetingService service = new GoogleMeetServiceImpl(
                properties,
                mock(TeacherGoogleMeetConnectionService.class)
        );

        assertThatThrownBy(() -> service.syncMeeting(new ClassroomSession()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GOOGLE_MEET_CLIENT_ID")
                .hasMessageContaining("GOOGLE_MEET_CLIENT_SECRET");
    }

    @Test
    void explainsHowToRecoverWhenRefreshTokenIsExpiredOrRevoked() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> respond(
                exchange,
                400,
                """
                        {"error":"invalid_grant","error_description":"Token has been expired or revoked."}
                        """
        ));
        server.start();

        TeacherGoogleMeetConnectionService connectionService = connectedTeacherService();
        VirtualMeetingService service = new GoogleMeetServiceImpl(configuredProperties(), connectionService);

        assertThatThrownBy(() -> service.syncMeeting(sessionWithTeacher()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Quyền Google Meet của giáo viên đã hết hạn hoặc bị thu hồi");
        verify(connectionService).markReauthenticationRequired(any(User.class));
    }

    @Test
    void refreshesRejectedAccessTokenOnceAndRetriesMeetingCreation() throws IOException {
        AtomicInteger tokenRequests = new AtomicInteger();
        AtomicInteger spaceRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> {
            int tokenNumber = tokenRequests.incrementAndGet();
            respond(exchange, 200, "{\"access_token\":\"access-token-" + tokenNumber + "\",\"expires_in\":3600}");
        });
        server.createContext("/v2/spaces", exchange -> {
            int requestNumber = spaceRequests.incrementAndGet();
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (requestNumber == 1) {
                assertThat(authorization).isEqualTo("Bearer access-token-1");
                respond(exchange, 401, "{\"error\":{\"message\":\"expired token\"}}");
                return;
            }
            assertThat(authorization).isEqualTo("Bearer access-token-2");
            respond(
                    exchange,
                    200,
                    """
                            {
                              "name":"spaces/retried-space",
                              "meetingUri":"https://meet.google.com/retry-room-ok",
                              "meetingCode":"retry-room-ok"
                            }
                            """
            );
        });
        server.start();

        VirtualMeetingService service = new GoogleMeetServiceImpl(
                configuredProperties(),
                connectedTeacherService()
        );
        ClassroomSession session = sessionWithTeacher();

        service.syncMeeting(session);

        assertThat(tokenRequests).hasValue(2);
        assertThat(spaceRequests).hasValue(2);
        assertThat(session.getLarkMeetingUrl()).isEqualTo("https://meet.google.com/retry-room-ok");
        assertThat(session.getLarkSyncStatus()).isEqualTo("SYNCED");
    }

    @Test
    void createsRoomWithoutAutoRecordingWhenGoogleDoesNotAllowTheFeature() throws IOException {
        AtomicInteger spaceRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> respond(exchange, 200, "{\"access_token\":\"access-token\",\"expires_in\":3600}"));
        server.createContext("/v2/spaces", exchange -> {
            if (spaceRequests.incrementAndGet() == 1) {
                respond(exchange, 403, "{\"error\":{\"status\":\"PERMISSION_DENIED\",\"details\":[{\"reason\":\"FEATURE_UNAVAILABLE_TO_USER\",\"metadata\":{\"feature_name\":\"updateAutoRecordingGeneration\"}}]}}");
                return;
            }
            respond(exchange, 200, "{\"name\":\"spaces/manual-recording-room\",\"meetingUri\":\"https://meet.google.com/manual-recording-room\",\"meetingCode\":\"manual-recording-room\"}");
        });
        server.start();

        ClassroomSession session = sessionWithTeacher();
        new GoogleMeetServiceImpl(configuredProperties(), connectedTeacherService()).syncMeeting(session);

        assertThat(spaceRequests).hasValue(2);
        assertThat(session.getLarkMeetingUrl()).isEqualTo("https://meet.google.com/manual-recording-room");
        assertThat(session.getRecordingSyncStatus()).isEqualTo(RecordingSyncStatus.NOT_AVAILABLE);
        assertThat(session.getRecordingSyncError()).contains("ghi hình tự động");
    }

    @Test
    void reusesTheExistingGoogleMeetRoomForAnotherSessionInTheSameClassroom() {
        ClassroomSessionRepository sessionRepository = mock(ClassroomSessionRepository.class);
        ClassroomOffering offering = ClassroomOffering.builder().id(9L).build();
        ClassroomSession existing = new ClassroomSession();
        existing.setId(20L);
        existing.setClassroomOffering(offering);
        existing.setLarkMeetingId("spaces/shared-room");
        existing.setLarkMeetingNo("shared-room");
        existing.setLarkMeetingUrl("https://meet.google.com/shared-room");
        existing.setRecordingSyncStatus(RecordingSyncStatus.SCHEDULED);

        ClassroomSession session = sessionWithTeacher();
        session.setId(21L);
        session.setClassroomOffering(offering);
        when(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(9L))
                .thenReturn(List.of(existing, session));

        new GoogleMeetServiceImpl(configuredPropertiesWithoutServer(), connectedTeacherService(), sessionRepository)
                .syncMeeting(session);

        assertThat(session.getLarkMeetingId()).isEqualTo("spaces/shared-room");
        assertThat(session.getLarkMeetingUrl()).isEqualTo("https://meet.google.com/shared-room");
        assertThat(offering.getDefaultLarkMeetingUrl()).isEqualTo("https://meet.google.com/shared-room");
        assertThat(session.getRecordingSyncStatus()).isEqualTo(RecordingSyncStatus.SCHEDULED);
    }

    @Test
    void retrievesGeneratedRecordingFromTheGoogleMeetConference() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> respond(exchange, 200, "{\"access_token\":\"access-token\",\"expires_in\":3600}"));
        server.createContext("/v2/conferenceRecords", exchange -> respond(
                exchange,
                200,
                "{\"conferenceRecords\":[{\"name\":\"conferenceRecords/conference-1\"}]}"
        ));
        server.createContext("/v2/conferenceRecords/conference-1/recordings", exchange -> respond(
                exchange,
                200,
                """
                        {"recordings":[{
                          "state":"FILE_GENERATED",
                          "startTime":"2026-08-09T10:00:00Z",
                          "endTime":"2026-08-09T10:30:00Z",
                          "driveDestination":{"exportUri":"https://drive.google.com/file/d/recording-1/view"}
                        }]}
                        """
        ));
        server.start();

        ClassroomSession session = sessionWithTeacher();
        session.setLarkMeetingId("spaces/space-resource-id");

        VirtualMeetingRecordingInfo recording = new GoogleMeetServiceImpl(
                configuredProperties(),
                connectedTeacherService()
        ).getRecording(session);

        assertThat(recording.url()).isEqualTo("https://drive.google.com/file/d/recording-1/view");
        assertThat(recording.durationMs()).isEqualTo(1_800_000L);
    }

    @Test
    void matchesARecordingToTheConferenceHeldOnTheScheduledSessionDate() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> respond(exchange, 200, "{\"access_token\":\"access-token\",\"expires_in\":3600}"));
        server.createContext("/v2/conferenceRecords", exchange -> respond(
                exchange,
                200,
                """
                        {"conferenceRecords":[
                          {"name":"conferenceRecords/older","startTime":"2026-08-08T12:30:00Z"},
                          {"name":"conferenceRecords/matching","startTime":"2026-08-09T12:30:00Z"}
                        ]}
                        """
        ));
        server.createContext("/v2/conferenceRecords/matching/recordings", exchange -> respond(
                exchange,
                200,
                """
                        {"recordings":[{"state":"FILE_GENERATED","startTime":"2026-08-09T12:30:00Z","endTime":"2026-08-09T13:00:00Z","driveDestination":{"exportUri":"https://drive.google.com/file/d/matching/view"}}]}
                        """
        ));
        server.start();

        ClassroomSession session = sessionWithTeacher();
        session.setLarkMeetingId("spaces/shared-room");
        session.setSessionDate(LocalDate.of(2026, 8, 9));
        session.setStartTime(LocalTime.of(19, 30));

        VirtualMeetingRecordingInfo recording = new GoogleMeetServiceImpl(
                configuredProperties(),
                connectedTeacherService()
        ).getRecording(session);

        assertThat(recording.url()).isEqualTo("https://drive.google.com/file/d/matching/view");
    }

    private GoogleMeetProperties configuredProperties() {
        int port = server.getAddress().getPort();
        GoogleMeetProperties properties = new GoogleMeetProperties();
        properties.setEnabled(true);
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setTokenUri("http://localhost:" + port + "/token");
        properties.setApiBaseUrl("http://localhost:" + port + "/v2");
        return properties;
    }

    private GoogleMeetProperties configuredPropertiesWithoutServer() {
        GoogleMeetProperties properties = new GoogleMeetProperties();
        properties.setEnabled(true);
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        return properties;
    }

    private TeacherGoogleMeetConnectionService connectedTeacherService() {
        TeacherGoogleMeetConnectionService service = mock(TeacherGoogleMeetConnectionService.class);
        when(service.requireRefreshToken(any(User.class))).thenReturn("teacher-refresh-token");
        return service;
    }

    private ClassroomSession sessionWithTeacher() {
        User teacher = User.builder()
                .id(77L)
                .fullName("Giáo viên kiểm thử")
                .email("teacher@example.com")
                .build();
        ClassroomSession session = new ClassroomSession();
        session.setTeacher(teacher);
        return session;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
