package fu.sep490.g23.backend.entity.assessment;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.assessment.enums.*;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assessment_component_records")
@SQLRestriction("record_type = 'rubric_criteria'")
public class RubricCriterion extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "rubric_criteria";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
