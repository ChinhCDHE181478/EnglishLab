package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
    private final HomeworkAttachmentStorageService storageService;

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> download(@PathVariable String fileName, Authentication authentication) {
        Resource resource = storageService.load(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, storageService.contentType(fileName))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
