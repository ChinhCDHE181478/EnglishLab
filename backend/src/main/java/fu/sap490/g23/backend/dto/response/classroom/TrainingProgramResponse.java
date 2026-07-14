package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TrainingProgramResponse {
    private Long id;
    private String title;
    private String code;
    private String slug;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private Long curriculumProgramId;
    private String curriculumProgramTitle;
    private String curriculumProgramCode;
    private String curriculumProgramExamCategory;
    private String curriculumProgramStatus;
    private String shortDescription;
    private String description;
    private String entryLevel;
    private String targetScore;
    private String targetOutcome;
    private Integer defaultCapacity;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String duration;
    private String studyMode;
    private String thumbnailUrl;
    private String syllabusSummary;
    private String programOutcomes;
    private String teacherGuide;
    private String interactionActivities;
    private PackageStatus status;
    private String statusLabel;
    private Integer displayOrder;
    private boolean featured;
    private Integer materialCount;
    private Integer classroomCount;
    private Integer activeClassroomCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MaterialSummary> materials;

    @Data
    @Builder
    public static class MaterialSummary {
        private Long id;
        private Long materialId;
        private String title;
        private String materialType;
        private String fileType;
        private String provider;
        private String status;
        private Integer displayOrder;
        private String note;
    }
}
