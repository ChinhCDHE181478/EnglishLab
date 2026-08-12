package fu.sep490.g23.backend.service.auth.impl;

import fu.sep490.g23.backend.service.auth.AuthTokenService;

import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.enums.AuthTokenType;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration OTP_RESEND_COOLDOWN = Duration.ofMinutes(1);

    private final AuthTokenRepository authTokenRepository;

    @Value("${englishlab.auth.email-verification-expiration-minutes:15}")
    private long emailVerificationExpirationMinutes;

    @Value("${englishlab.auth.password-reset-expiration-minutes:15}")
    private long passwordResetExpirationMinutes;

    @Transactional
    public AuthToken issueEmailVerificationToken(User user) {
        enforceResendCooldown(user, AuthTokenType.EMAIL_VERIFICATION);
        return replaceEmailVerificationToken(user);
    }

    @Transactional
    public AuthToken issueEmailVerificationTokenForRegistration(User user) {
        return replaceEmailVerificationToken(user);
    }

    private AuthToken replaceEmailVerificationToken(User user) {
        authTokenRepository.deleteByUserAndType(user, AuthTokenType.EMAIL_VERIFICATION);
        return authTokenRepository.save(AuthToken.builder()
                .user(user)
                .type(AuthTokenType.EMAIL_VERIFICATION)
                .token(generateEmailVerificationCode())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(emailVerificationExpirationMinutes))
                .build());
    }

    @Transactional
    public AuthToken issuePasswordResetToken(User user) {
        enforceResendCooldown(user, AuthTokenType.PASSWORD_RESET);
        authTokenRepository.deleteByUserAndType(user, AuthTokenType.PASSWORD_RESET);
        return authTokenRepository.save(AuthToken.builder()
                .user(user)
                .type(AuthTokenType.PASSWORD_RESET)
                .token(generateOneTimeCode(AuthTokenType.PASSWORD_RESET))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes))
                .build());
    }

    @Override
    @Transactional
    public AuthToken issueGoogleMeetConnectionState(User user) {
        authTokenRepository.deleteByUserAndType(user, AuthTokenType.GOOGLE_MEET_CONNECTION);
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return authTokenRepository.save(AuthToken.builder()
                .user(user)
                .type(AuthTokenType.GOOGLE_MEET_CONNECTION)
                .token(Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build());
    }

    public AuthToken requireValidEmailVerificationCode(User user, String rawCode, String invalidMessage) {
        return requireValidCode(user, rawCode, AuthTokenType.EMAIL_VERIFICATION, invalidMessage);
    }

    public AuthToken requireValidPasswordResetCode(User user, String rawCode, String invalidMessage) {
        return requireValidCode(user, rawCode, AuthTokenType.PASSWORD_RESET, invalidMessage);
    }

    @Override
    public AuthToken requireValidGoogleMeetConnectionState(String rawState) {
        String normalizedState = rawState == null ? "" : rawState.trim();
        AuthToken token = authTokenRepository.findByTokenAndType(
                        normalizedState,
                        AuthTokenType.GOOGLE_MEET_CONNECTION
                )
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu kết nối Google Meet không hợp lệ hoặc đã hết hạn."));
        if (token.isUsed() || token.isExpired()) {
            throw new IllegalArgumentException("Yêu cầu kết nối Google Meet không hợp lệ hoặc đã hết hạn.");
        }
        return token;
    }

    private AuthToken requireValidCode(User user, String rawCode, AuthTokenType type, String invalidMessage) {
        String normalizedCode = rawCode == null ? "" : rawCode.trim();
        AuthToken token = authTokenRepository.findByUserAndTokenAndType(user, normalizedCode, type)
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

    @Transactional
    public void deleteTokens(User user, AuthTokenType type) {
        authTokenRepository.deleteByUserAndType(user, type);
    }

    private String generateEmailVerificationCode() {
        return generateOneTimeCode(AuthTokenType.EMAIL_VERIFICATION);
    }

    private void enforceResendCooldown(User user, AuthTokenType type) {
        authTokenRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user, type)
                .filter((token) -> token.getCreatedAt() != null)
                .ifPresent((token) -> {
                    LocalDateTime nextAllowedAt = token.getCreatedAt().plus(OTP_RESEND_COOLDOWN);
                    if (LocalDateTime.now().isBefore(nextAllowedAt)) {
                        long remainingSeconds = Math.max(1, Duration.between(LocalDateTime.now(), nextAllowedAt).toSeconds());
                        throw new RuntimeException("Vui lòng chờ " + remainingSeconds + " giây trước khi gửi lại OTP.");
                    }
                });
    }

    private String generateOneTimeCode(AuthTokenType type) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            if (authTokenRepository.findByTokenAndType(code, type).isEmpty()) {
                return code;
            }
        }
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
