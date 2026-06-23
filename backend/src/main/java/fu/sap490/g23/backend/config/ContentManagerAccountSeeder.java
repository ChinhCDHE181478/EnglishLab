package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.service.impl.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(200)
@RequiredArgsConstructor
public class ContentManagerAccountSeeder implements CommandLineRunner {

    static final String CONTENT_MANAGER_EMAIL = "0386852628z@gmail.com";

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByEmail(CONTENT_MANAGER_EMAIL).ifPresent(this::ensureContentManagerRole);
    }

    private void ensureContentManagerRole(User user) {
        if (user.hasRole(RoleEnum.CONTENT_MANAGER)) {
            return;
        }
        userRoleService.ensureRole(user, RoleEnum.CONTENT_MANAGER);
        userRepository.save(user);
    }
}
