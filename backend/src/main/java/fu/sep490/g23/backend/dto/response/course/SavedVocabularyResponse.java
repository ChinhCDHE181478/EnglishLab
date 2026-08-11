package fu.sep490.g23.backend.dto.response.course;

import fu.sep490.g23.backend.entity.course.enums.VocabularyMasteryStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedVocabularyResponse {
    private Long id;
    private String word;
    private String phonetic;
    private String primaryDefinition;
    private String note;
    private VocabularyMasteryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
