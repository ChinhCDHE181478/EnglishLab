package fu.sep490.g23.backend.entity.curriculum;

import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.service.curriculum.ContentBankPayloadSupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Assessment bank view of {@code content_bank_items} ({@code bank_type = ASSESSMENT}).
 *
 * <p>{@code status} is the single lifecycle and availability source of truth.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "content_bank_items")
@SQLRestriction("bank_type = 'ASSESSMENT'")
@EntityListeners(AuditingEntityListener.class)
public class AssessmentBankItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_type", nullable = false, length = 30)
    @Builder.Default
    private String bankType = "ASSESSMENT";

    @Column(nullable = false, length = 220)
    private String title;

    @Column(length = 120, nullable = false)
    private String code;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private AssessmentSkill skill;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_data", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> contentData = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_bank_item_id")
    private AssessmentRubric rubric;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private AssessmentType type;

    @Transient
    @Builder.Default
    private AiEvaluationMode aiEvaluationMode = AiEvaluationMode.RUBRIC_FEEDBACK;

    @Transient
    private String instructions;

    @Transient
    private String objectiveAnswerKey;

    @Transient
    private String uiConfigJson;

    @Transient
    private BigDecimal passingScore;

    @Transient
    @Builder.Default
    private BigDecimal maxScore = BigDecimal.TEN;

    @Transient
    private Integer timeLimitMinutes;

    @PostLoad
    private void hydrateFromPayload() {
        Map<String, Object> payload = ContentBankPayloadSupport.ensure(contentData);
        type = parseType(ContentBankPayloadSupport.getString(payload, "type"));
        aiEvaluationMode = parseAiMode(ContentBankPayloadSupport.getString(payload, "aiEvaluationMode"));
        instructions = ContentBankPayloadSupport.getString(payload, "instructions");
        objectiveAnswerKey = ContentBankPayloadSupport.getString(payload, "objectiveAnswerKey");
        uiConfigJson = ContentBankPayloadSupport.getString(payload, "uiConfigJson");
        passingScore = ContentBankPayloadSupport.getBigDecimal(payload, "passingScore");
        BigDecimal loadedMax = ContentBankPayloadSupport.getBigDecimal(payload, "maxScore");
        maxScore = loadedMax == null ? BigDecimal.TEN : loadedMax;
        timeLimitMinutes = ContentBankPayloadSupport.getInteger(payload, "timeLimitMinutes");
    }

    @PrePersist
    @PreUpdate
    private void flushToPayload() {
        bankType = "ASSESSMENT";
        if (code == null || code.isBlank()) {
            String sanitized = (title != null ? title : "ASM").replaceAll("[^A-Za-z0-9]", "-").toUpperCase();
            code = "ASM-" + System.nanoTime() + "-" + sanitized;
            if (code.length() > 120) {
                code = code.substring(0, 120);
            }
        }
        if (contentData == null) {
            contentData = new HashMap<>();
        }
        AssessmentType effectiveType = type == null ? AssessmentType.MODULE_TEST : type;
        type = effectiveType;
        ContentBankPayloadSupport.put(contentData, "type", effectiveType.name());
        ContentBankPayloadSupport.put(contentData, "aiEvaluationMode",
                aiEvaluationMode == null ? AiEvaluationMode.NONE.name() : aiEvaluationMode.name());
        ContentBankPayloadSupport.put(contentData, "instructions", instructions);
        ContentBankPayloadSupport.put(contentData, "objectiveAnswerKey", objectiveAnswerKey);
        ContentBankPayloadSupport.put(contentData, "uiConfigJson", uiConfigJson);
        ContentBankPayloadSupport.put(contentData, "passingScore", passingScore);
        ContentBankPayloadSupport.put(contentData, "maxScore", maxScore == null ? BigDecimal.TEN : maxScore);
        ContentBankPayloadSupport.put(contentData, "timeLimitMinutes", timeLimitMinutes);
    }

    private static AssessmentType parseType(String value) {
        if (value == null || value.isBlank()) {
            return AssessmentType.MODULE_TEST;
        }
        try {
            return AssessmentType.valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
            return AssessmentType.MODULE_TEST;
        }
    }

    private static AiEvaluationMode parseAiMode(String value) {
        if (value == null || value.isBlank()) {
            return AiEvaluationMode.RUBRIC_FEEDBACK;
        }
        try {
            return AiEvaluationMode.valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
            return AiEvaluationMode.RUBRIC_FEEDBACK;
        }
    }
}
