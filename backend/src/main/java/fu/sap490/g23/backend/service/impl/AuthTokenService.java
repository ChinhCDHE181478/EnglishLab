package fu.sap490.g23.backend.service.impl;

import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.AuthTokenType;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;

    @Value("${englishlab.auth.email-verification-expiration-hours:24}")
    private long emailVerificationExpirationHours;

    @Value("${englishlab.auth.password-reset-expiration-minutes:30}")
    private long passwordResetExpirationMinutes;

    @Transactional
    public AuthToken issueEmailVerificationToken(User user) {
        authTokenRepository.deleteByUserAndType(user, AuthTokenType.EMAIL_VERIFICATION);
        return authTokenRepository.save(AuthToken.builder()
                .user(user)
                .type(AuthTokenType.EMAIL_VERIFICATION)
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(emailVerificationExpirationHours))
                .build());
    }

    @Transactional
    public AuthToken issuePasswordResetToken(User user) {
        authTokenRepository.deleteByUserAndType(user, AuthTokenType.PASSWORD_RESET);
        return authTokenRepository.save(AuthToken.builder()
                .user(user)
                .type(AuthTokenType.PASSWORD_RESET)
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes))
                .build());
    }

    public AuthToken requireValidToken(String rawToken, AuthTokenType type, String invalidMessage) {
        AuthToken token = authTokenRepository.findByTokenAndType(rawToken, type)
                .orElseThrow(() -> new RuntimeException(invalidMessage));

        if (token.isUsed() || token.isExpired()) {
            throw new RuntimeException(invalidMessage);
        }

        return token;
    }

    @Transactional
    public void markUsed(AuthToken token) {
        token.setUsedAt(LocalDateTime.now());
        authTokenRepository.save(token);
    }
}
