package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
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
@Table(name = "learner_progress_records")
@SQLRestriction("record_type = 'lesson_progress'")
@EntityListeners(AuditingEntityListener.class)
public class LessonProgress extends DomainRecord {

    @Override
    protected String domainRecordType() {
        return "lesson_progress";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private PackageEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_version_id")
    private OnlineCourseVersion courseVersion;

    @Column(name = "lesson_key", length = 120)
    private String lessonKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_progress_status", nullable = false, length = 30)
    @Builder.Default
    private LessonProgressStatus status = LessonProgressStatus.NOT_STARTED;

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
