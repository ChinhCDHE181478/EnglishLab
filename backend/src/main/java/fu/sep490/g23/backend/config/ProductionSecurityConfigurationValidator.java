package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.service.payment.PayosProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ProductionSecurityConfigurationValidator {

    private static final String LOCAL_JWT_SECRET =
            "englishlab-local-development-secret-change-before-production";

    private final Environment environment;
    private final PayosProperties payosProperties;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET phải có ít nhất 32 ký tự.");
        }
        if (isProductionProfile() && LOCAL_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("Môi trường production phải cấu hình JWT_SECRET riêng.");
        }
        if (payosProperties.isEnabled()
                && (!StringUtils.hasText(payosProperties.getClientId())
                || !StringUtils.hasText(payosProperties.getApiKey())
                || !StringUtils.hasText(payosProperties.getChecksumKey()))) {
            throw new IllegalStateException(
                    "PayOS đang bật nhưng thiếu PAYOS_CLIENT_ID, PAYOS_API_KEY hoặc PAYOS_CHECKSUM_KEY."
            );
        }
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile)
                        || "production".equalsIgnoreCase(profile));
    }
}
