package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface HomeworkAttachmentStorageService {

    HomeworkAttachmentUploadResponse store(MultipartFile file, String publicUrlBase, String ownerKey);
    Resource load(String fileName);
    String contentType(String fileName);
    Optional<StoredHomeworkAttachment> loadStoredAttachmentFromUrl(String attachmentUrl);
    List<String> findStoredFileNamesOlderThan(Duration minimumAge);
    void delete(String fileName);

    record StoredHomeworkAttachment(String fileName, String contentType, long size, byte[] bytes) {
    }
}
