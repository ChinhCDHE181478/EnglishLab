package fu.sep490.g23.backend.repository.notification;

import fu.sep490.g23.backend.entity.notification.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    boolean existsByUserIdAndDeduplicationKey(Long userId, String deduplicationKey);
}
