package fu.sep490.g23.backend.service.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "englishlab.payos")
public class PayosProperties {
    private boolean enabled;
    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String returnUrl;
    private String cancelUrl;
    private String webhookUrl;
    private boolean autoConfirmWebhook;
}
