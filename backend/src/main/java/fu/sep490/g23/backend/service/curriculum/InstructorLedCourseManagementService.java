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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface InstructorLedCourseManagementService {
    List<InstructorLedCourseResponse> listInstructorLedCourses();

    Page<InstructorLedCourseResponse> pageInstructorLedCourses(
            String keyword,
            String examCategory,
            String entryLevel,
            String status,
            Pageable pageable
    );

    InstructorLedCourseResponse getInstructorLedCourse(Long id);

    InstructorLedCourseResponse createInstructorLedCourse(InstructorLedCourseRequest request);

    InstructorLedCourseResponse updateInstructorLedCourse(Long id, InstructorLedCourseRequest request);

    void archiveInstructorLedCourse(Long id);

    InstructorLedCourseResponse cloneInstructorLedCourse(Long id);

    InstructorLedCourseResponse publishInstructorLedCourse(Long id, String actorEmail);

    CourseUnitResponse createUnit(Long instructorLedCourseId, CourseUnitRequest request);

    CourseUnitResponse updateUnit(Long unitId, CourseUnitRequest request);

    void deleteUnit(Long unitId);

    CourseLessonResponse createCourseLesson(Long unitId, CourseLessonRequest request);

    CourseLessonResponse updateCourseLesson(Long lessonId, CourseLessonRequest request);

    void deleteCourseLesson(Long lessonId);

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
