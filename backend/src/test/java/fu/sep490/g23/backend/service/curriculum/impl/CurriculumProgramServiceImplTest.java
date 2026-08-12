package fu.sep490.g23.backend.service.curriculum.impl;

import fu.sep490.g23.backend.dto.request.curriculum.CurriculumProgramRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumAssessmentRefRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumExerciseRefRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumFlashcardRefRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumMaterialRefRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumProgramRepository;
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
    @Mock private CurriculumMaterialRefRepository materialRefRepository;
    @Mock private CurriculumExerciseRefRepository exerciseRefRepository;
    @Mock private CurriculumAssessmentRefRepository assessmentRefRepository;
    @Mock private CurriculumFlashcardRefRepository flashcardRefRepository;
    @Mock private CenterMaterialLibraryItemRepository materialRepository;
    @Mock private ExerciseBankItemRepository exerciseRepository;
    @Mock private AssessmentRubricRepository assessmentRubricRepository;
    @Mock private AssessmentBankItemRepository assessmentBankRepository;
    @Mock private FlashcardSetRepository flashcardSetRepository;
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
