package fu.sep490.g23.backend.security;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;

import java.util.Set;

public final class ContentManagementRolePolicy {
    public static final Set<String> EDITOR_ROLES = Set.of(RoleCodes.CONTENT_MANAGER, RoleCodes.ADMIN);

    private ContentManagementRolePolicy() {
    }

    public static boolean canEdit(User user) {
        return user != null && user.hasAnyRoleCodes(EDITOR_ROLES);
    }

}
