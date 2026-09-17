package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.seed.master.MasterSeedGate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(210)
@RequiredArgsConstructor
public class DemoLearnerOnboardingSeeder implements CommandLineRunner {

    private final DemoLearnerOnboardingSupport demoLearnerOnboardingSupport;
    private final MasterSeedGate masterSeedGate;

    @Override
    public void run(String... args) {
        if (masterSeedGate.shouldSkipLegacyDemoSeeders()) {
            return;
        }
        demoLearnerOnboardingSupport.ensureAllDemoLearners();
    }
}
