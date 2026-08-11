package fu.sep490.g23.backend.entity.course;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;

import fu.sep490.g23.backend.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "course_discussion_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_discussion_report_user_target", columnNames = {"target_type", "target_id", "reporter_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class CourseDiscussionReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private CourseDiscussionReportTarget targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", nullable = false, length = 30)
    @Builder.Default
    private CourseDiscussionReportReasonCategory reasonCategory = CourseDiscussionReportReasonCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CourseDiscussionReportStatus status = CourseDiscussionReportStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "action_note", length = 500)
    private String actionNote;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
