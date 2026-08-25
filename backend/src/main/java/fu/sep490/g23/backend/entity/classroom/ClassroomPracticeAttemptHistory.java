package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_practice_attempt_history")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomPracticeAttemptHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_content_bank_item_id")
    private ExerciseBankItem exercise;

    /** Legacy exercise_bank_items id retained for Slice-3 dual column; drop in Slice 10. */
    @Column(name = "exercise_id", nullable = false)
    private Long legacyExerciseId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "response_text", columnDefinition = "text")
    private String responseText;

    @Column(name = "answers_json", columnDefinition = "text")
    private String answersJson;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "score_percent")
    private Double scorePercent;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
