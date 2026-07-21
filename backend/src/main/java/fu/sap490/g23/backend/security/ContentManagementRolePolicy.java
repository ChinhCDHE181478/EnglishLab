package fu.sap490.g23.backend.security;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;

import java.util.EnumSet;
import java.util.Set;

public final class ContentManagementRolePolicy {
    public static final Set<RoleEnum> EDITOR_ROLES = Set.copyOf(EnumSet.of(
            RoleEnum.CONTENT_MANAGER,
            RoleEnum.MANAGER,
            RoleEnum.ADMIN
    ));

    public static final Set<RoleEnum> APPROVAL_ROLES = Set.copyOf(EnumSet.of(
            RoleEnum.MANAGER,
            RoleEnum.ADMIN
    ));

    private ContentManagementRolePolicy() {
    }

    public static boolean canEdit(User user) {
        return user != null && user.hasAnyRole(EDITOR_ROLES);
    }

    public static boolean canApprove(User user) {
        return user != null && user.hasAnyRole(APPROVAL_ROLES);
    }
}
