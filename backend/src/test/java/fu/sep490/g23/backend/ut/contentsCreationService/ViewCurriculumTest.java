package fu.sep490.g23.backend.ut.contentsCreationService;

import fu.sep490.g23.backend.dto.response.curriculum.CourseUnitResponse;
import fu.sep490.g23.backend.dto.response.curriculum.InstructorLedCourseResponse;
import fu.sep490.g23.backend.entity.course.CourseUnit;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.classroom.CenterMaterialLibraryItemRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitContentRefRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.curriculum.impl.InstructorLedCourseManagementServiceImpl;
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
    private InstructorLedCourseRepository programRepository;
    @Mock
    private CourseUnitRepository unitRepository;
    @Mock
    private CourseLessonRepository sessionPlanRepository;
    @Mock
    private CourseUnitContentRefRepository contentRefRepository;
    @Mock
    private ContentBankItemRepository contentBankItemRepository;
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
    private InstructorLedCourseManagementServiceImpl service;

    @Test
    void listPrograms_shouldReturnAllCurriculums() {
        InstructorLedCourse program1 = InstructorLedCourse.builder()
                .id(1L)
                .title("IELTS 7.0 Standard")
                .code("IELTS-7-STANDARD")
                .examType("IELTS")
                .focusSkills("LISTENING,SPEAKING")
                .entryLevel("5.0")
                .targetBand(new BigDecimal("7.0"))
                .publicationStatus(PackageStatus.DRAFT)
                .build();

        InstructorLedCourse program2 = InstructorLedCourse.builder()
                .id(2L)
                .title("TOEIC 600 Communicator")
                .code("TOEIC-600-COMM")
                .examType("TOEIC")
                .focusSkills("LISTENING,READING")
                .entryLevel("400")
                .targetScore(600)
                .publicationStatus(PackageStatus.PUBLISHED)
                .build();

        when(programRepository.findAllByOrderByUpdatedAtDescIdDesc())
                .thenReturn(List.of(program1, program2));

        List<InstructorLedCourseResponse> result = service.listPrograms();

        assert result != null;
        assert result.size() == 2;
        assert result.get(0).getId() == 1L && "IELTS 7.0 Standard".equals(result.get(0).getTitle());
        assert "IELTS-7-STANDARD".equals(result.get(0).getCode());
        assert "DRAFT".equals(result.get(0).getStatus());
        assert result.get(1).getId() == 2L && "TOEIC 600 Communicator".equals(result.get(1).getTitle());
        assert "TOEIC-600-COMM".equals(result.get(1).getCode());
        assert "PUBLISHED".equals(result.get(1).getStatus());
        System.out.println("Action completed successfully");
    }

    @Test
    void getProgram_withValidId_shouldReturnCurriculumDetail() {
        Long programId = 1L;

        CourseUnit unit1 = CourseUnit.builder()
                .id(11L)
                .title("Unit 1: Giới thiệu IELTS Listening")
                .description("Làm quen các dạng câu hỏi Listening Section 1-2")
                .sequenceNumber(1)
                .build();

        CourseUnit unit2 = CourseUnit.builder()
                .id(12L)
                .title("Unit 2: IELTS Listening Section 3-4")
                .description("Thực hành bài nghe hội thoại và bài giảng")
                .sequenceNumber(2)
                .build();

        InstructorLedCourse program = InstructorLedCourse.builder()
                .id(programId)
                .title("IELTS 7.0 Standard")
                .code("IELTS-7-STANDARD")
                .examType("IELTS")
                .focusSkills("LISTENING,SPEAKING")
                .entryLevel("5.0")
                .targetBand(new BigDecimal("7.0"))
                .learningOutcomes("Đạt band 7.0 trong vòng 6 tháng")
                .publicationStatus(PackageStatus.PUBLISHED)
                .build();
        program.addUnit(unit1);
        program.addUnit(unit2);

        when(programRepository.findById(programId)).thenReturn(Optional.of(program));

        InstructorLedCourseResponse response = service.getProgram(programId);

        assert response != null;
        assert response.getId() == programId;
        assert "IELTS 7.0 Standard".equals(response.getTitle());
        assert "IELTS-7-STANDARD".equals(response.getCode());
        assert response.getTargetBand() != null && response.getTargetBand().compareTo(new BigDecimal("7.0")) == 0;
        assert "PUBLISHED".equals(response.getStatus());
        assert response.getTotalUnits() == 2;
        List<CourseUnitResponse> units = response.getUnits();
        assert units != null && units.size() == 2;
        assert "Unit 1: Giới thiệu IELTS Listening".equals(units.get(0).getTitle());
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

        assert thrown.getMessage() != null;
        System.out.println("Action completed successfully");
    }
}
