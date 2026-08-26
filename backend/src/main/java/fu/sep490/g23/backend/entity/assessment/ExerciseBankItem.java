package fu.sep490.g23.backend.entity.assessment;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.service.curriculum.ContentBankPayloadSupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Exercise bank view of {@code content_bank_items} ({@code bank_type = EXERCISE}).
 *
 * <p>status vs active: both kept; historically only {@code active} existed — V4 derived status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "content_bank_items")
@SQLRestriction("bank_type = 'EXERCISE'")
@EntityListeners(AuditingEntityListener.class)
public class ExerciseBankItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_type", nullable = false, length = 30)
    @Builder.Default
    private String bankType = "EXERCISE";

    @Column(nullable = false, length = 220)
    private String title;

    @Column(length = 60)
    private String skill;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PUBLISHED";

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
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String level;

    @Transient
    @Builder.Default
    private String exerciseType = "HOMEWORK";

    @Transient
    private String prompt;

    @Transient
    private String answerKey;

    @Transient
    private String explanation;

    @PostLoad
    private void hydrateFromPayload() {
        Map<String, Object> payload = ContentBankPayloadSupport.ensure(payloadJsonb);
        level = ContentBankPayloadSupport.getString(payload, "level");
        String loadedType = ContentBankPayloadSupport.getString(payload, "exerciseType");
        exerciseType = loadedType == null || loadedType.isBlank() ? "HOMEWORK" : loadedType;
        prompt = ContentBankPayloadSupport.getString(payload, "prompt");
        if (prompt == null) {
            prompt = "";
        }
        answerKey = ContentBankPayloadSupport.getString(payload, "answerKey");
        explanation = ContentBankPayloadSupport.getString(payload, "explanation");
    }

    @PrePersist
    @PreUpdate
    private void flushToPayload() {
        bankType = "EXERCISE";
        if (payloadJsonb == null) {
            payloadJsonb = new HashMap<>();
        }
        if (status == null || status.isBlank()) {
            status = active ? "PUBLISHED" : "ARCHIVED";
        }
        if (prompt == null) {
            prompt = "";
        }
        if (exerciseType == null || exerciseType.isBlank()) {
            exerciseType = "HOMEWORK";
        }
        ContentBankPayloadSupport.put(payloadJsonb, "level", level);
        ContentBankPayloadSupport.put(payloadJsonb, "exerciseType", exerciseType);
        ContentBankPayloadSupport.put(payloadJsonb, "prompt", prompt);
        ContentBankPayloadSupport.put(payloadJsonb, "answerKey", answerKey);
        ContentBankPayloadSupport.put(payloadJsonb, "explanation", explanation);
    }
}
