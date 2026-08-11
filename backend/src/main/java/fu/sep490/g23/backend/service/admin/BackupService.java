package fu.sep490.g23.backend.service.admin;

import fu.sep490.g23.backend.dto.response.admin.BackupCapabilityResponse;
import fu.sep490.g23.backend.dto.response.admin.BackupRecordResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;

public interface BackupService {
    BackupCapabilityResponse capabilities();
    Page<BackupRecordResponse> list(Pageable pageable);
    BackupRecordResponse create(String actorEmail);
    Resource download(Long id);
    BackupRecordResponse restore(String actorEmail, InputStream input, String originalFileName,
                                 long uploadSize, String confirmation);
    void delete(String actorEmail, Long id);
}
