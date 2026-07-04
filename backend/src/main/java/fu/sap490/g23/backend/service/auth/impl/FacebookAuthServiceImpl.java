package fu.sap490.g23.backend.service.auth.impl;

import fu.sap490.g23.backend.service.auth.*;

import fu.sap490.g23.backend.service.user.UserRoleService;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.enums.AuthTokenType;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.security.JwtService;
import fu.sap490.g23.backend.service.assessment.PlacementTestDefinitionService;
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
public class FacebookAuthServiceImpl implements FacebookAuthService {

    private static final String FACEBOOK_ME_URL = "https://graph.facebook.com/me";

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final UserRoleService userRoleService;

    public AuthResponse loginWithFacebook(String accessToken) {
        FacebookProfile profile = verifyAccessToken(accessToken);

        String facebookId = profile.getId();
        String email = profile.getEmail();
        String name = profile.getName();

        if (facebookId == null || facebookId.isBlank()) {
            throw new RuntimeException("Access token Facebook không hợp lệ.");
        }

        if (email == null || email.isBlank()) {
            email = "facebook_" + facebookId + "@facebook.local";
        }
        email = email.trim().toLowerCase();

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
            }
            user.setEmailVerified(true);
            user = userRepository.save(user);
            authTokenService.deleteTokens(user, AuthTokenType.EMAIL_VERIFICATION);
        } else {
            user = User.builder()
                    .fullName(name)
                    .email(email)
                    .facebookId(facebookId)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .emailVerified(true)
                    .build();
            userRoleService.assignRole(user, RoleEnum.LEARNER);
            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .message("Đăng nhập Facebook thành công.")
                .accessToken(token)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
    }

    private FacebookProfile verifyAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("Facebook access token là bắt buộc.");
        }

        String url = UriComponentsBuilder
                .fromUriString(FACEBOOK_ME_URL)
                .queryParam("fields", "id,name,email")
                .queryParam("access_token", accessToken)
                .toUriString();

        FacebookProfile profile = new RestTemplate().getForObject(url, FacebookProfile.class);
        if (profile == null) {
            throw new RuntimeException("Không thể xác minh Facebook access token.");
        }
        return profile;
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .roles(user.getRoleCodes().stream().map(Enum::name).sorted().toList())
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .currentBand(user.getCurrentBand())
                .studyGoal(user.getStudyGoal())
                .profileCompleted(user.isProfileCompleted())
                .placementTestCompleted(placementTestAttemptRepository.existsByStudentAndTestCode(user, PlacementTestDefinitionService.TEST_CODE))
                .build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FacebookProfile {
        private String id;
        private String name;
        private String email;
    }
}
