package fu.sep490.g23.backend.entity.assessment;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;

import fu.sep490.g23.backend.entity.assessment.enums.*;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
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
@Table(name = "placement_test_attempts")
public class PlacementTestAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "test_code", nullable = false, length = 80)
    private String testCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_bank_item_id")
    private ContentBankItem contentBankItem;

    @Column(name = "answers_json", nullable = false, columnDefinition = "text")
    private String answersJson;

    @Column(name = "device_check_json", columnDefinition = "text")
    private String deviceCheckJson;

    @Column(name = "listening_score", precision = 4, scale = 1)
    private BigDecimal listeningScore;

    @Column(name = "reading_score", precision = 4, scale = 1)
    private BigDecimal readingScore;

    @Column(name = "writing_score", precision = 4, scale = 1)
    private BigDecimal writingScore;

    @Column(name = "speaking_score", precision = 4, scale = 1)
    private BigDecimal speakingScore;

    @Column(name = "overall_score", precision = 4, scale = 1)
    private BigDecimal overallScore;

    @Column(name = "correct_listening")
    private Integer correctListening;

    @Column(name = "correct_reading")
    private Integer correctReading;

    @Column(name = "ai_feedback_json", columnDefinition = "text")
    private String aiFeedbackJson;

    @Column(nullable = false, length = 40)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false, length = 40, columnDefinition = "varchar(40) default 'SUBMITTED'")
    @Builder.Default
    private PlacementEvaluationStatus evaluationStatus = PlacementEvaluationStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_level", length = 30)
    private PlacementLevel recommendedLevel;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 700)
    private String reviewNote;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
}
