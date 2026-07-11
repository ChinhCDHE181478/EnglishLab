package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CenterMaterialLibraryUpsertRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateAnnouncementRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateMaterialRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateSyllabusItemRequest;
import fu.sap490.g23.backend.dto.request.classroom.TrainingProgramRequest;
import fu.sap490.g23.backend.dto.request.classroom.UpdateClassroomProgramRequest;
import fu.sap490.g23.backend.dto.response.curriculum.CurriculumProgramResponse;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomRoom;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sap490.g23.backend.service.classroom.CenterMaterialLibraryService;
import fu.sap490.g23.backend.service.classroom.ClassroomContentApprovalService;
import fu.sap490.g23.backend.service.classroom.ClassroomContentService;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sap490.g23.backend.service.classroom.ClassroomProgramService;
import fu.sap490.g23.backend.service.classroom.HomeworkAttachmentStorageService;
import fu.sap490.g23.backend.service.classroom.TrainingProgramService;
import fu.sap490.g23.backend.service.curriculum.CurriculumProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/content-manager/classrooms")
@RequiredArgsConstructor
public class ContentManagerClassroomController {

    private final ClassroomOfferingService classroomOfferingService;
    private final ClassroomContentService contentService;
    private final ClassroomProgramService programService;
    private final TrainingProgramService trainingProgramService;
    private final ClassroomContentApprovalService approvalService;
    private final CenterMaterialLibraryService centerMaterialLibraryService;
    private final HomeworkAttachmentStorageService homeworkAttachmentStorageService;
    private final CurriculumProgramService curriculumProgramService;
    private final UserRepository userRepository;
    private final ClassroomRoomRepository roomRepository;

    @GetMapping
    public ResponseEntity<List<ClassroomOfferingResponse>> listOfferings() {
        return ResponseEntity.ok(classroomOfferingService.getManagerOfferings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getOffering(@PathVariable Long id) {
        return ResponseEntity.ok(classroomOfferingService.getOffering(id, true));
    }

    @PostMapping
    public ResponseEntity<ClassroomOfferingResponse> createOffering(
            @Valid @RequestBody CreateClassroomOfferingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.createOffering(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> updateOffering(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassroomOfferingRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.updateOffering(id, request));
    }

    @GetMapping("/teachers")
    public ResponseEntity<List<ClassroomPickerOptionResponse>> listTeachers() {
        List<ClassroomPickerOptionResponse> options = userRepository.findDistinctByRoles_CodeIn(Set.of(RoleEnum.TEACHER))
                .stream()
                .sorted(Comparator.comparing(User::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(user -> ClassroomPickerOptionResponse.builder()
                        .id(user.getId())
                        .label((user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName())
                                + " - " + user.getEmail())
                        .build())
                .toList();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ClassroomPickerOptionResponse>> listRooms() {
        List<ClassroomPickerOptionResponse> options = roomRepository.findByActiveTrue()
                .stream()
                .sorted(Comparator.comparing(ClassroomRoom::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(room -> ClassroomPickerOptionResponse.builder()
                        .id(room.getId())
                        .label(room.getCapacity() == null ? room.getName() : room.getName() + " - " + room.getCapacity() + " chỗ")
                        .capacity(room.getCapacity())
                        .build())
                .toList();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/curriculum-programs")
    public ResponseEntity<List<CurriculumProgramResponse>> listCurriculumPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        return ResponseEntity.ok(curriculumProgramService.listPrograms(deliveryMode));
    }

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

    @PutMapping("/materials/{materialId}")
    public ResponseEntity<ClassroomMaterialResponse> updateMaterial(
            @PathVariable Long materialId,
            @Valid @RequestBody CreateMaterialRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(contentService.updateMaterial(materialId, request, authentication.getName()));
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

    @GetMapping("/programs")
    public ResponseEntity<List<ClassroomOfferingResponse>> listPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        return ResponseEntity.ok(programService.listPrograms(deliveryMode));
    }

    @PutMapping("/{id}/program-profile")
    public ResponseEntity<ClassroomOfferingResponse> updateProgramProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassroomProgramRequest request
    ) {
        return ResponseEntity.ok(programService.updateProgramProfile(id, request));
    }

    @PostMapping("/materials/{materialId}/submit-review")
    public ResponseEntity<ClassroomMaterialResponse> submitMaterialForReview(
            @PathVariable Long materialId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(approvalService.submitMaterialForReview(materialId, authentication.getName()));
    }

    @PostMapping("/syllabus/{itemId}/submit-review")
    public ResponseEntity<ClassroomSyllabusItemResponse> submitSyllabusForReview(
            @PathVariable Long itemId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(approvalService.submitSyllabusForReview(itemId, authentication.getName()));
    }
}
