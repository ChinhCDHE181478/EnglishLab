package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionProofStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TuitionProofResponse {
    private Long id;
    private Long enrollmentId;
    private Long classroomOfferingId;
    private String classroomTitle;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private BigDecimal amount;
    private TuitionPaymentKind paymentKind;
    private String paymentKindLabel;
    private String fileUrl;
    private String note;
    private TuitionProofStatus status;
    private String statusLabel;
    private String reviewNote;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
