package fu.sep490.g23.backend.entity.classroom;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementType;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;

import fu.sep490.g23.backend.entity.classroom.enums.*;

import fu.sep490.g23.backend.entity.User;
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
        name = "class_enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_class_enrollment_student_section",
                columnNames = {"student_id", "class_section_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
public class ClassEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    private ClassSection classSection;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 40)
    @Builder.Default
    private ClassroomRegistrationStatus registrationStatus = ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT;

    @Column(name = "hold_spot", nullable = false)
    @Builder.Default
    private boolean holdSpot = false;

    @Column(name = "waitlist_priority")
    private Integer waitlistPriority;

    @Column(name = "agreed_tuition_fee_vnd", nullable = false, precision = 12, scale = 2)
    private BigDecimal agreedTuitionFeeVnd;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "tuition_settlement_status", length = 30)
    @Builder.Default
    private TuitionSettlementStatus tuitionSettlementStatus = TuitionSettlementStatus.NONE;

    @Column(name = "tuition_settlement_resolved_at")
    private LocalDateTime tuitionSettlementResolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuition_settlement_resolved_by_id")
    private User tuitionSettlementResolvedBy;

    @Column(name = "tuition_settlement_resolution_note", length = 700)
    private String tuitionSettlementResolutionNote;

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

    @Column(name = "homework_score", precision = 6, scale = 2)
    private BigDecimal homeworkScore;

    @Column(name = "quiz_score", precision = 6, scale = 2)
    private BigDecimal quizScore;

    @Column(name = "attendance_percent", precision = 5, scale = 2)
    private BigDecimal attendancePercent;

    @Column(name = "participation_score", precision = 6, scale = 2)
    private BigDecimal participationScore;

    @Column(name = "final_result", precision = 6, scale = 2)
    private BigDecimal finalResult;

    @Column(name = "teacher_comment", columnDefinition = "text")
    private String teacherComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "gradebook_status", nullable = false, length = 20)
    @Builder.Default
    private GradebookEntryStatus gradebookStatus = GradebookEntryStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gradebook_updated_by_id")
    private User gradebookUpdatedBy;

    @Column(name = "gradebook_updated_at")
    private LocalDateTime gradebookUpdatedAt;

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
            registrationStatus = ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT;
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
        if (tuitionSettlementStatus == null) {
            tuitionSettlementStatus = TuitionSettlementStatus.NONE;
        }
        if (registrationStatus != ClassroomRegistrationStatus.WAITLIST) {
            waitlistPriority = null;
        }
    }
}
