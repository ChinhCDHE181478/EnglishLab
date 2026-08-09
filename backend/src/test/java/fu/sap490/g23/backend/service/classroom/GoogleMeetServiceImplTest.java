package fu.sap490.g23.backend.service.classroom;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sap490.g23.backend.service.classroom.impl.GoogleMeetServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
