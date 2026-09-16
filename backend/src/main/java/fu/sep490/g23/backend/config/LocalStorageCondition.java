package fu.sep490.g23.backend.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Activates local storage whenever the R2 backend is not fully configured. */
public class LocalStorageCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !R2StorageCondition.isR2Enabled(context.getEnvironment());
    }
}
