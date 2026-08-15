package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class StaffDashboardResponse {
    private int pendingRegistrationCount;
    private int pendingChangeRequestCount;
    private int pendingConfirmationCount;
    private int pendingTuitionCount;
    private int readyToAssignCount;
    private int registeredLearnerCount;
    private int consultedLearnerCount;
    private BigDecimal teacherAverageScore;
    private BigDecimal studentAverageScore;
    private List<StaffActionItemResponse> actionItems;
    private List<StaffClassroomAlertResponse> classroomAlerts;
    private List<StaffDashboardScoreItemResponse> teacherScores;
    private List<StaffDashboardScoreItemResponse> studentScores;
}
