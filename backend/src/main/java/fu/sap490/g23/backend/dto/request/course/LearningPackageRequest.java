package fu.sap490.g23.backend.dto.request.course;

import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPackageRequest {

    private PackageTypeCode packageTypeCode;

    @NotBlank(message = "Tên gói không được để trống")
    @Size(max = 180)
    private String title;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    @Size(max = 80)
    private String targetScore;

    @Size(max = 80)
    private String duration;

    @Size(max = 120)
    private String studyMode;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gói phải >= 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá khuyến mãi phải >= 0")
    private BigDecimal salePrice;

    @Size(max = 700)
    private String thumbnailUrl;

    private PackageStatus status;

    @Min(0)
    private Integer displayOrder;

    private Boolean featured;

    /** Thứ tự gói con khi tạo bundle (tuỳ chọn). */
    private List<Long> childPackageIds;
}
