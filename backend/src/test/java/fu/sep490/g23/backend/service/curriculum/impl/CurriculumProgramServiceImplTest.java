package fu.sep490.g23.backend.service.curriculum.impl;

import fu.sep490.g23.backend.dto.request.curriculum.InstructorLedCourseRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CourseLessonRequest;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.CourseLesson;
import fu.sep490.g23.backend.entity.course.CourseUnit;
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

    @Mock private InstructorLedCourseRepository programRepository;
    @Mock private CourseUnitRepository unitRepository;
    @Mock private CourseLessonRepository sessionPlanRepository;
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
    void createProgramPersistsCanonicalIeltsProfile() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setFocusSkills("SPEAKING,LISTENING,READING,WRITING,LISTENING");
        when(programRepository.existsByCodeIgnoreCase("IELTS-65")).thenReturn(false);
        when(programRepository.save(any(InstructorLedCourse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createProgram(request);

        ArgumentCaptor<InstructorLedCourse> captor = ArgumentCaptor.forClass(InstructorLedCourse.class);
        verify(programRepository).save(captor.capture());
        InstructorLedCourse saved = captor.getValue();
        assertThat(saved.getExamType()).isEqualTo("IELTS");
        assertThat(saved.getFocusSkills()).isEqualTo("LISTENING,READING,WRITING,SPEAKING");
        assertThat(saved.getTargetBand()).isEqualByComparingTo("6.5");
        assertThat(saved.getTargetScore()).isNull();
    }

    @Test
    void createProgramGeneratesUniqueCodeWhenCodeIsMissing() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setCode(null);
        when(programRepository.existsByCodeIgnoreCase("ILC-IELTS-ACADEMIC-6-5")).thenReturn(true);
        when(programRepository.existsByCodeIgnoreCase("ILC-IELTS-ACADEMIC-6-5-2")).thenReturn(false);
        when(programRepository.save(any(InstructorLedCourse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createProgram(request);

        ArgumentCaptor<InstructorLedCourse> captor = ArgumentCaptor.forClass(InstructorLedCourse.class);
        verify(programRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("ILC-IELTS-ACADEMIC-6-5-2");
    }

    @Test
    void updateProgramKeepsExistingCodeWhenCodeIsMissing() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setCode(null);
        InstructorLedCourse existing = InstructorLedCourse.builder()
                .id(9L)
                .title("IELTS cũ")
                .code("OFFLINE-IELTS-CU")
                .examType("IELTS")
                .publicationStatus(PackageStatus.DRAFT)
                .build();
        when(programRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(programRepository.save(any(InstructorLedCourse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProgram(9L, request);

        assertThat(existing.getCode()).isEqualTo("OFFLINE-IELTS-CU");
    }

    @Test
    void createProgramRejectsToeicUsingIeltsBand() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setExamCategory("TOEIC");
        request.setEntryLevel("450");
        request.setTargetScore(650);

        assertThatThrownBy(() -> service.createProgram(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không sử dụng band IELTS");
        verify(programRepository, never()).save(any());
    }

    @Test
    void createProgramRejectsEntryBandHigherThanTargetBand() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setEntryLevel("7.0");

        assertThatThrownBy(() -> service.createProgram(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đầu vào không thể cao hơn band mục tiêu");
        verify(programRepository, never()).save(any());
    }

    @Test
    void createProgramRejectsInvalidGeneralEnglishCefrLevel() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setExamCategory("GENERAL_ENGLISH");
        request.setEntryLevel("Sơ cấp");
        request.setTargetBand(null);

        assertThatThrownBy(() -> service.createProgram(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CEFR");
        verify(programRepository, never()).save(any());
    }

    @Test
    void createProgramRejectsCategoryOutsideEnglishTraining() {
        InstructorLedCourseRequest request = validIeltsRequest();
        request.setExamCategory("PROGRAMMING");

        assertThatThrownBy(() -> service.createProgram(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IELTS, TOEIC hoặc General English");
        verify(programRepository, never()).save(any());
    }

    @Test
    void createSessionPlanPersistsAndSynchronizesTotalSessions() {
        CourseUnit unit = unit(10L, program(1L));
        CourseLessonRequest request = sessionPlanRequest(1, "Reading Overview");
        when(unitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(sessionPlanRepository.existsDuplicateSequenceNumber(1L, 1, null)).thenReturn(false);
        when(sessionPlanRepository.save(any(CourseLesson.class))).thenAnswer(invocation -> {
            CourseLesson saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        var response = service.createSessionPlan(10L, request);

        assertThat(response.getSessionNumber()).isEqualTo(1);
        assertThat(response.getTitle()).isEqualTo("Reading Overview");
        assertThat(response.getSessionNumber()).isEqualTo(unit.getLessons().isEmpty() ? 1 : unit.getLessons().getFirst().getSequenceNumber());
    }

    @Test
    void updateSessionPlanPersistsChanges() {
        CourseLesson plan = plan(101L, unit(10L, program(1L)), 1, "Cũ");
        when(sessionPlanRepository.findById(101L)).thenReturn(Optional.of(plan));
        when(sessionPlanRepository.existsDuplicateSequenceNumber(1L, 2, 101L)).thenReturn(false);
        when(sessionPlanRepository.save(plan)).thenReturn(plan);

        var response = service.updateSessionPlan(101L, sessionPlanRequest(2, "Scanning + Keywords"));

        assertThat(response.getSessionNumber()).isEqualTo(2);
        assertThat(response.getTitle()).isEqualTo("Scanning + Keywords");
    }

    @Test
    void createSessionPlanRejectsNumberBelowOne() {
        when(unitRepository.findById(10L)).thenReturn(Optional.of(unit(10L, program(1L))));

        assertThatThrownBy(() -> service.createSessionPlan(10L, sessionPlanRequest(0, "Sai")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bắt đầu từ 1");
        verify(sessionPlanRepository, never()).save(any());
    }

    @Test
    void createSessionPlanRejectsDuplicateNumberInsideProgram() {
        when(unitRepository.findById(10L)).thenReturn(Optional.of(unit(10L, program(1L))));
        when(sessionPlanRepository.existsDuplicateSequenceNumber(1L, 1, null)).thenReturn(true);

        assertThatThrownBy(() -> service.createSessionPlan(10L, sessionPlanRequest(1, "Trùng")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bài học số 1 đã tồn tại");
    }

    @Test
    void differentProgramsMayBothUseSessionNumberOne() {
        CourseUnit firstUnit = unit(10L, program(1L));
        CourseUnit secondUnit = unit(20L, program(2L));
        when(unitRepository.findById(10L)).thenReturn(Optional.of(firstUnit));
        when(unitRepository.findById(20L)).thenReturn(Optional.of(secondUnit));
        when(sessionPlanRepository.existsDuplicateSequenceNumber(any(), any(), any())).thenReturn(false);
        when(sessionPlanRepository.save(any(CourseLesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createSessionPlan(10L, sessionPlanRequest(1, "Program A"));
        service.createSessionPlan(20L, sessionPlanRequest(1, "Program B"));

        verify(sessionPlanRepository).existsDuplicateSequenceNumber(1L, 1, null);
        verify(sessionPlanRepository).existsDuplicateSequenceNumber(2L, 1, null);
    }

    @Test
    void deleteUnusedSessionPlanSucceedsAndSynchronizesTotal() {
        InstructorLedCourse program = program(1L);
        CourseLesson plan = plan(101L, unit(10L, program), 1, "Reading");
        when(sessionPlanRepository.findById(101L)).thenReturn(Optional.of(plan));

        service.deleteSessionPlan(101L);

        verify(sessionPlanRepository).delete(plan);
        verify(sessionPlanRepository).flush();
    }

    @Test
    void deleteCourseLessonUsedByScheduleIsRejectedByRelationalIntegrity() {
        CourseLesson lesson = plan(101L, unit(10L, program(1L)), 5, "Multiple Choice");
        when(sessionPlanRepository.findById(101L)).thenReturn(Optional.of(lesson));
        doThrow(new DataIntegrityViolationException("course lesson is referenced by class schedule"))
                .when(sessionPlanRepository).flush();

        assertThatThrownBy(() -> service.deleteSessionPlan(101L))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(sessionPlanRepository).delete(lesson);
        verify(sessionPlanRepository).flush();
    }

    @Test
    void publishStructuredCurriculumWithContinuousSessionsSucceeds() {
        InstructorLedCourse program = publishableProgram(1, 2, 3);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(accessHelper.requireUser("manager@englishlab.vn")).thenReturn(User.builder().id(99L).build());
        when(programRepository.save(program)).thenReturn(program);

        service.publishProgram(1L, "manager@englishlab.vn");

        assertThat(program.getPublicationStatus()).isEqualTo(PackageStatus.PUBLISHED);
    }

    @Test
    void publishResponseDerivesTotalSessionsFromCanonicalCourseLessons() {
        InstructorLedCourse program = publishableProgram(1, 2, 3);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(accessHelper.requireUser("manager@englishlab.vn")).thenReturn(User.builder().id(99L).build());
        when(programRepository.save(program)).thenReturn(program);

        var response = service.publishProgram(1L, "manager@englishlab.vn");

        assertThat(response.getTotalSessions()).isEqualTo(3);
    }

    @Test
    void publishStructuredCurriculumRejectsMissingSessionNumber() {
        InstructorLedCourse program = publishableProgram(1, 2, 4);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        assertThatThrownBy(() -> service.publishProgram(1L, "manager@englishlab.vn"))
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
