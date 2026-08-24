package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_auxiliary_records")
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("record_type = 'learner_lesson_notes'")
public class LearnerLessonNote extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "learner_lesson_notes";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private OnlineCourse course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Lesson lesson;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "selected_text", length = 2000)
    private String selectedText;

    @Column(name = "transcript_start_seconds")
    private Integer transcriptStartSeconds;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
