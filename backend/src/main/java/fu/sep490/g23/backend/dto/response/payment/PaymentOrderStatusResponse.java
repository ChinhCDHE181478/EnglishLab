package fu.sep490.g23.backend.dto.response.payment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentOrderStatusResponse {
    private Long orderCode;
    private String status;
    private boolean paid;
    private String message;
    private Long classroomOfferingId;
    private Long enrollmentId;
}
