package fu.sep490.g23.backend.service.curriculum;

import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.ExerciseBankItem;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Dual-column helpers: remounted bank entities use content-bank IDs; legacy FK columns
 * stay populated via {@link ContentBankIdResolver} reverse map when available.
 */
@Component
@RequiredArgsConstructor
public class ContentBankLinkSync {

    private final ContentBankIdResolver idResolver;
    private final ContentBankItemRepository contentBankItemRepository;

    public ContentBankItem toContentBankItem(AssessmentBankItem assessment) {
        if (assessment == null || assessment.getId() == null) {
            return null;
        }
        return contentBankItemRepository.findByIdAndBankType(assessment.getId(), ContentBankType.ASSESSMENT)
                .orElse(null);
    }

    public ContentBankItem toContentBankItem(AssessmentRubric rubric) {
        if (rubric == null || rubric.getId() == null) {
            return null;
        }
        return contentBankItemRepository.findByIdAndBankType(rubric.getId(), ContentBankType.RUBRIC)
                .orElse(null);
    }

    public ContentBankItem toContentBankItem(ExerciseBankItem exercise) {
        if (exercise == null || exercise.getId() == null) {
            return null;
        }
        return contentBankItemRepository.findByIdAndBankType(exercise.getId(), ContentBankType.EXERCISE)
                .orElse(null);
    }

    public ContentBankItem toContentBankItem(FlashcardSet flashcardSet) {
        if (flashcardSet == null || flashcardSet.getId() == null) {
            return null;
        }
        return contentBankItemRepository.findByIdAndBankType(flashcardSet.getId(), ContentBankType.FLASHCARD)
                .orElse(null);
    }

    public Long legacyId(ContentBankType type, Long contentBankItemId) {
        return idResolver.reverseResolve(type, contentBankItemId).orElse(null);
    }

    public Long legacyIdForAssessment(AssessmentBankItem assessment) {
        if (assessment == null || assessment.getId() == null) {
            return null;
        }
        return legacyId(ContentBankType.ASSESSMENT, assessment.getId());
    }

    public Long legacyIdForRubric(AssessmentRubric rubric) {
        if (rubric == null || rubric.getId() == null) {
            return null;
        }
        return legacyId(ContentBankType.RUBRIC, rubric.getId());
    }

    public Long legacyIdForExercise(ExerciseBankItem exercise) {
        if (exercise == null || exercise.getId() == null) {
            return null;
        }
        return legacyId(ContentBankType.EXERCISE, exercise.getId());
    }

    public Long legacyIdForFlashcard(FlashcardSet flashcardSet) {
        if (flashcardSet == null || flashcardSet.getId() == null) {
            return null;
        }
        return legacyId(ContentBankType.FLASHCARD, flashcardSet.getId());
    }
}
