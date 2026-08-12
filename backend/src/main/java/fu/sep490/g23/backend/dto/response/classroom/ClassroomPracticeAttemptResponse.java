package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomPracticeAttemptResponse {
    private Long id;
    private Long classroomOfferingId;
    private Long exerciseId;
    private String exerciseTitle;
    private Integer attemptNumber;
    private String responseText;
    private String answersJson;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Double scorePercent;
    private Integer durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String explanation;
}
