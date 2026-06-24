package fu.sap490.g23.backend.service.classroom;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.service.classroom.impl.LarkMeetingServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class LarkMeetingServiceImplTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createsRealMeetingAndStoresLarkIdentifiers() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/open-apis/auth/v3/tenant_access_token/internal", exchange ->
                respond(exchange, """
                        {"code":0,"msg":"ok","tenant_access_token":"test-token","expire":7200}
                        """));
        server.createContext("/open-apis/calendar/v4/calendars/calendar-test/events", exchange ->
                {
                    if (exchange.getRequestURI().getPath().endsWith("/attendees")) {
                        respond(exchange, """
                                {"code":0,"msg":"success","data":{"attendees":[]}}
                                """);
                    } else {
                        respond(exchange, """
                                {
                                  "code":0,
                                  "msg":"success",
                                  "data":{
                                    "event":{
                                      "event_id":"event-test",
                                      "vchat":{"meeting_url":"https://meet.larksuite.com/s/real-meeting"}
                                    }
                                  }
                                }
                                """);
                    }
                });
        server.createContext("/open-apis/contact/v3/users/batch_get_id", exchange ->
                respond(exchange, """
                        {"code":0,"msg":"success","data":{"user_list":[{"user_id":"teacher-user-id","email":"teacher@example.com"}]}}
                        """));
        server.start();

        LarkProperties properties = new LarkProperties();
        properties.setEnabled(true);
        properties.setAppId("app-id");
        properties.setAppSecret("app-secret");
        properties.setCalendarId("calendar-test");
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/open-apis");

        LarkMeetingServiceImpl service = new LarkMeetingServiceImpl(properties);
        ClassroomSession session = buildVirtualSession();

        service.syncMeeting(session);

        assertThat(session.getLarkCalendarId()).isEqualTo("calendar-test");
        assertThat(session.getLarkEventId()).isEqualTo("event-test");
        assertThat(session.getLarkMeetingUrl()).isEqualTo("https://meet.larksuite.com/s/real-meeting");
        assertThat(session.getLarkMeetingStatus()).isEqualTo(LarkMeetingStatus.SCHEDULED);
        assertThat(session.getLarkSyncStatus()).isEqualTo("SYNCED");
        assertThat(session.getLarkSyncError()).isNull();
        assertThat(session.getLarkSyncedAt()).isNotNull();

        service.inviteAttendee(session, "learner@englishlab.test");
    }

    @Test
    void rejectsOldDemoLinks() {
        LarkMeetingServiceImpl service = new LarkMeetingServiceImpl(new LarkProperties());

        assertThat(service.isDemoUrl("https://meet.larksuite.com/demo/ielts-speaking-live")).isTrue();
        assertThat(service.resolveStatus("https://meet.larksuite.com/demo/ielts-speaking-live"))
                .isEqualTo(LarkMeetingStatus.NOT_CREATED);
        assertThat(service.isJoinable(
                "https://meet.larksuite.com/demo/ielts-speaking-live",
                LarkMeetingStatus.OPEN
        )).isFalse();
    }

    private ClassroomSession buildVirtualSession() {
        LearningPackage learningPackage = LearningPackage.builder()
                .title("IELTS Speaking Live")
                .build();
        ClassroomOffering offering = ClassroomOffering.builder()
                .learningPackage(learningPackage)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .build();
        User teacher = User.builder()
                .fullName("Giảng viên EnglishLab")
                .email("teacher@englishlab.test")
                .build();

        return ClassroomSession.builder()
                .classroomOffering(offering)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .teacher(teacher)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .sessionContent("Speaking practice")
                .build();
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
