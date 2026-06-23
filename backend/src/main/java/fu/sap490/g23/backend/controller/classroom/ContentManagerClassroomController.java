package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CenterMaterialLibraryUpsertRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateAnnouncementRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateMaterialRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateSyllabusItemRequest;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.service.classroom.CenterMaterialLibraryService;
import fu.sap490.g23.backend.service.classroom.ClassroomContentService;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sap490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/content-manager/classrooms")
@RequiredArgsConstructor
public class ContentManagerClassroomController {

    private final ClassroomOfferingService classroomOfferingService;
    private final ClassroomContentService contentService;
    private final CenterMaterialLibraryService centerMaterialLibraryService;
    private final HomeworkAttachmentStorageService homeworkAttachmentStorageService;

    @GetMapping
    public ResponseEntity<List<ClassroomOfferingResponse>> listOfferings() {
        return ResponseEntity.ok(classroomOfferingService.getManagerOfferings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getOffering(@PathVariable Long id) {
        return ResponseEntity.ok(classroomOfferingService.getOffering(id, true));
    }

    @GetMapping("/{id}/materials")
    public ResponseEntity<List<ClassroomMaterialResponse>> getMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getMaterials(id));
    }

    @PostMapping("/{id}/materials")
    public ResponseEntity<ClassroomMaterialResponse> createMaterial(
            @PathVariable Long id,
            @Valid @RequestBody CreateMaterialRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(contentService.createMaterial(id, request, authentication.getName()));
    }

    @PostMapping(value = "/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeworkAttachmentUploadResponse> uploadMaterial(
            @RequestPart("file") MultipartFile file
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom-homework/attachments")
                .toUriString();
        return ResponseEntity.ok(homeworkAttachmentStorageService.store(file, publicUrlBase));
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long materialId) {
        contentService.deleteMaterial(materialId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/library")
    public ResponseEntity<List<CenterMaterialLibraryItemResponse>> listMaterialLibrary() {
        return ResponseEntity.ok(centerMaterialLibraryService.listForContentManager());
    }

    @PostMapping("/library")
    public ResponseEntity<CenterMaterialLibraryItemResponse> createMaterialLibraryItem(
            @Valid @RequestBody CenterMaterialLibraryUpsertRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(centerMaterialLibraryService.create(request, authentication.getName()));
    }

    @PutMapping("/library/{materialId}")
    public ResponseEntity<CenterMaterialLibraryItemResponse> updateMaterialLibraryItem(
            @PathVariable Long materialId,
            @Valid @RequestBody CenterMaterialLibraryUpsertRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(centerMaterialLibraryService.update(materialId, request, authentication.getName()));
    }

    @DeleteMapping("/library/{materialId}")
    public ResponseEntity<Void> deleteMaterialLibraryItem(
            @PathVariable Long materialId,
            Authentication authentication
    ) {
        centerMaterialLibraryService.delete(materialId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/library/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HomeworkAttachmentUploadResponse> uploadMaterialLibraryFile(
            @RequestPart("file") MultipartFile file
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/classroom-homework/attachments")
                .toUriString();
        return ResponseEntity.ok(homeworkAttachmentStorageService.store(file, publicUrlBase));
    }

    @GetMapping("/{id}/announcements")
    public ResponseEntity<List<ClassroomAnnouncementResponse>> getAnnouncements(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getAnnouncements(id));
    }

    @PostMapping("/{id}/announcements")
    public ResponseEntity<ClassroomAnnouncementResponse> createAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody CreateAnnouncementRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(contentService.createAnnouncement(id, request, authentication.getName()));
    }

    @DeleteMapping("/announcements/{announcementId}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long announcementId) {
        contentService.deleteAnnouncement(announcementId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/syllabus")
    public ResponseEntity<List<ClassroomSyllabusItemResponse>> getSyllabus(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getSyllabus(id));
    }

    @PostMapping("/{id}/syllabus")
    public ResponseEntity<ClassroomSyllabusItemResponse> createSyllabusItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateSyllabusItemRequest request
    ) {
        return ResponseEntity.ok(contentService.createSyllabusItem(id, request));
    }

    @PutMapping("/syllabus/{itemId}")
    public ResponseEntity<ClassroomSyllabusItemResponse> updateSyllabusItem(
            @PathVariable Long itemId,
            @Valid @RequestBody CreateSyllabusItemRequest request
    ) {
        return ResponseEntity.ok(contentService.updateSyllabusItem(itemId, request));
    }

    @DeleteMapping("/syllabus/{itemId}")
    public ResponseEntity<Void> deleteSyllabusItem(@PathVariable Long itemId) {
        contentService.deleteSyllabusItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
