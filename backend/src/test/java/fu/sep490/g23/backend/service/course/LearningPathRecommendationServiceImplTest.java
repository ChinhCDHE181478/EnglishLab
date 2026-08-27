package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.course.LearningPath;
import fu.sep490.g23.backend.entity.course.LearningPathCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContext;
import fu.sep490.g23.backend.service.course.impl.LearningPathRecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningPathRecommendationServiceImplTest {
    @Mock private LearningPathRepository pathRepository;
    @Mock private LearningPathCourseRepository pathCourseRepository;
    @Mock private OnlineCourseEnrollmentRepository enrollmentRepository;

    private LearningPathRecommendationServiceImpl service;
    private User learner;

    @BeforeEach
    void setUp() {
        service = new LearningPathRecommendationServiceImpl(pathRepository, pathCourseRepository, enrollmentRepository);
        learner = User.builder().id(1L).email("learner@example.com").build();
    }

    @Test
    void intermediatePlacementWaivesBeginnerWithoutFakeCompletion() {
        LearningPath path = LearningPath.builder().id(1L).code("IELTS_65").name("IELTS 6.5").examCategory("IELTS").targetBand(BigDecimal.valueOf(6.5)).build();
        List<LearningPathCourse> refs = List.of(ref(path, course(10L, CourseLevel.BEGINNER), 1), ref(path, course(20L, CourseLevel.INTERMEDIATE), 2), ref(path, course(30L, CourseLevel.ADVANCED), 3));
        when(pathRepository.findAll()).thenReturn(List.of(path));
        when(pathCourseRepository.findByLearningPathIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(refs);
        when(enrollmentRepository.findByStudentOrderByRegisteredAtDesc(learner)).thenReturn(List.of());

        LearnerLearningPathResponse.PathOverview result = service.recommend(learner, context(PlacementLevel.INTERMEDIATE), true);

        assertThat(result.getCourses()).extracting("stepStatus").containsExactly("PLACEMENT_WAIVED", "CURRENT", "NEXT");
        assertThat(result.getCompletedCourses()).isZero();
        assertThat(result.getWaivedCourses()).isEqualTo(1);
        assertThat(result.getRecommendedStartCourseId()).isEqualTo(20L);
    }

    @Test
    void completedEnrollmentRemainsCompletedInsteadOfWaived() {
        LearningPath path = LearningPath.builder().id(1L).code("IELTS_65").name("IELTS 6.5").examCategory("IELTS").build();
        OnlineCourse beginner = course(10L, CourseLevel.BEGINNER);
        OnlineCourse intermediate = course(20L, CourseLevel.INTERMEDIATE);
        when(pathRepository.findAll()).thenReturn(List.of(path));
        when(pathCourseRepository.findByLearningPathIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(List.of(ref(path, beginner, 1), ref(path, intermediate, 2)));
        when(enrollmentRepository.findByStudentOrderByRegisteredAtDesc(learner)).thenReturn(List.of(OnlineCourseEnrollment.builder()
                .id(50L).student(learner).onlineCourse(beginner).status(EnrollmentStatus.COMPLETED).progressPercent(100).build()));

        LearnerLearningPathResponse.PathOverview result = service.recommend(learner, context(PlacementLevel.INTERMEDIATE), true);

        assertThat(result.getCourses().getFirst().getStepStatus()).isEqualTo("COMPLETED");
        assertThat(result.getCompletedCourses()).isEqualTo(1);
    }

    private PlacementRecommendationContext context(PlacementLevel level) {
        return PlacementRecommendationContext.builder().learnerId(1L).examType("IELTS").overallScore(BigDecimal.valueOf(5.5)).recommendedLevel(level).targetScore(BigDecimal.valueOf(6.5)).weakSkills(Set.of()).build();
    }

    private OnlineCourse course(Long id, CourseLevel level) {
        return OnlineCourse.builder().id(id).title(level.name()).slug(level.name().toLowerCase()).status(PackageStatus.PUBLISHED).level(level).build();
    }

    private LearningPathCourse ref(LearningPath path, OnlineCourse course, int order) {
        return LearningPathCourse.builder().id((long) order).learningPath(path).onlineCourse(course).displayOrder(order).build();
    }
}
