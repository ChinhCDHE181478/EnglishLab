package fu.sap490.g23.backend.service.admin.impl;

import fu.sap490.g23.backend.dto.request.admin.*;
import fu.sap490.g23.backend.dto.response.admin.*;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.service.admin.AdminUserService;
import fu.sap490.g23.backend.service.admin.AuditLogService;
import fu.sap490.g23.backend.service.auth.AuthTokenService;
import fu.sap490.g23.backend.service.mail.AuthMailService;
import fu.sap490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final AuthTokenService authTokenService;
    private final AuthMailService authMailService;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        var users = userRepository.findAll();
        return AdminDashboardResponse.builder()
                .totalUsers(users.size())
                .learners(users.stream().filter(user -> user.hasRole(RoleEnum.LEARNER)).count())
                .teachers(users.stream().filter(user -> user.hasRole(RoleEnum.TEACHER)).count())
                .staffAndAdmins(users.stream().filter(user -> user.getRoleCodes().stream().anyMatch(role -> role != RoleEnum.LEARNER && role != RoleEnum.TEACHER)).count())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(String keyword, RoleEnum role, Pageable pageable) {
        Specification<User> specification = (root, query, cb) -> cb.conjunction();
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phoneNumber")), pattern)));
        }
        if (role != null) {
            specification = specification.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.join("roles").get("code"), role);
            });
        }
        return userRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    public AdminUserResponse createUser(UpsertAdminUserRequest request, String requesterEmail) {
        requireAdmin(requesterEmail);
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("Email đã được sử dụng.");
        Set<RoleEnum> roles = rolesOrLearner(request.getRoles());
        boolean teacher = roles.contains(RoleEnum.TEACHER);
        if (!teacher && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw new IllegalArgumentException("Mật khẩu là bắt buộc khi tạo người dùng.");
        }
        String initialPassword = request.getPassword() == null || request.getPassword().isBlank()
                ? UUID.randomUUID().toString()
                : request.getPassword();
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phoneNumber(trimToNull(request.getPhoneNumber()))
                .password(passwordEncoder.encode(initialPassword))
                .emailVerified(true)
                .profileCompleted(false)
                .build();
        userRoleService.replaceRoles(user, roles);
        User saved = userRepository.save(user);
        if (teacher) {
            var setupToken = authTokenService.issuePasswordResetToken(saved);
            authMailService.sendStaffCreatedAccountEmail(saved, setupToken.getToken());
        }
        auditLogService.record(requesterEmail,"ADMIN_USER_CREATED","USER",saved.getId().toString(),"Tạo người dùng " + saved.getEmail());
        return toResponse(saved);
    }

    @Override
    public AdminUserResponse updateUser(Long id, UpsertAdminUserRequest request, String requesterEmail) {
        requireAdmin(requesterEmail);
        User user = findUser(id);
        boolean wasTeacher = user.hasRole(RoleEnum.TEACHER);
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> { throw new IllegalArgumentException("Email đã được sử dụng."); });
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        if (request.getPassword() != null && !request.getPassword().isBlank()) user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getRoles() != null && !request.getRoles().isEmpty()) userRoleService.replaceRoles(user, request.getRoles());
        User saved = userRepository.save(user);
        notifyWhenTeacherRoleIsAdded(saved, wasTeacher);
        auditLogService.record(requesterEmail,"ADMIN_USER_UPDATED","USER",id.toString(),"Cập nhật hồ sơ " + saved.getEmail());
        return toResponse(saved);
    }

    @Override
    public AdminUserResponse updateRoles(Long id, UpdateUserRolesRequest request, String requesterEmail) {
        requireAdmin(requesterEmail);
        User user = findUser(id);
        boolean wasTeacher = user.hasRole(RoleEnum.TEACHER);
        userRoleService.replaceRoles(user, request.getRoles());
        User saved = userRepository.save(user);
        notifyWhenTeacherRoleIsAdded(saved, wasTeacher);
        auditLogService.record(requesterEmail,"ADMIN_USER_ROLES_UPDATED","USER",id.toString(),"Vai trò: " + request.getRoles());
        return toResponse(saved);
    }

    @Override
    public AdminUserResponse updateStatus(Long id, UpdateAdminUserStatusRequest request, String requesterEmail) {
        User requester = requireAdmin(requesterEmail);
        User user = findUser(id);
        if (requester.getId().equals(id) && !request.getEnabled()) throw new IllegalArgumentException("Bạn không thể tự vô hiệu hóa tài khoản đang đăng nhập.");
        user.setEmailVerified(request.getEnabled());
        User saved = userRepository.save(user);
        auditLogService.record(requesterEmail,"ADMIN_USER_STATUS_UPDATED","USER",id.toString(),"Trạng thái: " + (request.getEnabled()?"ENABLED":"DISABLED"));
        return toResponse(saved);
    }

    @Override
    public void resendTeacherOnboardingEmail(Long id, String requesterEmail) {
        requireAdmin(requesterEmail);
        User teacher = findUser(id);
        if (!teacher.hasRole(RoleEnum.TEACHER)) {
            throw new IllegalArgumentException("Người dùng này không có vai trò giáo viên.");
        }
        var setupToken = authTokenService.issuePasswordResetToken(teacher);
        authMailService.sendStaffCreatedAccountEmail(teacher, setupToken.getToken());
        auditLogService.record(
                requesterEmail,
                "TEACHER_ONBOARDING_EMAIL_RESENT",
                "USER",
                id.toString(),
                "Gửi lại email thiết lập cho " + teacher.getEmail()
        );
    }

    private User requireAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy quản trị viên."));
        if (!user.hasRole(RoleEnum.ADMIN)) throw new SecurityException("Chỉ ADMIN được quản lý người dùng.");
        return user;
    }

    private User findUser(Long id) { return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng.")); }
    private Set<RoleEnum> rolesOrLearner(Set<RoleEnum> roles) { return roles == null || roles.isEmpty() ? Set.of(RoleEnum.LEARNER) : roles; }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void notifyWhenTeacherRoleIsAdded(User user, boolean wasTeacher) {
        if (!wasTeacher && user.hasRole(RoleEnum.TEACHER)) {
            authMailService.sendTeacherGoogleMeetInvitation(user);
        }
    }
    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder().id(user.getId()).fullName(user.getFullName()).email(user.getEmail()).phoneNumber(user.getPhoneNumber())
                .roles(user.getRoleCodes()).profileCompleted(user.isProfileCompleted()).emailVerified(user.isEmailVerified()).enabled(user.isEnabled())
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }
}
