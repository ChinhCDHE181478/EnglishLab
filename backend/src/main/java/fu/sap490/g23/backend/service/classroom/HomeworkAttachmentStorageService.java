package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface HomeworkAttachmentStorageService {

    HomeworkAttachmentUploadResponse store(MultipartFile file, String publicUrlBase);
    Resource load(String fileName);
    String contentType(String fileName);
}
