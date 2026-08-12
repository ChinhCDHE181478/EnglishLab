package fu.sep490.g23.backend.entity.admin;

import fu.sep490.g23.backend.entity.admin.enums.BackupStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_backup_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, unique = true, length = 220)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackupStatus status;

    @Column(name = "file_size_bytes", nullable = false)
    @Builder.Default
    private long fileSizeBytes = 0;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "restored_by", length = 150)
    private String restoredBy;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
