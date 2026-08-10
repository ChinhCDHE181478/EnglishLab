package fu.sap490.g23.backend.controller.payment;

import fu.sap490.g23.backend.dto.request.payment.CreatePaymentLinkRequest;
import fu.sap490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentQuoteResponse;
import fu.sap490.g23.backend.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/payments")
@RequiredArgsConstructor
public class StudentPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payos/link")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(
            @Valid @RequestBody CreatePaymentLinkRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(paymentService.createPaymentLink(
                request.getCourseIds(),
                request.getClassroomOfferingIds(),
                request.getCouponCode(),
                authentication.getName()
        ));
    }

    @PostMapping("/quote")
    public ResponseEntity<PaymentQuoteResponse> quotePayment(
            @Valid @RequestBody CreatePaymentLinkRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(paymentService.quotePayment(
                request.getCourseIds(),
                request.getClassroomOfferingIds(),
                request.getCouponCode(),
                authentication.getName()
        ));
    }

    @GetMapping("/orders/{orderCode}")
    public ResponseEntity<PaymentOrderStatusResponse> getOrderStatus(
            @PathVariable Long orderCode,
            Authentication authentication
    ) {
        return ResponseEntity.ok(paymentService.getOrderStatus(orderCode, authentication.getName()));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<PaymentOrderSummaryResponse>> listMyOrders(Authentication authentication) {
        return ResponseEntity.ok(paymentService.listMyOrders(authentication.getName()));
    }

    @PostMapping("/payos/confirm-webhook")
    public ResponseEntity<Void> confirmWebhook() {
        paymentService.confirmWebhook();
        return ResponseEntity.ok().build();
    }
}
