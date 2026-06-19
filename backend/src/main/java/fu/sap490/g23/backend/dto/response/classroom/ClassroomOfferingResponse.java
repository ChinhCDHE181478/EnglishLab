package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.entity.course.PackageStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClassroomOfferingResponse {
    private Long id;
    private Long packageId;
    private String title;
    private String slug;
    private String shortDescription;
    private String description;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private ClassroomOfferingStatus classroomStatus;
    private PackageStatus packageStatus;
    private String entryLevel;
    private String targetOutcome;
    private Integer maxCapacity;
    private Integer enrolledCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long primaryTeacherId;
    private String primaryTeacherName;
    private Long campusId;
    private String campusName;
    private Long roomId;
    private String roomName;
    private String offlineAddress;
    private String locationNote;
    private String defaultLarkMeetingUrl;
    private LarkMeetingStatus larkMeetingStatus;
    private String larkPlatformName;
    private String recordingUrl;
    private boolean recordingVisible;
    private String syllabusSummary;
    private BigDecimal price;
    private String thumbnailUrl;
    private ClassroomSessionResponse nextSession;
    private Integer progressPercent;
    private Long enrollmentId;
    private boolean enrolled;
    private boolean registered;
    private boolean hasClassAccess;
    private ClassroomRegistrationStatus registrationStatus;
    private String registrationStatusLabel;
    private boolean holdSpot;
    private BigDecimal tuitionAmountDue;
    private BigDecimal tuitionAmountPaid;
    private BigDecimal tuitionRemaining;
    private TuitionSettlementType tuitionSettlementType;
    private String tuitionSettlementTypeLabel;
    private String tuitionSettlementNote;
    private Integer waitlistCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ClassroomSessionResponse> sessions;
    private List<ClassroomEnrollmentResponse> enrollments;
    private List<ClassroomTeacherSummaryResponse> teachers;
}
