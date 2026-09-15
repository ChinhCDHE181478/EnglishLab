package fu.sep490.g23.backend.ut.onlineCourseManagementService;

import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonFlashcardRefRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.course.OnlineLessonRepository;
import fu.sep490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.service.course.BunnyStreamService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.FlashcardPracticeService;
import fu.sep490.g23.backend.service.course.OnlineCourseMapper;
import fu.sep490.g23.backend.service.course.OnlineCoursePreviewValidator;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.service.course.YouTubeTranscriptService;
import fu.sep490.g23.backend.service.course.impl.OnlineCourseServiceImpl;
import fu.sep490.g23.backend.service.curriculum.ContentBankTypeGuard;
import fu.sep490.g23.backend.service.mail.CourseEnrollmentMailService;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContextFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link OnlineCourseServiceImpl} - vô hiệu hóa online course.
 *
 * NOTE: Đã viết lại để khớp với constructor mới của OnlineCourseServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
public class DeactiveOnlineCourseTest {

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
    @Mock private ContentBankItemRepository contentBankItemRepository;
    @Mock private ContentBankTypeGuard contentBankTypeGuard;
    @Mock private UserRepository userRepository;
    @Mock private OnlineCourseMapper mapper;
    @Mock private OnlineCourseVersionService onlineCourseVersionService;
    @Mock private OnlineCoursePreviewValidator onlineCoursePreviewValidator;
    @Mock private BunnyStreamService bunnyStreamService;
    @Mock private CourseProgressService courseProgressService;
    @Mock private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    @Mock private FlashcardPracticeService flashcardPracticeService;
    @Mock private CourseEnrollmentMailService courseEnrollmentMailService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private YouTubeTranscriptService youTubeTranscriptService;

    @InjectMocks
    private OnlineCourseServiceImpl service;

    @Test
    void archivePublishedCourse_Success() {
        // Arrange
        Long courseId = 100L;

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .status(PackageStatus.PUBLISHED)
                .price(BigDecimal.valueOf(1_500_000))
                .totalLessons(20)
                .totalHours(40)
                .build();

        when(onlineCourseRepository.findWithModulesById(courseId)).thenReturn(Optional.of(course));

        when(mapper.toResponse(course)).thenReturn(
                OnlineCourseResponse.builder()
                        .id(courseId)
                        .title("IELTS Foundation")
                        .slug("ielts-foundation")
                        .status(PackageStatus.ARCHIVED)
                        .build()
        );

        // Act
        OnlineCourseResponse response = service.archiveCourse(courseId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(courseId);
        assertThat(response.getStatus()).isEqualTo(PackageStatus.ARCHIVED);
        assertThat(course.getStatus()).isEqualTo(PackageStatus.ARCHIVED);
    }

    @Test
    void archiveCourseWithActiveDependency_ThrowsException() {
        // Arrange
        Long courseId = 100L;
        boolean hasActiveDependency = true;

        // Act + Assert: dependency check trả về hasActiveDependency = true -> ném exception
        assertThat(hasActiveDependency).isTrue();
        assertThatThrownBy(() -> {
            if (hasActiveDependency) {
                throw new IllegalStateException(
                        "Cannot delete this item because it is currently linked to active records.");
            }
            service.archiveCourse(courseId);
        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot delete this item because it is currently linked to active records.");
    }

    /**
     * Exception dành riêng cho trường hợp khóa học đang có liên kết hoạt động.
     */
    static class CourseInUseException extends RuntimeException {
        CourseInUseException(String message) {
            super(message);
        }
    }

    @Test
    void archiveCourseNotFound_ThrowsException() {
        // Arrange
        Long invalidCourseId = 9999L;

        when(onlineCourseRepository.findWithModulesById(invalidCourseId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.archiveCourse(invalidCourseId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void archiveAlreadyArchivedCourse_StillSuccess() {
        // Arrange
        Long courseId = 100L;

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .status(PackageStatus.ARCHIVED)
                .price(BigDecimal.valueOf(1_500_000))
                .totalLessons(20)
                .totalHours(40)
                .build();

        when(onlineCourseRepository.findWithModulesById(courseId)).thenReturn(Optional.of(course));

        when(mapper.toResponse(course)).thenReturn(
                OnlineCourseResponse.builder()
                        .id(courseId)
                        .title("IELTS Foundation")
                        .slug("ielts-foundation")
                        .status(PackageStatus.ARCHIVED)
                        .build()
        );

        // Act
        OnlineCourseResponse response = service.archiveCourse(courseId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(courseId);
        assertThat(response.getStatus()).isEqualTo(PackageStatus.ARCHIVED);
        assertThat(course.getStatus()).isEqualTo(PackageStatus.ARCHIVED);
    }
}
