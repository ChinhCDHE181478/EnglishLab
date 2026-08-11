package fu.sep490.g23.backend.service.admin.impl;

import fu.sep490.g23.backend.dto.request.admin.ScheduleAdminBroadcastRequest;
import fu.sep490.g23.backend.dto.request.admin.UpsertAdminBroadcastRequest;
import fu.sep490.g23.backend.dto.response.admin.AdminBroadcastResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.admin.AdminBroadcast;
import fu.sep490.g23.backend.entity.admin.enums.BroadcastStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.admin.AdminBroadcastRepository;
import fu.sep490.g23.backend.service.admin.AdminBroadcastService;
import fu.sep490.g23.backend.service.admin.AuditLogService;
import fu.sep490.g23.backend.service.mail.LearningReminderMailService;
import fu.sep490.g23.backend.service.notification.AppNotificationService;
import fu.sep490.g23.backend.service.notification.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBroadcastServiceImpl implements AdminBroadcastService {
    private final AdminBroadcastRepository repository;
    private final UserRepository userRepository;
    private final AppNotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final LearningReminderMailService mailService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminBroadcastResponse> list(BroadcastStatus status, Pageable pageable) {
        return (status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public AdminBroadcastResponse create(String actorEmail, UpsertAdminBroadcastRequest request) {
        User actor = requireUser(actorEmail);
        AdminBroadcast item = AdminBroadcast.builder().createdBy(actor).build();
        applyEditableFields(item, request);
        item = repository.save(item);
        auditLogService.record(actorEmail, "BROADCAST_CREATE", "ADMIN_BROADCAST", item.getId().toString(),
                "Tạo bản nháp thông báo \"" + item.getTitle() + "\".");
        return toResponse(item);
    }

    @Override
    @Transactional
    public AdminBroadcastResponse update(String actorEmail, Long id, UpsertAdminBroadcastRequest request) {
        AdminBroadcast item = requireEditable(id);
        applyEditableFields(item, request);
        item = repository.save(item);
        auditLogService.record(actorEmail, "BROADCAST_UPDATE", "ADMIN_BROADCAST", id.toString(),
                "Cập nhật bản nháp thông báo.");
        return toResponse(item);
    }

    @Override
    @Transactional
    public AdminBroadcastResponse schedule(String actorEmail, Long id, ScheduleAdminBroadcastRequest request) {
        AdminBroadcast item = requireEditable(id);
        if (request.getScheduledAt().isBefore(LocalDateTime.now().plusMinutes(1))) {
            throw new IllegalArgumentException("Thời gian gửi phải cách thời điểm hiện tại ít nhất 1 phút.");
        }
        item.setStatus(BroadcastStatus.SCHEDULED);
        item.setScheduledAt(request.getScheduledAt());
        item.setFailureReason(null);
        item = repository.save(item);
        auditLogService.record(actorEmail, "BROADCAST_SCHEDULE", "ADMIN_BROADCAST", id.toString(),
                "Hẹn gửi lúc " + request.getScheduledAt() + ".");
        return toResponse(item);
    }

    @Override
    @Transactional
    public AdminBroadcastResponse sendNow(String actorEmail, Long id) {
        AdminBroadcast item = requireEditable(id);
        deliver(item);
        auditLogService.record(actorEmail, "BROADCAST_SEND", "ADMIN_BROADCAST", id.toString(),
                "Gửi thông báo ngay tới " + item.getRecipientCount() + " người nhận.");
        return toResponse(item);
    }

    @Override
    @Transactional
    public AdminBroadcastResponse cancel(String actorEmail, Long id) {
        AdminBroadcast item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo."));
        if (item.getStatus() != BroadcastStatus.SCHEDULED) {
            throw new IllegalArgumentException("Chỉ thông báo đang hẹn giờ mới có thể hủy.");
        }
        item.setStatus(BroadcastStatus.CANCELLED);
        item.setScheduledAt(null);
        item = repository.save(item);
        auditLogService.record(actorEmail, "BROADCAST_CANCEL", "ADMIN_BROADCAST", id.toString(),
                "Hủy lịch gửi thông báo.");
        return toResponse(item);
    }

    @Override
    @Scheduled(fixedDelayString = "${englishlab.broadcast.scan-delay-ms:60000}",
            initialDelayString = "${englishlab.broadcast.initial-delay-ms:30000}")
    @Transactional
    public void dispatchScheduled() {
        repository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                BroadcastStatus.SCHEDULED, LocalDateTime.now()).forEach(item -> {
            try {
                deliver(item);
            } catch (Exception exception) {
                item.setStatus(BroadcastStatus.FAILED);
                item.setFailureReason(trim(exception.getMessage(), 1000));
                repository.save(item);
                log.error("Không thể gửi broadcast #{}: {}", item.getId(), exception.getMessage(), exception);
            }
        });
    }

    private void deliver(AdminBroadcast item) {
        if (!item.isSendInApp() && !item.isSendEmail()) {
            throw new IllegalArgumentException("Thông báo phải có ít nhất một kênh gửi.");
        }
        item.setStatus(BroadcastStatus.SENDING);
        repository.save(item);
        List<User> recipients = item.getTargetRole() == null
                ? userRepository.findAll().stream().filter(User::isEnabled).toList()
                : userRepository.findDistinctByRoles_CodeIn(List.of(item.getTargetRole())).stream()
                        .filter(User::isEnabled).toList();
        int inAppCount = 0;
        int emailCount = 0;
        for (User recipient : recipients) {
            if (item.isSendInApp() && notificationService.createForUserOnce(
                    recipient,
                    "ADMIN_BROADCAST",
                    item.getTitle(),
                    item.getMessage(),
                    item.getActionPath(),
                    "ADMIN_BROADCAST_" + item.getId(),
                    Map.of("broadcastId", item.getId())
            )) {
                inAppCount++;
            }
            if (item.isSendEmail() && preferenceService.isEmailEnabled(recipient)) {
                mailService.sendReminder(recipient, item.getTitle() + " - EnglishLab",
                        item.getTitle(), item.getMessage(), item.getActionPath());
                emailCount++;
            }
        }
        item.setRecipientCount(recipients.size());
        item.setInAppSuccessCount(inAppCount);
        item.setEmailQueuedCount(emailCount);
        item.setScheduledAt(null);
        item.setSentAt(LocalDateTime.now());
        item.setFailureReason(null);
        item.setStatus(BroadcastStatus.SENT);
        repository.save(item);
    }

    private void applyEditableFields(AdminBroadcast item, UpsertAdminBroadcastRequest request) {
        String path = trim(request.getActionPath(), 500);
        if (path != null && !path.isBlank() && (!path.startsWith("/") || path.startsWith("//"))) {
            throw new IllegalArgumentException("Đường dẫn hành động phải là đường dẫn nội bộ bắt đầu bằng một dấu /.");
        }
        if (!Boolean.TRUE.equals(request.getSendInApp()) && !Boolean.TRUE.equals(request.getSendEmail())) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một kênh gửi.");
        }
        item.setTitle(request.getTitle().trim());
        item.setMessage(request.getMessage().trim());
        item.setTargetRole(request.getTargetRole());
        item.setActionPath(path == null || path.isBlank() ? null : path);
        item.setSendInApp(Boolean.TRUE.equals(request.getSendInApp()));
        item.setSendEmail(Boolean.TRUE.equals(request.getSendEmail()));
    }

    private AdminBroadcast requireEditable(Long id) {
        AdminBroadcast item = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo."));
        if (item.getStatus() != BroadcastStatus.DRAFT && item.getStatus() != BroadcastStatus.SCHEDULED
                && item.getStatus() != BroadcastStatus.FAILED) {
            throw new IllegalArgumentException("Thông báo đã gửi hoặc đã hủy không thể chỉnh sửa.");
        }
        if (item.getStatus() == BroadcastStatus.SCHEDULED) {
            item.setStatus(BroadcastStatus.DRAFT);
            item.setScheduledAt(null);
        }
        return item;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị."));
    }

    private AdminBroadcastResponse toResponse(AdminBroadcast item) {
        return AdminBroadcastResponse.builder()
                .id(item.getId()).title(item.getTitle()).message(item.getMessage())
                .targetRole(item.getTargetRole()).actionPath(item.getActionPath())
                .sendInApp(item.isSendInApp()).sendEmail(item.isSendEmail()).status(item.getStatus())
                .scheduledAt(item.getScheduledAt()).sentAt(item.getSentAt())
                .recipientCount(item.getRecipientCount()).inAppSuccessCount(item.getInAppSuccessCount())
                .emailQueuedCount(item.getEmailQueuedCount()).failureReason(item.getFailureReason())
                .createdBy(item.getCreatedBy().getEmail()).createdAt(item.getCreatedAt()).updatedAt(item.getUpdatedAt())
                .build();
    }

    private String trim(String value, int maximumLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= maximumLength ? trimmed : trimmed.substring(0, maximumLength);
    }
}
