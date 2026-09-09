package fu.sep490.g23.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Activates the {@link ObjectStorageProperties} binding so the {@code englishlab.storage.*} keys
 * from {@code application.properties} get populated. Mirrors the pattern used by
 * {@code PayosConfig}.
 */
@Configuration
@EnableConfigurationProperties(ObjectStorageProperties.class)
public class ObjectStorageConfig {
}
