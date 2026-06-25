package fu.sap490.g23.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(210)
@RequiredArgsConstructor
public class DemoLearnerOnboardingSeeder implements CommandLineRunner {

    private final DemoLearnerOnboardingSupport demoLearnerOnboardingSupport;

    @Override
    public void run(String... args) {
        demoLearnerOnboardingSupport.ensureAllDemoLearners();
    }
}
