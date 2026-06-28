package fu.sap490.g23.backend.dto.response.curriculum;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CurriculumProgramResponse {
    private Long id;
    private String title;
    private String code;
    private String slug;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private String examCategory;
    private BigDecimal targetBand;
    private Integer targetScore;
    private String entryLevel;
    private String outcomes;
    private String teacherGuide;
    private String interactionActivities;
    private Integer totalSessions;
    private String status;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CurriculumUnitResponse> units;
}
