package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "classroom_quiz_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quiz_attempt_student",
                columnNames = {"quiz_id", "student_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class ClassroomQuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private ClassroomQuiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "answers_json", columnDefinition = "text")
    private String answersJson;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "passed", nullable = false)
    @Builder.Default
    private boolean passed = false;

    @CreatedDate
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
}
