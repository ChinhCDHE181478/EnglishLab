package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sap490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourseEnrollmentRequestResponse {
    private Long id;
    private Long learnerId;
    private String learnerName;
    private String learnerEmail;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String facebookUrl;
    private String desiredClassCode;
    private String consultationTrack;
    private String studyWorkGoal;
    private Long courseOfferingId;
    private String courseOfferingTitle;
    private Long requestedClassroomId;
    private String requestedClassroomTitle;
    private String requestedClassroomCode;
    private LocalDate requestedClassroomStartDate;
    private String requestedClassroomSchedule;
    private String requestedClassroomTeacherName;
    private String requestedClassroomLocation;
    private ClassroomDeliveryMode deliveryType;
    private LocalDate plannedStartDate;
    private String plannedSchedule;
    private Integer capacity;
    private EnrollmentRequestStatus status;
    private String statusLabel;
    private PlacementLevel confirmedLevel;
    private String preferredSchedule;
    private String campusPreference;
    private String learnerNote;
    private String staffNote;
    private String rejectionReason;
    private Long placementAttemptId;
    private PlacementEligibilityResult placementEligibility;
    private Long assignedClassroomId;
    private List<EnrollmentRequestHistoryResponse> history;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
