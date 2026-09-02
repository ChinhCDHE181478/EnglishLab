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
 * <p>{@code status} is the single lifecycle and availability source of truth.
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_data", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> contentData = new HashMap<>();

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
        Map<String, Object> payload = ContentBankPayloadSupport.ensure(contentData);
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
        if (contentData == null) {
            contentData = new HashMap<>();
        }
        if (status == null || status.isBlank()) status = "PUBLISHED";
        if (prompt == null) {
            prompt = "";
        }
        if (exerciseType == null || exerciseType.isBlank()) {
            exerciseType = "HOMEWORK";
        }
        ContentBankPayloadSupport.put(contentData, "level", level);
        ContentBankPayloadSupport.put(contentData, "exerciseType", exerciseType);
        ContentBankPayloadSupport.put(contentData, "prompt", prompt);
        ContentBankPayloadSupport.put(contentData, "answerKey", answerKey);
        ContentBankPayloadSupport.put(contentData, "explanation", explanation);
    }
}
