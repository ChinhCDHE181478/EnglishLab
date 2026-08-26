package fu.sep490.g23.backend.controller.curriculum;

import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.dto.request.curriculum.InstructorLedCourseRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseUnitContentRefRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseLessonRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseUnitRequest;
import fu.sep490.g23.backend.dto.request.curriculum.FlashcardSetRequest;
import fu.sep490.g23.backend.dto.response.curriculum.AssessmentBankItemResponse;
import fu.sep490.g23.backend.dto.response.curriculum.InstructorLedCourseResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CourseLessonResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CourseUnitResponse;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.service.curriculum.InstructorLedCourseManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content-manager")
@RequiredArgsConstructor
public class ContentManagerInstructorLedCourseController {

    private final InstructorLedCourseManagementService instructorLedCourseManagementService;

    @GetMapping("/curriculum-programs")
    public ResponseEntity<List<InstructorLedCourseResponse>> listPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.listPrograms(deliveryMode));
    }

    @GetMapping("/curriculum-programs/page")
    public ResponseEntity<Page<InstructorLedCourseResponse>> pagePrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String examCategory,
            @RequestParam(required = false) String entryLevel,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 8, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.pagePrograms(
                deliveryMode, keyword, examCategory, entryLevel, status, pageable));
    }

    @GetMapping("/curriculum-programs/{id}")
    public ResponseEntity<InstructorLedCourseResponse> getProgram(@PathVariable Long id) {
        return ResponseEntity.ok(instructorLedCourseManagementService.getProgram(id));
    }

    @PostMapping("/curriculum-programs")
    public ResponseEntity<InstructorLedCourseResponse> createProgram(
            @Valid @RequestBody InstructorLedCourseRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.createProgram(request));
    }

    @PutMapping("/curriculum-programs/{id}")
    public ResponseEntity<InstructorLedCourseResponse> updateProgram(
            @PathVariable Long id,
            @Valid @RequestBody InstructorLedCourseRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.updateProgram(id, request));
    }

    @DeleteMapping("/curriculum-programs/{id}")
    public ResponseEntity<Void> archiveProgram(@PathVariable Long id) {
        instructorLedCourseManagementService.archiveProgram(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/curriculum-programs/{id}/clone")
    public ResponseEntity<InstructorLedCourseResponse> cloneProgram(@PathVariable Long id) {
        return ResponseEntity.ok(instructorLedCourseManagementService.cloneProgram(id));
    }

    @PostMapping("/curriculum-programs/{id}/publish")
    public ResponseEntity<InstructorLedCourseResponse> publishProgram(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.publishProgram(id, authentication.getName()));
    }

    @PostMapping("/curriculum-programs/{programId}/units")
    public ResponseEntity<CourseUnitResponse> createUnit(
            @PathVariable Long programId,
            @Valid @RequestBody CourseUnitRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.createUnit(programId, request));
    }

    @PutMapping("/curriculum-units/{unitId}")
    public ResponseEntity<CourseUnitResponse> updateUnit(
            @PathVariable Long unitId,
            @Valid @RequestBody CourseUnitRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.updateUnit(unitId, request));
    }

    @DeleteMapping("/curriculum-units/{unitId}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long unitId) {
        instructorLedCourseManagementService.deleteUnit(unitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/curriculum-units/{unitId}/session-plans")
    public ResponseEntity<CourseLessonResponse> createSessionPlan(
            @PathVariable Long unitId,
            @Valid @RequestBody CourseLessonRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.createSessionPlan(unitId, request));
    }

    @PutMapping("/curriculum-session-plans/{sessionPlanId}")
    public ResponseEntity<CourseLessonResponse> updateSessionPlan(
            @PathVariable Long sessionPlanId,
            @Valid @RequestBody CourseLessonRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.updateSessionPlan(sessionPlanId, request));
    }

    @DeleteMapping("/curriculum-session-plans/{sessionPlanId}")
    public ResponseEntity<Void> deleteSessionPlan(@PathVariable Long sessionPlanId) {
        instructorLedCourseManagementService.deleteSessionPlan(sessionPlanId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/curriculum-units/{unitId}/materials")
    public ResponseEntity<CourseUnitResponse> attachMaterial(
            @PathVariable Long unitId,
            @Valid @RequestBody CourseUnitContentRefRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.attachMaterial(unitId, request));
    }

    @PostMapping("/curriculum-units/{unitId}/exercises")
    public ResponseEntity<CourseUnitResponse> attachExercise(
            @PathVariable Long unitId,
            @Valid @RequestBody CourseUnitContentRefRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.attachExercise(unitId, request));
    }

    @PostMapping("/curriculum-units/{unitId}/assessments")
    public ResponseEntity<CourseUnitResponse> attachAssessment(
            @PathVariable Long unitId,
            @Valid @RequestBody CourseUnitContentRefRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.attachAssessment(unitId, request));
    }

    @PostMapping("/curriculum-units/{unitId}/flashcards")
    public ResponseEntity<CourseUnitResponse> attachFlashcard(
            @PathVariable Long unitId,
            @Valid @RequestBody CourseUnitContentRefRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.attachFlashcard(unitId, request));
    }

    @DeleteMapping("/curriculum-refs/{type}/{referenceId}")
    public ResponseEntity<Void> detachReference(
            @PathVariable String type,
            @PathVariable Long referenceId
    ) {
        instructorLedCourseManagementService.detachReference(type, referenceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assessment-bank")
    public ResponseEntity<List<AssessmentBankItemResponse>> listAssessmentBank(
            @RequestParam(required = false) AssessmentSkill skill,
            @RequestParam(required = false) AssessmentType type
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.listAssessmentBank(skill, type));
    }

    @GetMapping("/assessment-bank/page")
    public ResponseEntity<Page<AssessmentBankItemResponse>> pageAssessmentBank(
            @RequestParam(required = false) AssessmentSkill skill,
            @RequestParam(required = false) AssessmentType type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String examCategory,
            @PageableDefault(size = 8, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.pageAssessmentBank(
                skill, type, status, keyword, examCategory, pageable));
    }

    @GetMapping("/assessment-bank/stats")
    public ResponseEntity<Map<String, Long>> getAssessmentBankStats(
            @RequestParam(required = false) AssessmentSkill skill,
            @RequestParam(required = false) AssessmentType type
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.getAssessmentBankStats(skill, type));
    }

    @GetMapping("/assessment-bank/{id}")
    public ResponseEntity<AssessmentBankItemResponse> getAssessmentBankItem(@PathVariable Long id) {
        return ResponseEntity.ok(instructorLedCourseManagementService.getAssessmentBankItem(id));
    }

    @PostMapping("/assessment-bank")
    public ResponseEntity<AssessmentBankItemResponse> createAssessmentBankItem(
            @Valid @RequestBody AssessmentBankItemRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.createAssessmentBankItem(request));
    }

    @PutMapping("/assessment-bank/{id}")
    public ResponseEntity<AssessmentBankItemResponse> updateAssessmentBankItem(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentBankItemRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.updateAssessmentBankItem(id, request));
    }

    @DeleteMapping("/assessment-bank/{id}")
    public ResponseEntity<Void> archiveAssessmentBankItem(@PathVariable Long id) {
        instructorLedCourseManagementService.archiveAssessmentBankItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/flashcard-sets")
    public ResponseEntity<List<FlashcardSetResponse>> listFlashcardSets() {
        return ResponseEntity.ok(instructorLedCourseManagementService.listFlashcardSets());
    }

    @GetMapping("/flashcard-sets/page")
    public ResponseEntity<Page<FlashcardSetResponse>> pageFlashcardSets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String examCategory,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 8, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.pageFlashcardSets(
                keyword, examCategory, skill, status, pageable));
    }

    @GetMapping("/flashcard-sets/stats")
    public ResponseEntity<Map<String, Long>> getFlashcardSetStats(
            @RequestParam(required = false) String examCategory,
            @RequestParam(required = false) String skill
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.getFlashcardSetStats(examCategory, skill));
    }

    @GetMapping("/flashcard-sets/{id}")
    public ResponseEntity<FlashcardSetResponse> getFlashcardSet(@PathVariable Long id) {
        return ResponseEntity.ok(instructorLedCourseManagementService.getFlashcardSet(id));
    }

    @PostMapping("/flashcard-sets")
    public ResponseEntity<FlashcardSetResponse> createFlashcardSet(
            @Valid @RequestBody FlashcardSetRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.createFlashcardSet(request));
    }

    @PutMapping("/flashcard-sets/{id}")
    public ResponseEntity<FlashcardSetResponse> updateFlashcardSet(
            @PathVariable Long id,
            @Valid @RequestBody FlashcardSetRequest request
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.updateFlashcardSet(id, request));
    }

    @DeleteMapping("/flashcard-sets/{id}")
    public ResponseEntity<Void> archiveFlashcardSet(@PathVariable Long id) {
        instructorLedCourseManagementService.archiveFlashcardSet(id);
        return ResponseEntity.noContent().build();
    }
}
