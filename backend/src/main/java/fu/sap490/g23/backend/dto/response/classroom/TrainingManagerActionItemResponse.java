package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrainingManagerActionItemResponse {
    private String kind;
    private String title;
    private String subtitle;
    private Long enrollmentId;
    private Long classroomOfferingId;
    private Long changeRequestId;
    private ClassroomRegistrationStatus registrationStatus;
    private String registrationStatusLabel;
    private LocalDateTime createdAt;
    private String href;
}
