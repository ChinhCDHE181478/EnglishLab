package fu.sap490.g23.backend.service.classroom.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.response.teacher.TeacherGoogleMeetConnectionResponse;
import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.teacher.TeacherGoogleMeetConnection;
import fu.sap490.g23.backend.entity.teacher.enums.GoogleMeetConnectionStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.teacher.TeacherGoogleMeetConnectionRepository;
import fu.sap490.g23.backend.service.auth.AuthTokenService;
import fu.sap490.g23.backend.service.classroom.GoogleMeetProperties;
import fu.sap490.g23.backend.service.classroom.GoogleMeetTokenCipher;
import fu.sap490.g23.backend.service.classroom.TeacherGoogleMeetConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TeacherGoogleMeetConnectionServiceImpl implements TeacherGoogleMeetConnectionService {

    private static final String MEET_SCOPE = "https://www.googleapis.com/auth/meetings.space.created";
    private static final String MEET_SETTINGS_SCOPE = "https://www.googleapis.com/auth/meetings.space.settings";
    private static final String SCOPES = "openid email " + MEET_SCOPE + " " + MEET_SETTINGS_SCOPE;

    private final UserRepository userRepository;
    private final TeacherGoogleMeetConnectionRepository connectionRepository;
    private final AuthTokenService authTokenService;
    private final GoogleMeetProperties properties;
    private final GoogleMeetTokenCipher tokenCipher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    @Transactional(readOnly = true)
    public TeacherGoogleMeetConnectionResponse getConnection(String teacherEmail) {
        User teacher = requireGoogleMeetAccount(teacherEmail);
        return connectionRepository.findByTeacherId(teacher.getId())
                .map(connection -> toResponse(connection, properties.isEnabled()))
                .orElseGet(() -> TeacherGoogleMeetConnectionResponse.builder()
                        .connected(false)
                        .integrationEnabled(properties.isEnabled())
                        .status(GoogleMeetConnectionStatus.DISCONNECTED)
                        .build());
    }

    @Override
    @Transactional
    public String createAuthorizationUrl(String teacherEmail) {
        validateOAuthConfiguration();
        User teacher = requireGoogleMeetAccount(teacherEmail);
        AuthToken state = authTokenService.issueGoogleMeetConnectionState(teacher);
        return properties.getAuthorizationUri()
                + "?client_id=" + encode(properties.getClientId())
                + "&redirect_uri=" + encode(properties.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + encode(SCOPES)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true"
                + "&state=" + encode(state.getToken());
    }

    @Override
    @Transactional
    public String completeAuthorization(String code, String state) {
        validateOAuthConfiguration();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Google không trả về mã cấp quyền.");
        }
        AuthToken stateToken = authTokenService.requireValidGoogleMeetConnectionState(state);
        User teacher = stateToken.getUser();
        if (!canConnectGoogleMeet(teacher)) {
            throw new SecurityException("Tài khoản không còn quyền liên kết Google Meet.");
        }

        JsonNode tokenResponse = exchangeAuthorizationCode(code);
        String refreshToken = tokenResponse.path("refresh_token").asText("");
        String accessToken = tokenResponse.path("access_token").asText("");
        if (refreshToken.isBlank() || accessToken.isBlank()) {
            throw new IllegalStateException("Google không trả về refresh token. Hãy cấp quyền lại và chọn Consent Screen.");
        }
        JsonNode profile = loadGoogleProfile(accessToken);
        String subject = profile.path("sub").asText("");
        String googleEmail = profile.path("email").asText("");
        if (subject.isBlank() || googleEmail.isBlank()) {
            throw new IllegalStateException("Không thể xác định tài khoản Google đã kết nối.");
        }

        LocalDateTime now = LocalDateTime.now();
        TeacherGoogleMeetConnection connection = connectionRepository.findByTeacherId(teacher.getId())
                .orElseGet(() -> TeacherGoogleMeetConnection.builder().teacher(teacher).build());
        connection.setGoogleSubject(subject);
        connection.setGoogleEmail(googleEmail);
        connection.setEncryptedRefreshToken(tokenCipher.encrypt(refreshToken));
        connection.setScopes(tokenResponse.path("scope").asText(SCOPES));
        connection.setStatus(GoogleMeetConnectionStatus.CONNECTED);
        connection.setConnectedAt(now);
        connection.setRevokedAt(null);
        connectionRepository.save(connection);
        authTokenService.markUsed(stateToken);
        return frontendReturnUrlFor(teacher) + "?googleMeet=connected";
    }

    @Override
    @Transactional
    public void disconnect(String teacherEmail) {
        User teacher = requireGoogleMeetAccount(teacherEmail);
        connectionRepository.findByTeacherId(teacher.getId()).ifPresent(connection -> {
            connection.setStatus(GoogleMeetConnectionStatus.DISCONNECTED);
            connection.setEncryptedRefreshToken("");
            connection.setRevokedAt(LocalDateTime.now());
            connectionRepository.save(connection);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String requireRefreshToken(User teacher) {
        if (teacher == null) {
            throw new IllegalStateException("Lớp học chưa có giáo viên phụ trách.");
        }
        TeacherGoogleMeetConnection connection = connectionRepository.findByTeacherId(teacher.getId())
                .filter(item -> item.getStatus() == GoogleMeetConnectionStatus.CONNECTED)
                .orElseThrow(() -> new IllegalStateException(
                        "Giáo viên " + teacher.getFullName()
                                + " chưa kết nối Google Meet. Hãy yêu cầu giáo viên liên kết Google trong Hồ sơ giáo viên trước khi tạo phòng."
                ));
        connection.setLastUsedAt(LocalDateTime.now());
        connectionRepository.save(connection);
        return tokenCipher.decrypt(connection.getEncryptedRefreshToken());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markReauthenticationRequired(User teacher) {
        if (teacher == null) return;
        connectionRepository.findByTeacherId(teacher.getId()).ifPresent(connection -> {
            connection.setStatus(GoogleMeetConnectionStatus.REAUTH_REQUIRED);
            connectionRepository.save(connection);
        });
    }

    private JsonNode exchangeAuthorizationCode(String code) {
        String form = "client_id=" + encode(properties.getClientId())
                + "&client_secret=" + encode(properties.getClientSecret())
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(properties.getRedirectUri())
                + "&grant_type=authorization_code";
        return sendForm(properties.getTokenUri(), form, "Không thể đổi mã cấp quyền Google.");
    }

    private JsonNode loadGoogleProfile(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Google không trả về hồ sơ tài khoản đã kết nối.");
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Yêu cầu Google OAuth đã bị gián đoạn.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể đọc hồ sơ Google: " + exception.getMessage(), exception);
        }
    }

    private JsonNode sendForm(String uri, String form, String fallbackMessage) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String detail = objectMapper.readTree(response.body()).path("error_description").asText(fallbackMessage);
                throw new IllegalStateException(detail);
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Yêu cầu Google OAuth đã bị gián đoạn.", exception);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(fallbackMessage + " " + exception.getMessage(), exception);
        }
    }

    private User requireGoogleMeetAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if (!canConnectGoogleMeet(user)) {
            throw new SecurityException("Chỉ giáo viên hoặc nhân viên vận hành được liên kết Google Meet.");
        }
        return user;
    }

    private boolean canConnectGoogleMeet(User user) {
        return user.hasAnyRole(java.util.Set.of(RoleEnum.TEACHER));
    }

    private String frontendReturnUrlFor(User user) {
        return properties.getFrontendReturnUrl();
    }

    private TeacherGoogleMeetConnectionResponse toResponse(
            TeacherGoogleMeetConnection connection,
            boolean integrationEnabled
    ) {
        return TeacherGoogleMeetConnectionResponse.builder()
                .connected(connection.getStatus() == GoogleMeetConnectionStatus.CONNECTED)
                .integrationEnabled(integrationEnabled)
                .status(connection.getStatus())
                .googleEmail(connection.getGoogleEmail())
                .connectedAt(connection.getConnectedAt())
                .lastUsedAt(connection.getLastUsedAt())
                .build();
    }

    private void validateOAuthConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Tích hợp Google Meet chưa được bật.");
        }
        if (isBlank(properties.getClientId()) || isBlank(properties.getClientSecret())
                || isBlank(properties.getRedirectUri())) {
            throw new IllegalStateException("Cấu hình OAuth Google Meet chưa đầy đủ.");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
