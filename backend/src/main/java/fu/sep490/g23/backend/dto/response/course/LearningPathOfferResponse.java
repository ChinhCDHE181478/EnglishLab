package fu.sep490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LearningPathOfferResponse {
    private Long id;
    private String code;
    private String name;
    private String examCategory;
    private BigDecimal targetBand;
    private Integer targetScore;
    private Integer discountPercent;
    private Integer minimumCoursesForDiscount;
    private Integer totalCourses;
    private Integer ownedCourses;
    private Integer remainingCourses;
    private Long originalAmount;
    private Long subtotalAmount;
    private Long learningPathDiscountAmount;
    private Long totalAmount;
    private boolean discountApplied;
    private boolean purchaseAvailable;
    private List<LearningPathOfferCourseResponse> courses;
}
