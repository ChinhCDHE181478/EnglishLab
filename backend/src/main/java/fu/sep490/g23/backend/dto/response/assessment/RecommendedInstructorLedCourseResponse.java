package fu.sep490.g23.backend.dto.response.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RecommendedInstructorLedCourseResponse {
    private Long id;
    private String title;
    private String shortDescription;
    private PlacementLevel entryPlacementLevel;
    private String examCategory;
    private List<String> focusSkills;
    private BigDecimal targetBand;
    private Integer targetScore;
    private Integer totalSessions;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String recommendationReason;
}
