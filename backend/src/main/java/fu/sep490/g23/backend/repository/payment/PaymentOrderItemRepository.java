package fu.sep490.g23.backend.repository.payment;

import fu.sep490.g23.backend.entity.payment.PaymentOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;

public interface PaymentOrderItemRepository extends JpaRepository<PaymentOrderItem, Long> {
    List<PaymentOrderItem> findByPaymentOrderIdOrderById(Long paymentOrderId);
    boolean existsByClassEnrollmentIdAndPaymentOrderStatusIn(
            Long classEnrollmentId,
            Collection<PaymentOrderStatus> statuses
    );

    @Query("""
            select count(item)
            from PaymentOrderItem item
            where item.paymentOrder.student.id = :studentId
              and item.onlineCourse.id in :courseIds
              and item.paymentOrder.status in :statuses
            """)
    long countActiveOnlineCourseOrders(
            @Param("studentId") Long studentId,
            @Param("courseIds") Collection<Long> courseIds,
            @Param("statuses") Collection<PaymentOrderStatus> statuses
    );
}
