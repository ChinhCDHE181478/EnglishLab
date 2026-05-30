package fu.sap490.g23.backend.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import fu.sap490.g23.backend.dto.request.GoogleAuthRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.security.JwtService;
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
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleProfile profile = resolveGoogleProfile(request);

        String email = profile.email();
        String googleId = profile.googleId();
        String name = profile.name();
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        // Find existing user by email or googleId, or create new one
        Optional<User> existing = userRepository.findByEmail(email);
        User user;

        if (existing.isPresent()) {
            user = existing.get();
            // Link googleId if not yet linked
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                userRepository.save(user);
            }
        } else {
            // New user — register via Google
            user = User.builder()
                    .fullName(name)
                    .email(email)
                    .googleId(googleId)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Role.USER)
                    .build();
            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .studyGoal(user.getStudyGoal())
                .profileCompleted(user.isProfileCompleted())
                .build();

        return AuthResponse.builder()
                .message("Google login successful")
                .accessToken(token)
                .tokenType("Bearer")
                .user(userResponse)
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

        throw new RuntimeException("Missing Google credential");
    }

    private GoogleProfile verifyAccessToken(String accessToken) {
        String url = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/oauth2/v3/userinfo")
                .queryParam("access_token", accessToken)
                .build()
                .toUriString();

        Map<?, ?> profile = restTemplate.getForObject(url, Map.class);
        if (profile == null || profile.get("sub") == null || profile.get("email") == null) {
            throw new RuntimeException("Invalid Google access token");
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
                throw new RuntimeException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google ID token: " + e.getMessage(), e);
        }
    }

    private record GoogleProfile(String googleId, String email, String name) {
    }
}
