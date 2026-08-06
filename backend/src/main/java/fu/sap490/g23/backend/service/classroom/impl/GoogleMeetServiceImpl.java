package fu.sap490.g23.backend.service.classroom.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sap490.g23.backend.service.classroom.GoogleMeetProperties;
import fu.sap490.g23.backend.service.classroom.TeacherGoogleMeetConnectionService;
import fu.sap490.g23.backend.service.classroom.VirtualMeetingService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class GoogleMeetServiceImpl implements VirtualMeetingService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String OPEN_SPACE_PAYLOAD = "{\"config\":{\"accessType\":\"OPEN\"}}";

    private final GoogleMeetProperties properties;
    private final TeacherGoogleMeetConnectionService connectionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ConcurrentMap<Long, CachedAccessToken> accessTokens = new ConcurrentHashMap<>();

    @Override
    public String getPlatformName() {
        return "Google Meet";
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public LarkMeetingStatus resolveStatus(String meetingUrl) {
        if (meetingUrl == null || meetingUrl.isBlank() || isLegacyOrPlaceholderUrl(meetingUrl)) {
            return LarkMeetingStatus.NOT_CREATED;
        }
        return LarkMeetingStatus.SCHEDULED;
    }

    @Override
    public boolean isJoinable(String meetingUrl, LarkMeetingStatus status) {
        return isGoogleMeetUrl(meetingUrl)
                && (status == LarkMeetingStatus.OPEN || status == LarkMeetingStatus.IN_PROGRESS);
    }

    @Override
    public boolean isLegacyOrPlaceholderUrl(String meetingUrl) {
        if (meetingUrl == null || meetingUrl.isBlank()) return false;
        String normalized = meetingUrl.trim().toLowerCase();
        return normalized.contains("meet.larksuite.com")
                || normalized.contains("englishlab-")
                || !isGoogleMeetUrl(normalized);
    }

    @Override
    public void syncMeeting(ClassroomSession session) {
        validateConfiguration();
        if (isGoogleMeetUrl(session.getLarkMeetingUrl())
                && session.getLarkMeetingId() != null
                && session.getLarkMeetingId().startsWith("spaces/")) {
            markSynced(session);
            return;
        }

        User teacher = requireSessionTeacher(session);
        String refreshToken = connectionService.requireRefreshToken(teacher);
        JsonNode space = sendMeetRequest("POST", "/spaces", OPEN_SPACE_PAYLOAD, teacher, refreshToken);
        String resourceName = space.path("name").asText("");
        String meetingUri = space.path("meetingUri").asText("");
        String meetingCode = space.path("meetingCode").asText("");
        if (resourceName.isBlank() || !isGoogleMeetUrl(meetingUri)) {
            throw new RuntimeException("Google Meet không trả về đầy đủ mã phòng và liên kết tham gia.");
        }

        // Keep legacy column names until a dedicated database migration is scheduled.
        session.setLarkMeetingId(resourceName);
        session.setLarkMeetingNo(meetingCode);
        session.setLarkMeetingUrl(meetingUri);
        markSynced(session);
    }

    @Override
    public void inviteInternalAttendee(ClassroomSession session, String email) {
        // The space uses OPEN access. Invitations remain separate from room creation.
    }

    @Override
    public void deleteMeeting(ClassroomSession session) {
        if (!properties.isEnabled()
                || session.getLarkMeetingId() == null
                || !session.getLarkMeetingId().startsWith("spaces/")) {
            return;
        }
        User teacher = requireSessionTeacher(session);
        String refreshToken = connectionService.requireRefreshToken(teacher);
        sendMeetRequest("POST", "/" + session.getLarkMeetingId() + ":endActiveConference", "{}", teacher, refreshToken);
    }

    private User requireSessionTeacher(ClassroomSession session) {
        User teacher = session.getTeacher();
        if (teacher == null && session.getClassroomOffering() != null) {
            teacher = session.getClassroomOffering().getPrimaryTeacher();
        }
        if (teacher == null) {
            throw new IllegalStateException("Buổi học chưa có giáo viên phụ trách.");
        }
        return teacher;
    }

    private void markSynced(ClassroomSession session) {
        session.setLarkMeetingStatus(LarkMeetingStatus.SCHEDULED);
        session.setLarkSyncStatus("SYNCED");
        session.setLarkSyncError(null);
        session.setLarkSyncedAt(LocalDateTime.now());
    }

    private boolean isGoogleMeetUrl(String meetingUrl) {
        if (meetingUrl == null || meetingUrl.isBlank()) return false;
        try {
            URI uri = URI.create(meetingUrl.trim());
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && (host.equalsIgnoreCase("meet.google.com")
                    || host.toLowerCase().endsWith(".meet.google.com"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private JsonNode sendMeetRequest(
            String method,
            String path,
            String body,
            User teacher,
            String refreshToken
    ) {
        try {
            String token = getAccessToken(teacher, refreshToken);
            HttpResponse<String> response = sendAuthorizedRequest(method, path, body, token);
            if (response.statusCode() == 401) {
                accessTokens.remove(teacher.getId());
                response = sendAuthorizedRequest(method, path, body, getAccessToken(teacher, refreshToken));
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError("Google Meet", response);
            }
            return response.body() == null || response.body().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Yêu cầu Google Meet đã bị gián đoạn.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new RuntimeException("Không thể kết nối Google Meet: " + exception.getMessage(), exception);
        }
    }

    private HttpResponse<String> sendAuthorizedRequest(
            String method,
            String path,
            String body,
            String token
    ) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(properties.getApiBaseUrl() + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=UTF-8");
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            throw new IllegalArgumentException("Phương thức Google Meet không được hỗ trợ: " + method);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String getAccessToken(User teacher, String refreshToken) {
        CachedAccessToken cached = accessTokens.get(teacher.getId());
        if (cached != null && Instant.now().isBefore(cached.expiresAt().minusSeconds(60))) {
            return cached.value();
        }
        JsonNode tokenResponse = requestAccessToken(teacher, refreshToken);
        String token = tokenResponse.path("access_token").asText("");
        long expiresIn = tokenResponse.path("expires_in").asLong(3600);
        if (token.isBlank()) {
            throw new RuntimeException("Google OAuth không trả về access token.");
        }
        accessTokens.put(teacher.getId(), new CachedAccessToken(
                token,
                Instant.now().plusSeconds(Math.max(expiresIn, 120))
        ));
        return token;
    }

    private JsonNode requestAccessToken(User teacher, String refreshToken) {
        String form = "client_id=" + encode(properties.getClientId())
                + "&client_secret=" + encode(properties.getClientSecret())
                + "&refresh_token=" + encode(refreshToken)
                + "&grant_type=refresh_token";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getTokenUri()))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (isInvalidGrant(response.body())) {
                    accessTokens.remove(teacher.getId());
                    connectionService.markReauthenticationRequired(teacher);
                    throw new RuntimeException(
                            "Quyền Google Meet của giáo viên đã hết hạn hoặc bị thu hồi. Giáo viên cần kết nối lại Google."
                    );
                }
                throw providerError("Google OAuth", response);
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Yêu cầu Google OAuth đã bị gián đoạn.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new RuntimeException("Không thể xác thực Google OAuth: " + exception.getMessage(), exception);
        }
    }

    private boolean isInvalidGrant(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return false;
        try {
            return "invalid_grant".equals(objectMapper.readTree(responseBody).path("error").asText());
        } catch (IOException exception) {
            return false;
        }
    }

    private RuntimeException providerError(String provider, HttpResponse<String> response) {
        String detail = response.body() == null ? "" : response.body();
        if (detail.length() > 500) detail = detail.substring(0, 500);
        return new RuntimeException(
                provider + " trả về lỗi HTTP " + response.statusCode()
                        + (detail.isBlank() ? "." : ": " + detail)
        );
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new RuntimeException("Tích hợp Google Meet chưa được bật.");
        }
        if (isBlank(properties.getClientId()) || isBlank(properties.getClientSecret())) {
            throw new RuntimeException("Thiếu GOOGLE_MEET_CLIENT_ID hoặc GOOGLE_MEET_CLIENT_SECRET.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record CachedAccessToken(String value, Instant expiresAt) {
    }
}
