package fu.sap490.g23.backend.entity.payment;

import fu.sap490.g23.backend.entity.payment.enums.*;

import fu.sap490.g23.backend.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_orders")
@EntityListeners(AuditingEntityListener.class)
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "payment_link_id", length = 100)
    private String paymentLinkId;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "course_ids_csv", nullable = false, length = 1000)
    private String courseIdsCsv;

    @Column(name = "classroom_offering_ids_csv", length = 1000)
    private String classroomOfferingIdsCsv;

    /** Enrollment học phí lớp gắn với đơn PayOS (null nếu đơn khóa học online). */
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @Column(name = "course_titles", columnDefinition = "text")
    private String courseTitles;

    @Column(name = "amount_vnd", nullable = false)
    private Long amount;

    @Column(name = "original_amount_vnd", nullable = false)
    @Builder.Default
    private Long originalAmount = 0L;

    @Column(name = "system_discount_amount_vnd", nullable = false)
    @Builder.Default
    private Long systemDiscountAmount = 0L;

    @Column(name = "coupon_discount_amount_vnd", nullable = false)
    @Builder.Default
    private Long couponDiscountAmount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_code_id")
    private DiscountCode discountCode;

    @Column(name = "discount_code_text", length = 40)
    private String discountCodeText;

    @Column(name = "coupon_reservation_released", nullable = false)
    @Builder.Default
    private boolean couponReservationReleased = false;

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentOrderStatus status;

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "webhook_confirmed_at")
    private LocalDateTime webhookConfirmedAt;

    @Column(name = "last_webhook_payload", columnDefinition = "text")
    private String lastWebhookPayload;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
