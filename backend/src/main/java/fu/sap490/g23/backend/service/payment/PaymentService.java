package fu.sap490.g23.backend.service.payment;

import fu.sap490.g23.backend.dto.request.payment.RefundPaymentOrderRequest;
import fu.sap490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentQuoteResponse;
import fu.sap490.g23.backend.dto.response.payment.RevenueAnalyticsResponse;
import java.util.List;
import java.util.Map;
import fu.sap490.g23.backend.entity.payment.enums.PaymentOrderStatus;

public interface PaymentService {
    PaymentQuoteResponse quotePayment(List<Long> courseIds, List<Long> classroomOfferingIds, String couponCode, String studentEmail);
    PaymentLinkResponse createPaymentLink(List<Long> courseIds, List<Long> classroomOfferingIds, String couponCode, String studentEmail);
    PaymentOrderStatusResponse getOrderStatus(Long orderCode, String studentEmail);
    List<PaymentOrderSummaryResponse> listMyOrders(String studentEmail);
    List<PaymentOrderSummaryResponse> listStaffOrders(PaymentOrderStatus status);
    PaymentOrderSummaryResponse refundCourseOrder(Long orderCode, RefundPaymentOrderRequest request, String actorEmail);
    byte[] downloadCourseReceipt(Long orderCode, String studentEmail);
    RevenueAnalyticsResponse getRevenueAnalytics();
    void handlePayosWebhook(Map<String, Object> payload);
    void confirmWebhook();
}
