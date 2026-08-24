package fu.sep490.g23.backend.controller.payment;

import fu.sep490.g23.backend.dto.request.payment.CreatePaymentLinkRequest;
import fu.sep490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentQuoteResponse;
import fu.sep490.g23.backend.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
  import org.springframework.http.ResponseEntity;
  import org.springframework.data.domain.Page;
  import org.springframework.data.domain.PageRequest;
  import org.springframework.data.domain.Sort;
  import org.springframework.web.bind.annotation.RequestParam;
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
                request.getLearningPathId(),
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
                request.getLearningPathId(),
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

      @GetMapping("/orders/page")
      public ResponseEntity<Page<PaymentOrderSummaryResponse>> pageMyOrders(
              @RequestParam(defaultValue = "0") int page,
              @RequestParam(defaultValue = "5") int size,
              Authentication authentication
      ) {
          int safeSize = Math.min(Math.max(size, 1), 100);
          return ResponseEntity.ok(paymentService.pageMyOrders(
                  authentication.getName(),
                  PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
          ));
      }

    @PostMapping("/payos/confirm-webhook")
    public ResponseEntity<Void> confirmWebhook() {
        paymentService.confirmWebhook();
        return ResponseEntity.ok().build();
    }
}
