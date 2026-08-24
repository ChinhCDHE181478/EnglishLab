package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.response.classroom.AppNotificationResponse;
import fu.sep490.g23.backend.service.notification.AppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/notifications")
@RequiredArgsConstructor
public class StudentNotificationController {

    private final AppNotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<AppNotificationResponse>> listNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.listForUser(authentication.getName()));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<AppNotificationResponse>> pageNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            Authentication authentication
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(notificationService.pageForUser(
                authentication.getName(),
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread(Authentication authentication) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(authentication.getName())));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<AppNotificationResponse> markRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.markRead(notificationId, authentication.getName()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
