package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "classroom_financial_records")
@SQLRestriction("record_type = 'classroom_tuition_payments'")
@EntityListeners(AuditingEntityListener.class)
public class ClassroomTuitionPayment extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "classroom_tuition_payments";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClassEnrollment enrollment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_kind", nullable = false, length = 30)
    private TuitionPaymentKind paymentKind;

    @Column(columnDefinition = "text")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User recordedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
