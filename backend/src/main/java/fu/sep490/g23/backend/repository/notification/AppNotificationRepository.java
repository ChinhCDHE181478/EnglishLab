package fu.sep490.g23.backend.repository.notification;

import fu.sep490.g23.backend.entity.notification.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    Page<AppNotification> findByUserId(Long userId, Pageable pageable);

    List<AppNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("update AppNotification notification set notification.read = true, notification.readAt = :readAt "
            + "where notification.user.id = :userId and notification.read = false")
    int markAllRead(Long userId, LocalDateTime readAt);

    long countByUserIdAndReadFalse(Long userId);

    boolean existsByUserIdAndDeduplicationKey(Long userId, String deduplicationKey);
}
