package fu.sep490.g23.backend.entity.assessment;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;

import fu.sep490.g23.backend.entity.assessment.enums.*;

import fu.sep490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
@Table(name = "assessment_submissions")
@EntityListeners(AuditingEntityListener.class)
public class AssessmentSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private CourseAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "submitted_text", columnDefinition = "text")
    private String submittedText;

    @Column(name = "submitted_audio_url", length = 700)
    private String submittedAudioUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "objective_answers", columnDefinition = "jsonb")
    private String objectiveAnswers;

    @Column(name = "fullscreen_exit_count")
    private Integer fullscreenExitCount;

    @Column(name = "tab_switch_count")
    private Integer tabSwitchCount;

    @Column(precision = 4, scale = 1)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_feedback", columnDefinition = "jsonb")
    private String aiFeedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @CreatedDate
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
}
