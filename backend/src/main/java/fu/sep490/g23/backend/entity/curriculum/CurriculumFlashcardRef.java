package fu.sep490.g23.backend.entity.curriculum;

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
@DiscriminatorValue("FLASHCARD")
public class CurriculumFlashcardRef extends CurriculumResourceRef {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_bank_item_id")
    private FlashcardSet flashcardSet;

    @Column(name = "flashcard_set_id")
    private Long legacyFlashcardSetId;
}
