package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface BunnyStreamService {

    BunnyVideoUploadResponse uploadVideo(MultipartFile file, String title);
}
