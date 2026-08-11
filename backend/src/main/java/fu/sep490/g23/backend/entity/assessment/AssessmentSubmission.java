package fu.sep490.g23.backend.entity.assessment;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;

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

    @Column(name = "objective_answers_json", columnDefinition = "text")
    private String objectiveAnswersJson;

    @Column(name = "fullscreen_exit_count")
    private Integer fullscreenExitCount;

    @Column(name = "tab_switch_count")
    private Integer tabSwitchCount;

    @Column(name = "microphone_checked")
    private Boolean microphoneChecked;

    @Column(name = "device_check_passed")
    private Boolean deviceCheckPassed;

    @Column(name = "ai_score", precision = 4, scale = 1)
    private BigDecimal aiScore;

    @Column(name = "ai_feedback_json", columnDefinition = "text")
    private String aiFeedbackJson;

    @Column(name = "ai_prompt_snapshot", columnDefinition = "text")
    private String aiPromptSnapshot;

    @Column(name = "ai_provider", length = 40)
    private String aiProvider;

    @Column(name = "ai_model", length = 120)
    private String aiModel;

    @Column(name = "ai_raw_response", columnDefinition = "text")
    private String aiRawResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @CreatedDate
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
}
