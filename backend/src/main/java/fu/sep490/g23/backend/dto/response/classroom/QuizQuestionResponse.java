package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizQuestionResponse {
    private Long id;
    private Integer sortOrder;
    private String prompt;
    private String optionsJson;
    private String correctAnswer;
    private String explanation;
}
