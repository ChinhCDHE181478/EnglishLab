package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.response.course.LessonResponse;
import fu.sap490.g23.backend.dto.response.course.ModuleResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sap490.g23.backend.entity.course.enums.FlashcardPracticeSource;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.commerce.WishlistItemRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sap490.g23.backend.service.course.impl.FlashcardPracticeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashcardPracticeVersionSnapshotTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private OnlineCourseRepository onlineCourseRepository;
    @Mock
    private PackageEnrollmentRepository enrollmentRepository;
    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private VocabularyProgressRepository progressRepository;
    @Mock
    private OnlineCourseVersionService onlineCourseVersionService;

    @InjectMocks
    private FlashcardPracticeServiceImpl service;

    @Test
    void enrolledPracticeReadsVocabularyFromEnrollmentVersionSnapshot() {
        User learner = User.builder().id(1L).email("learner@test.com").build();
        LearningPackage learningPackage = LearningPackage.builder().id(2L).title("IELTS v1").deleted(false).build();
        OnlineCourse course = OnlineCourse.builder().id(3L).learningPackage(learningPackage).build();
        OnlineCourseVersion versionOne = OnlineCourseVersion.builder()
                .id(4L)
                .onlineCourse(course)
                .versionNumber(1)
                .status(CourseVersionStatus.RETIRED)
                .build();
        PackageEnrollment enrollment = PackageEnrollment.builder()
                .id(5L)
                .student(learner)
                .learningPackage(learningPackage)
                .courseVersion(versionOne)
                .build();
        OnlineCourseResponse snapshot = OnlineCourseResponse.builder()
                .id(course.getId())
                .title("IELTS v1")
                .modules(List.of(ModuleResponse.builder()
                        .id(6L)
                        .title("Vocabulary v1")
                        .displayOrder(1)
                        .lessons(List.of(LessonResponse.builder()
                                .id(7L)
                                .lessonKey("VOCAB-001")
                                .title("Family vocabulary")
                                .displayOrder(1)
                                .contentText("### 1. immediate family\n**Meaning:** gia đình ruột thịt\n**Example:** My immediate family is small.")
                                .build()))
                        .build()))
                .build();

        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(enrollmentRepository.findByStudentOrderByRegisteredAtDesc(learner)).thenReturn(List.of(enrollment));
        when(onlineCourseRepository.findByLearningPackage(learningPackage)).thenReturn(Optional.of(course));
        when(onlineCourseVersionService.readEnrollmentSnapshot(enrollment, course)).thenReturn(snapshot);
        when(progressRepository.findByStudentAndCourse(learner, course)).thenReturn(List.of());

        List<VocabularyTermResponse> terms = service.getPracticeTerms(
                FlashcardPracticeSource.ENROLLED,
                course.getId(),
                false,
                learner.getEmail()
        );

        assertThat(terms).singleElement().satisfies(term -> {
            assertThat(term.getTerm()).isEqualTo("immediate family");
            assertThat(term.getMeaning()).isEqualTo("gia đình ruột thịt");
            assertThat(term.getCourseTitle()).isEqualTo("IELTS v1");
        });
    }
}
