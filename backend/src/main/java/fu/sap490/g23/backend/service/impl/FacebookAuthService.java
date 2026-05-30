package fu.sap490.g23.backend.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fu.sap490.g23.backend.dto.request.FacebookAuthRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.security.JwtService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacebookAuthService {

    private static final String FACEBOOK_ME_URL = "https://graph.facebook.com/me";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse loginWithFacebook(FacebookAuthRequest request) {
        FacebookProfile profile = verifyAccessToken(request.getAccessToken());

        String facebookId = profile.getId();
        String email = profile.getEmail();
        String name = profile.getName();

        if (facebookId == null || facebookId.isBlank()) {
            throw new RuntimeException("Invalid Facebook access token");
        }

        if (email == null || email.isBlank()) {
            email = "facebook_" + facebookId + "@facebook.local";
        }

        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        Optional<User> existing = userRepository.findByFacebookId(facebookId);
        if (existing.isEmpty()) {
            existing = userRepository.findByEmail(email);
        }

        User user;
        if (existing.isPresent()) {
            user = existing.get();
            if (user.getFacebookId() == null) {
                user.setFacebookId(facebookId);
                userRepository.save(user);
            }
        } else {
            user = User.builder()
                    .fullName(name)
                    .email(email)
                    .facebookId(facebookId)
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
                .message("Facebook login successful")
                .accessToken(token)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    private FacebookProfile verifyAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("Facebook access token is required");
        }

        String url = UriComponentsBuilder
                .fromUriString(FACEBOOK_ME_URL)
                .queryParam("fields", "id,name,email")
                .queryParam("access_token", accessToken)
                .toUriString();

        FacebookProfile profile = new RestTemplate().getForObject(url, FacebookProfile.class);
        if (profile == null) {
            throw new RuntimeException("Failed to verify Facebook access token");
        }
        return profile;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FacebookProfile {
        private String id;
        private String name;
        private String email;
    }
}
