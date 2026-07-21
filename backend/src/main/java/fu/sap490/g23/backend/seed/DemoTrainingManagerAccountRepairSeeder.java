package fu.sap490.g23.backend.seed;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(201)
@RequiredArgsConstructor
public class DemoTrainingManagerAccountRepairSeeder implements CommandLineRunner {

    static final String TRAINING_MANAGER_EMAIL = "training.manager@englishlab.vn";
    static final String STAFF_EMAIL = "staff@englishlab.vn";
    static final String CLASSROOM_MANAGER_EMAIL = "classroom.manager@englishlab.vn";
    static final String DEMO_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        ensureDemoAccount(TRAINING_MANAGER_EMAIL, "Quản Lý Đào Tạo", RoleEnum.TRAINING_MANAGER);
        ensureDemoAccount(STAFF_EMAIL, "Nhân Viên Đào Tạo", RoleEnum.STAFF);
        ensureDemoAccount(CLASSROOM_MANAGER_EMAIL, "Quản Lý Lớp Học", RoleEnum.MANAGER);
    }

    private void ensureDemoAccount(String email, String fullName, RoleEnum role) {
        User user = userRepository.findByEmail(email).orElseGet(() -> User.builder()
                .email(email)
                .build());
        user.setFullName(fullName);
        user.setEmailVerified(true);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        userRoleService.replaceRoles(user, role);
        userRepository.save(user);
    }
}
