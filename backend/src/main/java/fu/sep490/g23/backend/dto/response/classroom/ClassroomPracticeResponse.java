package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomPracticeResponse {
    private Long classSectionId;
    private String classroomTitle;
    private Long unitId;
    private Integer unitDisplayOrder;
    private String unitTitle;
    private Long exerciseId;
    private String title;
    private String skill;
    private String exerciseType;
    private String instruction;
    private String note;
    private boolean completed;
    private String responseText;
    private LocalDateTime completedAt;
    private long attemptCount;
    private Double lastScorePercent;
}
