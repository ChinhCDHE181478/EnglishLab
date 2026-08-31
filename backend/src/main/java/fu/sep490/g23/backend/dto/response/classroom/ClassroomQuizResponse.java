package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ClassroomQuizResponse {
    private Long id;
    private Long classSectionId;
    private Long sessionId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private String status;
    private LocalDateTime dueAt;
    private boolean submitted;
    private BigDecimal myScore;
    private List<QuizQuestionResponse> questions;
    private LocalDateTime createdAt;
}
