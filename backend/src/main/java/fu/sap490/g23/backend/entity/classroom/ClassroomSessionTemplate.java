package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_session_templates")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomSessionTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "text", nullable = false)
    private String slotsJson;

    @Column(length = 500)
    private String description;

    @Column(name = "teacher_guide", columnDefinition = "text")
    private String teacherGuide;

    @Column(name = "interaction_activities", columnDefinition = "text")
    private String interactionActivities;

    @Column(name = "post_session_homework", columnDefinition = "text")
    private String postSessionHomework;

    @Column(name = "default_duration_minutes")
    private Integer defaultDurationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
