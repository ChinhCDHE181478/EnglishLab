package fu.sap490.g23.backend.controller.curriculum;

import fu.sap490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sap490.g23.backend.dto.request.curriculum.CurriculumProgramRequest;
import fu.sap490.g23.backend.dto.request.curriculum.CurriculumReferenceRequest;
import fu.sap490.g23.backend.dto.request.curriculum.CurriculumUnitRequest;
import fu.sap490.g23.backend.dto.request.curriculum.FlashcardSetRequest;
import fu.sap490.g23.backend.dto.response.curriculum.AssessmentBankItemResponse;
import fu.sap490.g23.backend.dto.response.curriculum.CurriculumProgramResponse;
import fu.sap490.g23.backend.dto.response.curriculum.CurriculumUnitResponse;
import fu.sap490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.service.curriculum.CurriculumProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/content-manager")
@RequiredArgsConstructor
public class ContentManagerCurriculumController {

    private final CurriculumProgramService curriculumProgramService;

    @GetMapping("/curriculum-programs")
    public ResponseEntity<List<CurriculumProgramResponse>> listPrograms(
            @RequestParam(required = false) ClassroomDeliveryMode deliveryMode
    ) {
        return ResponseEntity.ok(curriculumProgramService.listPrograms(deliveryMode));
    }

    @GetMapping("/curriculum-programs/{id}")
    public ResponseEntity<CurriculumProgramResponse> getProgram(@PathVariable Long id) {
        return ResponseEntity.ok(curriculumProgramService.getProgram(id));
    }

    @PostMapping("/curriculum-programs")
    public ResponseEntity<CurriculumProgramResponse> createProgram(
            @Valid @RequestBody CurriculumProgramRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.createProgram(request));
    }

    @PutMapping("/curriculum-programs/{id}")
    public ResponseEntity<CurriculumProgramResponse> updateProgram(
            @PathVariable Long id,
            @Valid @RequestBody CurriculumProgramRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.updateProgram(id, request));
    }

    @DeleteMapping("/curriculum-programs/{id}")
    public ResponseEntity<Void> archiveProgram(@PathVariable Long id) {
        curriculumProgramService.archiveProgram(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/curriculum-programs/{programId}/units")
    public ResponseEntity<CurriculumUnitResponse> createUnit(
            @PathVariable Long programId,
            @Valid @RequestBody CurriculumUnitRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.createUnit(programId, request));
    }

    @PutMapping("/curriculum-units/{unitId}")
    public ResponseEntity<CurriculumUnitResponse> updateUnit(
            @PathVariable Long unitId,
            @Valid @RequestBody CurriculumUnitRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.updateUnit(unitId, request));
    }

    @DeleteMapping("/curriculum-units/{unitId}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long unitId) {
        curriculumProgramService.deleteUnit(unitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/curriculum-units/{unitId}/materials")
    public ResponseEntity<CurriculumUnitResponse> attachMaterial(
            @PathVariable Long unitId,
            @Valid @RequestBody CurriculumReferenceRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.attachMaterial(unitId, request));
    }

    @PostMapping("/curriculum-units/{unitId}/exercises")
    public ResponseEntity<CurriculumUnitResponse> attachExercise(
            @PathVariable Long unitId,
            @Valid @RequestBody CurriculumReferenceRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.attachExercise(unitId, request));
    }

    @PostMapping("/curriculum-units/{unitId}/assessments")
    public ResponseEntity<CurriculumUnitResponse> attachAssessment(
            @PathVariable Long unitId,
            @Valid @RequestBody CurriculumReferenceRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.attachAssessment(unitId, request));
    }

    @PostMapping("/curriculum-units/{unitId}/flashcards")
    public ResponseEntity<CurriculumUnitResponse> attachFlashcard(
            @PathVariable Long unitId,
            @Valid @RequestBody CurriculumReferenceRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.attachFlashcard(unitId, request));
    }

    @DeleteMapping("/curriculum-refs/{type}/{referenceId}")
    public ResponseEntity<Void> detachReference(
            @PathVariable String type,
            @PathVariable Long referenceId
    ) {
        curriculumProgramService.detachReference(type, referenceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assessment-bank")
    public ResponseEntity<List<AssessmentBankItemResponse>> listAssessmentBank(
            @RequestParam(required = false) AssessmentSkill skill,
            @RequestParam(required = false) AssessmentType type
    ) {
        return ResponseEntity.ok(curriculumProgramService.listAssessmentBank(skill, type));
    }

    @GetMapping("/assessment-bank/{id}")
    public ResponseEntity<AssessmentBankItemResponse> getAssessmentBankItem(@PathVariable Long id) {
        return ResponseEntity.ok(curriculumProgramService.getAssessmentBankItem(id));
    }

    @PostMapping("/assessment-bank")
    public ResponseEntity<AssessmentBankItemResponse> createAssessmentBankItem(
            @Valid @RequestBody AssessmentBankItemRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.createAssessmentBankItem(request));
    }

    @PutMapping("/assessment-bank/{id}")
    public ResponseEntity<AssessmentBankItemResponse> updateAssessmentBankItem(
            @PathVariable Long id,
            @Valid @RequestBody AssessmentBankItemRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.updateAssessmentBankItem(id, request));
    }

    @DeleteMapping("/assessment-bank/{id}")
    public ResponseEntity<Void> archiveAssessmentBankItem(@PathVariable Long id) {
        curriculumProgramService.archiveAssessmentBankItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/flashcard-sets")
    public ResponseEntity<List<FlashcardSetResponse>> listFlashcardSets() {
        return ResponseEntity.ok(curriculumProgramService.listFlashcardSets());
    }

    @GetMapping("/flashcard-sets/{id}")
    public ResponseEntity<FlashcardSetResponse> getFlashcardSet(@PathVariable Long id) {
        return ResponseEntity.ok(curriculumProgramService.getFlashcardSet(id));
    }

    @PostMapping("/flashcard-sets")
    public ResponseEntity<FlashcardSetResponse> createFlashcardSet(
            @Valid @RequestBody FlashcardSetRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.createFlashcardSet(request));
    }

    @PutMapping("/flashcard-sets/{id}")
    public ResponseEntity<FlashcardSetResponse> updateFlashcardSet(
            @PathVariable Long id,
            @Valid @RequestBody FlashcardSetRequest request
    ) {
        return ResponseEntity.ok(curriculumProgramService.updateFlashcardSet(id, request));
    }

    @DeleteMapping("/flashcard-sets/{id}")
    public ResponseEntity<Void> archiveFlashcardSet(@PathVariable Long id) {
        curriculumProgramService.archiveFlashcardSet(id);
        return ResponseEntity.noContent().build();
    }
}
