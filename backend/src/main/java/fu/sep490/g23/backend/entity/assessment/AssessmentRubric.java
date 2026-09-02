package fu.sep490.g23.backend.entity.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.service.curriculum.ContentBankPayloadSupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rubric bank view of {@code content_bank_items} ({@code bank_type = RUBRIC}).
 * Criteria live in {@code content_data.criteria} (no longer STI {@code RubricCriterion} rows).
 *
 * <p>{@code status} is the single lifecycle and availability source of truth.
 * {@code name} maps to the shared {@code title} column.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "content_bank_items")
@SQLRestriction("bank_type = 'RUBRIC'")
public class AssessmentRubric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_type", nullable = false, length = 30)
    @Builder.Default
    private String bankType = "RUBRIC";

    @Column(name = "title", nullable = false, length = 220)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "exam_category", length = 40)
    private String examType;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private AssessmentSkill skill;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PUBLISHED";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_data", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> contentData = new HashMap<>();

    @Transient
    private String taskType;

    @Transient
    private String scoringScale;

    @Transient
    @Builder.Default
    private List<RubricCriterion> criteria = new ArrayList<>();

    public void addCriterion(RubricCriterion criterion) {
        if (criteria == null) {
            criteria = new ArrayList<>();
        }
        criteria.add(criterion);
        criterion.setRubric(this);
    }

    @PostLoad
    private void hydrateFromPayload() {
        Map<String, Object> payload = ContentBankPayloadSupport.ensure(contentData);
        taskType = ContentBankPayloadSupport.getString(payload, "taskType");
        scoringScale = ContentBankPayloadSupport.getString(payload, "scoringScale");
        List<Map<String, Object>> rawCriteria = ContentBankPayloadSupport.getObjectList(payload, "criteria");
        List<RubricCriterion> loaded = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> row : rawCriteria) {
            RubricCriterion criterion = RubricCriterion.builder()
                    .id(asLong(row.get("id")))
                    .name(stringOrEmpty(row.get("name")))
                    .weight(asInt(row.get("weight"), 25))
                    .description(stringOrNull(row.get("description")))
                    .bandDescriptors(stringOrNull(row.get("bandDescriptors")))
                    .displayOrder(asInt(row.get("displayOrder"), index + 1))
                    .build();
            criterion.setRubric(this);
            loaded.add(criterion);
            index++;
        }
        criteria = loaded;
    }

    @PrePersist
    @PreUpdate
    private void flushToPayload() {
        bankType = "RUBRIC";
        if (contentData == null) {
            contentData = new HashMap<>();
        }
        if (status == null || status.isBlank()) status = "PUBLISHED";
        ContentBankPayloadSupport.put(contentData, "taskType", taskType);
        ContentBankPayloadSupport.put(contentData, "scoringScale", scoringScale);
        List<Map<String, Object>> serialized = new ArrayList<>();
        if (criteria != null) {
            for (RubricCriterion criterion : criteria) {
                Map<String, Object> row = new HashMap<>();
                if (criterion.getId() != null) {
                    row.put("id", criterion.getId());
                }
                row.put("name", criterion.getName());
                row.put("weight", criterion.getWeight() == null ? 25 : criterion.getWeight());
                row.put("description", criterion.getDescription());
                row.put("bandDescriptors", criterion.getBandDescriptors());
                row.put("displayOrder", criterion.getDisplayOrder() == null ? 0 : criterion.getDisplayOrder());
                serialized.add(row);
            }
        }
        ContentBankPayloadSupport.put(contentData, "criteria", serialized);
    }

    private static String stringOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
