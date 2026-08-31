package fu.sep490.g23.backend.service.notification.impl;

import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.service.notification.AppNotificationService;
import fu.sep490.g23.backend.service.notification.ClassroomNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ClassroomNotificationServiceImpl implements ClassroomNotificationService {

    private static final Set<String> TRAINING_STAFF_ROLES = Set.of(
            RoleCodes.STAFF, RoleCodes.MANAGER, RoleCodes.ADMIN);

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
