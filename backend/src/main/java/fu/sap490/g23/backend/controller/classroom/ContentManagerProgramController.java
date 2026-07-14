package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CenterMaterialLibraryUpsertRequest;
import fu.sap490.g23.backend.dto.request.classroom.TrainingProgramRequest;
import fu.sap490.g23.backend.dto.response.classroom.CenterMaterialLibraryItemResponse;
import fu.sap490.g23.backend.dto.response.classroom.HomeworkAttachmentUploadResponse;
import fu.sap490.g23.backend.dto.response.classroom.TrainingProgramResponse;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.service.classroom.CenterMaterialLibraryService;
import fu.sap490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sap490.g23.backend.service.classroom.TrainingProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/content-manager")
@RequiredArgsConstructor
public class ContentManagerProgramController {

    private final TrainingProgramService trainingProgramService;
    private final CenterMaterialLibraryService centerMaterialLibraryService;
    private final HomeworkAttachmentStorageService homeworkAttachmentStorageService;

    @GetMapping("/training-programs")
    public ResponseEntity<List<TrainingProgramResponse>> listTrainingPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        return ResponseEntity.ok(trainingProgramService.listPrograms(deliveryMode));
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
            @RequestPart("file") MultipartFile file
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom-homework/attachments")
                .toUriString();
        return ResponseEntity.ok(homeworkAttachmentStorageService.store(file, publicUrlBase));
    }
}
