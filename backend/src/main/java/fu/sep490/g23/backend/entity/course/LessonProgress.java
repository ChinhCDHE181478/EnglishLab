package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lesson_progress_enrollment_lesson",
                columnNames = {"online_course_enrollment_id", "online_lesson_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class LessonProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_lesson_id", nullable = false)
    private OnlineLesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_course_enrollment_id", nullable = false)
    private OnlineCourseEnrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LessonProgressStatus status = LessonProgressStatus.NOT_STARTED;

    @Column(name = "first_accessed_at")
    private LocalDateTime firstAccessedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
