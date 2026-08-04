package fu.sap490.g23.backend.service.classroom.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sap490.g23.backend.service.classroom.GoogleMeetProperties;
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

@Service
@RequiredArgsConstructor
public class GoogleMeetServiceImpl implements VirtualMeetingService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final GoogleMeetProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

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
        if (meetingUrl == null || meetingUrl.isBlank()) {
            return false;
        }
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

        JsonNode space = sendMeetRequest("POST", "/spaces", "{}");
        String resourceName = space.path("name").asText("");
        String meetingUri = space.path("meetingUri").asText("");
        String meetingCode = space.path("meetingCode").asText("");

        if (resourceName.isBlank() || meetingUri.isBlank()) {
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
        // A Meet space is joined by URL. Calendar invitations are intentionally
        // separate from room creation and are not required for classroom access.
    }

    @Override
    public void deleteMeeting(ClassroomSession session) {
        if (!properties.isEnabled()
                || session.getLarkMeetingId() == null
                || !session.getLarkMeetingId().startsWith("spaces/")) {
            return;
        }
        sendMeetRequest("POST", "/" + session.getLarkMeetingId() + ":endActiveConference", "{}");
    }

    private void markSynced(ClassroomSession session) {
        session.setLarkMeetingStatus(LarkMeetingStatus.SCHEDULED);
        session.setLarkSyncStatus("SYNCED");
        session.setLarkSyncError(null);
        session.setLarkSyncedAt(java.time.LocalDateTime.now());
    }

    private boolean isGoogleMeetUrl(String meetingUrl) {
        if (meetingUrl == null || meetingUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(meetingUrl.trim());
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && (host.equalsIgnoreCase("meet.google.com")
                    || host.toLowerCase().endsWith(".meet.google.com"));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private JsonNode sendMeetRequest(String method, String path, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(properties.getApiBaseUrl() + path))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + getAccessToken())
                    .header("Content-Type", "application/json; charset=UTF-8");
            if ("POST".equals(method)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(body));
            } else {
                throw new IllegalArgumentException("Phương thức Google Meet không được hỗ trợ: " + method);
            }

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError("Google Meet", response);
            }
            return response.body() == null || response.body().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Yêu cầu Google Meet đã bị gián đoạn.", ex);
        } catch (IOException | IllegalArgumentException ex) {
            throw new RuntimeException("Không thể kết nối Google Meet: " + ex.getMessage(), ex);
        }
    }

    private String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(60))) {
            return accessToken;
        }

        synchronized (this) {
            if (accessToken != null && Instant.now().isBefore(accessTokenExpiresAt.minusSeconds(60))) {
                return accessToken;
            }
            JsonNode tokenResponse = requestAccessToken();
            String token = tokenResponse.path("access_token").asText("");
            long expiresIn = tokenResponse.path("expires_in").asLong(3600);
            if (token.isBlank()) {
                throw new RuntimeException("Google OAuth không trả về access token.");
            }
            accessToken = token;
            accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(expiresIn, 120));
            return accessToken;
        }
    }

    private JsonNode requestAccessToken() {
        String form = "client_id=" + encode(properties.getClientId())
                + "&client_secret=" + encode(properties.getClientSecret())
                + "&refresh_token=" + encode(properties.getRefreshToken())
                + "&grant_type=refresh_token";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getTokenUri()))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError("Google OAuth", response);
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Yêu cầu Google OAuth đã bị gián đoạn.", ex);
        } catch (IOException | IllegalArgumentException ex) {
            throw new RuntimeException("Không thể xác thực Google OAuth: " + ex.getMessage(), ex);
        }
    }

    private RuntimeException providerError(String provider, HttpResponse<String> response) {
        String detail = response.body() == null ? "" : response.body();
        if (detail.length() > 500) {
            detail = detail.substring(0, 500);
        }
        return new RuntimeException(
                provider + " trả về lỗi HTTP " + response.statusCode()
                        + (detail.isBlank() ? "." : ": " + detail)
        );
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new RuntimeException("Tích hợp Google Meet chưa được bật.");
        }
        if (isBlank(properties.getClientId())
                || isBlank(properties.getClientSecret())
                || isBlank(properties.getRefreshToken())) {
            throw new RuntimeException(
                    "Thiếu GOOGLE_MEET_CLIENT_ID, GOOGLE_MEET_CLIENT_SECRET hoặc GOOGLE_MEET_REFRESH_TOKEN."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
