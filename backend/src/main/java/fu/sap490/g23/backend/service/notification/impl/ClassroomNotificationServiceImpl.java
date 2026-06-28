package fu.sap490.g23.backend.service.notification.impl;

import fu.sap490.g23.backend.service.notification.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.notification.AppNotification;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.notification.AppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomNotificationServiceImpl implements ClassroomNotificationService {

    private static final Set<RoleEnum> TRAINING_MANAGER_ROLES = EnumSet.of(
            RoleEnum.TRAINING_MANAGER,
            RoleEnum.MANAGER,
            RoleEnum.ADMIN
    );

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void notifyUser(User user, String type, String title, String body, Map<String, Object> metadata) {
        try {
            String metadataJson = metadata == null ? null : objectMapper.writeValueAsString(metadata);
            notificationRepository.save(AppNotification.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .body(body)
                    .metadataJson(metadataJson)
                    .build());
        } catch (JsonProcessingException ex) {
            log.warn("Không thể tạo thông báo cho user {}: {}", user.getId(), ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<User> findTrainingManagers() {
        return userRepository.findDistinctByRoles_CodeIn(TRAINING_MANAGER_ROLES);
    }

    @Transactional
    public void notifyTrainingManagers(String type, String title, String body, Map<String, Object> metadata) {
        findTrainingManagers().forEach(manager -> notifyUser(manager, type, title, body, metadata));
    }
}
