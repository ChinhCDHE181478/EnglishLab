package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPackageResponse {
    private Long id;
    private PackageTypeCode packageTypeCode;
    private String packageTypeName;
    private String title;
    private String slug;
    private String shortDescription;
    private String description;
    private String targetScore;
    private String duration;
    private String studyMode;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String thumbnailUrl;
    private PackageStatus status;
    private Integer displayOrder;
    private boolean featured;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer childCount;

    @Builder.Default
    private List<LearningPackageSummaryResponse> childPackages = new ArrayList<>();
}
