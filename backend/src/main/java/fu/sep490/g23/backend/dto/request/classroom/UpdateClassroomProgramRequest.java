package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateClassroomProgramRequest {
    @Size(max = 120)
    private String entryLevel;

    @Size(max = 700)
    private String targetOutcome;

    private String programOutcomes;
    private String teacherGuide;
    private String interactionActivities;
    private String syllabusSummary;
    private ClassroomDeliveryMode deliveryMode;
}
