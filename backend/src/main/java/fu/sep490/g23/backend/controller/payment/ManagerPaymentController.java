package fu.sep490.g23.backend.controller.payment;

import fu.sep490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;


@RestController
@RequestMapping({"/api/manager/payments", "/api/content-manager/payments"})
@RequiredArgsConstructor
public class ManagerPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/orders")
    public ResponseEntity<Page<PaymentOrderSummaryResponse>> listOrders(
            @RequestParam(required = false) PaymentOrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(paymentService.listStaffOrders(status, pageable));
    }
}
