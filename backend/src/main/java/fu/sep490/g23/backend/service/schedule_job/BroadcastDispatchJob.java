package fu.sep490.g23.backend.service.schedule_job;

import fu.sep490.g23.backend.service.admin.AdminBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically dispatches scheduled admin broadcasts.
 * Delegates all business logic to {@link AdminBroadcastService}.
 */
@Component
@RequiredArgsConstructor
public class BroadcastDispatchJob {

    private final AdminBroadcastService adminBroadcastService;

    @Scheduled(
            fixedDelayString = "${englishlab.broadcast.scan-delay-ms:60000}",
            initialDelayString = "${englishlab.broadcast.initial-delay-ms:30000}"
    )
    public void run() {
        adminBroadcastService.dispatchScheduled();
    }
}
