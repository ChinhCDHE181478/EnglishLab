package fu.sap490.g23.backend.controller.admin;

import fu.sap490.g23.backend.dto.request.admin.ScheduleAdminBroadcastRequest;
import fu.sap490.g23.backend.dto.request.admin.UpsertAdminBroadcastRequest;
import fu.sap490.g23.backend.dto.response.admin.AdminBroadcastResponse;
import fu.sap490.g23.backend.entity.admin.enums.BroadcastStatus;
import fu.sap490.g23.backend.service.admin.AdminBroadcastService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/broadcasts")
@RequiredArgsConstructor
public class AdminBroadcastController {
    private final AdminBroadcastService service;

    @GetMapping
    public ResponseEntity<Page<AdminBroadcastResponse>> list(
            @RequestParam(required = false) BroadcastStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(service.list(status, pageable));
    }

    @PostMapping
    public ResponseEntity<AdminBroadcastResponse> create(
            Principal principal,
            @Valid @RequestBody UpsertAdminBroadcastRequest request
    ) {
        return ResponseEntity.ok(service.create(principal.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminBroadcastResponse> update(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpsertAdminBroadcastRequest request
    ) {
        return ResponseEntity.ok(service.update(principal.getName(), id, request));
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<AdminBroadcastResponse> schedule(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody ScheduleAdminBroadcastRequest request
    ) {
        return ResponseEntity.ok(service.schedule(principal.getName(), id, request));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<AdminBroadcastResponse> send(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(service.sendNow(principal.getName(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AdminBroadcastResponse> cancel(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(principal.getName(), id));
    }
}
