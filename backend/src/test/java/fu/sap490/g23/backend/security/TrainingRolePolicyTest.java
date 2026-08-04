package fu.sap490.g23.backend.security;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingRolePolicyTest {

    @Test
    void staffCanOperateButCannotApprove() {
        User staff = user(RoleEnum.STAFF);

        assertThat(TrainingRolePolicy.canOperate(staff)).isTrue();
        assertThat(TrainingRolePolicy.canPerformStaffAction(staff)).isTrue();
        assertThat(TrainingRolePolicy.canApprove(staff)).isFalse();
    }

    @Test
    void managerApprovesButDoesNotPerformStaffActions() {
        User manager = user(RoleEnum.MANAGER);

        assertThat(TrainingRolePolicy.canOperate(manager)).isTrue();
        assertThat(TrainingRolePolicy.canPerformStaffAction(manager)).isFalse();
        assertThat(TrainingRolePolicy.canApprove(manager)).isTrue();
    }

    @Test
    void adminHasBothCapabilitiesAndLearnerHasNeither() {
        User admin = user(RoleEnum.ADMIN);
        User learner = user(RoleEnum.LEARNER);

        assertThat(TrainingRolePolicy.canPerformStaffAction(admin)).isTrue();
        assertThat(TrainingRolePolicy.canApprove(admin)).isTrue();
        assertThat(TrainingRolePolicy.canOperate(learner)).isFalse();
        assertThat(TrainingRolePolicy.canPerformStaffAction(learner)).isFalse();
        assertThat(TrainingRolePolicy.canApprove(learner)).isFalse();
        assertThat(TrainingRolePolicy.canOperate(null)).isFalse();
    }

    private User user(RoleEnum role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}
