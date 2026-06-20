package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClassroomOfferingRequest {

    @NotBlank(message = "Tiêu đề lớp học không được để trống")
    @Size(max = 180)
    private String title;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    @NotNull(message = "Hình thức đào tạo không được để trống")
    private ClassroomDeliveryMode deliveryMode;

    private ClassroomOfferingStatus classroomStatus;
    private PackageStatus packageStatus;

    @Size(max = 120)
    private String entryLevel;

    @Size(max = 700)
    private String targetOutcome;

    @Min(1)
    private Integer maxCapacity;

    private LocalDate startDate;
    private LocalDate endDate;

    private Long primaryTeacherId;
    private Long defaultRoomId;

    @Size(max = 500)
    private String offlineAddress;

    @Size(max = 500)
    private String locationNote;

    @Size(max = 700)
    private String defaultLarkMeetingUrl;

    @Size(max = 700)
    private String recordingUrl;

    private Boolean recordingVisible;
    private String syllabusSummary;

    private BigDecimal price;
    private BigDecimal salePrice;

    @Size(max = 700)
    private String thumbnailUrl;

    @Size(max = 80)
    private String duration;

    @Size(max = 120)
    private String studyMode;

    @Size(max = 80)
    private String targetScore;

    @Min(0)
    private Integer displayOrder;

    private Boolean featured;
}
