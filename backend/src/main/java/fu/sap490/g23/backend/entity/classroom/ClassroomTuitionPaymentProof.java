package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionProofStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Minh chứng chuyển khoản học phí do học viên nộp, chờ Nhân viên đào tạo xác nhận.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_tuition_payment_proofs")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomTuitionPaymentProof {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private ClassroomEnrollment enrollment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_kind", nullable = false, length = 30)
    private TuitionPaymentKind paymentKind;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(columnDefinition = "text")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TuitionProofStatus status = TuitionProofStatus.PENDING;

    @Column(name = "review_note", columnDefinition = "text")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
