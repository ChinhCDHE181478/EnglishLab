package fu.sep490.g23.backend.repository.payment;

import fu.sep490.g23.backend.entity.payment.PaymentOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;

public interface PaymentOrderItemRepository extends JpaRepository<PaymentOrderItem, Long> {
    List<PaymentOrderItem> findByPaymentOrderIdOrderById(Long paymentOrderId);
    boolean existsByClassEnrollmentIdAndPaymentOrderStatusIn(
            Long classEnrollmentId,
            Collection<PaymentOrderStatus> statuses
    );
}
