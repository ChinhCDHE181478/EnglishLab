package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.service.course.CourseThumbnailStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-thumbnails")
@RequiredArgsConstructor
public class CourseThumbnailController {

    private final CourseThumbnailStorageService courseThumbnailStorageService;

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String fileName) {
        Resource resource = courseThumbnailStorageService.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(courseThumbnailStorageService.contentType(fileName)))
                .header("Cache-Control", "public, max-age=86400")
                .body(resource);
    }
}
