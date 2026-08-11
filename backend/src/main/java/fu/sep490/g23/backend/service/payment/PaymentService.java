package fu.sep490.g23.backend.service.payment;

import fu.sep490.g23.backend.dto.request.payment.RefundCourseOrderRequest;
import fu.sep490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentQuoteResponse;
import fu.sep490.g23.backend.dto.response.payment.RevenueAnalyticsResponse;
import java.util.List;
import java.util.Map;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentQuoteResponse quotePayment(List<Long> courseIds, List<Long> classroomOfferingIds, String couponCode, String studentEmail);
    PaymentLinkResponse createPaymentLink(List<Long> courseIds, List<Long> classroomOfferingIds, String couponCode, String studentEmail);
    PaymentOrderStatusResponse getOrderStatus(Long orderCode, String studentEmail);
    List<PaymentOrderSummaryResponse> listMyOrders(String studentEmail);
    Page<PaymentOrderSummaryResponse> listStaffOrders(PaymentOrderStatus status, Pageable pageable);
    PaymentOrderSummaryResponse refundCourseOrder(Long orderCode, RefundCourseOrderRequest request, String actorEmail);
    byte[] downloadCourseReceipt(Long orderCode, String studentEmail);
    RevenueAnalyticsResponse getRevenueAnalytics();
    void handlePayosWebhook(Map<String, Object> payload);
    void confirmWebhook();
}
