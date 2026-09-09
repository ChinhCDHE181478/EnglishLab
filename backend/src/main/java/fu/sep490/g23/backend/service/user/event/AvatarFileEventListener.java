package fu.sep490.g23.backend.service.user.event;

import fu.sep490.g23.backend.service.user.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for avatar lifecycle events and performs the actual object-store delete
 * AFTER the database transaction has successfully committed.
 *
 * <p>This decouples the file-system operation from the database transaction so that
 * a failed delete does not cause the transaction to roll back, and a rolled-back
 * transaction does not leave a partially-deleted state.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AvatarFileEventListener {

    private final AvatarStorageService avatarStorageService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAvatarUpdated(AvatarUpdatedEvent event) {
        if (event.getPreviousAvatarKey() == null) {
            return;
        }
        try {
            avatarStorageService.deleteByKey(event.getPreviousAvatarKey());
            log.debug("Deleted previous avatar after update: {}", event.getPreviousAvatarKey());
        } catch (RuntimeException e) {
            log.warn("Failed to delete previous avatar {} (will be reclaimed by orphan cleanup): {}",
                    event.getPreviousAvatarKey(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAvatarDeleted(AvatarDeletedEvent event) {
        if (event.getAvatarKey() == null) {
            return;
        }
        try {
            avatarStorageService.deleteByKey(event.getAvatarKey());
            log.debug("Deleted avatar after user deletion: {}", event.getAvatarKey());
        } catch (RuntimeException e) {
            log.warn("Failed to delete avatar {} (will be reclaimed by orphan cleanup): {}",
                    event.getAvatarKey(), e.getMessage());
        }
    }
}
