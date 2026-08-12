package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.HomeworkAnnotationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkTextAnnotationResponse {
    private String id;
    private HomeworkAnnotationType type;
    private Integer startOffset;
    private Integer endOffset;
    private String selectedText;
    private String replacementText;
    private String note;
}
