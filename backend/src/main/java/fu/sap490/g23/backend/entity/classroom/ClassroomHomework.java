package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_homework")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomHomework {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ClassroomSession session;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(columnDefinition = "text")
    private String instruction;

    @Column
    private LocalDateTime deadline;

    @Column(name = "max_score", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal maxScore = BigDecimal.valueOf(10);

    @Column(name = "allow_resubmission", nullable = false)
    @Builder.Default
    private boolean allowResubmission = false;

    @Column(name = "attachment_url", length = 700)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HomeworkStatus status = HomeworkStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
