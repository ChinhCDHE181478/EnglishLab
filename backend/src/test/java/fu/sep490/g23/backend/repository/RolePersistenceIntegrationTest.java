package fu.sep490.g23.backend.repository;

import fu.sep490.g23.backend.entity.Role;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.service.user.UserRoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RolePersistenceIntegrationTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleService userRoleService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void existingSixRolesAreSeeded() {
        assertThat(roleRepository.findAll())
                .extracting(Role::getCode)
                .contains(RoleCodes.LEARNER, RoleCodes.TEACHER, RoleCodes.MANAGER,
                        RoleCodes.CONTENT_MANAGER, RoleCodes.STAFF, RoleCodes.ADMIN);
    }

    @Test
    void userCanHaveOneRole() {
        User user = newUser();
        userRoleService.assignRole(user, RoleCodes.LEARNER);

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getRoleCodes()).containsExactly(RoleCodes.LEARNER);
    }

    @Test
    void userCanHaveMultipleRoles() {
        User user = newUser();
        userRoleService.assignRole(user, RoleCodes.TEACHER);
        userRoleService.assignRole(user, RoleCodes.STAFF);

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getRoleCodes()).containsExactlyInAnyOrder(RoleCodes.TEACHER, RoleCodes.STAFF);
    }

    @Test
    void duplicateUserRolePairIsRejectedByDatabase() {
        User user = newUser();
        userRoleService.assignRole(user, RoleCodes.LEARNER);
        User saved = userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into user_roles(user_id, role_code) values (?, ?)",
                saved.getId(), RoleCodes.LEARNER))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nonexistentRoleCannotBeAssigned() {
        assertThatThrownBy(() -> userRoleService.assignRole(newUser(), "NOT_A_ROLE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NOT_A_ROLE");
    }

    @Test
    void invalidReplacementDoesNotPartiallyClearExistingRoles() {
        User user = newUser();
        userRoleService.assignRole(user, RoleCodes.LEARNER);

        assertThatThrownBy(() -> userRoleService.replaceRoles(
                user, List.of(RoleCodes.TEACHER, "NOT_A_ROLE")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(user.getRoleCodes()).containsExactly(RoleCodes.LEARNER);
    }

    @Test
    void nonexistentRoleCodeIsRejectedByDatabaseForeignKey() {
        User saved = userRepository.saveAndFlush(newUser());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into user_roles(user_id, role_code) values (?, ?)",
                saved.getId(), "NOT_A_ROLE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void inactiveRoleDoesNotProduceGrantedAuthority() {
        Role inactive = roleRepository.save(Role.builder()
                .code("INACTIVE_TEST_ROLE")
                .displayName("Inactive test role")
                .active(false)
                .build());
        User user = newUser();
        user.getRoles().add(inactive);

        assertThat(user.getAuthorities()).isEmpty();
    }

    @Test
    void dynamicRoleCanBeLoadedAssignedAndConvertedToAuthorityWithoutEnumChange() {
        roleRepository.saveAndFlush(Role.builder()
                .code("ACADEMIC_COORDINATOR")
                .displayName("Academic Coordinator")
                .active(true)
                .build());
        User user = newUser();
        userRoleService.assignRole(user, "ACADEMIC_COORDINATOR");

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getRoleCodes()).contains("ACADEMIC_COORDINATOR");
        assertThat(saved.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ACADEMIC_COORDINATOR");
    }

    @Test
    void reservedRolesStillProduceExistingSpringAuthorities() {
        User user = newUser();
        Set.of(RoleCodes.ADMIN, RoleCodes.STAFF, RoleCodes.TEACHER)
                .forEach(code -> userRoleService.assignRole(user, code));

        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_STAFF", "ROLE_TEACHER");
    }

    private User newUser() {
        String suffix = UUID.randomUUID().toString();
        return User.builder()
                .fullName("Role integration test")
                .email("role-test-" + suffix + "@englishlab.test")
                .password("not-used")
                .emailVerified(true)
                .build();
    }
}
