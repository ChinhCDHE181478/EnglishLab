package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource;
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
    private EnrollmentRequestStatus status;
    private String statusLabel;
    private EnrollmentRequestSource requestSource;
    private boolean learnerAccountCreated;
    private boolean accountSetupEmailSent;
    private PlacementLevel confirmedLevel;
    private String preferredSchedule;
    private String campusPreference;
    private String learnerNote;
    private String staffNote;
    private String rejectionReason;
    private LocalDateTime invitationSentAt;
    private LocalDateTime testAppointmentAt;
    private String testLocation;
    private LocalDateTime testCompletedAt;
    private Long placementAttemptId;
    private PlacementEligibilityResult placementEligibility;
    private Long assignedClassroomId;
    private List<EnrollmentRequestHistoryResponse> history;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
