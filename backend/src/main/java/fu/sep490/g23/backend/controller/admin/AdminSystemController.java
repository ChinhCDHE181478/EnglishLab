package fu.sep490.g23.backend.controller.admin;

import fu.sep490.g23.backend.dto.response.admin.AdminSystemConfigResponse;
import fu.sep490.g23.backend.service.admin.AdminSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {
    private final AdminSystemService adminSystemService;
    @GetMapping("/config") public ResponseEntity<AdminSystemConfigResponse> config() { return ResponseEntity.ok(adminSystemService.getConfig()); }
}
