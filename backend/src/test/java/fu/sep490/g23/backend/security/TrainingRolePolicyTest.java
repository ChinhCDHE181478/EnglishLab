package fu.sep490.g23.backend.security;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingRolePolicyTest {

    @Test
    void staffCanOperateButCannotApprove() {
        User staff = user(RoleCodes.STAFF);

        assertThat(TrainingRolePolicy.canOperate(staff)).isTrue();
        assertThat(TrainingRolePolicy.canPerformStaffAction(staff)).isTrue();
        assertThat(TrainingRolePolicy.canApprove(staff)).isFalse();
    }

    @Test
    void managerApprovesButDoesNotPerformStaffActions() {
        User manager = user(RoleCodes.MANAGER);

        assertThat(TrainingRolePolicy.canOperate(manager)).isTrue();
        assertThat(TrainingRolePolicy.canPerformStaffAction(manager)).isFalse();
        assertThat(TrainingRolePolicy.canApprove(manager)).isTrue();
    }

    @Test
    void adminHasBothCapabilitiesAndLearnerHasNeither() {
        User admin = user(RoleCodes.ADMIN);
        User learner = user(RoleCodes.LEARNER);

        assertThat(TrainingRolePolicy.canPerformStaffAction(admin)).isTrue();
        assertThat(TrainingRolePolicy.canApprove(admin)).isTrue();
        assertThat(TrainingRolePolicy.canOperate(learner)).isFalse();
        assertThat(TrainingRolePolicy.canPerformStaffAction(learner)).isFalse();
        assertThat(TrainingRolePolicy.canApprove(learner)).isFalse();
        assertThat(TrainingRolePolicy.canOperate(null)).isFalse();
    }

    private User user(String roleCode) {
        User user = new User();
        user.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(roleCode));
        return user;
    }
}
