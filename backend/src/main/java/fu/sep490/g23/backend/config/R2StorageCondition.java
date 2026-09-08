package fu.sep490.g23.backend.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class R2StorageCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();

        // If local override is explicitly enabled, skip R2
        if (env.getProperty("englishlab.storage.local-enabled", Boolean.class, false)) {
            return false;
        }

        // All R2 credentials must be present
        String endpoint = env.getProperty("englishlab.storage.r2-endpoint", "");
        String accessKey = env.getProperty("englishlab.storage.r2-access-key", "");
        String secretKey = env.getProperty("englishlab.storage.r2-secret-key", "");
        String bucket = env.getProperty("englishlab.storage.r2-bucket", "");

        boolean hasAllCredentials = !endpoint.isBlank()
                && !accessKey.isBlank()
                && !secretKey.isBlank()
                && !bucket.isBlank();

        return hasAllCredentials;
    }
}
