package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCompletionStatus;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.course.impl.CourseProgressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseVersionProgressStabilityTest {

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;

    @Mock
    private AssessmentSubmissionRepository assessmentSubmissionRepository;

    @Mock
    private OnlineCourseEnrollmentRepository enrollmentRepository;

    @Mock
    private OnlineCourseVersionService onlineCourseVersionService;

    @InjectMocks
    private CourseProgressServiceImpl progressService;

    @Test
    void publishedNewContentDoesNotChangeOldEnrollmentDenominator() {
        User learner = User.builder().id(7L).email("learner@englishlab.vn").build();
        OnlineCourse courseWithTwentyLiveLessons = OnlineCourse.builder()
                .id(1L)
                .build();
        OnlineCourseVersion enrollmentVersion = OnlineCourseVersion.builder()
                .id(101L)
                .onlineCourse(courseWithTwentyLiveLessons)
                .versionNumber(1)
                .totalRequiredLessons(10)
                .totalRequiredAssessments(0)
                .modules(new ArrayList<>(List.of(OnlineCourseModule.builder()
                        .lessons(new ArrayList<>(java.util.stream.IntStream.range(0, 20)
                                .mapToObj(index -> OnlineLesson.builder().id((long) index + 1).build())
                                .toList()))
                        .build())))
                .build();
        OnlineCourseEnrollment enrollment = OnlineCourseEnrollment.builder()
                .id(55L)
                .student(learner)
                .courseVersion(enrollmentVersion)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(80)
                .build();

        when(courseAssessmentRepository.countByOnlineCourseAndActiveTrue(courseWithTwentyLiveLessons)).thenReturn(0L);
        when(lessonProgressRepository.countByEnrollmentAndStatus(enrollment, LessonProgressStatus.COMPLETED)).thenReturn(8L);
        when(lessonProgressRepository.findByEnrollment(enrollment)).thenReturn(List.of());

        OnlineCourseEnrollment refreshed = progressService.refreshEnrollmentProgress(
                enrollment,
                courseWithTwentyLiveLessons,
                learner
        );

        assertThat(refreshed.getProgressPercent()).isEqualTo(80);
        assertThat(refreshed.getCourseVersion().getVersionNumber()).isEqualTo(1);
        verify(enrollmentRepository, never()).save(enrollment);
    }

    @Test
    void completionIgnoresStaleEnrollmentPercentageWhenLearnerHasNotStarted() {
        User learner = User.builder().id(9L).email("not-started@englishlab.vn").build();
        OnlineCourse course = OnlineCourse.builder()
                .id(3L)
                .title("Khóa học chưa bắt đầu")
                .slug("not-started")
                .build();
        OnlineCourseVersion enrollmentVersion = OnlineCourseVersion.builder()
                .id(103L)
                .onlineCourse(course)
                .versionNumber(1)
                .totalRequiredLessons(2)
                .totalRequiredAssessments(0)
                .modules(new ArrayList<>(List.of(OnlineCourseModule.builder()
                        .lessons(new ArrayList<>(List.of(
                                OnlineLesson.builder().id(1L).build(),
                                OnlineLesson.builder().id(2L).build()
                        )))
                        .build())))
                .build();
        OnlineCourseEnrollment enrollment = OnlineCourseEnrollment.builder()
                .id(57L)
                .student(learner)
                .courseVersion(enrollmentVersion)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(35)
                .build();

        when(courseAssessmentRepository.countByOnlineCourseAndActiveTrue(course)).thenReturn(0L);
        when(lessonProgressRepository.countByEnrollmentAndStatus(enrollment, LessonProgressStatus.COMPLETED))
                .thenReturn(0L);
        when(onlineCourseVersionService.getProgressBaselineAssessmentIds(enrollment)).thenReturn(List.of());
        when(courseAssessmentRepository.findAllById(List.of())).thenReturn(List.of());
        when(lessonProgressRepository.findByEnrollment(enrollment)).thenReturn(List.of());

        CourseCompletionResponse completion = progressService.buildCompletionResponse(enrollment, course, learner);

        assertThat(completion.getProgressPercent()).isZero();
        assertThat(completion.getCompletedLessons()).isZero();
        assertThat(completion.getStatus()).isEqualTo(CourseCompletionStatus.CHUA_BAT_DAU);
    }

    @Test
    void historicalAssessmentSubmissionStillCountsAfterAssessmentRowIsVersioned() {
        User learner = User.builder().id(8L).email("learner2@englishlab.vn").build();
        OnlineLesson lesson = OnlineLesson.builder().id(1L).build();
        OnlineCourse course = OnlineCourse.builder()
                .id(2L)
                .build();
        OnlineCourseVersion baselineVersion = OnlineCourseVersion.builder()
                .id(102L)
                .onlineCourse(course)
                .versionNumber(1)
                .totalRequiredLessons(1)
                .totalRequiredAssessments(1)
                .modules(new ArrayList<>(List.of(OnlineCourseModule.builder()
                        .lessons(new ArrayList<>(List.of(lesson)))
                        .build())))
                .build();
        OnlineCourseEnrollment enrollment = OnlineCourseEnrollment.builder()
                .id(56L)
                .student(learner)
                .courseVersion(baselineVersion)
                .status(EnrollmentStatus.ACTIVE)
                .build();
        CourseAssessment baselineAssessment = CourseAssessment.builder()
                .id(100L)
                .onlineCourseVersion(baselineVersion)
                .progressKey("module-1-writing")
                .build();

        when(courseAssessmentRepository.countByOnlineCourseAndActiveTrue(course)).thenReturn(1L);
        when(lessonProgressRepository.countByEnrollmentAndStatus(enrollment, LessonProgressStatus.COMPLETED))
                .thenReturn(1L);
        when(onlineCourseVersionService.getProgressBaselineAssessmentIds(enrollment)).thenReturn(List.of(100L));
        when(courseAssessmentRepository.findAllById(List.of(100L))).thenReturn(List.of(baselineAssessment));
        when(assessmentSubmissionRepository.existsByAssessmentProgressKeyAndStudentAndStatusIn(
                org.mockito.ArgumentMatchers.eq(baselineAssessment.getProgressKey()),
                org.mockito.ArgumentMatchers.eq(learner),
                org.mockito.ArgumentMatchers.anySet()
        )).thenReturn(true);
        when(lessonProgressRepository.findByEnrollment(enrollment)).thenReturn(List.of());
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        OnlineCourseEnrollment refreshed = progressService.refreshEnrollmentProgress(enrollment, course, learner);

        assertThat(refreshed.getProgressPercent()).isEqualTo(100);
        assertThat(refreshed.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }
}
