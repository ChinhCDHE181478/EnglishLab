package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StaffActionItemResponse {
    private String kind;
    private String title;
    private String subtitle;
    private Long enrollmentId;
    private Long classSectionId;
    private Long changeRequestId;
    private ClassroomRegistrationStatus registrationStatus;
    private String registrationStatusLabel;
    private LocalDateTime createdAt;
    private String href;
}
