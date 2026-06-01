package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.entity.course.VocabularyProgressStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyTermResponse {
    private String termKey;
    private String term;
    private String meaning;
    private String example;
    private String commonError;
    private Long lessonId;
    private String lessonTitle;
    private Long moduleId;
    private String moduleTitle;
    private VocabularyProgressStatus status;
    private boolean starred;
}
