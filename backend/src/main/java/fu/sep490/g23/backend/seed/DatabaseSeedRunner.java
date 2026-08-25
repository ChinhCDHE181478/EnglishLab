package fu.sep490.g23.backend.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeedRunner implements ApplicationRunner {

    private final List<SeedDataContributor> contributors;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.seed.test.enabled:false}")
    private boolean testEnabled;

    @Value("${app.seed.review.enabled:false}")
    private boolean reviewEnabled;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean sheetEnabled;

    @Value("${app.seed.assessment-reference.enabled:false}")
    private boolean assessmentReferenceEnabled;

    @Override
    public void run(ApplicationArguments args) {
        Set<SeedMode> activeModes = activeModes();
        if (activeModes.isEmpty()) {
            return;
        }

        contributors.stream()
                .filter(contributor -> contributor.supports(activeModes))
                .sorted(Comparator.comparingInt(SeedDataContributor::order))
                .forEach(contributor -> runContributor(contributor, activeModes));
    }

    private void runContributor(SeedDataContributor contributor, Set<SeedMode> activeModes) {
        log.info("Bắt đầu seed dữ liệu: {}", contributor.name());
        transactionTemplate.executeWithoutResult(status -> contributor.seed(activeModes));
        log.info("Hoàn tất seed dữ liệu: {}", contributor.name());
    }

    private Set<SeedMode> activeModes() {
        EnumSet<SeedMode> modes = EnumSet.noneOf(SeedMode.class);
        if (testEnabled) {
            modes.add(SeedMode.TEST);
        }
        if (reviewEnabled) {
            modes.add(SeedMode.REVIEW);
        }
        if (sheetEnabled) {
            modes.add(SeedMode.SHEET);
        }
        if (assessmentReferenceEnabled) {
            modes.add(SeedMode.ASSESSMENT_REFERENCE);
        }
        return modes;
    }
}
