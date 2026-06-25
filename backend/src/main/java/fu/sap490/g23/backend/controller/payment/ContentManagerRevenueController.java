package fu.sap490.g23.backend.controller.payment;

import fu.sap490.g23.backend.dto.response.payment.RevenueAnalyticsResponse;
import fu.sap490.g23.backend.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content-manager/revenue")
@RequiredArgsConstructor
public class ContentManagerRevenueController {

    private final PaymentService paymentService;

    @GetMapping("/analytics")
    public ResponseEntity<RevenueAnalyticsResponse> getRevenueAnalytics() {
        return ResponseEntity.ok(paymentService.getRevenueAnalytics());
    }
}
