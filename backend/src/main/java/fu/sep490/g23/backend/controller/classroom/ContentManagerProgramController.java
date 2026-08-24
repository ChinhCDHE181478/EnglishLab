package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CenterMaterialLibraryUpsertRequest;
import fu.sep490.g23.backend.dto.request.classroom.TrainingProgramRequest;
import fu.sep490.g23.backend.dto.response.classroom.CenterMaterialLibraryItemResponse;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import fu.sep490.g23.backend.dto.response.classroom.TrainingProgramResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.service.classroom.CenterMaterialLibraryService;
import fu.sep490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sep490.g23.backend.service.classroom.TrainingProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content-manager")
@RequiredArgsConstructor
public class ContentManagerProgramController {

    private final TrainingProgramService trainingProgramService;
    private final CenterMaterialLibraryService centerMaterialLibraryService;
    private final HomeworkAttachmentStorageService homeworkAttachmentStorageService;

    @GetMapping("/training-programs")
    public ResponseEntity<List<TrainingProgramResponse>> listTrainingPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryType,
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        if (deliveryType != null && deliveryMode != null && deliveryType != deliveryMode) {
            throw new IllegalArgumentException("deliveryType và deliveryMode không được mâu thuẫn.");
        }
        return ResponseEntity.ok(trainingProgramService.listPrograms(
                deliveryType != null ? deliveryType : deliveryMode
        ));
    }

    @GetMapping("/training-programs/{id}")
    public ResponseEntity<TrainingProgramResponse> getTrainingProgram(@PathVariable Long id) {
        return ResponseEntity.ok(trainingProgramService.getProgram(id));
    }

    @PostMapping("/training-programs")
    public ResponseEntity<TrainingProgramResponse> createTrainingProgram(
            @Valid @RequestBody TrainingProgramRequest request
    ) {
        return ResponseEntity.ok(trainingProgramService.createProgram(request));
    }

    @PutMapping("/training-programs/{id}")
    public ResponseEntity<TrainingProgramResponse> updateTrainingProgram(
            @PathVariable Long id,
            @Valid @RequestBody TrainingProgramRequest request
    ) {
        return ResponseEntity.ok(trainingProgramService.updateProgram(id, request));
    }

    @PostMapping("/training-programs/{id}/clone")
    public ResponseEntity<TrainingProgramResponse> cloneTrainingProgram(@PathVariable Long id) {
        return ResponseEntity.ok(trainingProgramService.cloneProgram(id));
    }

    @DeleteMapping("/training-programs/{id}")
    public ResponseEntity<Void> archiveTrainingProgram(@PathVariable Long id) {
        trainingProgramService.archiveProgram(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/material-library")
    public ResponseEntity<List<CenterMaterialLibraryItemResponse>> listMaterialLibrary() {
        return ResponseEntity.ok(centerMaterialLibraryService.listForContentManager());
    }

    @GetMapping("/material-library/page")
    public ResponseEntity<Page<CenterMaterialLibraryItemResponse>> pageMaterialLibrary(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String examCategory,
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String provider,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(centerMaterialLibraryService.pageForContentManager(
                keyword, examCategory, materialType, skill, status, provider,
                PageRequest.of(Math.max(page, 0), safeSize,
                        Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")))
        ));
    }

    @GetMapping("/material-library/stats")
    public ResponseEntity<Map<String, Long>> getMaterialLibraryStats() {
        return ResponseEntity.ok(centerMaterialLibraryService.getStats());
    }

    @GetMapping("/material-library/providers")
    public ResponseEntity<List<String>> listMaterialLibraryProviders() {
        return ResponseEntity.ok(centerMaterialLibraryService.listProviders());
    }

    @PostMapping("/material-library")
    public ResponseEntity<CenterMaterialLibraryItemResponse> createMaterialLibraryItem(
            @Valid @RequestBody CenterMaterialLibraryUpsertRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(centerMaterialLibraryService.create(request, authentication.getName()));
    }

    @PutMapping("/material-library/{materialId}")
    public ResponseEntity<CenterMaterialLibraryItemResponse> updateMaterialLibraryItem(
            @PathVariable Long materialId,
            @Valid @RequestBody CenterMaterialLibraryUpsertRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(centerMaterialLibraryService.update(materialId, request, authentication.getName()));
    }

    @DeleteMapping("/material-library/{materialId}")
    public ResponseEntity<Void> deleteMaterialLibraryItem(
            @PathVariable Long materialId,
            Authentication authentication
    ) {
        centerMaterialLibraryService.delete(materialId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/material-library/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeworkAttachmentUploadResponse> uploadMaterialLibraryFile(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom-homework/attachments")
                .toUriString();
        return ResponseEntity.ok(homeworkAttachmentStorageService.store(file, publicUrlBase, authentication.getName()));
    }
}
