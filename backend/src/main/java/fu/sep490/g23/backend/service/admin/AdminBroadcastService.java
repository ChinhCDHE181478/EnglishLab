package fu.sap490.g23.backend.service.admin;

import fu.sap490.g23.backend.dto.request.admin.ScheduleAdminBroadcastRequest;
import fu.sap490.g23.backend.dto.request.admin.UpsertAdminBroadcastRequest;
import fu.sap490.g23.backend.dto.response.admin.AdminBroadcastResponse;
import fu.sap490.g23.backend.entity.admin.enums.BroadcastStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminBroadcastService {
    Page<AdminBroadcastResponse> list(BroadcastStatus status, Pageable pageable);
    AdminBroadcastResponse create(String actorEmail, UpsertAdminBroadcastRequest request);
    AdminBroadcastResponse update(String actorEmail, Long id, UpsertAdminBroadcastRequest request);
    AdminBroadcastResponse schedule(String actorEmail, Long id, ScheduleAdminBroadcastRequest request);
    AdminBroadcastResponse sendNow(String actorEmail, Long id);
    AdminBroadcastResponse cancel(String actorEmail, Long id);
    void dispatchScheduled();
}
