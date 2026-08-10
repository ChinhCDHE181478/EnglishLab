package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.service.payment.PayosProperties;
import fu.sap490.g23.backend.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PayosWebhookRegistrar {

    private final PaymentService paymentService;
    private final PayosProperties payosProperties;

    @Bean
    ApplicationRunner payosWebhookRunner() {
        return args -> {
            if (!payosProperties.isEnabled() || !payosProperties.isAutoConfirmWebhook()) {
                return;
            }
            if (isLocalWebhookUrl(payosProperties.getWebhookUrl())) {
                log.info("Bỏ qua xác nhận webhook PayOS tự động vì URL hiện tại vẫn là môi trường local: {}", payosProperties.getWebhookUrl());
                return;
            }

            try {
                paymentService.confirmWebhook();
                log.info("Đã xác nhận webhook PayOS với URL {}", payosProperties.getWebhookUrl());
            } catch (RuntimeException ex) {
                log.warn("Không thể xác nhận webhook PayOS khi khởi động: {}", ex.getMessage());
            }
        };
    }

    private boolean isLocalWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return true;
        }

        String normalized = webhookUrl.toLowerCase();
        return normalized.contains("localhost") || normalized.contains("127.0.0.1");
    }
}
