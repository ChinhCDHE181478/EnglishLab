package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.request.course.LearningPackageRequest;
import fu.sap490.g23.backend.dto.request.course.UpdatePackageBundleItemsRequest;
import fu.sap490.g23.backend.dto.response.ApiResponse;
import fu.sap490.g23.backend.dto.response.course.LearningPackageResponse;
import fu.sap490.g23.backend.dto.response.course.LearningPackageSummaryResponse;
import fu.sap490.g23.backend.dto.response.course.PackageTypeResponse;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sap490.g23.backend.service.course.LearningPackageManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content-manager/packages")
@RequiredArgsConstructor
public class ContentManagerPackageController {

    private final LearningPackageManagementService packageManagementService;

    @GetMapping("/types")
    public ResponseEntity<List<PackageTypeResponse>> getPackageTypes() {
        return ResponseEntity.ok(packageManagementService.getPackageTypes());
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<LearningPackageSummaryResponse>> getBundleCandidates() {
        return ResponseEntity.ok(packageManagementService.listBundleCandidates());
    }

    @GetMapping
    public ResponseEntity<Page<LearningPackageResponse>> listPackages(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PackageTypeCode packageTypeCode,
            @RequestParam(required = false) PackageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        return ResponseEntity.ok(packageManagementService.listPackages(keyword, packageTypeCode, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LearningPackageResponse> getPackage(@PathVariable Long id) {
        return ResponseEntity.ok(packageManagementService.getPackage(id));
    }

    @PostMapping
    public ResponseEntity<LearningPackageResponse> createBundle(
            @Valid @RequestBody LearningPackageRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageManagementService.createBundle(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LearningPackageResponse> updateBundle(
            @PathVariable Long id,
            @Valid @RequestBody LearningPackageRequest request
    ) {
        return ResponseEntity.ok(packageManagementService.updateBundle(id, request));
    }

    @PutMapping("/{id}/bundle-items")
    public ResponseEntity<LearningPackageResponse> replaceBundleItems(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePackageBundleItemsRequest request
    ) {
        return ResponseEntity.ok(packageManagementService.replaceBundleItems(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<LearningPackageResponse> publishBundle(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        return ResponseEntity.ok(packageManagementService.publishBundle(id, authentication.getName()));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<LearningPackageResponse> archiveBundle(@PathVariable Long id) {
        return ResponseEntity.ok(packageManagementService.archiveBundle(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteBundle(@PathVariable Long id) {
        packageManagementService.deleteBundle(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Đã xóa gói bundle.")
                .description("Gói không còn xuất hiện trong danh sách quản lý.")
                .build());
    }
}
