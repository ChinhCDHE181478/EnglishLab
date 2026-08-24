package fu.sep490.g23.backend.service.curriculum;

import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CurriculumProgramRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CurriculumReferenceRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CurriculumSessionPlanRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CurriculumUnitRequest;
import fu.sep490.g23.backend.dto.request.curriculum.FlashcardSetRequest;
import fu.sep490.g23.backend.dto.response.curriculum.AssessmentBankItemResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumProgramResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumSessionPlanResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumUnitResponse;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface CurriculumProgramService {
    List<CurriculumProgramResponse> listPrograms(ClassroomDeliveryMode deliveryMode);

    Page<CurriculumProgramResponse> pagePrograms(
            ClassroomDeliveryMode deliveryMode,
            String keyword,
            String examCategory,
            String entryLevel,
            String status,
            Pageable pageable
    );

    CurriculumProgramResponse getProgram(Long id);

    CurriculumProgramResponse createProgram(CurriculumProgramRequest request);

    CurriculumProgramResponse updateProgram(Long id, CurriculumProgramRequest request);

    void archiveProgram(Long id);

    CurriculumProgramResponse cloneProgram(Long id);

    CurriculumProgramResponse publishProgram(Long id, String actorEmail);

    CurriculumUnitResponse createUnit(Long programId, CurriculumUnitRequest request);

    CurriculumUnitResponse updateUnit(Long unitId, CurriculumUnitRequest request);

    void deleteUnit(Long unitId);

    CurriculumSessionPlanResponse createSessionPlan(Long unitId, CurriculumSessionPlanRequest request);

    CurriculumSessionPlanResponse updateSessionPlan(Long sessionPlanId, CurriculumSessionPlanRequest request);

    void deleteSessionPlan(Long sessionPlanId);

    CurriculumUnitResponse attachMaterial(Long unitId, CurriculumReferenceRequest request);

    CurriculumUnitResponse attachExercise(Long unitId, CurriculumReferenceRequest request);

    CurriculumUnitResponse attachAssessment(Long unitId, CurriculumReferenceRequest request);

    CurriculumUnitResponse attachFlashcard(Long unitId, CurriculumReferenceRequest request);

    void detachReference(String type, Long referenceId);

    List<AssessmentBankItemResponse> listAssessmentBank(AssessmentSkill skill, AssessmentType type);

    Page<AssessmentBankItemResponse> pageAssessmentBank(
            AssessmentSkill skill,
            AssessmentType type,
            String status,
            String keyword,
            String examCategory,
            Pageable pageable
    );

    Map<String, Long> getAssessmentBankStats(AssessmentSkill skill, AssessmentType type);

    AssessmentBankItemResponse getAssessmentBankItem(Long id);

    List<AssessmentBankItemResponse> listPublishedMockTests();

    AssessmentBankItemResponse getPublishedMockTest(Long id);

    AssessmentBankItemResponse createAssessmentBankItem(AssessmentBankItemRequest request);

    AssessmentBankItemResponse updateAssessmentBankItem(Long id, AssessmentBankItemRequest request);

    void archiveAssessmentBankItem(Long id);

    List<FlashcardSetResponse> listFlashcardSets();

    Page<FlashcardSetResponse> pageFlashcardSets(
            String keyword,
            String examCategory,
            String skill,
            String status,
            Pageable pageable
    );

    Map<String, Long> getFlashcardSetStats(String examCategory, String skill);

    FlashcardSetResponse getFlashcardSet(Long id);

    FlashcardSetResponse createFlashcardSet(FlashcardSetRequest request);

    FlashcardSetResponse updateFlashcardSet(Long id, FlashcardSetRequest request);

    void archiveFlashcardSet(Long id);
}
