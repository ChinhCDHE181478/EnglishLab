package fu.sep490.g23.backend.entity.curriculum;

import fu.sep490.g23.backend.service.curriculum.ContentBankPayloadSupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * Flashcard set view of {@code content_bank_items} ({@code bank_type = FLASHCARD}).
 * {@code cardsJson} is backed by {@code payload_jsonb.cards}.
 *
 * <p>status vs active: both kept; historically only {@code status} existed — V4 derived active.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "content_bank_items")
@SQLRestriction("bank_type = 'FLASHCARD'")
@EntityListeners(AuditingEntityListener.class)
public class FlashcardSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_type", nullable = false, length = 30)
    @Builder.Default
    private String bankType = "FLASHCARD";

    @Column(nullable = false, length = 220)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "exam_category", length = 40)
    private String examCategory;

    @Column(length = 60)
    private String skill;

    @Column(length = 500)
    private String tags;

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

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String cardsJson;

    @PostLoad
    private void hydrateFromPayload() {
        cardsJson = ContentBankPayloadSupport.cardsJsonFromPayload(payloadJsonb);
        if (status != null && (status.equalsIgnoreCase("ARCHIVED") || status.equalsIgnoreCase("INACTIVE"))) {
            active = false;
        }
    }

    @PrePersist
    @PreUpdate
    private void flushToPayload() {
        bankType = "FLASHCARD";
        if (payloadJsonb == null) {
            payloadJsonb = new HashMap<>();
        }
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
        active = !(status.equalsIgnoreCase("ARCHIVED") || status.equalsIgnoreCase("INACTIVE"));
        ContentBankPayloadSupport.put(payloadJsonb, "cards", ContentBankPayloadSupport.cardsFromJson(cardsJson));
    }
}
