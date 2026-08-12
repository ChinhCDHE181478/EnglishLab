package fu.sep490.g23.backend.controller.admin;

import fu.sep490.g23.backend.dto.response.admin.AuditLogResponse;
import fu.sep490.g23.backend.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/audit-logs") @RequiredArgsConstructor
public class AdminAuditLogController {
    private final AuditLogService service;
    @GetMapping public ResponseEntity<Page<AuditLogResponse>> logs(@RequestParam(required=false)String keyword,@RequestParam(required=false)String actor,@RequestParam(required=false)String action,Pageable pageable){return ResponseEntity.ok(service.getLogs(keyword,actor,action,pageable));}
}
