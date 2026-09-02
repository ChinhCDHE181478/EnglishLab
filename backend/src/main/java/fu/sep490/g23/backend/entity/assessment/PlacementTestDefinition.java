package fu.sep490.g23.backend.entity.assessment;

import fu.sep490.g23.backend.service.curriculum.ContentBankPayloadSupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Placement test definition view of {@code content_bank_items} ({@code bank_type = PLACEMENT_TEST}).
 * {@code testCode} maps to shared {@code code}; skill configs live in payload.
 *
 * <p>{@code status} is the single lifecycle and availability source of truth.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "content_bank_items")
@SQLRestriction("bank_type = 'PLACEMENT_TEST'")
public class PlacementTestDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_type", nullable = false, length = 30)
    @Builder.Default
    private String bankType = "PLACEMENT_TEST";

    @Column(name = "code", nullable = false, unique = true, length = 120)
    private String testCode;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "exam_category", length = 40)
    @Builder.Default
    private String examType = "IELTS";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PUBLISHED";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_data", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> contentData = new HashMap<>();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Transient
    private Integer maxAttempts;

    @Transient
    private String listeningConfigJson;

    @Transient
    private String readingConfigJson;

    @Transient
    private String writingConfigJson;

    @Transient
    private String speakingConfigJson;

    @Transient
    private String toeicConfigJson;

    @PostLoad
    private void hydrateFromPayload() {
        Map<String, Object> payload = ContentBankPayloadSupport.ensure(contentData);
        maxAttempts = ContentBankPayloadSupport.getInteger(payload, "maxAttempts");
        listeningConfigJson = stringifyConfig(payload.get("listeningConfig"));
        readingConfigJson = stringifyConfig(payload.get("readingConfig"));
        writingConfigJson = stringifyConfig(payload.get("writingConfig"));
        speakingConfigJson = stringifyConfig(payload.get("speakingConfig"));
        toeicConfigJson = stringifyConfig(payload.get("toeicConfig"));
    }

    @PrePersist
    @PreUpdate
    private void flushToPayload() {
        bankType = "PLACEMENT_TEST";
        if (contentData == null) {
            contentData = new HashMap<>();
        }
        if (status == null || status.isBlank()) status = "PUBLISHED";
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = updatedAt;
        }
        if (maxAttempts == null) {
            maxAttempts = 3;
        }
        ContentBankPayloadSupport.put(contentData, "maxAttempts", maxAttempts);
        ContentBankPayloadSupport.put(contentData, "listeningConfig", ContentBankPayloadSupport.readJsonNode(listeningConfigJson));
        ContentBankPayloadSupport.put(contentData, "readingConfig", ContentBankPayloadSupport.readJsonNode(readingConfigJson));
        ContentBankPayloadSupport.put(contentData, "writingConfig", ContentBankPayloadSupport.readJsonNode(writingConfigJson));
        ContentBankPayloadSupport.put(contentData, "speakingConfig", ContentBankPayloadSupport.readJsonNode(speakingConfigJson));
        ContentBankPayloadSupport.put(contentData, "toeicConfig", ContentBankPayloadSupport.readJsonNode(toeicConfigJson));
    }

    private static String stringifyConfig(Object value) {
        if (value == null) {
            return "{}";
        }
        if (value instanceof String text) {
            return text.isBlank() ? "{}" : text;
        }
        return ContentBankPayloadSupport.writeJson(value);
    }
}
