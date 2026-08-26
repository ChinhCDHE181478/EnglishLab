package fu.sep490.g23.backend.repository.admin;

import fu.sep490.g23.backend.entity.admin.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog,Long>, JpaSpecificationExecutor<AuditLog> {
    java.util.List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtAscIdAsc(String targetType, String targetId);
}
