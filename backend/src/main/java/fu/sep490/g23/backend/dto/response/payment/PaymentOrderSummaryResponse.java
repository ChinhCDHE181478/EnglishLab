package fu.sep490.g23.backend.dto.response.payment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PaymentOrderSummaryResponse {
    private Long orderCode;
    private String status;
    private boolean paid;
    private Long amount;
    private Long originalAmount;
    private Long systemDiscountAmount;
    private Long couponDiscountAmount;
    private String discountCodeText;
    private String description;
    private List<String> courseTitles;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    /** COURSE | CLASSROOM_TUITION */
    private String orderType;
    private boolean refundable;
    private boolean hasReceipt;
    private Long refundedAmount;
    private LocalDateTime refundedAt;
    private String refundReason;
    private String studentEmail;
    private String studentName;
}
