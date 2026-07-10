package fu.sap490.g23.backend.dto.response.commerce;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CommerceCourseItemResponse {
    private Long id;
    private String slug;
    private String title;
    private String thumbnailUrl;
    private String category;
    private String categoryName;
    private String duration;
    private Integer totalLessons;
    private Double targetBand;
    private String targetOutcome;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private boolean registered;
    private String status;
    private LocalDateTime addedAt;
}
