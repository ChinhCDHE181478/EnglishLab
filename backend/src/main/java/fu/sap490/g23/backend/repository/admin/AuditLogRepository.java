package fu.sap490.g23.backend.repository.admin;

import fu.sap490.g23.backend.entity.admin.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog,Long>, JpaSpecificationExecutor<AuditLog> {}
