package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sap490.g23.backend.entity.course.enums.FlashcardPracticeSource;

import java.util.List;

public interface FlashcardPracticeService {
    List<VocabularyTermResponse> getPracticeTerms(
            FlashcardPracticeSource source,
            Long courseId,
            boolean starredOnly,
            String studentEmail
    );
}
