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
 * <p>status vs active: both kept as distinct concepts (lifecycle vs availability).
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

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private AssessmentSkill skill;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "DRAFT";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_jsonb", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> payloadJsonb = new HashMap<>();

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
    private String legacyAssessmentType;

    @Transient
    private String legacyContentJson;

    @Transient
    @Builder.Default
    private AiEvaluationMode aiEvaluationMode = AiEvaluationMode.NONE;

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
        Map<String, Object> payload = ContentBankPayloadSupport.ensure(payloadJsonb);
        String typeName = ContentBankPayloadSupport.getString(payload, "type");
        if (typeName == null || typeName.isBlank()) {
            typeName = ContentBankPayloadSupport.getString(payload, "assessmentType");
        }
        type = parseType(typeName);
        legacyAssessmentType = ContentBankPayloadSupport.getString(payload, "assessmentType");
        if (legacyAssessmentType == null || legacyAssessmentType.isBlank()) {
            legacyAssessmentType = type == null ? AssessmentType.MODULE_TEST.name() : type.name();
        }
        aiEvaluationMode = parseAiMode(ContentBankPayloadSupport.getString(payload, "aiEvaluationMode"));
        instructions = ContentBankPayloadSupport.getString(payload, "instructions");
        objectiveAnswerKey = ContentBankPayloadSupport.getString(payload, "objectiveAnswerKey");
        uiConfigJson = ContentBankPayloadSupport.getString(payload, "uiConfigJson");
        String contentJson = ContentBankPayloadSupport.getString(payload, "contentJson");
        legacyContentJson = contentJson == null || contentJson.isBlank()
                ? (uiConfigJson == null || uiConfigJson.isBlank() ? "{}" : uiConfigJson)
                : contentJson;
        passingScore = ContentBankPayloadSupport.getBigDecimal(payload, "passingScore");
        BigDecimal loadedMax = ContentBankPayloadSupport.getBigDecimal(payload, "maxScore");
        maxScore = loadedMax == null ? BigDecimal.TEN : loadedMax;
        timeLimitMinutes = ContentBankPayloadSupport.getInteger(payload, "timeLimitMinutes");
    }

    @PrePersist
    @PreUpdate
    private void flushToPayload() {
        bankType = "ASSESSMENT";
        if (payloadJsonb == null) {
            payloadJsonb = new HashMap<>();
        }
        AssessmentType effectiveType = type == null ? AssessmentType.MODULE_TEST : type;
        type = effectiveType;
        legacyAssessmentType = effectiveType.name();
        String content = uiConfigJson == null || uiConfigJson.isBlank() ? "{}" : uiConfigJson;
        legacyContentJson = content;
        ContentBankPayloadSupport.put(payloadJsonb, "type", effectiveType.name());
        ContentBankPayloadSupport.put(payloadJsonb, "assessmentType", effectiveType.name());
        ContentBankPayloadSupport.put(payloadJsonb, "aiEvaluationMode",
                aiEvaluationMode == null ? AiEvaluationMode.NONE.name() : aiEvaluationMode.name());
        ContentBankPayloadSupport.put(payloadJsonb, "instructions", instructions);
        ContentBankPayloadSupport.put(payloadJsonb, "objectiveAnswerKey", objectiveAnswerKey);
        ContentBankPayloadSupport.put(payloadJsonb, "uiConfigJson", uiConfigJson);
        ContentBankPayloadSupport.put(payloadJsonb, "contentJson", content);
        ContentBankPayloadSupport.put(payloadJsonb, "passingScore", passingScore);
        ContentBankPayloadSupport.put(payloadJsonb, "maxScore", maxScore == null ? BigDecimal.TEN : maxScore);
        ContentBankPayloadSupport.put(payloadJsonb, "timeLimitMinutes", timeLimitMinutes);
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
            return AiEvaluationMode.NONE;
        }
        try {
            return AiEvaluationMode.valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
            return AiEvaluationMode.NONE;
        }
    }
}
