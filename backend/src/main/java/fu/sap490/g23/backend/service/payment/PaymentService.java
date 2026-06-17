package fu.sap490.g23.backend.service.payment;

import fu.sap490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;

import java.util.List;
import java.util.Map;

public interface PaymentService {
    PaymentLinkResponse createPaymentLink(List<Long> courseIds, String studentEmail);
    PaymentOrderStatusResponse getOrderStatus(Long orderCode, String studentEmail);
    void handlePayosWebhook(Map<String, Object> payload);
    void confirmWebhook();
}
