package fu.sap490.g23.backend.service.classroom;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sap490.g23.backend.service.classroom.impl.GoogleMeetServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        VirtualMeetingService service = new GoogleMeetServiceImpl(properties);
        ClassroomSession session = new ClassroomSession();

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
        VirtualMeetingService service = new GoogleMeetServiceImpl(new GoogleMeetProperties());

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
        VirtualMeetingService service = new GoogleMeetServiceImpl(properties);

        assertThatThrownBy(() -> service.syncMeeting(new ClassroomSession()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GOOGLE_MEET_CLIENT_ID")
                .hasMessageContaining("GOOGLE_MEET_REFRESH_TOKEN");
    }

    private GoogleMeetProperties configuredProperties() {
        int port = server.getAddress().getPort();
        GoogleMeetProperties properties = new GoogleMeetProperties();
        properties.setEnabled(true);
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRefreshToken("refresh-token");
        properties.setTokenUri("http://localhost:" + port + "/token");
        properties.setApiBaseUrl("http://localhost:" + port + "/v2");
        return properties;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
