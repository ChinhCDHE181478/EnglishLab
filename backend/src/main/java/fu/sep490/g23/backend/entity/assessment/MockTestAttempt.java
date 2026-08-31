package fu.sep490.g23.backend.entity.assessment;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mock_test_attempts")
public class MockTestAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_content_bank_item_id")
    private AssessmentBankItem assessmentBankItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssessmentSkill skill;

    @Column(name = "objective_answers_json", columnDefinition = "text")
    private String objectiveAnswersJson;

    @Column(name = "submitted_text", columnDefinition = "text")
    private String submittedText;

    @Column(name = "submitted_audio_url", length = 700)
    private String submittedAudioUrl;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(precision = 6, scale = 2)
    private BigDecimal percent;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
}
