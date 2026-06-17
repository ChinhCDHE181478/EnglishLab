package fu.sap490.g23.backend.dto.response.payment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentLinkResponse {
    private Long orderCode;
    private String paymentLinkId;
    private String checkoutUrl;
    private String qrCode;
    private String status;
}
