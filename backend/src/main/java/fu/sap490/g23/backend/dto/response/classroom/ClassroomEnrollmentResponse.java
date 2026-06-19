package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.classroom.TuitionSettlementType;
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
    private Long classroomOfferingId;
    private String classroomTitle;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private ClassroomRegistrationStatus registrationStatus;
    private String registrationStatusLabel;
    private boolean holdSpot;
    private BigDecimal tuitionAmountDue;
    private BigDecimal tuitionAmountPaid;
    private BigDecimal tuitionDepositPaid;
    private BigDecimal tuitionRemaining;
    private TuitionSettlementType tuitionSettlementType;
    private String tuitionSettlementTypeLabel;
    private String tuitionSettlementNote;
    private boolean hasClassAccess;
    private Long transferredFromEnrollmentId;
    private LocalDateTime enrolledAt;
    private LocalDateTime assignedAt;
    private String assignedByName;
    private String assignmentNote;
    private LocalDateTime confirmedAt;
    private String confirmedByName;
    private LocalDateTime tuitionRecordedAt;
    private String tuitionRecordedByName;
    private String note;
    private List<ClassroomTuitionPaymentResponse> tuitionPayments;
}
