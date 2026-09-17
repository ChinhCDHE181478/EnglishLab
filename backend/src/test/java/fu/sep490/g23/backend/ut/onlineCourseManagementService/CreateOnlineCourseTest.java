package fu.sep490.g23.backend.ut.onlineCourseManagementService;

import fu.sep490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContextFactory;
import fu.sep490.g23.backend.repository.course.CourseLessonFlashcardRefRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineLessonRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.service.curriculum.ContentBankTypeGuard;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseMapper;
import fu.sep490.g23.backend.service.course.OnlineCoursePreviewValidator;
import fu.sep490.g23.backend.service.course.BunnyStreamService;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.FlashcardPracticeService;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.service.course.YouTubeTranscriptService;
import fu.sep490.g23.backend.service.course.impl.OnlineCourseServiceImpl;
import fu.sep490.g23.backend.service.mail.CourseEnrollmentMailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link OnlineCourseServiceImpl} - khởi tạo online course.
 */
@ExtendWith(MockitoExtension.class)
public class CreateOnlineCourseTest {

    @Mock private OnlineCourseRepository onlineCourseRepository;
    @Mock private OnlineCourseVersionRepository onlineCourseVersionRepository;
    @Mock private CourseCategoryRepository courseCategoryRepository;
    @Mock private OnlineCourseEnrollmentRepository enrollmentRepository;
    @Mock private OnlineLessonRepository lessonRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private VocabularyProgressRepository vocabularyProgressRepository;
    @Mock private CourseAssessmentRepository courseAssessmentRepository;
    @Mock private AssessmentBankItemRepository assessmentBankItemRepository;
    @Mock private FlashcardSetRepository flashcardSetRepository;
    @Mock private CourseLessonFlashcardRefRepository courseLessonFlashcardRefRepository;
    @Mock private AssessmentRubricRepository assessmentRubricRepository;
    @Mock private AssessmentSubmissionRepository assessmentSubmissionRepository;
    @Mock private PlacementTestAttemptRepository placementTestAttemptRepository;
    @Mock private PlacementRecommendationContextFactory placementRecommendationContextFactory;
    @Mock private UserRepository userRepository;
    @Mock private OnlineCourseMapper mapper;
    @Mock private OnlineCourseVersionService onlineCourseVersionService;
    @Mock private OnlineCoursePreviewValidator previewValidator;
    @Mock private BunnyStreamService bunnyStreamService;
    @Mock private CourseProgressService courseProgressService;
    @Mock private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    @Mock private FlashcardPracticeService flashcardPracticeService;
    @Mock private CourseEnrollmentMailService enrollmentMailService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private YouTubeTranscriptService youTubeTranscriptService;
    @Mock private ContentBankItemRepository contentBankItemRepository;
    @Mock private ContentBankTypeGuard contentBankTypeGuard;

    @InjectMocks
    private OnlineCourseServiceImpl service;

    @Test
    void createCourse_DraftStatus_Success() {
        // Arrange
        CourseCategory ieltsCategory = CourseCategory.builder()
                .id(1L).code("IELTS").name("IELTS").active(true).build();

        OnlineCourseVersion draftVersion = OnlineCourseVersion.builder()
                .id(200L)
                .versionNumber(1)
                .status(CourseVersionStatus.DRAFT)
                .modules(new ArrayList<>())
                .build();

        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Master 7.0")
                .category("IELTS")
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(1500000))
                .targetBand(7.0)
                .recommendedCurrentBandMin(5.0)
                .modules(new ArrayList<>())
                .build();

        when(courseCategoryRepository.findByCode("IELTS"))
                .thenReturn(Optional.of(ieltsCategory));
        when(onlineCourseRepository.existsBySlug(anyString())).thenReturn(false);

        when(onlineCourseRepository.save(any(OnlineCourse.class))).thenAnswer(invocation -> {
            OnlineCourse toSave = invocation.getArgument(0);
            toSave.setId(100L);
            toSave.setSlug("ielts-master-70");
            return toSave;
        });

        lenient().when(onlineCourseVersionService.requireEditableVersion(any(OnlineCourse.class)))
                .thenReturn(draftVersion);

        OnlineCourseResponse mockResponse = OnlineCourseResponse.builder()
                .id(100L)
                .title("IELTS Master 7.0")
                .slug("ielts-master-70")
                .category("IELTS")
                .categoryName("IELTS")
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(1500000))
                .originalPrice(new BigDecimal(1500000))
                .salePrice(new BigDecimal(1500000))
                .build();
        when(mapper.toResponse(any(OnlineCourse.class))).thenReturn(mockResponse);

        // Act
        OnlineCourseResponse result = service.createCourse(request, null);

        // Assert
        assertNotNull(result);

        // Verify saved course has DRAFT status
        ArgumentCaptor<OnlineCourse> courseCaptor = ArgumentCaptor.forClass(OnlineCourse.class);
        verify(onlineCourseRepository).save(courseCaptor.capture());
        OnlineCourse savedCourse = courseCaptor.getValue();

        assertNotNull(savedCourse);
        assertEquals("IELTS Master 7.0", savedCourse.getTitle());
        assertEquals(PackageStatus.DRAFT, savedCourse.getStatus());
        assertEquals(new BigDecimal(1500000), savedCourse.getPrice());
        assertEquals(ieltsCategory, savedCourse.getCategory());
        assertEquals(CourseLevel.ADVANCED, savedCourse.getLevel());

        verify(mapper).toResponse(savedCourse);
        verify(onlineCourseVersionService).createDraft(eq(100L), eq(null), eq(null));
        verify(onlineCourseVersionService).requireEditableVersion(any(OnlineCourse.class));
    }

    @Test
    void createCourse_MissingTargetBand_ThrowsException() {
        // Arrange
        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("IELTS Master 7.0")
                .category("IELTS")
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(1500000))
                .modules(new ArrayList<>())
                .build();

        // Act + Assert
        // validateCourseRequest throws before reaching courseCategoryRepository.findByCode
        assertThrows(RuntimeException.class, () -> service.createCourse(request, null));

        verify(onlineCourseRepository, never()).save(any(OnlineCourse.class));
        verify(onlineCourseVersionService, never()).createDraft(any(), any(), any());
    }

    @Test
    void createCourse_InvalidCategoryCode_ThrowsException() {
        // Arrange
        OnlineCourseRequest request = OnlineCourseRequest.builder()
                .title("General English")
                .category("9999")
                .level(CourseLevel.BEGINNER)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal(500000))
                .modules(new ArrayList<>())
                .build();

        // Act + Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createCourse(request, null));
        assertTrue(ex.getMessage() != null && !ex.getMessage().isBlank());

        verify(onlineCourseRepository, never()).save(any(OnlineCourse.class));
        verify(onlineCourseVersionService, never()).createDraft(any(), any(), any());
    }
}
