package fu.sep490.g23.backend.service.schedule_job;

import fu.sep490.g23.backend.service.classroom.ClassroomLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Scheduled job that periodically reconciles classroom session and offering statuses.
 * Delegates all business logic to {@link ClassroomLifecycleService}.
 */
@Component
@RequiredArgsConstructor
public class ClassroomLifecycleJob {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassroomLifecycleService classroomLifecycleService;

    @Scheduled(
            fixedDelayString = "${englishlab.classroom.lifecycle-delay-ms:60000}",
            initialDelayString = "${englishlab.classroom.lifecycle-initial-delay-ms:5000}"
    )
    public void run() {
        classroomLifecycleService.reconcileStatuses(LocalDateTime.now(BUSINESS_ZONE));
    }
}
