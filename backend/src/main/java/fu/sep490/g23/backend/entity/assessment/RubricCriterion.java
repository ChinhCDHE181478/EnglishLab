package fu.sep490.g23.backend.entity.assessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * In-memory rubric criterion shape (hydrated from {@code content_bank_items.payload_jsonb.criteria}).
 * No longer a JPA entity / DomainRecord STI row.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricCriterion {
    private Long id;

    private AssessmentRubric rubric;

    private String name;

    @Builder.Default
    private Integer weight = 25;

    private String description;

    private String bandDescriptors;

    @Builder.Default
    private Integer displayOrder = 0;
}
