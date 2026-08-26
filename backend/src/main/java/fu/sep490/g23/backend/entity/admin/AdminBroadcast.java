package fu.sep490.g23.backend.entity.admin;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.admin.enums.BroadcastStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_broadcasts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBroadcast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(name = "target_role", length = 30)
    private String targetRole;

    @Column(name = "action_path", length = 500)
    private String actionPath;

    @Column(name = "send_in_app", nullable = false)
    @Builder.Default
    private boolean sendInApp = true;

    @Column(name = "send_email", nullable = false)
    @Builder.Default
    private boolean sendEmail = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BroadcastStatus status = BroadcastStatus.DRAFT;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "recipient_count", nullable = false)
    @Builder.Default
    private int recipientCount = 0;

    @Column(name = "in_app_success_count", nullable = false)
    @Builder.Default
    private int inAppSuccessCount = 0;

    @Column(name = "email_queued_count", nullable = false)
    @Builder.Default
    private int emailQueuedCount = 0;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
