package fu.sep490.g23.backend.service.auth.impl;

import fu.sep490.g23.backend.service.auth.AuthTokenService;
import fu.sep490.g23.backend.service.auth.GoogleAuthService;
import fu.sep490.g23.backend.service.user.UserRoleService;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import fu.sep490.g23.backend.dto.request.GoogleAuthRequest;
import fu.sep490.g23.backend.dto.response.AuthResponse;
import fu.sep490.g23.backend.dto.response.UserResponse;
import fu.sep490.g23.backend.entity.enums.AuthTokenType;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.security.JwtService;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final UserRoleService userRoleService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleProfile profile = resolveGoogleProfile(request);

        String email = profile.email().trim().toLowerCase();
        String googleId = profile.googleId();
        String name = profile.name();
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        Optional<User> existing = userRepository.findByEmail(email);
        User user;

        if (existing.isPresent()) {
            user = existing.get();
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }
            user.setEmailVerified(true);
            user = userRepository.save(user);
            authTokenService.deleteTokens(user, AuthTokenType.EMAIL_VERIFICATION);
        } else {
            user = User.builder()
                    .fullName(name)
                    .email(email)
                    .googleId(googleId)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .passwordSet(false)
                    .emailVerified(true)
                    .build();
            userRoleService.assignRole(user, RoleCodes.LEARNER);
            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .message("Đăng nhập Google thành công.")
                .accessToken(token)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
    }

    private GoogleProfile resolveGoogleProfile(GoogleAuthRequest request) {
        if (request.getIdToken() != null && !request.getIdToken().isBlank()) {
            GoogleIdToken.Payload payload = verifyIdToken(request.getIdToken());
            return new GoogleProfile(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name")
            );
        }

        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            return verifyAccessToken(request.getAccessToken());
        }

        throw new RuntimeException("Thiếu thông tin đăng nhập Google.");
    }

    private GoogleProfile verifyAccessToken(String accessToken) {
        String url = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/oauth2/v3/userinfo")
                .queryParam("access_token", accessToken)
                .build()
                .toUriString();

        Map<?, ?> profile = restTemplate.getForObject(url, Map.class);
        if (profile == null || profile.get("sub") == null || profile.get("email") == null) {
            throw new RuntimeException("Access token Google không hợp lệ.");
        }

        return new GoogleProfile(
                String.valueOf(profile.get("sub")),
                String.valueOf(profile.get("email")),
                profile.get("name") == null ? null : String.valueOf(profile.get("name"))
        );
    }

    private GoogleIdToken.Payload verifyIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Google ID token không hợp lệ.");
            }
            return idToken.getPayload();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Không thể xác minh Google ID token: " + ex.getMessage(), ex);
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getPrimaryRoleCode())
                .roles(user.getRoleCodes().stream().sorted().toList())
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .currentBand(user.getCurrentBand())
                .studyGoal(user.getStudyGoal())
                .avatarUrl(user.getAvatarUrl())
                .passwordSet(user.isPasswordSet())
                .profileCompleted(user.isProfileCompleted())
                .placementTestCompleted(placementTestAttemptRepository.existsByStudentAndTestCode(user, PlacementTestDefinitionService.TEST_CODE))
                .build();
    }

    private record GoogleProfile(String googleId, String email, String name) {
    }
}
