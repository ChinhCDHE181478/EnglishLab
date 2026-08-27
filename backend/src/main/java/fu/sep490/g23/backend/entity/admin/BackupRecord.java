package fu.sep490.g23.backend.entity.admin;

import fu.sep490.g23.backend.entity.admin.enums.BackupStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupRecord {
    private Long id;

    private String fileName;

    private BackupStatus status;

    @Builder.Default
    private long fileSizeBytes = 0;

    private String sha256;

    private String createdBy;

    private String restoredBy;

    private LocalDateTime restoredAt;

    private String failureReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
