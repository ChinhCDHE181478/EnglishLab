package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sap490.g23.backend.entity.course.enums.CourseLevel;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineCourseResponse {
    private Long id;
    private Long packageId;
    private String title;
    private String slug;
    private String shortDescription;
    private String description;
    private CourseCategoryCode category;
    private String categoryName;
    private CourseLevel level;
    private PackageStatus status;
    private String targetScore;
    private Double recommendedCurrentBandMin;
    private Double recommendedCurrentBandMax;
    private Double targetBand;
    private String learningPathCode;
    private String learningPathName;
    private Integer learningPathOrder;
    private String targetOutcome;
    private String recommendedNextCourseSlug;
    private String duration;
    private String studyMode;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private Integer discountPercent;
    private String thumbnailUrl;
    private Integer totalLessons;
    private Integer totalHours;
    private Integer displayOrder;
    private boolean featured;
    private boolean registered;
    private Integer progressPercent;
    private Long enrollmentId;
    private Long enrollmentCount;
    private Double averageRating;
    private Long reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<String> focusSkills = new ArrayList<>();

    @Builder.Default
    private List<ModuleResponse> modules = new ArrayList<>();
}
