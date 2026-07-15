package fu.sap490.g23.backend.service.admin.impl;

import fu.sap490.g23.backend.dto.response.admin.AdminSystemConfigResponse;
import fu.sap490.g23.backend.service.admin.AdminSystemService;
import fu.sap490.g23.backend.service.payment.PayosProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AdminSystemServiceImpl implements AdminSystemService {
    private final PayosProperties payosProperties;
    private final Environment environment;

    @Override
    public AdminSystemConfigResponse getConfig() {
        String mailHost = environment.getProperty("spring.mail.host", "");
        String mailUsername = environment.getProperty("spring.mail.username", "");
        int mailPort = environment.getProperty("spring.mail.port", Integer.class, 587);
        boolean payosConfigured = present(payosProperties.getClientId()) && present(payosProperties.getApiKey()) && present(payosProperties.getChecksumKey());
        String profiles = Arrays.stream(environment.getActiveProfiles()).reduce((a, b) -> a + ", " + b).orElse("default");
        return AdminSystemConfigResponse.builder()
                .payos(AdminSystemConfigResponse.PayosConfig.builder().configured(payosConfigured)
                        .clientIdMasked(mask(payosProperties.getClientId())).apiKeyMasked(mask(payosProperties.getApiKey())).checksumKeyMasked(mask(payosProperties.getChecksumKey())).build())
                .mail(AdminSystemConfigResponse.MailConfig.builder().host(present(mailHost) ? mailHost : "Chưa cấu hình").port(mailPort)
                        .usernameMasked(mask(mailUsername)).configured(present(mailHost) && present(mailUsername)).build())
                .app(AdminSystemConfigResponse.AppConfig.builder().activeProfile(profiles).build()).build();
    }

    private boolean present(String value) { return value != null && !value.isBlank(); }
    private String mask(String value) {
        if (!present(value)) return "Chưa cấu hình";
        String text = value.trim();
        if (text.length() <= 5) return "Đã cấu hình";
        return text.substring(0, 3) + "****" + text.substring(text.length() - 2);
    }
}
