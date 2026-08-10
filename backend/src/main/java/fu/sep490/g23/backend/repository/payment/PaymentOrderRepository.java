package fu.sap490.g23.backend.repository.payment;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.payment.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import fu.sap490.g23.backend.entity.payment.enums.PaymentOrderStatus;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderCode(Long orderCode);
    Optional<PaymentOrder> findByPaymentLinkId(String paymentLinkId);
    Optional<PaymentOrder> findTopByStudentOrderByCreatedAtDesc(User student);
    List<PaymentOrder> findByStatusIn(Collection<PaymentOrderStatus> statuses);
    Page<PaymentOrder> findByStatus(PaymentOrderStatus status, Pageable pageable);
    List<PaymentOrder> findByStudentOrderByCreatedAtDesc(User student);
    long countByDiscountCode_Id(Long discountCodeId);
    long countByStatus(PaymentOrderStatus status);
    long countByStatusIn(Collection<PaymentOrderStatus> statuses);

    @Query("select coalesce(sum(paymentOrder.amount), 0) from PaymentOrder paymentOrder where paymentOrder.status = :status")
    Long sumAmountByStatus(@Param("status") PaymentOrderStatus status);

    @Query("""
            select coalesce(sum(coalesce(paymentOrder.systemDiscountAmount, 0) + coalesce(paymentOrder.couponDiscountAmount, 0)), 0)
            from PaymentOrder paymentOrder
            where paymentOrder.status = :status
            """)
    Long sumDiscountByStatus(@Param("status") PaymentOrderStatus status);

    @Query("select coalesce(sum(paymentOrder.couponDiscountAmount), 0) from PaymentOrder paymentOrder where paymentOrder.status = :status")
    Long sumCouponDiscountByStatus(@Param("status") PaymentOrderStatus status);

    @Query("""
            select year(paymentOrder.paidAt) as yearValue,
                   month(paymentOrder.paidAt) as monthValue,
                   coalesce(sum(paymentOrder.amount), 0) as revenueVnd,
                   count(paymentOrder) as orderCount
            from PaymentOrder paymentOrder
            where paymentOrder.status = :status and paymentOrder.paidAt is not null
            group by year(paymentOrder.paidAt), month(paymentOrder.paidAt)
            order by year(paymentOrder.paidAt), month(paymentOrder.paidAt)
            """)
    List<PaymentMonthlyRevenueProjection> summarizeMonthlyRevenue(@Param("status") PaymentOrderStatus status);

    boolean existsByEnrollmentIdAndStatusIn(Long enrollmentId, Collection<PaymentOrderStatus> statuses);
}
