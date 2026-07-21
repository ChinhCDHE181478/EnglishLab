package fu.sap490.g23.backend.security;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;

import java.util.EnumSet;
import java.util.Set;

/**
 * Nguồn sự thật tập trung cho quyền vận hành đào tạo.
 * TRAINING_MANAGER chỉ được giữ như alias tương thích trong giai đoạn chuyển đổi sang STAFF.
 */
public final class TrainingRolePolicy {

    public static final Set<RoleEnum> OPERATIONS_ROLES = Set.copyOf(EnumSet.of(
            RoleEnum.STAFF,
            RoleEnum.TRAINING_MANAGER,
            RoleEnum.MANAGER,
            RoleEnum.ADMIN
    ));

    public static final Set<RoleEnum> STAFF_ACTION_ROLES = Set.copyOf(EnumSet.of(
            RoleEnum.STAFF,
            RoleEnum.TRAINING_MANAGER,
            RoleEnum.ADMIN
    ));

    public static final Set<RoleEnum> APPROVAL_ROLES = Set.copyOf(EnumSet.of(
            RoleEnum.MANAGER,
            RoleEnum.ADMIN
    ));

    private TrainingRolePolicy() {
    }

    public static boolean canOperate(User user) {
        return user != null && user.hasAnyRole(OPERATIONS_ROLES);
    }

    public static boolean canPerformStaffAction(User user) {
        return user != null && user.hasAnyRole(STAFF_ACTION_ROLES);
    }

    public static boolean canApprove(User user) {
        return user != null && user.hasAnyRole(APPROVAL_ROLES);
    }
}
