package fu.sep490.g23.backend.entity.course;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.course.enums.VocabularyProgressStatus;
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
@SQLRestriction("record_type = 'vocabulary_progress'")
@EntityListeners(AuditingEntityListener.class)
public class VocabularyProgress extends DomainRecord {

    @Override
    protected String domainRecordType() {
        return "vocabulary_progress";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private OnlineCourse course;

    @Column(name = "term_key", nullable = false, length = 220)
    private String termKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "vocabulary_status", nullable = false, length = 30)
    @Builder.Default
    private VocabularyProgressStatus status = VocabularyProgressStatus.NEW;

    @Column(nullable = false)
    @Builder.Default
    private boolean starred = false;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "correct_count")
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "incorrect_count")
    @Builder.Default
    private Integer incorrectCount = 0;

    @Column(name = "last_result_correct")
    private Boolean lastResultCorrect;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
