package fu.sap490.g23.backend.service.payment;

import fu.sap490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentQuoteResponse;

import java.util.List;
import java.util.Map;

public interface PaymentService {
    PaymentQuoteResponse quotePayment(List<Long> courseIds, String couponCode, String studentEmail);
    PaymentLinkResponse createPaymentLink(List<Long> courseIds, String couponCode, String studentEmail);
    PaymentOrderStatusResponse getOrderStatus(Long orderCode, String studentEmail);
    void handlePayosWebhook(Map<String, Object> payload);
    void confirmWebhook();
}
