package fu.sep490.g23.backend.service.schedule_job;

import fu.sep490.g23.backend.service.classroom.ClassroomRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically retries pending Google Meet recording syncs.
 * Delegates all business logic to {@link ClassroomRecordingService}.
 */
@Component
@RequiredArgsConstructor
public class RecordingSyncJob {

    private final ClassroomRecordingService classroomRecordingService;

    @Scheduled(
            fixedDelayString = "${englishlab.google-meet.recording-sync-delay-ms:60000}",
            initialDelayString = "${englishlab.google-meet.recording-sync-delay-ms:60000}"
    )
    public void run() {
        classroomRecordingService.reconcilePendingRecordings();
    }
}
