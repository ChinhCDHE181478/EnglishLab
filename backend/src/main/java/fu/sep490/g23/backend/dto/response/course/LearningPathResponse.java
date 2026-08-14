package fu.sep490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.math.BigDecimal;

@Data
@Builder
public class LearningPathResponse {
    private Long id;
    private String code;
    private String name;
    private String examCategory;
    private BigDecimal targetBand;
    private Integer targetScore;
    private Integer discountPercent;
    private Integer minimumCoursesForDiscount;
    private List<LearningPathCourseResponse> courses;
}
