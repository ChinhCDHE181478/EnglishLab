package fu.sap490.g23.backend.service.admin.impl;

import fu.sap490.g23.backend.dto.request.admin.*;
import fu.sap490.g23.backend.dto.response.admin.*;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.service.admin.AdminUserService;
import fu.sap490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

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
        if (request.getPassword() == null || request.getPassword().isBlank()) throw new IllegalArgumentException("Mật khẩu là bắt buộc khi tạo người dùng.");
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phoneNumber(trimToNull(request.getPhoneNumber()))
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(true)
                .profileCompleted(false)
                .build();
        userRoleService.replaceRoles(user, rolesOrLearner(request.getRoles()));
        return toResponse(userRepository.save(user));
    }

    @Override
    public AdminUserResponse updateUser(Long id, UpsertAdminUserRequest request, String requesterEmail) {
        requireAdmin(requesterEmail);
        User user = findUser(id);
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> { throw new IllegalArgumentException("Email đã được sử dụng."); });
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        if (request.getPassword() != null && !request.getPassword().isBlank()) user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (request.getRoles() != null && !request.getRoles().isEmpty()) userRoleService.replaceRoles(user, request.getRoles());
        return toResponse(userRepository.save(user));
    }

    @Override
    public AdminUserResponse updateRoles(Long id, UpdateUserRolesRequest request, String requesterEmail) {
        requireAdmin(requesterEmail);
        User user = findUser(id);
        userRoleService.replaceRoles(user, request.getRoles());
        return toResponse(userRepository.save(user));
    }

    @Override
    public AdminUserResponse updateStatus(Long id, UpdateAdminUserStatusRequest request, String requesterEmail) {
        User requester = requireAdmin(requesterEmail);
        User user = findUser(id);
        if (requester.getId().equals(id) && !request.getEnabled()) throw new IllegalArgumentException("Bạn không thể tự vô hiệu hóa tài khoản đang đăng nhập.");
        user.setEmailVerified(request.getEnabled());
        return toResponse(userRepository.save(user));
    }

    private User requireAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy quản trị viên."));
        if (!user.hasRole(RoleEnum.ADMIN)) throw new SecurityException("Chỉ ADMIN được quản lý người dùng.");
        return user;
    }

    private User findUser(Long id) { return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng.")); }
    private Set<RoleEnum> rolesOrLearner(Set<RoleEnum> roles) { return roles == null || roles.isEmpty() ? Set.of(RoleEnum.LEARNER) : roles; }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder().id(user.getId()).fullName(user.getFullName()).email(user.getEmail()).phoneNumber(user.getPhoneNumber())
                .roles(user.getRoleCodes()).profileCompleted(user.isProfileCompleted()).emailVerified(user.isEmailVerified()).enabled(user.isEnabled())
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }
}
