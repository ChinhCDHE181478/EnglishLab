package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CenterMaterialLibraryItemResponse {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private String fileType;
    private String materialType;
    private String provider;
    private String examCategory;
    private BigDecimal ieltsBandMin;
    private BigDecimal ieltsBandMax;
    private Integer toeicScoreMin;
    private Integer toeicScoreMax;
    private String skill;
    private String tags;
    private String status;
    private String createdByName;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
