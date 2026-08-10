package fu.sap490.g23.backend.entity.curriculum;

import fu.sap490.g23.backend.entity.assessment.ExerciseBankItem;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "curriculum_exercise_refs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unit_id", "exercise_id"})
)
public class CurriculumExerciseRef {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private CurriculumUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private ExerciseBankItem exercise;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(length = 500)
    private String note;
}
