package fu.sep490.g23.backend.controller.admin;
import fu.sep490.g23.backend.dto.request.admin.UpdateAdminUserStatusRequest;

import fu.sep490.g23.backend.dto.request.admin.UpdateUserRolesRequest;
import fu.sep490.g23.backend.dto.request.admin.UpsertAdminUserRequest;

import fu.sep490.g23.backend.dto.response.admin.AdminUserResponse;
import fu.sep490.g23.backend.dto.response.admin.AdminDashboardResponse;

import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;
    @GetMapping("/dashboard") public ResponseEntity<AdminDashboardResponse> dashboard() { return ResponseEntity.ok(adminUserService.getDashboard()); }
    @GetMapping("/users") public ResponseEntity<Page<AdminUserResponse>> users(@RequestParam(required=false) String keyword, @RequestParam(required=false) RoleEnum role, Pageable pageable) { return ResponseEntity.ok(adminUserService.getUsers(keyword, role, pageable)); }
    @PostMapping("/users") public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody UpsertAdminUserRequest request, Authentication auth) { return ResponseEntity.ok(adminUserService.createUser(request, auth.getName())); }
    @PutMapping("/users/{id}") public ResponseEntity<AdminUserResponse> update(@PathVariable Long id, @Valid @RequestBody UpsertAdminUserRequest request, Authentication auth) { return ResponseEntity.ok(adminUserService.updateUser(id, request, auth.getName())); }
    @PatchMapping("/users/{id}/roles") public ResponseEntity<AdminUserResponse> roles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request, Authentication auth) { return ResponseEntity.ok(adminUserService.updateRoles(id, request, auth.getName())); }
    @PatchMapping("/users/{id}/status") public ResponseEntity<AdminUserResponse> status(@PathVariable Long id, @Valid @RequestBody UpdateAdminUserStatusRequest request, Authentication auth) { return ResponseEntity.ok(adminUserService.updateStatus(id, request, auth.getName())); }
    @PostMapping("/users/{id}/teacher-onboarding-email") public ResponseEntity<Void> resendTeacherOnboarding(@PathVariable Long id, Authentication auth) { adminUserService.resendTeacherOnboardingEmail(id, auth.getName()); return ResponseEntity.noContent().build(); }
}
