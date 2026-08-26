package fu.sep490.g23.backend.service.classroom.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.service.classroom.GoogleMeetProperties;
import fu.sep490.g23.backend.service.classroom.TeacherGoogleMeetConnectionService;
import fu.sep490.g23.backend.service.classroom.VirtualMeetingRecordingInfo;
import fu.sep490.g23.backend.service.classroom.VirtualMeetingService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ClassScheduleRepository sessionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ConcurrentMap<Long, CachedAccessToken> accessTokens = new ConcurrentHashMap<>();

    @Autowired
    public GoogleMeetServiceImpl(
            GoogleMeetProperties properties,
            TeacherGoogleMeetConnectionService connectionService,
            ClassScheduleRepository sessionRepository
    ) {
        this.properties = properties;
        this.connectionService = connectionService;
        this.sessionRepository = sessionRepository;
    }

    // Retained for focused provider tests that do not need classroom-level room reuse.
    public GoogleMeetServiceImpl(
            GoogleMeetProperties properties,
            TeacherGoogleMeetConnectionService connectionService
    ) {
        this(properties, connectionService, null);
    }

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
        // Google Meet links are ready as soon as a space is created (SCHEDULED).
        // OPEN/IN_PROGRESS cover live rooms; ENDED/NOT_CREATED remain non-joinable.
        return isGoogleMeetUrl(meetingUrl)
                && (status == LarkMeetingStatus.SCHEDULED
                || status == LarkMeetingStatus.OPEN
                || status == LarkMeetingStatus.IN_PROGRESS);
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
    public void syncMeeting(ClassSchedule session) {
        validateConfiguration();
        if (isGoogleMeetUrl(session.getLarkMeetingUrl())
                && session.getLarkMeetingId() != null
                && session.getLarkMeetingId().startsWith("spaces/")) {
            restrictExistingSpace(session);
            markSynced(session);
            propagateSharedRoom(session);
            return;
        }

        ClassSchedule sharedRoomSession = findSharedRoomSession(session);
        if (sharedRoomSession != null) {
            reuseSharedRoom(session, sharedRoomSession);
            return;
        }

        User meetingOwner = requireMeetingOwner(session);
        String refreshToken = connectionService.requireRefreshToken(meetingOwner);
        boolean autoRecordingUnavailable = false;
        JsonNode space;
        try {
            space = sendMeetRequest("POST", "/spaces", spaceConfigPayload(), meetingOwner, refreshToken);
        } catch (RuntimeException exception) {
            if (!properties.isAutoRecording() || !isAutoRecordingUnavailable(exception)) {
                throw exception;
            }
            autoRecordingUnavailable = true;
            space = sendMeetRequest("POST", "/spaces", spaceConfigPayload(false), meetingOwner, refreshToken);
        }
        ensureRestrictedAccess(space, meetingOwner, refreshToken);
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
        setClassroomDefaultRoom(session, meetingUri);
        configureRecording(session, autoRecordingUnavailable);
        markSynced(session);
        propagateSharedRoom(session);
    }

    @Override
    public VirtualMeetingRecordingInfo getRecording(ClassSchedule session) {
        validateConfiguration();
        if (session.getLarkMeetingId() == null || !session.getLarkMeetingId().startsWith("spaces/")) {
            throw new IllegalStateException("Buổi học chưa có Google Meet space hợp lệ.");
        }

        User meetingOwner = requireMeetingOwner(session);
        String refreshToken = connectionService.requireRefreshToken(meetingOwner);
        String filter = "space.name = \"" + session.getLarkMeetingId() + "\"";
        JsonNode conferences = sendMeetRequest(
                "GET",
                "/conferenceRecords?pageSize=100&filter=" + encode(filter),
                null,
                meetingOwner,
                refreshToken
        );
        JsonNode conference = findConferenceForSession(conferences, session);
        String conferenceName = conference.path("name").asText("");
        if (conferenceName.isBlank()) {
            throw new IllegalStateException("Google Meet chưa có bản ghi cho đúng ngày của buổi học này.");
        }

        JsonNode recordings = sendMeetRequest("GET", "/" + conferenceName + "/recordings?pageSize=10", null, meetingOwner, refreshToken);
        if (!recordings.path("recordings").isArray() || recordings.path("recordings").isEmpty()) {
            throw new IllegalStateException("Google Meet đang xử lý file recording.");
        }
        for (JsonNode recording : recordings.path("recordings")) {
            if (!"FILE_GENERATED".equals(recording.path("state").asText())) continue;
            String url = recording.path("driveDestination").path("exportUri").asText("");
            if (url.isBlank()) continue;
            return new VirtualMeetingRecordingInfo(url, recordingDurationMs(recording));
        }
        throw new IllegalStateException("Google Meet đang xử lý file recording.");
    }

    private JsonNode findConferenceForSession(JsonNode conferences, ClassSchedule session) {
        JsonNode records = conferences.path("conferenceRecords");
        if (!records.isArray() || records.isEmpty()) {
            return objectMapper.createObjectNode();
        }
        // Focused provider tests and legacy records may not have a schedule to match yet.
        if (session.getSessionDate() == null || session.getStartTime() == null) {
            return records.path(0);
        }

        LocalDateTime expectedStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        JsonNode closest = null;
        long closestDifference = Long.MAX_VALUE;
        for (JsonNode record : records) {
            LocalDateTime actualStart = conferenceStartTime(record);
            if (actualStart == null || !session.getSessionDate().equals(actualStart.toLocalDate())) {
                continue;
            }
            long difference = Math.abs(Duration.between(expectedStart, actualStart).toMinutes());
            if (difference < closestDifference) {
                closest = record;
                closestDifference = difference;
            }
        }
        return closest == null ? objectMapper.createObjectNode() : closest;
    }

    private LocalDateTime conferenceStartTime(JsonNode conference) {
        String value = conference.path("startTime").asText("");
        if (value.isBlank()) return null;
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.of("Asia/Ho_Chi_Minh"));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Override
    public void inviteInternalAttendee(ClassSchedule session, String email) {
        // Learners join via the meeting link and wait for the host to admit them.
    }

    @Override
    public void deleteMeeting(ClassSchedule session) {
        if (!properties.isEnabled()
                || session.getLarkMeetingId() == null
                || !session.getLarkMeetingId().startsWith("spaces/")) {
            return;
        }
        User meetingOwner = requireMeetingOwner(session);
        String refreshToken = connectionService.requireRefreshToken(meetingOwner);
        sendMeetRequest("POST", "/" + session.getLarkMeetingId() + ":endActiveConference", "{}", meetingOwner, refreshToken);
    }

    private User requireMeetingOwner(ClassSchedule session) {
        ClassSection offering = session.getClassSection();
        User teacher = offering == null ? null : offering.getPrimaryTeacher();
        if (teacher == null) teacher = session.getTeacher();
        if (teacher == null) {
            throw new IllegalStateException("Lớp học chưa có giáo viên phụ trách để tạo phòng Google Meet.");
        }
        return teacher;
    }

    private void markSynced(ClassSchedule session) {
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
            builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else if ("PATCH".equals(method)) {
            builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else if ("GET".equals(method)) {
            builder.GET();
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

    private ClassSchedule findSharedRoomSession(ClassSchedule session) {
        ClassSection offering = session.getClassSection();
        if (sessionRepository == null || offering == null || offering.getId() == null) {
            return null;
        }
        return sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId())
                .stream()
                .filter(candidate -> candidate.getId() == null || !candidate.getId().equals(session.getId()))
                .filter(candidate -> candidate.getLarkMeetingId() != null
                        && candidate.getLarkMeetingId().startsWith("spaces/"))
                .filter(candidate -> isGoogleMeetUrl(candidate.getLarkMeetingUrl()))
                .findFirst()
                .orElse(null);
    }

    private void reuseSharedRoom(ClassSchedule session, ClassSchedule sharedRoomSession) {
        session.setLarkMeetingId(sharedRoomSession.getLarkMeetingId());
        session.setLarkMeetingNo(sharedRoomSession.getLarkMeetingNo());
        session.setLarkMeetingUrl(sharedRoomSession.getLarkMeetingUrl());
        setClassroomDefaultRoom(session, sharedRoomSession.getLarkMeetingUrl());
        session.setRecordingProvider("GOOGLE_MEET");
        session.setRecordingSyncStatus(sharedRoomSession.getRecordingSyncStatus() == RecordingSyncStatus.SCHEDULED
                ? RecordingSyncStatus.SCHEDULED
                : RecordingSyncStatus.NOT_AVAILABLE);
        session.setRecordingSyncError(sharedRoomSession.getRecordingSyncError());
        markSynced(session);
        propagateSharedRoom(session);
    }

    /**
     * A classroom owns one Google Meet space. Keep every active virtual session
     * aligned with that space so staff never has to create a room per session.
     */
    private void propagateSharedRoom(ClassSchedule sourceSession) {
        ClassSection offering = sourceSession.getClassSection();
        if (sessionRepository == null || offering == null || offering.getId() == null) return;

        sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(offering.getId())
                .stream()
                .filter(candidate -> candidate.getDeliveryMode() == ClassroomDeliveryMode.VIRTUAL)
                .filter(candidate -> candidate.getStatus() != ClassroomSessionStatus.COMPLETED
                        && candidate.getStatus() != ClassroomSessionStatus.CANCELLED)
                .forEach(candidate -> copySharedRoom(sourceSession, candidate));
    }

    private void copySharedRoom(ClassSchedule sourceSession, ClassSchedule targetClassSchedule) {
        targetClassSchedule.setLarkMeetingId(sourceSession.getLarkMeetingId());
        targetClassSchedule.setLarkMeetingNo(sourceSession.getLarkMeetingNo());
        targetClassSchedule.setLarkMeetingUrl(sourceSession.getLarkMeetingUrl());
        targetClassSchedule.setRecordingProvider(sourceSession.getRecordingProvider());
        targetClassSchedule.setRecordingSyncStatus(sourceSession.getRecordingSyncStatus());
        targetClassSchedule.setRecordingSyncError(sourceSession.getRecordingSyncError());
        markSynced(targetClassSchedule);
    }

    private void setClassroomDefaultRoom(ClassSchedule session, String meetingUri) {
        ClassSection offering = session.getClassSection();
        if (offering == null) return;
        offering.setDefaultLarkMeetingUrl(meetingUri);
        offering.setLarkMeetingStatus(LarkMeetingStatus.SCHEDULED);
    }

    private void configureRecording(ClassSchedule session, boolean autoRecordingUnavailable) {
        session.setRecordingProvider("GOOGLE_MEET");
        if (properties.isAutoRecording() && !autoRecordingUnavailable) {
            session.setRecordingSyncStatus(RecordingSyncStatus.SCHEDULED);
            session.setRecordingSyncError(null);
            return;
        }
        session.setRecordingSyncStatus(RecordingSyncStatus.NOT_AVAILABLE);
        session.setRecordingSyncError(autoRecordingUnavailable
                ? "Tài khoản Google của giáo viên chưa được phép bật ghi hình tự động. Giáo viên chủ phòng cần bật ghi hình thủ công trong Google Meet."
                : null);
    }

    private boolean isAutoRecordingUnavailable(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && (message.contains("FEATURE_UNAVAILABLE_TO_USER")
                || message.contains("updateAutoRecordingGeneration"));
    }

    private void restrictExistingSpace(ClassSchedule session) {
        User meetingOwner = requireMeetingOwner(session);
        String refreshToken = connectionService.requireRefreshToken(meetingOwner);
        JsonNode space;
        try {
            space = sendMeetRequest(
                    "PATCH",
                    "/" + session.getLarkMeetingId() + "?updateMask=config.accessType",
                    restrictedAccessPayload(),
                    meetingOwner,
                    refreshToken
            );
        } catch (RuntimeException exception) {
            throw restrictedAccessUnavailable(exception);
        }
        ensureRestrictedAccess(space, meetingOwner, refreshToken);
    }

    private void ensureRestrictedAccess(JsonNode space, User meetingOwner, String refreshToken) {
        String resourceName = space.path("name").asText("");
        JsonNode verifiedSpace = space;
        if (verifiedSpace.path("config").path("accessType").asText("").isBlank()
                && !resourceName.isBlank()) {
            verifiedSpace = sendMeetRequest("GET", "/" + resourceName, null, meetingOwner, refreshToken);
        }
        if (!"RESTRICTED".equals(verifiedSpace.path("config").path("accessType").asText(""))) {
            throw restrictedAccessUnavailable(null);
        }
    }

    private RuntimeException restrictedAccessUnavailable(RuntimeException cause) {
        return new RuntimeException(
                "Google không áp dụng chế độ RESTRICTED cho phòng họp. "
                        + "Tài khoản Gmail cá nhân không hỗ trợ bắt buộc khách chờ giáo viên duyệt; "
                        + "hãy liên kết tài khoản Google Workspace của giáo viên.",
                cause
        );
    }

    private String spaceConfigPayload() {
        return spaceConfigPayload(properties.isAutoRecording());
    }

    private String spaceConfigPayload(boolean withAutoRecording) {
        if (!withAutoRecording) {
            return restrictedAccessPayload();
        }
        return "{\"config\":{\"accessType\":\"RESTRICTED\",\"artifactConfig\":{\"recordingConfig\":{\"autoRecordingGeneration\":\"ON\"}}}}";
    }

    private String restrictedAccessPayload() {
        return "{\"config\":{\"accessType\":\"RESTRICTED\"}}";
    }

    private Long recordingDurationMs(JsonNode recording) {
        try {
            String start = recording.path("startTime").asText("");
            String end = recording.path("endTime").asText("");
            if (start.isBlank() || end.isBlank()) return null;
            return Math.max(0L, Duration.between(Instant.parse(start), Instant.parse(end)).toMillis());
        } catch (RuntimeException ignored) {
            return null;
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
