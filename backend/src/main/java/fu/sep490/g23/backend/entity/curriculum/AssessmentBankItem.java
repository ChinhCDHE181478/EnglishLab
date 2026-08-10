package fu.sap490.g23.backend.entity.curriculum;

import fu.sap490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sap490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assessment_bank_items")
@EntityListeners(AuditingEntityListener.class)
public class AssessmentBankItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 700)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssessmentType type;

    /** Giữ tương thích với schema cũ còn dùng cột assessment_type bắt buộc. */
    @Column(name = "assessment_type", nullable = false, length = 30)
    private String legacyAssessmentType;

    @Column(name = "content_json", nullable = false, columnDefinition = "text")
    private String legacyContentJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssessmentSkill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_evaluation_mode", nullable = false, length = 40)
    @Builder.Default
    private AiEvaluationMode aiEvaluationMode = AiEvaluationMode.NONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id")
    private AssessmentRubric rubric;

    @Column(name = "instructions", columnDefinition = "text")
    private String instructions;

    @Column(name = "objective_answer_key", columnDefinition = "text")
    private String objectiveAnswerKey;

    @Column(name = "ui_config_json", columnDefinition = "text")
    private String uiConfigJson;

    @Column(name = "passing_score", precision = 4, scale = 1)
    private BigDecimal passingScore;

    @Column(name = "max_score", precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal maxScore = BigDecimal.TEN;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void synchronizeLegacyAssessmentType() {
        legacyAssessmentType = type == null ? AssessmentType.MODULE_TEST.name() : type.name();
        legacyContentJson = uiConfigJson == null || uiConfigJson.isBlank() ? "{}" : uiConfigJson;
    }
}
