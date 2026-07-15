package fu.sap490.g23.backend.service.admin;

import fu.sap490.g23.backend.dto.response.admin.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void record(String actorEmail,String action,String targetType,String targetId,String detail);
    Page<AuditLogResponse> getLogs(String keyword,String actor,String action,Pageable pageable);
}
