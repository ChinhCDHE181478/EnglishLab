package fu.sep490.g23.backend.service.user.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.service.user.UserRoleService;
import java.util.Collection;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl implements UserRoleService {
    @Override
    public void assignRole(User user, RoleEnum role) {
        if (user.hasRole(role)) {
            return;
        }
        user.getRoles().add(role);
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
