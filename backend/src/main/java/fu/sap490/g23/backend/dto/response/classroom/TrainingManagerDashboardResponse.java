package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TrainingManagerDashboardResponse {
    private int pendingRegistrationCount;
    private int pendingChangeRequestCount;
    private int pendingConfirmationCount;
    private int pendingTuitionCount;
    private int readyToAssignCount;
    private List<TrainingManagerActionItemResponse> actionItems;
    private List<TrainingManagerClassroomAlertResponse> classroomAlerts;
}
