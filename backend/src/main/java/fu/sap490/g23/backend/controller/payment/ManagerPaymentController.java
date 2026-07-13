package fu.sap490.g23.backend.controller.payment;

import fu.sap490.g23.backend.dto.request.payment.RefundPaymentOrderRequest;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sap490.g23.backend.entity.payment.enums.PaymentOrderStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/manager/payments", "/api/content-manager/payments"})
@RequiredArgsConstructor
public class ManagerPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/orders")
    public ResponseEntity<List<PaymentOrderSummaryResponse>> listOrders(
            @RequestParam(required = false) PaymentOrderStatus status
    ) {
        return ResponseEntity.ok(paymentService.listStaffOrders(status));
    }

    @PostMapping("/orders/{orderCode}/refund")
    public ResponseEntity<PaymentOrderSummaryResponse> refundOrder(
            @PathVariable Long orderCode,
            @Valid @RequestBody RefundPaymentOrderRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(paymentService.refundCourseOrder(orderCode, request, authentication.getName()));
    }
}
