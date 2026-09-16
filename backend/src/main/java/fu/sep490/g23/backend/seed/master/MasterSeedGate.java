package fu.sep490.g23.backend.seed.master;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Legacy demo seeders should call {@link #shouldSkipLegacyDemoSeeders()} and return early
 * when MASTER mode is enabled.
 */
@Component
@RequiredArgsConstructor
public class MasterSeedGate {

    private final MasterDemoProperties properties;

    public boolean isMasterEnabled() {
        return properties.isEnabled();
    }

    public boolean shouldSkipLegacyDemoSeeders() {
        return properties.isEnabled();
    }
}
