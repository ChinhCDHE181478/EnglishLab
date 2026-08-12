package fu.sep490.g23.backend.entity.curriculum;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "curriculum_assessment_refs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unit_id", "assessment_id"})
)
public class CurriculumAssessmentRef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private CurriculumUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private AssessmentBankItem assessment;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(length = 500)
    private String note;
}
