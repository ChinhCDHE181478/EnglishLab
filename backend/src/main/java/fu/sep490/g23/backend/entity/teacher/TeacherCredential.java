package fu.sep490.g23.backend.entity.teacher;

import fu.sep490.g23.backend.entity.DomainRecord;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.teacher.enums.CredentialVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_auxiliary_records")
@SQLRestriction("record_type = 'teacher_credentials'")
public class TeacherCredential extends DomainRecord {
    @Override
    protected String domainRecordType() {
        return "teacher_credentials";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User teacher;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, length = 250)
    private String issuer;

    @Column(name = "credential_number", length = 150)
    private String credentialNumber;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "document_url", length = 700)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private CredentialVerificationStatus verificationStatus = CredentialVerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_note", length = 700)
    private String verificationNote;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
