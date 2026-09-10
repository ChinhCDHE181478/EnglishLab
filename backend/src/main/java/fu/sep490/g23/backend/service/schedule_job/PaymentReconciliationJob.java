package fu.sep490.g23.backend.service.schedule_job;

import fu.sep490.g23.backend.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically reconciles pending PayOS payment orders.
 * Delegates all business logic to {@link PaymentService}.
 */
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentService paymentService;

    @Scheduled(
            fixedDelayString = "${englishlab.payos.reconciliation-delay-ms:300000}",
            initialDelayString = "${englishlab.payos.reconciliation-initial-delay-ms:60000}"
    )
    public void run() {
        paymentService.reconcilePendingPaymentOrders();
    }
}
