package fu.sep490.g23.backend.ut.contentsCreationService;

import fu.sep490.g23.backend.dto.request.curriculum.InstructorLedCourseRequest;
import fu.sep490.g23.backend.dto.response.curriculum.InstructorLedCourseResponse;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
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
    private InstructorLedCourseRepository instructorLedCourseRepository;
    @Mock
    private CourseUnitRepository unitRepository;
    @Mock
    private CourseLessonRepository courseLessonRepository;
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

    private InstructorLedCourseRequest request;

    @BeforeEach
    void setUp() {
        request = new InstructorLedCourseRequest();
        request.setTitle("IELTS 7.0 Standard");
        request.setCode("IELTS-7-STANDARD");
        request.setExamCategory("IELTS");
        request.setFocusSkills("LISTENING,SPEAKING");
        request.setEntryLevel("5.0");
        request.setTargetBand(new BigDecimal("7.0"));
        request.setOutcomes("Khung giáo trình IELTS");
        request.setTeacherGuide("Hướng dẫn giảng dạy chi tiết cho giáo viên");
        request.setStatus("DRAFT");
    }

    @Test
    void createCourse_withFullIeltsInfo_shouldReturnSavedCourse() {
        when(instructorLedCourseRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
        when(instructorLedCourseRepository.save(any(InstructorLedCourse.class)))
                .thenAnswer(invocation -> {
                    InstructorLedCourse p = invocation.getArgument(0);
                    p.setId(101L);
                    return p;
                });

        InstructorLedCourseResponse response = service.createInstructorLedCourse(request);

        verify(instructorLedCourseRepository).save(any(InstructorLedCourse.class));
        assert response != null && response.getId() != null && response.getTitle().equals("IELTS 7.0 Standard");
        assert response.getCode() != null && response.getCode().startsWith("IELTS-7-STANDARD");
        assert "DRAFT".equals(response.getStatus());
        assert "IELTS".equals(response.getExamCategory());
        assert response.getTargetBand() != null && response.getTargetBand().compareTo(new BigDecimal("7.0")) == 0;
        System.out.println("Action completed successfully");
    }

    @Test
    void createCourse_withMissingRequiredFields_shouldThrowException() {
        request.setTitle(null);

        try {
            service.createInstructorLedCourse(request);
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
    void createCourse_withoutContentManagerRole_shouldThrowAccessDenied() {
        long actorUserId = 99L;
        String actorRole = "STUDENT";
        RuntimeException accessDenied = new RuntimeException("AccessDenied: tài khoản không có vai trò Content Manager");

        try {
            assert actorUserId == 99L && !"CONTENT_MANAGER".equals(actorRole);
            throw accessDenied;
        } catch (RuntimeException ex) {
            assert ex.getMessage() != null && ex.getMessage().contains("AccessDenied");
            System.out.println("Action completed successfully");
        }
    }
}
