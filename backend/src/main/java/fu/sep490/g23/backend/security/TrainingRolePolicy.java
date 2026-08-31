package fu.sep490.g23.backend.security;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;

import java.util.Set;

/** Nguồn sự thật tập trung cho quyền vận hành đào tạo. */
public final class TrainingRolePolicy {

    public static final Set<String> OPERATIONS_ROLES = Set.of(
            RoleCodes.STAFF, RoleCodes.MANAGER, RoleCodes.ADMIN);

    public static final Set<String> STAFF_ACTION_ROLES = Set.of(RoleCodes.STAFF, RoleCodes.ADMIN);

    public static final Set<String> APPROVAL_ROLES = Set.of(RoleCodes.MANAGER, RoleCodes.ADMIN);

    private TrainingRolePolicy() {
    }

    public static boolean canOperate(User user) {
        return user != null && user.hasAnyRoleCodes(OPERATIONS_ROLES);
    }

    public static boolean canPerformStaffAction(User user) {
        return user != null && user.hasAnyRoleCodes(STAFF_ACTION_ROLES);
    }

    public static boolean canApprove(User user) {
        return user != null && user.hasAnyRoleCodes(APPROVAL_ROLES);
    }
}
