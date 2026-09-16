package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentAccessService;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classroom-homework/attachments")
@RequiredArgsConstructor
public class ClassroomHomeworkAttachmentController {
    private final HomeworkAttachmentAccessService accessService;
    private final ObjectStore objectStore;

    private static final String LOCAL_FILES_PREFIX = "/local-files/";

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName, Authentication authentication) {
        Resource resource = accessService.loadAuthorized(fileName, authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, accessService.contentType(fileName))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    /**
     * Serves legacy local files stored under /local-files/{objectKey}.
     * These are stored in the local filesystem (classroom-attachments prefix).
     * Requires authentication and forces a download via Content-Disposition: attachment.
     */
    @GetMapping(value = "/local-files/{objectKey:.+}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> serveLocalFile(@PathVariable String objectKey, Authentication authentication) {
        // objectKey may look like: "classroom-attachments/homework-uuid.pdf"
        // Strip the prefix if present to get the object key
        String strippedKey = objectKey;
        if (strippedKey.startsWith("classroom-attachments/")) {
            strippedKey = strippedKey.substring("classroom-attachments/".length());
        }

        Resource resource = accessService.loadAuthorized(strippedKey, authentication.getName());
        String fileName = strippedKey.contains("/")
                ? strippedKey.substring(strippedKey.lastIndexOf('/') + 1)
                : strippedKey;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, accessService.contentType(strippedKey))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
