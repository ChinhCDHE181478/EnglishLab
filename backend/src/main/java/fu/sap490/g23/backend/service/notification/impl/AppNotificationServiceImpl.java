package fu.sap490.g23.backend.service.notification.impl;

import fu.sap490.g23.backend.service.notification.*;


import fu.sap490.g23.backend.dto.response.classroom.AppNotificationResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.notification.AppNotification;
import fu.sap490.g23.backend.repository.notification.AppNotificationRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.ClassroomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppNotificationServiceImpl implements AppNotificationService {

    private final AppNotificationRepository notificationRepository;
    private final ClassroomAccessHelper accessHelper;
    private final ClassroomMapper classroomMapper;

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
}
