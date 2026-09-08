package fu.sep490.g23.backend.config;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Condition that activates only when R2 storage is fully configured with credentials
 * AND local override is disabled.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(R2StorageCondition.class)
public @interface ConditionalOnR2Enabled {
}
