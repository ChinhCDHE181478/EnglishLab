package fu.sep490.g23.backend.entity.classroom;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "classroom_operation_records")
@SQLRestriction("record_type = 'course_enrollment_request_history'")
@EntityListeners(AuditingEntityListener.class)
public class EnrollmentRequestStatusHistory extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "course_enrollment_request_history";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_request_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private EnrollmentRequest enrollmentRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 40)
    private EnrollmentRequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    private EnrollmentRequestStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User actor;

    @Column(name = "transition_reason", length = 700)
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
