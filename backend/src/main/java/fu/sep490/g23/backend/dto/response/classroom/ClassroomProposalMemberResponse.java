package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomProposalMemberResponse {
    private Long enrollmentRequestId;
    private Long learnerId;
    private String learnerName;
    private String learnerEmail;
    private PlacementLevel placementLevel;
    private String preferredSchedule;
    private String campusPreference;
    private Long classroomEnrollmentId;
}
