package fu.sap490.g23.backend.controller.admin;

import fu.sap490.g23.backend.dto.response.admin.BackupCapabilityResponse;
import fu.sap490.g23.backend.dto.response.admin.BackupRecordResponse;
import fu.sap490.g23.backend.service.admin.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/admin/backups")
@RequiredArgsConstructor
public class AdminBackupController {
    private final BackupService service;

    @GetMapping("/capabilities")
    public ResponseEntity<BackupCapabilityResponse> capabilities() {
        return ResponseEntity.ok(service.capabilities());
    }

    @GetMapping
    public ResponseEntity<Page<BackupRecordResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(service.list(pageable));
    }

    @PostMapping
    public ResponseEntity<BackupRecordResponse> create(Principal principal) {
        return ResponseEntity.ok(service.create(principal.getName()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(resource.getFilename()).build().toString())
                .body(resource);
    }

    @PostMapping(path = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BackupRecordResponse> restore(
            Principal principal,
            @RequestPart("file") MultipartFile file,
            @RequestPart("confirmation") String confirmation
    ) throws IOException {
        return ResponseEntity.ok(service.restore(
                principal.getName(),
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getSize(),
                confirmation
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long id) {
        service.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
