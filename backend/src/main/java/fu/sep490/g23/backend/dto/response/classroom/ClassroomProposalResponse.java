package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ClassroomProposalResponse {
    private Long id;
    private String proposalCode;
    private String title;
    private Long courseOfferingId;
    private String courseOfferingTitle;
    private ClassroomDeliveryMode deliveryType;
    private PlacementLevel placementLevel;
    private Integer capacity;
    private Integer learnerCount;
    private Map<PlacementLevel, Long> levelDistribution;
    private LocalDate plannedStartDate;
    private LocalDate endDate;
    private List<DayOfWeek> weekdays;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private Integer plannedSessionCount;
    private Long primaryTeacherId;
    private String primaryTeacherName;
    private Long roomId;
    private String roomName;
    private String staffNote;
    private ClassroomApprovalStatus approvalStatus;
    private String approvalStatusLabel;
    private Long createdById;
    private String createdByName;
    private LocalDateTime submittedAt;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private Long approvedClassroomId;
    private List<ClassroomProposalScheduleItemResponse> scheduleItems;
    private List<ClassroomProposalMemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
