package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LarkMeetingServiceImpl implements LarkMeetingService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final LarkProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private volatile String tenantAccessToken;
    private volatile Instant tenantAccessTokenExpiresAt = Instant.EPOCH;
    private volatile String resolvedCalendarId;

    @Override
    public String getPlatformName() {
        return "Lark";
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public LarkMeetingStatus resolveStatus(String meetingUrl) {
        if (meetingUrl == null || meetingUrl.isBlank() || isDemoUrl(meetingUrl)) {
            return LarkMeetingStatus.NOT_CREATED;
        }
        return LarkMeetingStatus.SCHEDULED;
    }

    @Override
    public boolean isJoinable(String meetingUrl, LarkMeetingStatus status) {
        return meetingUrl != null && !meetingUrl.isBlank()
                && !isDemoUrl(meetingUrl)
                && (status == LarkMeetingStatus.OPEN || status == LarkMeetingStatus.IN_PROGRESS);
    }

    @Override
    public boolean isDemoUrl(String meetingUrl) {
        return meetingUrl != null && meetingUrl.contains("meet.larksuite.com/demo/");
    }

    @Override
    public void syncMeeting(ClassroomSession session) {
        validateConfiguration();

        String calendarId = firstNonBlank(session.getLarkCalendarId(), ensureCalendarId());
        JsonNode event;
        if (session.getLarkEventId() == null || session.getLarkEventId().isBlank()) {
            event = createEvent(calendarId, session);
        } else {
            event = updateEvent(calendarId, session.getLarkEventId(), session);
        }

        String eventId = textAt(event, "event_id");
        if (eventId.isBlank()) {
            eventId = session.getLarkEventId();
        }
        if (eventId == null || eventId.isBlank()) {
            throw new RuntimeException("Lark không trả về mã sự kiện cho buổi học.");
        }

        String meetingUrl = textAt(event, "vchat", "meeting_url");
        if (meetingUrl.isBlank()) {
            JsonNode refreshedEvent = getEvent(calendarId, eventId);
            meetingUrl = textAt(refreshedEvent, "vchat", "meeting_url");
        }

        session.setLarkCalendarId(calendarId);
        session.setLarkEventId(eventId);
        session.setLarkSyncedAt(LocalDateTime.now());

        if (meetingUrl.isBlank()) {
            session.setLarkSyncStatus("FAILED");
            session.setLarkSyncError("Lark đã tạo lịch nhưng chưa trả về đường dẫn phòng họp. Kiểm tra quyền Calendar và Video Conferencing của ứng dụng.");
            throw new RuntimeException(session.getLarkSyncError());
        }

        session.setLarkMeetingUrl(meetingUrl);
        session.setLarkMeetingNo(extractMeetingNo(meetingUrl));
        session.setLarkMeetingStatus(LarkMeetingStatus.SCHEDULED);
        session.setLarkSyncStatus("SYNCED");
        session.setLarkSyncError(null);
    }

    @Override
    public void deleteMeeting(ClassroomSession session) {
        if (!properties.isEnabled()
                || session.getLarkCalendarId() == null
                || session.getLarkEventId() == null) {
            return;
        }

        send(
                "DELETE",
                "/calendar/v4/calendars/%s/events/%s?need_notification=false".formatted(
                        encodePath(session.getLarkCalendarId()),
                        encodePath(session.getLarkEventId())
                ),
                null
        );
    }

    @Override
    public void inviteAttendee(ClassroomSession session, String email) {
        if (email == null || email.isBlank()
                || session.getLarkCalendarId() == null
                || session.getLarkEventId() == null) {
            return;
        }

        send(
                "POST",
                "/calendar/v4/calendars/%s/events/%s/attendees?need_notification=false".formatted(
                        encodePath(session.getLarkCalendarId()),
                        encodePath(session.getLarkEventId())
                ),
                Map.of("attendees", java.util.List.of(Map.of(
                        "type", "third_party",
                        "third_party_email", email.trim()
                )))
        );
    }

    @Override
    public void inviteInternalAttendee(ClassroomSession session, String email) {
        if (email == null || email.isBlank()
                || session.getLarkCalendarId() == null
                || session.getLarkEventId() == null) {
            return;
        }

        String userId = resolveInternalUserId(email.trim());
        if (userId.isBlank()) {
            throw new RuntimeException(
                    "Không tìm thấy tài khoản Lark nội bộ cho email " + email
                            + ". Kiểm tra tài khoản đã vào đúng tổ chức và cấp quyền "
                            + "contact:user.id:readonly cùng contact:user.employee_id:readonly cho ứng dụng Lark."
            );
        }

        send(
                "POST",
                "/calendar/v4/calendars/%s/events/%s/attendees?need_notification=false&user_id_type=user_id".formatted(
                        encodePath(session.getLarkCalendarId()),
                        encodePath(session.getLarkEventId())
                ),
                Map.of("attendees", java.util.List.of(Map.of(
                        "type", "user",
                        "user_id", userId
                )))
        );
    }

    private String resolveInternalUserId(String email) {
        JsonNode root = send(
                "POST",
                "/contact/v3/users/batch_get_id?user_id_type=user_id",
                Map.of("emails", java.util.List.of(email))
        );
        JsonNode users = root.path("data").path("user_list");
        if (!users.isArray() || users.isEmpty()) {
            return "";
        }
        return textAt(users.get(0), "user_id");
    }

    private JsonNode createEvent(String calendarId, ClassroomSession session) {
        JsonNode root = send(
                "POST",
                "/calendar/v4/calendars/%s/events".formatted(encodePath(calendarId)),
                buildEventBody(session)
        );
        return root.path("data").path("event");
    }

    private JsonNode updateEvent(String calendarId, String eventId, ClassroomSession session) {
        JsonNode root = send(
                "PATCH",
                "/calendar/v4/calendars/%s/events/%s".formatted(
                        encodePath(calendarId),
                        encodePath(eventId)
                ),
                buildEventBody(session)
        );
        return root.path("data").path("event");
    }

    private JsonNode getEvent(String calendarId, String eventId) {
        JsonNode root = send(
                "GET",
                "/calendar/v4/calendars/%s/events/%s".formatted(
                        encodePath(calendarId),
                        encodePath(eventId)
                ),
                null
        );
        return root.path("data").path("event");
    }

    private Map<String, Object> buildEventBody(ClassroomSession session) {
        ZoneId zoneId = ZoneId.of(properties.getTimezone());
        long startTimestamp = session.getStartDateTime().atZone(zoneId).toEpochSecond();
        long endTimestamp = session.getEndDateTime().atZone(zoneId).toEpochSecond();

        Map<String, Object> startTime = Map.of(
                "timestamp", String.valueOf(startTimestamp),
                "timezone", properties.getTimezone()
        );
        Map<String, Object> endTime = Map.of(
                "timestamp", String.valueOf(endTimestamp),
                "timezone", properties.getTimezone()
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", session.getClassroomOffering().getLearningPackage().getTitle());
        body.put("description", buildDescription(session));
        body.put("start_time", startTime);
        body.put("end_time", endTime);
        body.put("visibility", "default");
        body.put("free_busy_status", "busy");
        body.put("vchat", Map.of(
                "vc_type", "vc",
                "meeting_settings", Map.of(
                        "join_meeting_permission", "anyone_can_join",
                        "open_lobby", false,
                        "allow_attendees_start", true
                )
        ));
        return body;
    }

    private String buildDescription(ClassroomSession session) {
        String teacherName = session.getTeacher() == null ? "Chưa phân công" : session.getTeacher().getFullName();
        return """
                Buổi học trực tuyến của EnglishLab.
                Giảng viên: %s
                Nội dung: %s
                """.formatted(
                teacherName,
                firstNonBlank(session.getSessionContent(), "Đang cập nhật")
        ).trim();
    }

    private String ensureCalendarId() {
        if (properties.getCalendarId() != null && !properties.getCalendarId().isBlank()) {
            return properties.getCalendarId().trim();
        }
        if (resolvedCalendarId != null && !resolvedCalendarId.isBlank()) {
            return resolvedCalendarId;
        }

        synchronized (this) {
            if (resolvedCalendarId != null && !resolvedCalendarId.isBlank()) {
                return resolvedCalendarId;
            }

            JsonNode listRoot = send("GET", "/calendar/v4/calendars", null);
            JsonNode calendars = listRoot.path("data").path("calendar_list");
            if (!calendars.isArray()) {
                calendars = listRoot.path("data").path("items");
            }
            if (calendars.isArray()) {
                for (JsonNode calendar : calendars) {
                    if (properties.getCalendarName().equalsIgnoreCase(textAt(calendar, "summary"))) {
                        resolvedCalendarId = textAt(calendar, "calendar_id");
                        if (!resolvedCalendarId.isBlank()) {
                            return resolvedCalendarId;
                        }
                    }
                }
            }

            JsonNode createRoot = send("POST", "/calendar/v4/calendars", Map.of(
                    "summary", properties.getCalendarName(),
                    "description", "Lịch các buổi học trực tuyến được đồng bộ từ EnglishLab.",
                    "permissions", "private"
            ));
            resolvedCalendarId = textAt(createRoot.path("data").path("calendar"), "calendar_id");
            if (resolvedCalendarId.isBlank()) {
                throw new RuntimeException("Lark không trả về calendar_id khi tạo lịch EnglishLab.");
            }
            return resolvedCalendarId;
        }
    }

    private JsonNode send(String method, String path, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(properties.getBaseUrl() + path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + getTenantAccessToken())
                    .header("Content-Type", "application/json; charset=UTF-8");

            String jsonBody = body == null ? null : objectMapper.writeValueAsString(body);
            switch (method) {
                case "GET" -> builder.GET();
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody));
                case "DELETE" -> builder.DELETE();
                default -> throw new IllegalArgumentException("Phương thức Lark API không được hỗ trợ: " + method);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = parseJson(response.body());
            int code = root.path("code").asInt(response.statusCode() >= 400 ? response.statusCode() : 0);
            if (response.statusCode() < 200 || response.statusCode() >= 300 || code != 0) {
                String message = firstNonBlank(root.path("msg").asText(), response.body());
                throw new RuntimeException("Lark API lỗi (%s): %s".formatted(code, message));
            }
            return root;
        } catch (IOException ex) {
            throw new RuntimeException("Không thể kết nối Lark API: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Yêu cầu Lark API đã bị gián đoạn.", ex);
        }
    }

    private String getTenantAccessToken() {
        if (tenantAccessToken != null && Instant.now().isBefore(tenantAccessTokenExpiresAt)) {
            return tenantAccessToken;
        }

        synchronized (this) {
            if (tenantAccessToken != null && Instant.now().isBefore(tenantAccessTokenExpiresAt)) {
                return tenantAccessToken;
            }

            try {
                String body = objectMapper.writeValueAsString(Map.of(
                        "app_id", properties.getAppId(),
                        "app_secret", properties.getAppSecret()
                ));
                HttpRequest request = HttpRequest.newBuilder(
                                URI.create(properties.getBaseUrl() + "/auth/v3/tenant_access_token/internal"))
                        .timeout(REQUEST_TIMEOUT)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = parseJson(response.body());
                if (response.statusCode() < 200 || response.statusCode() >= 300 || root.path("code").asInt(-1) != 0) {
                    throw new RuntimeException("Không lấy được access token Lark: " + root.path("msg").asText(response.body()));
                }

                tenantAccessToken = root.path("tenant_access_token").asText();
                long expiresIn = root.path("expire").asLong(7200);
                tenantAccessTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 300));
                return tenantAccessToken;
            } catch (IOException ex) {
                throw new RuntimeException("Không thể xác thực với Lark: " + ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Yêu cầu xác thực Lark đã bị gián đoạn.", ex);
            }
        }
    }

    private JsonNode parseJson(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new RuntimeException("Tích hợp Lark API chưa được bật.");
        }
        if (properties.getAppId() == null || properties.getAppId().isBlank()
                || properties.getAppSecret() == null || properties.getAppSecret().isBlank()) {
            throw new RuntimeException("Thiếu LARK_APP_ID hoặc LARK_APP_SECRET.");
        }
    }

    private String textAt(JsonNode node, String... fields) {
        JsonNode current = node;
        for (String field : fields) {
            current = current.path(field);
        }
        return current.isMissingNode() || current.isNull() ? "" : current.asText("");
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String extractMeetingNo(String meetingUrl) {
        if (meetingUrl == null || meetingUrl.isBlank()) {
            return null;
        }
        String normalized = meetingUrl.split("[?#]", 2)[0];
        int slashIndex = normalized.lastIndexOf('/');
        String candidate = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        return candidate.matches("\\d{9}") ? candidate : null;
    }
}
