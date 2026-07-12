package fu.sap490.g23.backend.entity.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.*;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "classroom_enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_classroom_enrollment_student_offering",
                columnNames = {"student_id", "classroom_offering_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class ClassroomEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_offering_id", nullable = false)
    private ClassroomOffering classroomOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_enrollment_id")
    private PackageEnrollment packageEnrollment;

    /** @deprecated Dùng {@link #registrationStatus}. Giữ để tương thích schema cũ. */
    @Deprecated
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ClassroomEnrollmentStatus status = ClassroomEnrollmentStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 40)
    @Builder.Default
    private ClassroomRegistrationStatus registrationStatus = ClassroomRegistrationStatus.PENDING_CONFIRMATION;

    @Column(name = "hold_spot", nullable = false)
    @Builder.Default
    private boolean holdSpot = false;

    @Column(name = "waitlist_priority")
    private Integer waitlistPriority;

    @Column(name = "tuition_amount_due", precision = 12, scale = 2)
    private BigDecimal tuitionAmountDue;

    @Column(name = "tuition_amount_paid", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tuitionAmountPaid = BigDecimal.ZERO;

    @Column(name = "tuition_deposit_paid", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tuitionDepositPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tuition_settlement_type", length = 40)
    @Builder.Default
    private TuitionSettlementType tuitionSettlementType = TuitionSettlementType.NONE;

    @Column(name = "tuition_settlement_note", length = 700)
    private String tuitionSettlementNote;

    @Column(name = "transferred_from_enrollment_id")
    private Long transferredFromEnrollmentId;

    @Column(name = "enrolled_at", nullable = false)
    @Builder.Default
    private LocalDateTime enrolledAt = LocalDateTime.now();

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;

    @Column(name = "assignment_note", length = 500)
    private String assignmentNote;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    private User confirmedBy;

    @Column(name = "tuition_recorded_at")
    private LocalDateTime tuitionRecordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuition_recorded_by_id")
    private User tuitionRecordedBy;

    @Column(length = 500)
    private String note;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean hasClassAccess() {
        return registrationStatus == ClassroomRegistrationStatus.ASSIGNED;
    }

    public BigDecimal tuitionBalance() {
        BigDecimal due = tuitionAmountDue == null ? BigDecimal.ZERO : tuitionAmountDue;
        BigDecimal paid = tuitionAmountPaid == null ? BigDecimal.ZERO : tuitionAmountPaid;
        return due.subtract(paid);
    }

    @PrePersist
    @PreUpdate
    void synchronizeRegistrationFields() {
        if (registrationStatus == null) {
            if (status == ClassroomEnrollmentStatus.ENROLLED) {
                registrationStatus = ClassroomRegistrationStatus.ASSIGNED;
            } else if (status == ClassroomEnrollmentStatus.CANCELLED
                    || status == ClassroomEnrollmentStatus.DROPPED
                    || status == ClassroomEnrollmentStatus.TRANSFERRED) {
                registrationStatus = ClassroomRegistrationStatus.CANCELLED;
            } else {
                registrationStatus = ClassroomRegistrationStatus.PENDING_CONFIRMATION;
            }
        }
        if (tuitionAmountPaid == null) {
            tuitionAmountPaid = BigDecimal.ZERO;
        }
        if (tuitionDepositPaid == null) {
            tuitionDepositPaid = BigDecimal.ZERO;
        }
        if (tuitionSettlementType == null) {
            tuitionSettlementType = TuitionSettlementType.NONE;
        }
        if (registrationStatus != ClassroomRegistrationStatus.WAITLIST) {
            waitlistPriority = null;
        }
        fu.sap490.g23.backend.service.classroom.ClassroomRegistrationSupport.syncLegacyStatus(this);
    }
}
