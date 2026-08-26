package fu.sep490.g23.backend.service.classroom.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.GoogleMeetStatus;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.service.classroom.GoogleMeetProperties;
import fu.sep490.g23.backend.service.classroom.TeacherGoogleMeetConnectionService;
import fu.sep490.g23.backend.service.classroom.VirtualMeetingRecordingInfo;
import fu.sep490.g23.backend.service.classroom.VirtualMeetingService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class GoogleMeetServiceImpl implements VirtualMeetingService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final GoogleMeetProperties properties;
    private final TeacherGoogleMeetConnectionService connectionService;
    private final ClassSectionRepository classSectionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ConcurrentMap<Long, CachedAccessToken> accessTokens = new ConcurrentHashMap<>();

    public GoogleMeetServiceImpl(
            GoogleMeetProperties properties,
            TeacherGoogleMeetConnectionService connectionService,
            ClassSectionRepository classSectionRepository
    ) {
        this.properties = properties;
        this.connectionService = connectionService;
        this.classSectionRepository = classSectionRepository;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public boolean isGoogleMeetUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host != null
                    && (host.equalsIgnoreCase("meet.google.com")
                    || host.toLowerCase().endsWith(".meet.google.com"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public boolean isJoinable(ClassSection classSection) {
        return classSection != null
                && classSection.getGoogleMeetStatus() == GoogleMeetStatus.READY
                && isGoogleMeetUrl(classSection.getGoogleMeetUrl());
    }

    @Override
    public void syncMeeting(ClassSchedule schedule) {
        validateConfiguration();
        ClassSection classSection = requireClassSection(schedule);
        if (isJoinable(classSection)
                && classSection.getGoogleMeetSpaceName() != null
                && classSection.getGoogleMeetSpaceName().startsWith("spaces/")) {
            restrictExistingSpace(classSection);
            return;
        }

        User owner = requireMeetingOwner(classSection, schedule);
        classSection.setGoogleMeetOwner(owner);
        classSection.setGoogleMeetStatus(GoogleMeetStatus.CREATING);
        classSection.setGoogleMeetSyncError(null);
        classSectionRepository.save(classSection);

        String refreshToken = connectionService.requireRefreshToken(owner);
        JsonNode space;
        try {
            space = sendMeetRequest("POST", "/spaces", spaceConfigPayload(properties.isAutoRecording()), owner, refreshToken);
        } catch (RuntimeException exception) {
            if (!properties.isAutoRecording() || !isAutoRecordingUnavailable(exception)) throw exception;
            space = sendMeetRequest("POST", "/spaces", restrictedAccessPayload(), owner, refreshToken);
        }
        ensureRestrictedAccess(space, owner, refreshToken);

        String resourceName = space.path("name").asText("");
        String meetingUrl = space.path("meetingUri").asText("");
        if (resourceName.isBlank() || !isGoogleMeetUrl(meetingUrl)) {
            classSection.setGoogleMeetStatus(GoogleMeetStatus.FAILED);
            classSection.setGoogleMeetSyncError("Google Meet không trả về đầy đủ thông tin phòng học.");
            classSectionRepository.save(classSection);
            throw new IllegalStateException(classSection.getGoogleMeetSyncError());
        }

        classSection.setGoogleMeetSpaceName(resourceName);
        classSection.setGoogleMeetUrl(meetingUrl);
        classSection.setGoogleMeetStatus(GoogleMeetStatus.READY);
        classSection.setGoogleMeetSyncError(null);
        classSectionRepository.save(classSection);
    }

    @Override
    public VirtualMeetingRecordingInfo getRecording(ClassSchedule schedule) {
        validateConfiguration();
        ClassSection classSection = requireClassSection(schedule);
        String spaceName = classSection.getGoogleMeetSpaceName();
        if (spaceName == null || !spaceName.startsWith("spaces/")) {
            throw new IllegalStateException("Lớp học chưa có Google Meet space hợp lệ.");
        }
        User owner = requireMeetingOwner(classSection, schedule);
        String refreshToken = connectionService.requireRefreshToken(owner);
        String filter = "space.name = \"" + spaceName + "\"";
        JsonNode conferences = sendMeetRequest(
                "GET", "/conferenceRecords?pageSize=100&filter=" + encode(filter), null, owner, refreshToken);
        JsonNode conference = findConferenceForSchedule(conferences, schedule);
        String conferenceName = conference.path("name").asText("");
        if (conferenceName.isBlank()) {
            throw new IllegalStateException("Google Meet chưa có bản ghi hội nghị cho buổi học này.");
        }
        JsonNode recordings = sendMeetRequest(
                "GET", "/" + conferenceName + "/recordings?pageSize=10", null, owner, refreshToken);
        for (JsonNode recording : recordings.path("recordings")) {
            if (!"FILE_GENERATED".equals(recording.path("state").asText())) continue;
            String url = recording.path("driveDestination").path("exportUri").asText("");
            if (!url.isBlank()) return new VirtualMeetingRecordingInfo(url, null);
        }
        throw new IllegalStateException("Google Meet đang xử lý file ghi hình.");
    }

    @Override
    public void inviteInternalAttendee(ClassSchedule schedule, String email) {
        // Người học tham gia bằng URL của lớp; quyền duyệt người vào thuộc chủ phòng Google Meet.
    }

    @Override
    public void deleteMeeting(ClassSchedule schedule) {
        ClassSection classSection = requireClassSection(schedule);
        String spaceName = classSection.getGoogleMeetSpaceName();
        if (!properties.isEnabled() || spaceName == null || !spaceName.startsWith("spaces/")) return;
        User owner = requireMeetingOwner(classSection, schedule);
        sendMeetRequest("POST", "/" + spaceName + ":endActiveConference", "{}", owner,
                connectionService.requireRefreshToken(owner));
        classSection.setGoogleMeetStatus(GoogleMeetStatus.NOT_CREATED);
        classSection.setGoogleMeetSpaceName(null);
        classSection.setGoogleMeetUrl(null);
        classSection.setGoogleMeetSyncError(null);
        classSectionRepository.save(classSection);
    }

    private ClassSection requireClassSection(ClassSchedule schedule) {
        if (schedule == null || schedule.getClassSection() == null) {
            throw new IllegalArgumentException("Buổi học chưa thuộc lớp học.");
        }
        return schedule.getClassSection();
    }

    private User requireMeetingOwner(ClassSection classSection, ClassSchedule schedule) {
        User owner = classSection.getGoogleMeetOwner();
        if (owner == null) owner = classSection.getPrimaryTeacher();
        if (owner == null) owner = schedule.getEffectiveTeacher();
        if (owner == null) {
            throw new IllegalStateException("Lớp học chưa có giáo viên phụ trách để tạo phòng Google Meet.");
        }
        return owner;
    }

    private JsonNode findConferenceForSchedule(JsonNode conferences, ClassSchedule schedule) {
        JsonNode records = conferences.path("conferenceRecords");
        if (!records.isArray() || records.isEmpty()) return objectMapper.createObjectNode();
        if (schedule.getSessionDate() == null || schedule.getStartTime() == null) return records.path(0);
        LocalDateTime expected = LocalDateTime.of(schedule.getSessionDate(), schedule.getStartTime());
        JsonNode closest = null;
        long closestMinutes = Long.MAX_VALUE;
        for (JsonNode record : records) {
            String value = record.path("startTime").asText("");
            if (value.isBlank()) continue;
            try {
                LocalDateTime actual = LocalDateTime.ofInstant(Instant.parse(value), ZoneId.of("Asia/Ho_Chi_Minh"));
                if (!actual.toLocalDate().equals(schedule.getSessionDate())) continue;
                long minutes = Math.abs(Duration.between(expected, actual).toMinutes());
                if (minutes < closestMinutes) {
                    closest = record;
                    closestMinutes = minutes;
                }
            } catch (RuntimeException ignored) {
                // Ignore malformed provider records and continue matching valid records.
            }
        }
        return closest == null ? objectMapper.createObjectNode() : closest;
    }

    private JsonNode sendMeetRequest(String method, String path, String body, User teacher, String refreshToken) {
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
                    ? objectMapper.createObjectNode() : objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Yêu cầu Google Meet đã bị gián đoạn.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new RuntimeException("Không thể kết nối Google Meet: " + exception.getMessage(), exception);
        }
    }

    private HttpResponse<String> sendAuthorizedRequest(String method, String path, String body, String token)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(properties.getApiBaseUrl() + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=UTF-8");
        switch (method) {
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            case "GET" -> builder.GET();
            default -> throw new IllegalArgumentException("Phương thức Google Meet không được hỗ trợ: " + method);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String getAccessToken(User teacher, String refreshToken) {
        CachedAccessToken cached = accessTokens.get(teacher.getId());
        if (cached != null && Instant.now().isBefore(cached.expiresAt().minusSeconds(60))) return cached.value();
        JsonNode response = requestAccessToken(teacher, refreshToken);
        String token = response.path("access_token").asText("");
        if (token.isBlank()) throw new RuntimeException("Google OAuth không trả về access token.");
        accessTokens.put(teacher.getId(), new CachedAccessToken(
                token, Instant.now().plusSeconds(Math.max(response.path("expires_in").asLong(3600), 120))));
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
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.body() != null && response.body().contains("invalid_grant")) {
                    accessTokens.remove(teacher.getId());
                    connectionService.markReauthenticationRequired(teacher);
                }
                throw providerError("Google OAuth", response);
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Yêu cầu Google OAuth đã bị gián đoạn.", exception);
        } catch (IOException exception) {
            throw new RuntimeException("Không thể xác thực Google OAuth: " + exception.getMessage(), exception);
        }
    }

    private void restrictExistingSpace(ClassSection classSection) {
        User owner = classSection.getGoogleMeetOwner() != null
                ? classSection.getGoogleMeetOwner() : classSection.getPrimaryTeacher();
        if (owner == null) throw new IllegalStateException("Lớp học chưa có chủ phòng Google Meet.");
        String refreshToken = connectionService.requireRefreshToken(owner);
        JsonNode space = sendMeetRequest(
                "PATCH", "/" + classSection.getGoogleMeetSpaceName() + "?updateMask=config.accessType",
                restrictedAccessPayload(), owner, refreshToken);
        ensureRestrictedAccess(space, owner, refreshToken);
    }

    private void ensureRestrictedAccess(JsonNode space, User owner, String refreshToken) {
        JsonNode verified = space;
        String name = space.path("name").asText("");
        if (verified.path("config").path("accessType").asText("").isBlank() && !name.isBlank()) {
            verified = sendMeetRequest("GET", "/" + name, null, owner, refreshToken);
        }
        if (!"RESTRICTED".equals(verified.path("config").path("accessType").asText(""))) {
            throw new IllegalStateException(
                    "Google không áp dụng chế độ RESTRICTED. Hãy dùng tài khoản Google Workspace của giáo viên.");
        }
    }

    private RuntimeException providerError(String provider, HttpResponse<String> response) {
        String detail = response.body() == null ? "" : response.body();
        if (detail.length() > 500) detail = detail.substring(0, 500);
        return new RuntimeException(provider + " trả về lỗi HTTP " + response.statusCode()
                + (detail.isBlank() ? "." : ": " + detail));
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) throw new IllegalStateException("Tích hợp Google Meet chưa được bật.");
        if (properties.getClientId() == null || properties.getClientId().isBlank()
                || properties.getClientSecret() == null || properties.getClientSecret().isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình Google Meet OAuth.");
        }
    }

    private boolean isAutoRecordingUnavailable(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && (message.contains("FEATURE_UNAVAILABLE_TO_USER")
                || message.contains("updateAutoRecordingGeneration"));
    }

    private String spaceConfigPayload(boolean autoRecording) {
        return autoRecording
                ? "{\"config\":{\"accessType\":\"RESTRICTED\",\"artifactConfig\":{\"recordingConfig\":{\"autoRecordingGeneration\":\"ON\"}}}}"
                : restrictedAccessPayload();
    }

    private String restrictedAccessPayload() {
        return "{\"config\":{\"accessType\":\"RESTRICTED\"}}";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record CachedAccessToken(String value, Instant expiresAt) {
    }
}
