package fu.sap490.g23.backend.dto.response.assessment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ExerciseBankItemResponse {
    private Long id;
    private String title;
    private String skill;
    private String level;
    private String exerciseType;
    private String prompt;
    private String answerKey;
    private String explanation;
    private String tags;
    private boolean active;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
