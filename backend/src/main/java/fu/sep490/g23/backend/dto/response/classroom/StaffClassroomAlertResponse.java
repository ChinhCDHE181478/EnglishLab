package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class StaffClassroomAlertResponse {
    private Long classSectionId;
    private String title;
    private String deliveryMode;
    private LocalDate startDate;
    private Integer enrolledCount;
    private Integer capacity;
    private Integer sessionCount;
    private String alertType;
    private String alertMessage;
    private String href;
}
