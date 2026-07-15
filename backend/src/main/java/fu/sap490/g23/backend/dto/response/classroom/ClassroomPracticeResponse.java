package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomPracticeResponse {
    private Long unitId;
    private Integer unitDisplayOrder;
    private String unitTitle;
    private Long exerciseId;
    private String title;
    private String skill;
    private String instruction;
    private String note;
    private boolean completed;
    private String responseText;
    private LocalDateTime completedAt;
}
