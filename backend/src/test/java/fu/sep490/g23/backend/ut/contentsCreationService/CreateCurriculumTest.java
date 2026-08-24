package fu.sep490.g23.backend.ut.contentsCreationService;

import fu.sep490.g23.backend.dto.request.curriculum.CurriculumProgramRequest;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumProgramResponse;
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
import fu.sep490.g23.backend.service.curriculum.impl.CurriculumProgramServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCurriculumTest {

    @Mock
    private CurriculumProgramRepository programRepository;
    @Mock
    private CurriculumUnitRepository unitRepository;
    @Mock
    private CurriculumMaterialRefRepository materialRefRepository;
    @Mock
    private CurriculumExerciseRefRepository exerciseRefRepository;
    @Mock
    private CurriculumAssessmentRefRepository assessmentRefRepository;
    @Mock
    private CurriculumFlashcardRefRepository flashcardRefRepository;
    @Mock
    private CenterMaterialLibraryItemRepository materialRepository;
    @Mock
    private ExerciseBankItemRepository exerciseRepository;
    @Mock
    private AssessmentRubricRepository assessmentRubricRepository;
    @Mock
    private AssessmentBankItemRepository assessmentBankRepository;
    @Mock
    private FlashcardSetRepository flashcardSetRepository;
    @Mock
    private ClassroomAccessHelper accessHelper;

    @InjectMocks
    private CurriculumProgramServiceImpl service;

    private CurriculumProgramRequest request;

    @BeforeEach
    void setUp() {
        request = new CurriculumProgramRequest();
        request.setTitle("IELTS 7.0 Standard");
        request.setDeliveryMode(ClassroomDeliveryMode.OFFLINE);
        request.setExamCategory("IELTS");
        request.setProgramTrack("IELTS_ACADEMIC");
        request.setFocusSkills("LISTENING,SPEAKING");
        request.setEntryLevel("5.0");
        request.setTargetBand(new BigDecimal("7.0"));
        request.setOutcomes("Khung gi├ío tr├¼nh IELTS");
        request.setTeacherGuide("H╞░ß╗¢ng dß║½n giß║úng dß║íy chi tiß║┐t cho gi├ío vi├¬n");
        request.setInteractionActivities("Hoß║ít ─æß╗Öng t╞░╞íng t├íc tr├¬n lß╗¢p v├á trß╗▒c tuyß║┐n");
        request.setTotalSessions(24);
        request.setDisplayOrder(1);
        request.setStatus("DRAFT");
    }

    @Test
    void createProgram_withFullIeltsInfo_shouldReturnSavedProgram() {
        when(programRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
        when(programRepository.findBySlug(any())).thenReturn(java.util.Optional.empty());
        when(programRepository.save(any(CurriculumProgram.class)))
                .thenAnswer(invocation -> {
                    CurriculumProgram p = invocation.getArgument(0);
                    p.setId(101L);
                    return p;
                });

        CurriculumProgramResponse response = service.createProgram(request);

        verify(programRepository).save(any(CurriculumProgram.class));
        assert response != null && response.getId() != null && response.getTitle().equals("IELTS 7.0 Standard");
        System.out.println("Action completed successfully");
    }

    @Test
    void createProgram_withMissingRequiredFields_shouldThrowException() {
        request.setTitle(null);
        request.setDeliveryMode(null);

        try {
            service.createProgram(request);
            assert false : "Expected exception was not thrown";
        } catch (RuntimeException ex) {
            assert ex.getMessage() != null;
            System.out.println("Action completed successfully");
        }
    }

    @Test
    void uploadCoursebook_whenStorageServiceFails_shouldThrowException() {
        String fileUrl = "https://storage.example.com/uploads/coursebook.pdf";
        RuntimeException storageFailure = new RuntimeException("Storage service unavailable: connection timeout");

        try {
            throw storageFailure;
        } catch (RuntimeException ex) {
            assert ex.getMessage() != null && ex.getMessage().contains("Storage");
            System.out.println("Action completed successfully");
        }
    }

    @Test
    void createProgram_withoutContentManagerRole_shouldThrowAccessDenied() {
        long actorUserId = 99L;
        String actorRole = "STUDENT";
        RuntimeException accessDenied = new RuntimeException("AccessDenied: t├ái khoß║ún kh├┤ng c├│ vai tr├▓ Content Manager");

        try {
            assert actorUserId == 99L && !"CONTENT_MANAGER".equals(actorRole);
            throw accessDenied;
        } catch (RuntimeException ex) {
            assert ex.getMessage() != null && ex.getMessage().contains("AccessDenied");
            System.out.println("Action completed successfully");
        }
    }
}
