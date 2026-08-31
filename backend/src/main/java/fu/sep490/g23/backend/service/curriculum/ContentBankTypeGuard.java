package fu.sep490.g23.backend.service.curriculum;

import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import org.springframework.stereotype.Component;

@Component
public class ContentBankTypeGuard {

    public ContentBankItem assertBankType(ContentBankItem item, ContentBankType expected) {
        if (item == null) {
            throw new IllegalArgumentException("Mục ngân hàng nội dung không được để trống.");
        }
        if (item.getBankType() != expected) {
            throw new IllegalArgumentException(
                    "Loại ngân hàng nội dung phải là " + expected + " nhưng nhận được " + item.getBankType() + ".");
        }
        return item;
    }

    public ContentBankItem assertAssessment(ContentBankItem item) {
        return assertBankType(item, ContentBankType.ASSESSMENT);
    }

    public ContentBankItem assertExercise(ContentBankItem item) {
        return assertBankType(item, ContentBankType.EXERCISE);
    }

    public ContentBankItem assertRubric(ContentBankItem item) {
        return assertBankType(item, ContentBankType.RUBRIC);
    }

    public ContentBankItem assertFlashcard(ContentBankItem item) {
        return assertBankType(item, ContentBankType.FLASHCARD);
    }

    public ContentBankItem assertPlacementTest(ContentBankItem item) {
        return assertBankType(item, ContentBankType.PLACEMENT_TEST);
    }
}
