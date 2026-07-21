package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sap490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.service.course.impl.CourseProgressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    private PackageEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CourseProgressServiceImpl progressService;

    @Test
    void publishedNewContentDoesNotChangeOldEnrollmentDenominator() {
        User learner = User.builder().id(7L).email("learner@englishlab.vn").build();
        OnlineCourse courseWithTwentyLiveLessons = OnlineCourse.builder()
                .id(1L)
                .modules(new ArrayList<>(List.of(CourseModule.builder()
                        .lessons(new ArrayList<>(java.util.stream.IntStream.range(0, 20)
                                .mapToObj(index -> Lesson.builder().id((long) index + 1).build())
                                .toList()))
                        .build())))
                .build();
        OnlineCourseVersion enrollmentVersion = OnlineCourseVersion.builder()
                .id(101L)
                .onlineCourse(courseWithTwentyLiveLessons)
                .versionNumber(1)
                .totalRequiredLessons(10)
                .totalRequiredAssessments(0)
                .build();
        PackageEnrollment enrollment = PackageEnrollment.builder()
                .id(55L)
                .student(learner)
                .courseVersion(enrollmentVersion)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(80)
                .build();

        when(courseAssessmentRepository.countByOnlineCourseAndActiveTrue(courseWithTwentyLiveLessons)).thenReturn(0L);
        when(lessonProgressRepository.countByEnrollmentAndStatus(enrollment, LessonProgressStatus.COMPLETED)).thenReturn(8L);
        when(assessmentSubmissionRepository.countCompletedAssessments(
                org.mockito.ArgumentMatchers.eq(learner),
                org.mockito.ArgumentMatchers.eq(courseWithTwentyLiveLessons),
                org.mockito.ArgumentMatchers.anySet()
        )).thenReturn(0L);
        when(lessonProgressRepository.findByEnrollment(enrollment)).thenReturn(List.of());
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        PackageEnrollment refreshed = progressService.refreshEnrollmentProgress(
                enrollment,
                courseWithTwentyLiveLessons,
                learner
        );

        assertThat(refreshed.getProgressPercent()).isEqualTo(80);
        assertThat(refreshed.getCourseVersion().getVersionNumber()).isEqualTo(1);
    }
}
