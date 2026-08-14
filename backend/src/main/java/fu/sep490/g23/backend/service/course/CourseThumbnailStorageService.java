package fu.sep490.g23.backend.service.course;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface CourseThumbnailStorageService {

    String store(MultipartFile file);

    Resource load(String fileName);

    String contentType(String fileName);
}
