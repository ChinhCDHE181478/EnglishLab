package fu.sap490.g23.backend.dto.response.admin;

import fu.sap490.g23.backend.entity.admin.enums.BackupStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BackupRecordResponse {
    private Long id;
    private String fileName;
    private BackupStatus status;
    private long fileSizeBytes;
    private String sha256;
    private String createdBy;
    private String restoredBy;
    private LocalDateTime restoredAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean downloadable;
}
