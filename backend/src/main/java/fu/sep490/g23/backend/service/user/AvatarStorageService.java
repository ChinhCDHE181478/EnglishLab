package fu.sep490.g23.backend.service.user;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AvatarStorageService {

    String store(MultipartFile file);

    Resource load(String fileName);

    String contentType(String fileName);

    void delete(String fileName);

    void deleteByUrl(String avatarUrl);

    /**
     * Deletes the object with the given R2 key directly.
     * Used by event listeners that already have the object key (e.g. "avatars/avatar-xxx.jpg").
     */
    void deleteByKey(String objectKey);
}
