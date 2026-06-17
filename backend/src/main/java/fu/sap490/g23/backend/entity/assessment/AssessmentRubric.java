package fu.sap490.g23.backend.entity.assessment;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "assessment_rubrics")
public class AssessmentRubric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "exam_type", length = 40)
    private String examType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssessmentSkill skill;

    @Column(name = "task_type", length = 80)
    private String taskType;

    @Column(name = "scoring_scale", length = 60)
    private String scoringScale;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "rubric", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<RubricCriterion> criteria = new ArrayList<>();

    public void addCriterion(RubricCriterion criterion) {
        criteria.add(criterion);
        criterion.setRubric(this);
    }
}
