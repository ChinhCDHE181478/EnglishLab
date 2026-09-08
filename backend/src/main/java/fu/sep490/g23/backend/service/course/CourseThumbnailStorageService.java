package fu.sep490.g23.backend.service.course;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface CourseThumbnailStorageService {

    String store(MultipartFile file);

    Resource load(String fileName);

    String contentType(String fileName);

    /**
     * Deletes the thumbnail referenced by a previously-stored URL.
     *
     * <p>Used by CRUD flows so the previous thumbnail does not stay orphaned in the object
     * store once the course row no longer references it.
     */
    void deleteByUrl(String thumbnailUrl);

    /**
     * Deletes the object with the given R2 key directly.
     * Used by event listeners that already have the object key (e.g. "course-thumbnails/course-thumbnail-xxx.png").
     */
    void deleteByKey(String objectKey);
}
