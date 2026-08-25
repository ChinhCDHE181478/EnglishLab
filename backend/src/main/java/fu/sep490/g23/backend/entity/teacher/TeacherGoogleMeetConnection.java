package fu.sep490.g23.backend.entity.teacher;

import fu.sep490.g23.backend.entity.DomainRecord;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.teacher.enums.GoogleMeetConnectionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_auxiliary_records")
@SQLRestriction("record_type = 'teacher_google_meet_connections'")
@EntityListeners(AuditingEntityListener.class)
public class TeacherGoogleMeetConnection extends DomainRecord {

    @Override
    protected String domainRecordType() {
        return "teacher_google_meet_connections";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, unique = true)
    private User teacher;

    @Column(name = "google_subject", nullable = false, length = 255)
    private String googleSubject;

    @Column(name = "google_email", nullable = false, length = 255)
    private String googleEmail;

    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "text")
    private String encryptedRefreshToken;

    @Column(nullable = false, length = 500)
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(name = "meet_connection_status", nullable = false, length = 30)
    private GoogleMeetConnectionStatus status;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
