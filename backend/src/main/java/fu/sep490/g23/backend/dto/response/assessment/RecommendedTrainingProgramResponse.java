package fu.sep490.g23.backend.dto.response.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RecommendedTrainingProgramResponse {
    private Long id;
    private String slug;
    private String title;
    private ClassroomDeliveryMode deliveryMode;
    private String shortDescription;
    private PlacementLevel entryPlacementLevel;
    private String examCategory;
    private String programTrack;
    private List<String> focusSkills;
    private BigDecimal targetBand;
    private Integer targetScore;
    private Integer totalSessions;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String recommendationReason;
}
