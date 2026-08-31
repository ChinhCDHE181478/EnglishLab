package fu.sep490.g23.backend.service.curriculum;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface InstructorLedCourseManagementService {
    List<InstructorLedCourseResponse> listPrograms(ClassroomDeliveryMode deliveryMode);

    Page<InstructorLedCourseResponse> pagePrograms(
            ClassroomDeliveryMode deliveryMode,
            String keyword,
            String examCategory,
            String entryLevel,
            String status,
            Pageable pageable
    );

    InstructorLedCourseResponse getProgram(Long id);

    InstructorLedCourseResponse createProgram(InstructorLedCourseRequest request);

    InstructorLedCourseResponse updateProgram(Long id, InstructorLedCourseRequest request);

    void archiveProgram(Long id);

    InstructorLedCourseResponse cloneProgram(Long id);

    InstructorLedCourseResponse publishProgram(Long id, String actorEmail);

    CourseUnitResponse createUnit(Long programId, CourseUnitRequest request);

    CourseUnitResponse updateUnit(Long unitId, CourseUnitRequest request);

    void deleteUnit(Long unitId);

    CourseLessonResponse createSessionPlan(Long unitId, CourseLessonRequest request);

    CourseLessonResponse updateSessionPlan(Long sessionPlanId, CourseLessonRequest request);

    void deleteSessionPlan(Long sessionPlanId);

    CourseUnitResponse attachMaterial(Long unitId, CourseUnitContentRefRequest request);

    CourseUnitResponse attachExercise(Long unitId, CourseUnitContentRefRequest request);

    CourseUnitResponse attachAssessment(Long unitId, CourseUnitContentRefRequest request);

    CourseUnitResponse attachFlashcard(Long unitId, CourseUnitContentRefRequest request);

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
