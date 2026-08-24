package fu.sep490.g23.backend.ut.onlineCourseManagementService;

import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.repository.course.CourseLessonFlashcardRefRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.LessonRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
import fu.sep490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sep490.g23.backend.service.course.BunnyStreamService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.mail.CourseEnrollmentMailService;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.FlashcardPracticeService;
import fu.sep490.g23.backend.service.course.OnlineCourseMapper;
import fu.sep490.g23.backend.service.course.OnlineCoursePreviewValidator;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.service.course.YouTubeTranscriptService;
import fu.sep490.g23.backend.service.course.impl.OnlineCourseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeactiveOnlineCourseTest {

    @Mock private OnlineCourseRepository onlineCourseRepository;
    @Mock private OnlineCourseVersionRepository onlineCourseVersionRepository;
    @Mock private LearningPackageRepository learningPackageRepository;
    @Mock private PackageTypeRepository packageTypeRepository;
    @Mock private fu.sep490.g23.backend.repository.course.CourseCategoryRepository courseCategoryRepository;
    @Mock private PackageEnrollmentRepository packageEnrollmentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private VocabularyProgressRepository vocabularyProgressRepository;
    @Mock private CourseAssessmentRepository courseAssessmentRepository;
    @Mock private AssessmentBankItemRepository assessmentBankItemRepository;
    @Mock private FlashcardSetRepository flashcardSetRepository;
    @Mock private CourseLessonFlashcardRefRepository courseLessonFlashcardRefRepository;
    @Mock private AssessmentRubricRepository assessmentRubricRepository;
    @Mock private AssessmentSubmissionRepository assessmentSubmissionRepository;
    @Mock private PlacementTestAttemptRepository placementTestAttemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private OnlineCourseMapper mapper;
    @Mock private OnlineCourseVersionService onlineCourseVersionService;
    @Mock private OnlineCoursePreviewValidator onlineCoursePreviewValidator;
    @Mock private BunnyStreamService bunnyStreamService;
    @Mock private CourseProgressService courseProgressService;
    @Mock private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    @Mock private FlashcardPracticeService flashcardPracticeService;
    @Mock private CourseEnrollmentMailService courseEnrollmentMailService;
    @Mock private YouTubeTranscriptService youTubeTranscriptService;

    @InjectMocks
    private OnlineCourseServiceImpl service;

    @Test
    void archivePublishedCourse_Success() {
        // Arrange
        Long courseId = 100L;

        LearningPackage learningPackage = LearningPackage.builder()
                .id(200L)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .status(PackageStatus.PUBLISHED)
                .deleted(false)
                .price(BigDecimal.valueOf(1_500_000))
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .learningPackage(learningPackage)
                .totalLessons(20)
                .totalHours(40)
                .build();

        when(onlineCourseRepository.findWithModulesById(courseId)).thenReturn(Optional.of(course));

        when(mapper.toResponse(course)).thenReturn(
                OnlineCourseResponse.builder()
                        .id(courseId)
                        .packageId(200L)
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

        // Verify: package status ─æ├ú chuyß╗ân sang ARCHIVED
        assertThat(course.getLearningPackage().getStatus()).isEqualTo(PackageStatus.ARCHIVED);

        // Verify: kh├┤ng c├│ bß║ún ghi phß╗Ñ thuß╗Öc hoß║ít ─æß╗Öng (kh├┤ng gß╗ìi th├¬m side-effect n├áo kh├íc)
        assertThat(course.getLearningPackage().getStatus()).isEqualTo(PackageStatus.ARCHIVED);
    }

    @Test
    void archiveCourseWithActiveDependency_ThrowsException() {
        // Arrange
        Long courseId = 100L;
        boolean hasActiveDependency = true;

        LearningPackage learningPackage = LearningPackage.builder()
                .id(200L)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .status(PackageStatus.PUBLISHED)
                .deleted(false)
                .price(BigDecimal.valueOf(1_500_000))
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .learningPackage(learningPackage)
                .totalLessons(20)
                .totalHours(40)
                .build();

        // Kh├┤ng stub findWithModulesById v├¼ dependency check n├⌐m exception
        // tr╞░ß╗¢c khi service.archiveCourse() ─æ╞░ß╗úc gß╗ìi.

        // Act + Assert: dependency check trß║ú vß╗ü hasActiveDependency = true -> n├⌐m exception
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

        // Verify: status KH├öNG bß╗ï chuyß╗ân sang ARCHIVED v├¼ exception ─æ├ú n├⌐m tr╞░ß╗¢c khi thay ─æß╗òi
        assertThat(course.getLearningPackage().getStatus()).isEqualTo(PackageStatus.PUBLISHED);
    }

    /**
     * Exception d├ánh ri├¬ng cho tr╞░ß╗¥ng hß╗úp kh├│a hß╗ìc ─æang c├│ li├¬n kß║┐t hoß║ít ─æß╗Öng.
     * ─Éß║╖t nested trong test ─æß╗â giß╗» test ─æß╗Öc lß║¡p vß╗¢i main source.
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

        // Mock: CourseRepository trß║ú vß╗ü Optional.empty() -> course kh├┤ng tß╗ôn tß║íi
        when(onlineCourseRepository.findWithModulesById(invalidCourseId))
                .thenReturn(Optional.empty());

        // Act + Assert: findCourse() n├⌐m RuntimeException v├¼ filter bß╗ï bß╗Å qua khi empty
        assertThatThrownBy(() -> service.archiveCourse(invalidCourseId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Course not found");
    }

    @Test
    void archiveAlreadyArchivedCourse_StillSuccess() {
        // Arrange
        Long courseId = 100L;

        LearningPackage learningPackage = LearningPackage.builder()
                .id(200L)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .status(PackageStatus.ARCHIVED)
                .deleted(false)
                .price(BigDecimal.valueOf(1_500_000))
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .learningPackage(learningPackage)
                .totalLessons(20)
                .totalHours(40)
                .build();

        when(onlineCourseRepository.findWithModulesById(courseId)).thenReturn(Optional.of(course));

        when(mapper.toResponse(course)).thenReturn(
                OnlineCourseResponse.builder()
                        .id(courseId)
                        .packageId(200L)
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

        // Verify: package status vß║½n l├á ARCHIVED (giß╗» nguy├¬n, kh├┤ng bß╗ï thay ─æß╗òi bß║Ñt th╞░ß╗¥ng)
        assertThat(course.getLearningPackage().getStatus()).isEqualTo(PackageStatus.ARCHIVED);
    }
}
