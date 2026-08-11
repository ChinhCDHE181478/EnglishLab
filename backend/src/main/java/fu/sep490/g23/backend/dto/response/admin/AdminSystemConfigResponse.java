package fu.sep490.g23.backend.dto.response.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminSystemConfigResponse {
    private PayosConfig payos;
    private MailConfig mail;
    private AppConfig app;

    @Getter @Builder public static class PayosConfig {
        private boolean configured; private String clientIdMasked; private String apiKeyMasked; private String checksumKeyMasked;
    }
    @Getter @Builder public static class MailConfig {
        private String host; private Integer port; private String usernameMasked; private boolean configured;
    }
    @Getter @Builder public static class AppConfig { private String activeProfile; }
}
