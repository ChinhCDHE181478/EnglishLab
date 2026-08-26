package fu.sep490.g23.backend.entity.payment;

import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderItemType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_order_items")
public class PaymentOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private PaymentOrderItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "online_course_id")
    private OnlineCourse onlineCourse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_enrollment_id")
    private ClassEnrollment classEnrollment;

    @Column(name = "title_snapshot", nullable = false, length = 250)
    private String titleSnapshot;

    @Column(name = "unit_price_vnd", nullable = false)
    private Long unitPriceVnd;

    @Column(name = "discount_amount_vnd", nullable = false)
    private Long discountAmountVnd;

    @Column(name = "final_amount_vnd", nullable = false)
    private Long finalAmountVnd;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;
}
