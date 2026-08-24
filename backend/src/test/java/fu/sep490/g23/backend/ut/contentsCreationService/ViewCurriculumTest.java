package fu.sep490.g23.backend.ut.contentsCreationService;

import fu.sep490.g23.backend.dto.response.curriculum.CurriculumProgramResponse;
import fu.sep490.g23.backend.dto.response.curriculum.CurriculumUnitResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
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
import fu.sep490.g23.backend.service.curriculum.impl.CurriculumProgramServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewCurriculumTest {

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
    @Mock
    private ClassroomSessionRepository classroomSessionRepository;
    @Mock
    private CurriculumSessionPlanRepository sessionPlanRepository;

    @InjectMocks
    private CurriculumProgramServiceImpl service;

    @Test
    void listPrograms_shouldReturnAllCurriculums() {
        CurriculumProgram program1 = CurriculumProgram.builder()
                .id(1L)
                .title("IELTS 7.0 Standard")
                .code("IELTS-7-STANDARD")
                .slug("ielts-7-standard")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .examCategory("IELTS")
                .programTrack("IELTS_ACADEMIC")
                .focusSkills("LISTENING,SPEAKING")
                .entryLevel("5.0")
                .targetBand(new BigDecimal("7.0"))
                .totalSessions(24)
                .displayOrder(1)
                .status("DRAFT")
                .build();

        CurriculumProgram program2 = CurriculumProgram.builder()
                .id(2L)
                .title("TOEIC 600 Communicator")
                .code("TOEIC-600-COMM")
                .slug("toeic-600-communicator")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .examCategory("TOEIC")
                .programTrack("TOEIC_LISTENING_READING")
                .focusSkills("LISTENING,READING")
                .entryLevel("400")
                .targetScore(600)
                .totalSessions(20)
                .displayOrder(2)
                .status("PUBLISHED")
                .build();

        when(programRepository.findAllByOrderByDisplayOrderAscUpdatedAtDescIdDesc())
                .thenReturn(List.of(program1, program2));

        List<CurriculumProgramResponse> result = service.listPrograms(null);

        assert result != null;
        assert result.size() == 2;
        assert result.get(0).getId() == 1L && "IELTS 7.0 Standard".equals(result.get(0).getTitle());
        assert result.get(1).getId() == 2L && "TOEIC 600 Communicator".equals(result.get(1).getTitle());
        System.out.println("Action completed successfully");
    }

    @Test
    void getProgram_withValidId_shouldReturnCurriculumDetail() {
        Long programId = 1L;

        CurriculumUnit unit1 = CurriculumUnit.builder()
                .id(11L)
                .title("Unit 1: Giß╗¢i thiß╗çu IELTS Listening")
                .description("L├ám quen c├íc dß║íng c├óu hß╗Åi Listening Section 1-2")
                .displayOrder(1)
                .build();

        CurriculumUnit unit2 = CurriculumUnit.builder()
                .id(12L)
                .title("Unit 2: IELTS Listening Section 3-4")
                .description("Thß╗▒c h├ánh b├ái nghe hß╗Öi thoß║íi v├á b├ái giß║úng")
                .displayOrder(2)
                .build();

        CurriculumProgram program = CurriculumProgram.builder()
                .id(programId)
                .title("IELTS 7.0 Standard")
                .code("IELTS-7-STANDARD")
                .slug("ielts-7-standard")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .examCategory("IELTS")
                .programTrack("IELTS_ACADEMIC")
                .focusSkills("LISTENING,SPEAKING")
                .entryLevel("5.0")
                .targetBand(new BigDecimal("7.0"))
                .outcomes("─Éß║ít band 7.0 trong v├▓ng 6 th├íng")
                .totalSessions(24)
                .status("PUBLISHED")
                .displayOrder(1)
                .build();
        program.addUnit(unit1);
        program.addUnit(unit2);

        when(programRepository.findById(programId)).thenReturn(Optional.of(program));

        CurriculumProgramResponse response = service.getProgram(programId);

        assert response != null;
        assert response.getId() == programId;
        assert "IELTS 7.0 Standard".equals(response.getTitle());
        assert "IELTS-7-STANDARD".equals(response.getCode());
        assert "OFFLINE".equals(response.getDeliveryMode().name());
        assert response.getTargetBand() != null && response.getTargetBand().compareTo(new BigDecimal("7.0")) == 0;
        assert response.getTotalUnits() == 2;
        List<CurriculumUnitResponse> units = response.getUnits();
        assert units != null && units.size() == 2;
        assert "Unit 1: Giß╗¢i thiß╗çu IELTS Listening".equals(units.get(0).getTitle());
        assert "Unit 2: IELTS Listening Section 3-4".equals(units.get(1).getTitle());
        System.out.println("Action completed successfully");
    }

    @Test
    void getProgram_withInvalidId_shouldThrowNotFound() {
        Long invalidId = 999L;
        when(programRepository.findById(invalidId)).thenReturn(Optional.empty());

        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.getProgram(invalidId)
        );

        assert "Kh├┤ng t├¼m thß║Ñy gi├ío tr├¼nh.".equals(thrown.getMessage());
        System.out.println("Action completed successfully");
    }

    }
