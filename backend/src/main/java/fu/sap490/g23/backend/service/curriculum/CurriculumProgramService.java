package fu.sap490.g23.backend.service.curriculum;

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

import java.util.List;

public interface CurriculumProgramService {
    List<CurriculumProgramResponse> listPrograms(ClassroomDeliveryMode deliveryMode);

    CurriculumProgramResponse getProgram(Long id);

    CurriculumProgramResponse createProgram(CurriculumProgramRequest request);

    CurriculumProgramResponse updateProgram(Long id, CurriculumProgramRequest request);

    void archiveProgram(Long id);

    CurriculumUnitResponse createUnit(Long programId, CurriculumUnitRequest request);

    CurriculumUnitResponse updateUnit(Long unitId, CurriculumUnitRequest request);

    void deleteUnit(Long unitId);

    CurriculumUnitResponse attachMaterial(Long unitId, CurriculumReferenceRequest request);

    CurriculumUnitResponse attachExercise(Long unitId, CurriculumReferenceRequest request);

    CurriculumUnitResponse attachAssessment(Long unitId, CurriculumReferenceRequest request);

    CurriculumUnitResponse attachFlashcard(Long unitId, CurriculumReferenceRequest request);

    void detachReference(String type, Long referenceId);

    List<AssessmentBankItemResponse> listAssessmentBank(AssessmentSkill skill, AssessmentType type);

    AssessmentBankItemResponse getAssessmentBankItem(Long id);

    AssessmentBankItemResponse createAssessmentBankItem(AssessmentBankItemRequest request);

    AssessmentBankItemResponse updateAssessmentBankItem(Long id, AssessmentBankItemRequest request);

    void archiveAssessmentBankItem(Long id);

    List<FlashcardSetResponse> listFlashcardSets();

    FlashcardSetResponse getFlashcardSet(Long id);

    FlashcardSetResponse createFlashcardSet(FlashcardSetRequest request);

    FlashcardSetResponse updateFlashcardSet(Long id, FlashcardSetRequest request);

    void archiveFlashcardSet(Long id);
}
