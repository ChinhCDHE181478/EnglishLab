package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.service.payment.PayosProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PayosProperties.class)
public class PayosConfig {
}
