package fu.sep490.g23.backend.service.user.impl;
import fu.sep490.g23.backend.service.user.UserRoleService;

import fu.sep490.g23.backend.entity.Role;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import java.util.Collection;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {
    private final RoleRepository roleRepository;

    @Override
    public void assignRole(User user, RoleEnum role) {
        if (user.hasRole(role)) {
            return;
        }
        Role roleEntity = roleRepository.findByCodeAndActiveTrue(role)
                .orElseThrow(() -> new IllegalStateException("Role chưa được cấu hình: " + role));
        user.getRoles().add(roleEntity);
    }

    @Override
    public void ensureRole(User user, RoleEnum role) {
        assignRole(user, role);
    }

    @Override
    public void replaceRoles(User user, RoleEnum role) {
        user.getRoles().clear();
        assignRole(user, role);
    }

    @Override
    public void replaceRoles(User user, Collection<RoleEnum> roles) {
        user.getRoles().clear();
        roles.forEach(role -> assignRole(user, role));
    }
}
