package fu.sep490.g23.backend.ut.learningExperienceService;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomPracticeResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomPracticeAttemptHistoryRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.repository.course.CourseUnitContentRefRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.assessment.AssessmentPassingThresholdResolver;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomPracticeServiceImpl;
import fu.sep490.g23.backend.service.course.CourseProgressionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewPracticeInOnlineCourseTest {

    // ===== Mocks cho ClassroomPracticeServiceImpl (test listForLearner) =====
    @Mock private ClassSectionRepository offeringRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomPracticeAttemptHistoryRepository attemptHistoryRepository;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private CourseUnitContentRefRepository contentRefRepository;
    @Mock private ExerciseBankItemRepository exerciseRepository;

    @InjectMocks
    private ClassroomPracticeServiceImpl service;

    // ===== Mocks cho CourseProgressionGuard =====
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private CourseAssessmentRepository courseAssessmentRepository;
    @Mock private AssessmentSubmissionRepository assessmentSubmissionRepository;
    @Mock private AssessmentPassingThresholdResolver passingThresholdResolver;

    private CourseProgressionGuard guard;

    private User learner;
    private String learnerEmail;

    private OnlineCourse onlineCourse;
    private OnlineCourseModule moduleForProgression;
    private CourseAssessment moduleTestAssessment;

    @BeforeEach
    void setUp() {
        learnerEmail = "learner@englishlab.com";
        learner = User.builder()
                .id(1L)
                .email(learnerEmail)
                .fullName("Nguyen Van A")
                .build();

        // Course có targetBand = 8.5 -> threshold = 8.5 - 0.5 = 8.0
        onlineCourse = OnlineCourse.builder()
                .id(10L)
                .title("IELTS Master")
                .slug("ielts-master")
                .targetBand(8.5)
                .build();

        // Module chứa bài luyện tập cuối cùng là Module Test
        OnlineLesson lessonInModule = OnlineLesson.builder()
                .id(700L)
                .title("Speaking Practice 1")
                .sequenceNumber(1)
                .build();
        moduleForProgression = OnlineCourseModule.builder()
                .id(800L)
                .title("Module 1: Speaking Advanced")
                .sequenceNumber(1)
                .lessons(new ArrayList<>(List.of(lessonInModule)))
                .build();
        lessonInModule.setModule(moduleForProgression);

        // Bài test có cấu hình targetScore = 8.0
        moduleTestAssessment = CourseAssessment.builder()
                .id(900L)
                .onlineCourseVersion(OnlineCourseVersion.builder().id(1L).onlineCourse(onlineCourse).status(CourseVersionStatus.PUBLISHED).build())
                .module(moduleForProgression)
                .title("Module 1 Final Test - Writing")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .passingScore(BigDecimal.valueOf(8.0))
                .maxScore(BigDecimal.valueOf(9.0))
                .progressKey("")
                .active(true)
                .displayOrder(1)
                .build();

        guard = new CourseProgressionGuard(
                lessonProgressRepository,
                courseAssessmentRepository,
                assessmentSubmissionRepository,
                passingThresholdResolver
        );
    }

    /**
     * Test case: Học viên chưa ghi danh khóa học - không thể truy cập bài luyện tập.
     * Expected: Service throw RuntimeException với message "Bạn không thuộc lớp học này."
     */
    @Test
    void listPracticeInOnlineCourse_LearnerNotEnrolled() {
        // Arrange
        Long offeringId = 100L;
        when(accessHelper.requireUser(learnerEmail)).thenReturn(learner);

        // CHƯA có enrollment với trạng thái truy cập học tập cho offering này
        when(enrollmentRepository.existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(
                learner.getId(), offeringId, ClassroomRegistrationSupport.HAS_LEARNING_ACCESS))
                .thenReturn(false);

        // Act + Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.listForLearner(offeringId, learnerEmail)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bạn không thuộc lớp học này");
    }

    /**
     * Test case 2: Nhận diện bài kiểm tra cuối học phần và tính điểm đạt.
     * Expected:
     *   - Hệ thống nhận diện được bài Module Test thuộc module
     *   - Submission có aiScore = 8.0 -> đạt yêu cầu -> canAdvancePastModule = true
     */
    @Test
    void recognizeModuleFinalTest_LearnerMeetsTargetScore() {
        // Arrange
        // Đảm bảo lesson đã hoàn thành
        Long lessonId = moduleForProgression.getLessons().get(0).getId();
        LessonProgress completedLessonProgress = new LessonProgress();
        when(lessonProgressRepository
                .findByStudentAndLessonIdInAndStatus(
                        learner,
                        Set.of(lessonId),
                        LessonProgressStatus.COMPLETED))
                .thenReturn(List.of(completedLessonProgress));

        // Nhận diện module có 1 bài Module Test active
        when(courseAssessmentRepository.findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(moduleForProgression))
                .thenReturn(List.of(moduleTestAssessment));

        // Submission của learner cho bài Module Test
        when(assessmentSubmissionRepository.findTopByAssessmentAndStudentOrderBySubmittedAtDesc(
                moduleTestAssessment, learner))
                .thenReturn(Optional.of(
                        AssessmentSubmission.builder()
                                .id(999L)
                                .assessment(moduleTestAssessment)
                                .student(learner)
                                .score(BigDecimal.valueOf(8.0))
                                .status(SubmissionStatus.AI_EVALUATED)
                                .build()
                ));

        // Tính điểm đạt
        when(passingThresholdResolver.isScorePassing(BigDecimal.valueOf(8.0), moduleTestAssessment))
                .thenReturn(true);

        // Act
        boolean canAdvance = guard.canAdvancePastModule(learner, moduleForProgression);

        // Assert
        assertThat(canAdvance).isTrue();
    }

    /**
     * Test case 4: Học viên chưa hoàn thành lesson trong module - không thể vượt qua.
     */
    @Test
    void recognizeLockedModule_PreviousModuleTestNotPassed() {
        // Arrange - Setup cho khóa học 2 module
        OnlineCourse multiModuleCourse = OnlineCourse.builder()
                .id(20L)
                .title("IELTS Complete")
                .slug("ielts-complete")
                .targetBand(8.5)
                .build();

        // Module 1 - đã có lesson nhưng learner chưa hoàn thành
        OnlineLesson module1Lesson = OnlineLesson.builder()
                .id(901L)
                .title("Speaking Practice - Module 1")
                .sequenceNumber(1)
                .build();
        OnlineCourseModule module1 = OnlineCourseModule.builder()
                .id(901L)
                .onlineCourseVersion(OnlineCourseVersion.builder().id(2L).onlineCourse(multiModuleCourse).status(CourseVersionStatus.PUBLISHED).build())
                .title("Module 1: Foundations")
                .sequenceNumber(1)
                .lessons(new ArrayList<>(List.of(module1Lesson)))
                .build();
        module1Lesson.setModule(module1);

        CourseAssessment module1Test = CourseAssessment.builder()
                .id(901L)
                .onlineCourseVersion(module1.getOnlineCourseVersion())
                .module(module1)
                .title("Module 1 Final Test")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .passingScore(BigDecimal.valueOf(8.0))
                .maxScore(BigDecimal.valueOf(9.0))
                .progressKey("")
                .active(true)
                .displayOrder(1)
                .build();

        // Mock lessons trong Module 1 CHƯA hoàn thành -> trả empty list
        Long module1LessonId = module1.getLessons().get(0).getId();
        when(lessonProgressRepository
                .findByStudentAndLessonIdInAndStatus(
                        learner,
                        Set.of(module1LessonId),
                        LessonProgressStatus.COMPLETED))
                .thenReturn(List.of());

        lenient().when(courseAssessmentRepository.findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(module1))
                .thenReturn(List.of(module1Test));

        // Act
        boolean canAdvanceModule1 = guard.canAdvancePastModule(learner, module1);

        // Assert
        assertThat(canAdvanceModule1).isFalse();
    }
}
