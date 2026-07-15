package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPackageSummaryResponse {
    private Long id;
    private String title;
    private String slug;
    private PackageTypeCode packageTypeCode;
    private String packageTypeName;
    private PackageStatus status;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer displayOrder;
}
