package fu.sep490.g23.backend.entity.curriculum;

import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@DiscriminatorValue("EXERCISE")
public class CurriculumExerciseRef extends CurriculumResourceRef {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_bank_item_id")
    private ExerciseBankItem exercise;

    @Column(name = "exercise_id")
    private Long legacyExerciseId;
}
