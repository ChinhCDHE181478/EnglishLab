package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(200)
@RequiredArgsConstructor
public class LearnerAccountRoleRepairSeeder implements CommandLineRunner {

    static final String LEARNER_EMAIL = "0386852628z@gmail.com";

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByEmail(LEARNER_EMAIL).ifPresent(this::ensureLearnerRoleOnly);
    }

    private void ensureLearnerRoleOnly(User user) {
        if (user.getRoles().size() == 1 && user.hasRole(RoleEnum.LEARNER)) {
            return;
        }
        userRoleService.replaceRoles(user, RoleEnum.LEARNER);
        userRepository.save(user);
    }
}
