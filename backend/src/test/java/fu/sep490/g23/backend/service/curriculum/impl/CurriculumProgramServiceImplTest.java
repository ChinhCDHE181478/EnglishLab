package fu.sep490.g23.backend.service.curriculum.impl;

import fu.sep490.g23.backend.dto.request.curriculum.CurriculumProgramRequest;
import fu.sep490.g23.backend.dto.request.curriculum.CurriculumSessionPlanRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumSessionPlan;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumAssessmentRefRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumExerciseRefRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumFlashcardRefRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumMaterialRefRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumSessionPlanRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumUnitRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurriculumProgramServiceImplTest {

    @Mock private CurriculumProgramRepository programRepository;
    @Mock private CurriculumUnitRepository unitRepository;
    @Mock private CurriculumSessionPlanRepository sessionPlanRepository;
    @Mock private ClassroomSessionRepository classroomSessionRepository;
    @Mock private CurriculumMaterialRefRepository materialRefRepository;
    @Mock private CurriculumExerciseRefRepository exerciseRefRepository;
    @Mock private CurriculumAssessmentRefRepository assessmentRefRepository;
    @Mock private CurriculumFlashcardRefRepository flashcardRefRepository;
    @Mock private CenterMaterialLibraryItemRepository materialRepository;
    @Mock private ExerciseBankItemRepository exerciseRepository;
    @Mock private AssessmentRubricRepository assessmentRubricRepository;
    @Mock private AssessmentBankItemRepository assessmentBankRepository;
    @Mock private FlashcardSetRepository flashcardSetRepository;
    @Mock private fu.sep490.g23.backend.service.curriculum.ContentBankLinkSync contentBankLinkSync;
    @Mock private fu.sep490.g23.backend.service.curriculum.ContentBankIdResolver contentBankIdResolver;
    @Mock private ClassroomAccessHelper accessHelper;

    @InjectMocks
    private CurriculumProgramServiceImpl service;

    @Test
    void createProgramPersistsCanonicalIeltsProfile() {
        CurriculumProgramRequest request = validIeltsRequest();
        request.setFocusSkills("SPEAKING,LISTENING,READING,WRITING,LISTENING");
        when(programRepository.existsByCodeIgnoreCase("IELTS-65")).thenReturn(false);
        when(programRepository.findBySlug("ielts-academic-65")).thenReturn(Optional.empty());
        when(programRepository.save(any(CurriculumProgram.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createProgram(request);

        ArgumentCaptor<CurriculumProgram> captor = ArgumentCaptor.forClass(CurriculumProgram.class);
        verify(programRepository).save(captor.capture());
        CurriculumProgram saved = captor.getValue();
        assertThat(saved.getExamCategory()).isEqualTo("IELTS");
        assertThat(saved.getProgramTrack()).isEqualTo("IELTS_ACADEMIC");
        assertThat(saved.getFocusSkills()).isEqualTo("LISTENING,READING,WRITING,SPEAKING");
        assertThat(saved.getTargetBand()).isEqualByComparingTo("6.5");
        assertThat(saved.getTargetScore()).isNull();
    }

    @Test
    void createProgramGeneratesUniqueCodeWhenCodeIsMissing() {
        CurriculumProgramRequest request = validIeltsRequest();
        request.setCode(null);
        when(programRepository.existsByCodeIgnoreCase("OFFLINE-IELTS-ACADEMIC-6-5")).thenReturn(true);
        when(programRepository.existsByCodeIgnoreCase("OFFLINE-IELTS-ACADEMIC-6-5-2")).thenReturn(false);
        when(programRepository.findBySlug("ielts-academic-65")).thenReturn(Optional.empty());
        when(programRepository.save(any(CurriculumProgram.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createProgram(request);

        ArgumentCaptor<CurriculumProgram> captor = ArgumentCaptor.forClass(CurriculumProgram.class);
        verify(programRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("OFFLINE-IELTS-ACADEMIC-6-5-2");
    }

    @Test
    void updateProgramKeepsExistingCodeWhenCodeIsMissing() {
        CurriculumProgramRequest request = validIeltsRequest();
        request.setCode(null);
        CurriculumProgram existing = CurriculumProgram.builder()
                .id(9L)
                .title("IELTS cũ")
                .code("OFFLINE-IELTS-CU")
                .slug("ielts-cu")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status("DRAFT")
                .build();
        when(programRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(programRepository.findBySlug("ielts-academic-65")).thenReturn(Optional.empty());
        when(programRepository.save(any(CurriculumProgram.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateProgram(9L, request);

        assertThat(existing.getCode()).isEqualTo("OFFLINE-IELTS-CU");
    }

    @Test
    void createProgramRejectsToeicUsingIeltsBand() {
        CurriculumProgramRequest request = validIeltsRequest();
        request.setExamCategory("TOEIC");
        request.setProgramTrack("TOEIC_LISTENING_READING");
        request.setEntryLevel("450");
        request.setTargetScore(650);

        assertThatThrownBy(() -> service.createProgram(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không sử dụng band IELTS");
        verify(programRepository, never()).save(any());
    }

    @Test
    void createProgramRejectsEntryBandHigherThanTargetBand() {
        CurriculumProgramRequest request = validIeltsRequest();
        request.setEntryLevel("7.0");

        assertThatThrownBy(() -> service.createProgram(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đầu vào không thể cao hơn band mục tiêu");
        verify(programRepository, never()).save(any());
    }

    @Test
    void createProgramRejectsInvalidGeneralEnglishCefrLevel() {
        CurriculumProgramRequest request = validIeltsRequest();
        request.setExamCategory("GENERAL_ENGLISH");
        request.setProgramTrack("GENERAL_ENGLISH_FOUNDATION");
        request.setEntryLevel("Sơ cấp");
        request.setTargetBand(null);

        assertThatThrownBy(() -> service.createProgram(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CEFR");
        verify(programRepository, never()).save(any());
    }

    @Test
    void createProgramRejectsCategoryOutsideEnglishTraining() {
        CurriculumProgramRequest request = validIeltsRequest();
        request.setExamCategory("PROGRAMMING");

        assertThatThrownBy(() -> service.createProgram(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IELTS, TOEIC hoặc General English");
        verify(programRepository, never()).save(any());
    }

    @Test
    void createSessionPlanPersistsAndSynchronizesTotalSessions() {
        CurriculumUnit unit = unit(10L, program(1L));
        CurriculumSessionPlanRequest request = sessionPlanRequest(1, "Reading Overview");
        when(unitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(sessionPlanRepository.existsDuplicateSessionNumber(1L, 1, null)).thenReturn(false);
        when(sessionPlanRepository.save(any(CurriculumSessionPlan.class))).thenAnswer(invocation -> {
            CurriculumSessionPlan saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(sessionPlanRepository.countByProgramId(1L)).thenReturn(1L);

        var response = service.createSessionPlan(10L, request);

        assertThat(response.getSessionNumber()).isEqualTo(1);
        assertThat(response.getTitle()).isEqualTo("Reading Overview");
        assertThat(unit.getProgram().getTotalSessions()).isEqualTo(1);
        verify(programRepository).save(unit.getProgram());
    }

    @Test
    void updateSessionPlanPersistsChanges() {
        CurriculumSessionPlan plan = plan(101L, unit(10L, program(1L)), 1, "Cũ");
        when(sessionPlanRepository.findById(101L)).thenReturn(Optional.of(plan));
        when(sessionPlanRepository.existsDuplicateSessionNumber(1L, 2, 101L)).thenReturn(false);
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
        when(sessionPlanRepository.existsDuplicateSessionNumber(1L, 1, null)).thenReturn(true);

        assertThatThrownBy(() -> service.createSessionPlan(10L, sessionPlanRequest(1, "Trùng")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Buổi 1 đã tồn tại");
    }

    @Test
    void differentProgramsMayBothUseSessionNumberOne() {
        CurriculumUnit firstUnit = unit(10L, program(1L));
        CurriculumUnit secondUnit = unit(20L, program(2L));
        when(unitRepository.findById(10L)).thenReturn(Optional.of(firstUnit));
        when(unitRepository.findById(20L)).thenReturn(Optional.of(secondUnit));
        when(sessionPlanRepository.existsDuplicateSessionNumber(any(), any(), any())).thenReturn(false);
        when(sessionPlanRepository.save(any(CurriculumSessionPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createSessionPlan(10L, sessionPlanRequest(1, "Program A"));
        service.createSessionPlan(20L, sessionPlanRequest(1, "Program B"));

        verify(sessionPlanRepository).existsDuplicateSessionNumber(1L, 1, null);
        verify(sessionPlanRepository).existsDuplicateSessionNumber(2L, 1, null);
    }

    @Test
    void deleteUnusedSessionPlanSucceedsAndSynchronizesTotal() {
        CurriculumProgram program = program(1L);
        CurriculumSessionPlan plan = plan(101L, unit(10L, program), 1, "Reading");
        when(sessionPlanRepository.findById(101L)).thenReturn(Optional.of(plan));
        when(classroomSessionRepository.existsByCurriculumSessionPlanId(101L)).thenReturn(false);
        when(sessionPlanRepository.countByProgramId(1L)).thenReturn(0L);

        service.deleteSessionPlan(101L);

        verify(sessionPlanRepository).delete(plan);
        assertThat(program.getTotalSessions()).isZero();
    }

    @Test
    void deleteUsedSessionPlanIsRejected() {
        CurriculumSessionPlan plan = plan(101L, unit(10L, program(1L)), 5, "Multiple Choice");
        when(sessionPlanRepository.findById(101L)).thenReturn(Optional.of(plan));
        when(classroomSessionRepository.existsByCurriculumSessionPlanId(101L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteSessionPlan(101L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Buổi 5 đã được sử dụng trong lớp học và không thể xóa.");
        verify(sessionPlanRepository, never()).delete(any());
    }

    @Test
    void publishStructuredCurriculumWithContinuousSessionsSucceeds() {
        CurriculumProgram program = publishableProgram(1, 2, 3);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(accessHelper.requireUser("manager@englishlab.vn")).thenReturn(User.builder().id(99L).build());
        when(programRepository.save(program)).thenReturn(program);

        service.publishProgram(1L, "manager@englishlab.vn");

        assertThat(program.getStatus()).isEqualTo("PUBLISHED");
        assertThat(program.getTotalSessions()).isEqualTo(3);
    }

    @Test
    void publishStructuredCurriculumRejectsMissingSessionNumber() {
        CurriculumProgram program = publishableProgram(1, 2, 4);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        assertThatThrownBy(() -> service.publishProgram(1L, "manager@englishlab.vn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thiếu buổi 3");
    }

    @Test
    void publishStructuredCurriculumRejectsMismatchedTotalSessions() {
        CurriculumProgram program = publishableProgram(1, 2, 3);
        program.setTotalSessions(4);
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));

        assertThatThrownBy(() -> service.publishProgram(1L, "manager@englishlab.vn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phải bằng 3");
    }

    private CurriculumProgram publishableProgram(int... sessionNumbers) {
        CurriculumProgram program = program(1L);
        program.setTitle("General English Foundation");
        program.setCode("GE-A1");
        program.setSlug("general-english-a1");
        program.setStatus("DRAFT");
        program.setExamCategory("GENERAL_ENGLISH");
        program.setProgramTrack("GENERAL_ENGLISH_FOUNDATION");
        program.setFocusSkills("LISTENING,READING");
        program.setEntryLevel("A1");
        program.setOutcomes("Hoàn thành nền tảng A1.");
        CurriculumUnit unit = unit(10L, program);
        program.getUnits().add(unit);
        for (int number : sessionNumbers) {
            unit.addSessionPlan(plan((long) number, unit, number, "Buổi " + number));
        }
        program.setTotalSessions(sessionNumbers.length);
        return program;
    }

    private CurriculumProgram program(Long id) {
        return CurriculumProgram.builder()
                .id(id)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status("DRAFT")
                .totalSessions(0)
                .build();
    }

    private CurriculumUnit unit(Long id, CurriculumProgram program) {
        return CurriculumUnit.builder()
                .id(id)
                .program(program)
                .title("Reading Fundamentals")
                .displayOrder(1)
                .build();
    }

    private CurriculumSessionPlan plan(Long id, CurriculumUnit unit, int number, String title) {
        return CurriculumSessionPlan.builder()
                .id(id)
                .unit(unit)
                .sessionNumber(number)
                .displayOrder(number)
                .title(title)
                .build();
    }

    private CurriculumSessionPlanRequest sessionPlanRequest(int number, String title) {
        CurriculumSessionPlanRequest request = new CurriculumSessionPlanRequest();
        request.setSessionNumber(number);
        request.setDisplayOrder(number);
        request.setTitle(title);
        request.setDescription("Mô tả");
        request.setLearningObjectives("Mục tiêu");
        return request;
    }

    private CurriculumProgramRequest validIeltsRequest() {
        CurriculumProgramRequest request = new CurriculumProgramRequest();
        request.setTitle("IELTS Academic 6.5");
        request.setCode("IELTS-65");
        request.setSlug("ielts-academic-65");
        request.setDeliveryMode(ClassroomDeliveryMode.OFFLINE);
        request.setExamCategory("IELTS");
        request.setProgramTrack("IELTS_ACADEMIC");
        request.setFocusSkills("LISTENING,READING,WRITING,SPEAKING");
        request.setTargetBand(BigDecimal.valueOf(6.5));
        request.setEntryLevel("5.0");
        request.setTotalSessions(24);
        request.setStatus("DRAFT");
        return request;
    }
}
