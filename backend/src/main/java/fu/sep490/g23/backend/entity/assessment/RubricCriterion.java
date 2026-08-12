package fu.sep490.g23.backend.entity.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.*;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rubric_criteria")
public class RubricCriterion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    private AssessmentRubric rubric;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Integer weight = 25;

    @Column(length = 500)
    private String description;

    @Column(name = "band_descriptors", columnDefinition = "text")
    private String bandDescriptors;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}
