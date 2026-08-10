package fu.sap490.g23.backend.repository.curriculum;

import fu.sap490.g23.backend.entity.curriculum.CurriculumFlashcardRef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumFlashcardRefRepository extends JpaRepository<CurriculumFlashcardRef, Long> {
    boolean existsByUnitIdAndFlashcardSetId(Long unitId, Long flashcardSetId);
}
