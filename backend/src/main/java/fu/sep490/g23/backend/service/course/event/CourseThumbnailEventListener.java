package fu.sep490.g23.backend.service.course.event;

import fu.sep490.g23.backend.service.course.CourseThumbnailStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for course-thumbnail lifecycle events and deletes the previous thumbnail file
 * from the object store AFTER the database transaction has successfully committed.
 *
 * <p>Decoupling the file delete from the DB transaction prevents the following failure
 * modes:
 * <ul>
 *   <li>A rolled-back DB transaction does not leave the file already deleted on R2.</li>
 *   <li>A slow R2 delete call does not extend the DB transaction lifetime.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseThumbnailEventListener {

    private final CourseThumbnailStorageService courseThumbnailStorageService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onThumbnailReplaced(CourseThumbnailReplacedEvent event) {
        if (event.getPreviousThumbnailKey() == null) {
            return;
        }
        try {
            courseThumbnailStorageService.deleteByKey(event.getPreviousThumbnailKey());
            log.debug("Deleted previous course thumbnail after update: {}", event.getPreviousThumbnailKey());
        } catch (RuntimeException e) {
            log.warn("Failed to delete previous course thumbnail {} (will be reclaimed by orphan cleanup): {}",
                    event.getPreviousThumbnailKey(), e.getMessage());
        }
    }
}
