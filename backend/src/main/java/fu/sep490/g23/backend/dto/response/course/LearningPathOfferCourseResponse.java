package fu.sep490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LearningPathOfferCourseResponse {
    private Long courseId;
    private String slug;
    private String title;
    private String thumbnailUrl;
    private String shortDescription;
    private Integer displayOrder;
    private BigDecimal originalPrice;
    private BigDecimal currentPrice;
    private boolean owned;
}
