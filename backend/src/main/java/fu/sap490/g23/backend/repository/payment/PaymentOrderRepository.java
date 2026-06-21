package fu.sap490.g23.backend.repository.payment;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.payment.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import fu.sap490.g23.backend.entity.payment.enums.PaymentOrderStatus;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderCode(Long orderCode);
    Optional<PaymentOrder> findByPaymentLinkId(String paymentLinkId);
    Optional<PaymentOrder> findTopByStudentOrderByCreatedAtDesc(User student);
    List<PaymentOrder> findByStatusIn(Collection<PaymentOrderStatus> statuses);
}
