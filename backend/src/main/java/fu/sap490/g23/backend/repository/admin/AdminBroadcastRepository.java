package fu.sap490.g23.backend.repository.admin;

import fu.sap490.g23.backend.entity.admin.AdminBroadcast;
import fu.sap490.g23.backend.entity.admin.enums.BroadcastStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminBroadcastRepository extends JpaRepository<AdminBroadcast, Long> {
    Page<AdminBroadcast> findByStatus(BroadcastStatus status, Pageable pageable);
    List<AdminBroadcast> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            BroadcastStatus status,
            LocalDateTime dueAt
    );
}
