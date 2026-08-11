package fu.sep490.g23.backend.service.notification.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.classroom.AppNotificationResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.notification.AppNotification;
import fu.sep490.g23.backend.repository.notification.AppNotificationRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.ClassroomMapper;
import fu.sep490.g23.backend.service.notification.AppNotificationService;
import fu.sep490.g23.backend.service.notification.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppNotificationServiceImpl implements AppNotificationService {

    private final AppNotificationRepository notificationRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomMapper classroomMapper;
    private final NotificationPreferenceService preferenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<AppNotificationResponse> listForUser(String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(classroomMapper::toNotificationResponse)
                .toList();
    }

    @Override
    public AppNotificationResponse markRead(Long notificationId, String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo."));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật thông báo này.");
        }
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return classroomMapper.toNotificationResponse(notificationRepository.save(notification));
    }

    @Override
    public void markAllRead(String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(notification -> !notification.isRead())
                .forEach(notification -> {
                    notification.setRead(true);
                    notification.setReadAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Override
    public void createForUser(
            User user,
            String type,
            String title,
            String body,
            Map<String, Object> metadata
    ) {
        createForUserOnce(user, type, title, body, null, null, metadata);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createForUserOnce(
            User user,
            String type,
            String title,
            String body,
            String actionPath,
            String deduplicationKey,
            Map<String, Object> metadata
    ) {
        if (user == null || !preferenceService.isInAppEnabled(user)) {
            return false;
        }
        if (deduplicationKey != null
                && notificationRepository.existsByUserIdAndDeduplicationKey(user.getId(), deduplicationKey)) {
            return false;
        }
        try {
            String metadataJson = metadata == null ? null : objectMapper.writeValueAsString(metadata);
            notificationRepository.save(AppNotification.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .body(body)
                    .metadataJson(metadataJson)
                    .actionPath(actionPath)
                    .deduplicationKey(deduplicationKey)
                    .build());
            return true;
        } catch (JsonProcessingException exception) {
            log.warn("Không thể tạo thông báo cho user {}: {}", user.getId(), exception.getMessage());
            return false;
        }
    }
}
