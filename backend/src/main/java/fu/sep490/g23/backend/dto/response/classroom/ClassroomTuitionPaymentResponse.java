package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomTuitionPaymentResponse {
    private Long id;
    private BigDecimal amount;
    private String paymentKind;
    private String paymentKindLabel;
    private String note;
    private String recordedByName;
    private LocalDateTime createdAt;
}
