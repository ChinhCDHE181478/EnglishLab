package fu.sap490.g23.backend.entity.assessment;

import fu.sap490.g23.backend.entity.User;
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

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
}
