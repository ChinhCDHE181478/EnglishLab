package fu.sep490.g23.backend.dto.response.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentQuoteResponse {
    private Long originalAmount;
    private Long systemDiscountAmount;
    private Long learningPathDiscountAmount;
    private Long subtotalAmount;
    private Long couponDiscountAmount;
    private Long totalAmount;
    private String couponCode;
    private String couponMessage;
    private Long learningPathId;
    private String learningPathName;
}
