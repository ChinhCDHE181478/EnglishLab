package fu.sap490.g23.backend.service.notification.impl;

import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.service.notification.AppNotificationService;
import fu.sap490.g23.backend.service.notification.ClassroomNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassroomNotificationServiceImpl implements ClassroomNotificationService {

    private static final Set<RoleEnum> TRAINING_STAFF_ROLES = EnumSet.of(
            RoleEnum.STAFF,
            RoleEnum.MANAGER,
            RoleEnum.ADMIN
    );

    private final UserRepository userRepository;
    private final AppNotificationService appNotificationService;

    @Transactional
    public void notifyUser(User user, String type, String title, String body, Map<String, Object> metadata) {
        appNotificationService.createForUser(user, type, title, body, metadata);
    }

    @Transactional(readOnly = true)
    public List<User> findTrainingStaff() {
        return userRepository.findDistinctByRoles_CodeIn(TRAINING_STAFF_ROLES);
    }

    @Transactional
    public void notifyTrainingStaff(String type, String title, String body, Map<String, Object> metadata) {
        findTrainingStaff().forEach(staff -> notifyUser(staff, type, title, body, metadata));
    }
}
