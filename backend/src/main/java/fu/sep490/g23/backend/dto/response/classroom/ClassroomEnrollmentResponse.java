package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClassroomEnrollmentResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long classSectionId;
    private String classroomTitle;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private ClassroomRegistrationStatus registrationStatus;
    private String registrationStatusLabel;
    private Integer waitlistPriority;
    private Integer waitlistPosition;
    private Integer waitlistSize;
    private BigDecimal tuitionAmountDue;
    private BigDecimal tuitionAmountPaid;
    private boolean hasClassAccess;
    private Long transferredFromEnrollmentId;
    private LocalDateTime enrolledAt;
    private LocalDateTime assignedAt;
    private String assignedByName;
    private String assignmentNote;
    private LocalDateTime tuitionRecordedAt;
    private String tuitionRecordedByName;
    private String note;
    private List<ClassroomTuitionPaymentResponse> tuitionPayments;
}
