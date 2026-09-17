package fu.sep490.g23.backend.service.curriculum.impl;

import fu.sep490.g23.backend.dto.request.curriculum.InstructorLedCourseRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseLessonRequest;
import fu.sep490.g23.backend.dto.request.curriculum.AssessmentBankItemRequest;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.CourseLesson;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorLedCourseManagementServiceImplTest {

    @Mock private InstructorLedCourseRepository instructorLedCourseRepository;
    @Mock private CourseUnitRepository unitRepository;
    @Mock private CourseLessonRepository courseLessonRepository;
    @Mock private fu.sep490.g23.backend.repository.course.CourseUnitContentRefRepository contentRefRepository;
    @Mock private CenterMaterialLibraryItemRepository materialRepository;
    @Mock private ExerciseBankItemRepository exerciseRepository;
    @Mock private AssessmentRubricRepository assessmentRubricRepository;
    @Mock private AssessmentBankItemRepository assessmentBankRepository;
    @Mock private FlashcardSetRepository flashcardSetRepository;
    @Mock private ClassroomAccessHelper accessHelper;

    @InjectMocks
    private InstructorLedCourseManagementServiceImpl service;

    @Test
    void createAssessmentBankItemRejectsWritingWithoutRubric() {
        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle("IELTS Writing Task 2");
        request.setType(AssessmentType.LESSON_PRACTICE);
        request.setSkill(AssessmentSkill.WRITING);
        request.setUiConfigJson("{}");

        assertThatThrownBy(() -> service.createAssessmentBankItem(request))
                .hasMessage("Bài Writing/Speaking phải có bộ tiêu chí chấm.");
        verify(assessmentBankRepository, never()).save(any());
    }

    @Test
    void createAssessmentBankItemUsesAutomaticBandModeForObjectiveSkill() {
        AssessmentBankItemRequest request = new AssessmentBankItemRequest();
        request.setTitle("IELTS Listening");
        request.setType(AssessmentType.LESSON_PRACTICE);
        request.setSkill(AssessmentSkill.LISTENING);
        request.setAiEvaluationMode(AiEvaluationMode.NONE);
        request.setObjectiveAnswerKey("{}");
        when(assessmentBankRepository.save(any(AssessmentBankItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createAssessmentBankItem(request);

        ArgumentCaptor<AssessmentBankItem> captor = ArgumentCaptor.forClass(AssessmentBankItem.class);
        verify(assessmentBankRepository).save(captor.capture());
        assertThat(captor.getValue().getAiEvaluationMode()).isEqualTo(AiEvaluationMode.ESTIMATED_BAND);
    }

    @Test
    void createInstructorLedCoursePersistsCanonicalIeltsProfile() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setFocusSkills("SPEAKING,LISTENING,READING,WRITING,LISTENING");
        when(instructorLedCourseRepository.existsByCodeIgnoreCase("IELTS-65")).thenReturn(false);
        when(instructorLedCourseRepository.save(any(InstructorLedCourse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createInstructorLedCourse(request);

        ArgumentCaptor<InstructorLedCourse> captor = ArgumentCaptor.forClass(InstructorLedCourse.class);
        verify(instructorLedCourseRepository).save(captor.capture());
        InstructorLedCourse saved = captor.getValue();
        assertThat(saved.getExamType()).isEqualTo("IELTS");
        assertThat(saved.getFocusSkills()).isEqualTo("LISTENING,READING,WRITING,SPEAKING");
        assertThat(saved.getTargetBand()).isEqualByComparingTo("6.5");
        assertThat(saved.getTargetScore()).isNull();
    }

    @Test
    void createInstructorLedCourseGeneratesUniqueCodeWhenCodeIsMissing() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setCode(null);
        when(instructorLedCourseRepository.existsByCodeIgnoreCase("ILC-IELTS-ACADEMIC-6-5")).thenReturn(true);
        when(instructorLedCourseRepository.existsByCodeIgnoreCase("ILC-IELTS-ACADEMIC-6-5-2")).thenReturn(false);
        when(instructorLedCourseRepository.save(any(InstructorLedCourse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createInstructorLedCourse(request);

        ArgumentCaptor<InstructorLedCourse> captor = ArgumentCaptor.forClass(InstructorLedCourse.class);
        verify(instructorLedCourseRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("ILC-IELTS-ACADEMIC-6-5-2");
    }

    @Test
    void updateInstructorLedCourseKeepsExistingCodeWhenCodeIsMissing() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setCode(null);
        InstructorLedCourse existing = InstructorLedCourse.builder()
                .id(9L)
                .title("IELTS cũ")
                .code("OFFLINE-IELTS-CU")
                .examType("IELTS")
                .publicationStatus(PackageStatus.DRAFT)
                .build();
        when(instructorLedCourseRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(instructorLedCourseRepository.save(any(InstructorLedCourse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateInstructorLedCourse(9L, request);

        assertThat(existing.getCode()).isEqualTo("OFFLINE-IELTS-CU");
    }

    @Test
    void createInstructorLedCourseRejectsToeicUsingIeltsBand() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setExamCategory("TOEIC");
        request.setEntryLevel("450");
        request.setTargetScore(650);

        assertThatThrownBy(() -> service.createInstructorLedCourse(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không sử dụng band IELTS");
        verify(instructorLedCourseRepository, never()).save(any());
    }

    @Test
    void createInstructorLedCourseRejectsEntryBandHigherThanTargetBand() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setEntryLevel("7.0");

        assertThatThrownBy(() -> service.createInstructorLedCourse(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đầu vào không thể cao hơn band mục tiêu");
        verify(instructorLedCourseRepository, never()).save(any());
    }

    @Test
    void createInstructorLedCourseRejectsInvalidGeneralEnglishCefrLevel() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setExamCategory("GENERAL_ENGLISH");
        request.setEntryLevel("Sơ cấp");
        request.setTargetBand(null);

        assertThatThrownBy(() -> service.createInstructorLedCourse(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CEFR");
        verify(instructorLedCourseRepository, never()).save(any());
    }

    @Test
    void createInstructorLedCourseRejectsCategoryOutsideEnglishTraining() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setExamCategory("PROGRAMMING");

        assertThatThrownBy(() -> service.createInstructorLedCourse(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IELTS, TOEIC hoặc General English");
        verify(instructorLedCourseRepository, never()).save(any());
    }

    @Test
    void createCourseLessonPersistsAndSynchronizesTotalSessions() {
        CourseUnit unit = unit(10L, program(1L));
        CourseLessonRequest request = sessionPlanRequest(1, "Reading Overview");
        when(unitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(courseLessonRepository.existsDuplicateSequenceNumber(10L, 1, null)).thenReturn(false);
        when(courseLessonRepository.save(any(CourseLesson.class))).thenAnswer(invocation -> {
            CourseLesson saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        var response = service.createCourseLesson(10L, request);

        assertThat(response.getSessionNumber()).isEqualTo(1);
        assertThat(response.getTitle()).isEqualTo("Reading Overview");
        assertThat(response.getSessionNumber()).isEqualTo(unit.getLessons().isEmpty() ? 1 : unit.getLessons().getFirst().getSequenceNumber());
    }

    @Test
    void updateCourseLessonPersistsChanges() {
        CourseLesson plan = plan(101L, unit(10L, program(1L)), 1, "Cũ");
        when(courseLessonRepository.findById(101L)).thenReturn(Optional.of(plan));
        when(courseLessonRepository.existsDuplicateSequenceNumber(10L, 2, 101L)).thenReturn(false);
        when(courseLessonRepository.save(plan)).thenReturn(plan);

        var response = service.updateCourseLesson(101L, sessionPlanRequest(2, "Scanning + Keywords"));

        assertThat(response.getSessionNumber()).isEqualTo(2);
        assertThat(response.getTitle()).isEqualTo("Scanning + Keywords");
    }

    @Test
    void createCourseLessonRejectsNumberBelowOne() {
        when(unitRepository.findById(10L)).thenReturn(Optional.of(unit(10L, program(1L))));

        assertThatThrownBy(() -> service.createCourseLesson(10L, sessionPlanRequest(0, "Sai")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bắt đầu từ 1");
        verify(courseLessonRepository, never()).save(any());
    }

    @Test
    void createCourseLessonRejectsDuplicateNumberInsideUnit() {
        when(unitRepository.findById(10L)).thenReturn(Optional.of(unit(10L, program(1L))));
        when(courseLessonRepository.existsDuplicateSequenceNumber(10L, 1, null)).thenReturn(true);

        assertThatThrownBy(() -> service.createCourseLesson(10L, sessionPlanRequest(1, "Trùng")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bài học số 1 đã tồn tại");
    }

    @Test
    void differentUnitsMayBothUseLessonNumberOne() {
        CourseUnit firstUnit = unit(10L, program(1L));
        CourseUnit secondUnit = unit(20L, program(1L));
        when(unitRepository.findById(10L)).thenReturn(Optional.of(firstUnit));
        when(unitRepository.findById(20L)).thenReturn(Optional.of(secondUnit));
        when(courseLessonRepository.existsDuplicateSequenceNumber(any(), any(), any())).thenReturn(false);
        when(courseLessonRepository.save(any(CourseLesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createCourseLesson(10L, sessionPlanRequest(1, "Unit A"));
        service.createCourseLesson(20L, sessionPlanRequest(1, "Unit B"));

        verify(courseLessonRepository).existsDuplicateSequenceNumber(10L, 1, null);
        verify(courseLessonRepository).existsDuplicateSequenceNumber(20L, 1, null);
    }

    @Test
    void deleteUnusedCourseLessonSucceedsAndSynchronizesTotal() {
        InstructorLedCourse program = program(1L);
        CourseLesson plan = plan(101L, unit(10L, program), 1, "Reading");
        when(courseLessonRepository.findById(101L)).thenReturn(Optional.of(plan));

        service.deleteCourseLesson(101L);

        verify(courseLessonRepository).delete(plan);
        verify(courseLessonRepository).flush();
    }

    @Test
    void deleteCourseLessonUsedByScheduleIsRejectedByRelationalIntegrity() {
        CourseLesson lesson = plan(101L, unit(10L, program(1L)), 5, "Multiple Choice");
        when(courseLessonRepository.findById(101L)).thenReturn(Optional.of(lesson));
        doThrow(new DataIntegrityViolationException("course lesson is referenced by class schedule"))
                .when(courseLessonRepository).flush();

        assertThatThrownBy(() -> service.deleteCourseLesson(101L))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(courseLessonRepository).delete(lesson);
        verify(courseLessonRepository).flush();
    }

    @Test
    void publishStructuredCurriculumWithContinuousSessionsSucceeds() {
        InstructorLedCourse program = publishableProgram(1, 2, 3);
        when(instructorLedCourseRepository.findById(1L)).thenReturn(Optional.of(program));
        when(accessHelper.requireUser("manager@englishlab.vn")).thenReturn(User.builder().id(99L).build());
        when(instructorLedCourseRepository.save(program)).thenReturn(program);

        service.publishInstructorLedCourse(1L, "manager@englishlab.vn");

        assertThat(program.getPublicationStatus()).isEqualTo(PackageStatus.PUBLISHED);
    }

    @Test
    void publishResponseDerivesTotalSessionsFromCanonicalCourseLessons() {
        InstructorLedCourse program = publishableProgram(1, 2, 3);
        when(instructorLedCourseRepository.findById(1L)).thenReturn(Optional.of(program));
        when(accessHelper.requireUser("manager@englishlab.vn")).thenReturn(User.builder().id(99L).build());
        when(instructorLedCourseRepository.save(program)).thenReturn(program);

        var response = service.publishInstructorLedCourse(1L, "manager@englishlab.vn");

        assertThat(response.getTotalSessions()).isEqualTo(3);
    }

    @Test
    void publishStructuredCurriculumRejectsMissingSessionNumber() {
        InstructorLedCourse program = publishableProgram(1, 2, 4);
        when(instructorLedCourseRepository.findById(1L)).thenReturn(Optional.of(program));

        assertThatThrownBy(() -> service.publishInstructorLedCourse(1L, "manager@englishlab.vn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thiếu buổi 3");
    }

    private InstructorLedCourse publishableProgram(int... sessionNumbers) {
        InstructorLedCourse program = program(1L);
        program.setTitle("General English Foundation");
        program.setCode("GE-A1");
        program.setPublicationStatus(PackageStatus.DRAFT);
        program.setExamType("GENERAL_ENGLISH");
        program.setFocusSkills("LISTENING,READING");
        program.setEntryLevel("A1");
        program.setLearningOutcomes("Hoàn thành nền tảng A1.");
        CourseUnit unit = unit(10L, program);
        program.getUnits().add(unit);
        for (int number : sessionNumbers) {
            unit.addLesson(plan((long) number, unit, number, "Buổi " + number));
        }
        return program;
    }

    private InstructorLedCourse program(Long id) {
        return InstructorLedCourse.builder()
                .id(id)
                .examType("GENERAL_ENGLISH")
                .publicationStatus(PackageStatus.DRAFT)
                .build();
    }

    private CourseUnit unit(Long id, InstructorLedCourse program) {
        return CourseUnit.builder()
                .id(id)
                .instructorLedCourse(program)
                .title("Reading Fundamentals")
                .sequenceNumber(1)
                .build();
    }

    private CourseLesson plan(Long id, CourseUnit unit, int number, String title) {
        return CourseLesson.builder()
                .id(id)
                .courseUnit(unit)
                .sequenceNumber(number)
                .title(title)
                .build();
    }

    private CourseLessonRequest sessionPlanRequest(int number, String title) {
        CourseLessonRequest request = new CourseLessonRequest();
        request.setSessionNumber(number);
        request.setDisplayOrder(number);
        request.setTitle(title);
        request.setDescription("Mô tả");
        request.setLearningObjectives("Mục tiêu");
        return request;
    }

    private InstructorLedCourseRequest validIeltsRequest() {
        InstructorLedCourseRequest request = new InstructorLedCourseRequest();
        request.setTitle("IELTS Academic 6.5");
        request.setCode("IELTS-65");
        request.setExamCategory("IELTS");
        request.setFocusSkills("LISTENING,READING,WRITING,SPEAKING");
        request.setTargetBand(BigDecimal.valueOf(6.5));
        request.setEntryLevel("5.0");
        request.setStatus("DRAFT");
        return request;
    }
}
