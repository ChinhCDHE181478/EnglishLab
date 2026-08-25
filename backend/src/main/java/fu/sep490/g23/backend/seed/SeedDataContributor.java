package fu.sep490.g23.backend.seed;

import java.util.Set;

public interface SeedDataContributor {

    int order();

    String name();

    Set<SeedMode> supportedModes();

    void seed(Set<SeedMode> activeModes);

    default boolean supports(Set<SeedMode> activeModes) {
        return supportedModes().stream().anyMatch(activeModes::contains);
    }
}
