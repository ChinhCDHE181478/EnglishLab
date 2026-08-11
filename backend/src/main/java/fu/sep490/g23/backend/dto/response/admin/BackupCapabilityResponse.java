package fu.sep490.g23.backend.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BackupCapabilityResponse {
    private boolean backupAvailable;
    private boolean restoreAvailable;
    private String pgDumpVersion;
    private String pgRestoreVersion;
    private long maximumUploadBytes;
    private String restoreConfirmationPhrase;
    private String storageDirectory;
}
