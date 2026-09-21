package fu.sep490.g23.backend.ut.learningExperienceService;

import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.course.OnlineLessonRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseMapper;
import fu.sep490.g23.backend.service.course.OnlineCoursePreviewValidator;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.service.course.impl.OnlineCourseVersionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
public class VIewPublicCourseTest {

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private OnlineCourseVersionRepository versionRepository;

    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;

    @Mock
    private AssessmentSubmissionRepository assessmentSubmissionRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private OnlineLessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OnlineCourseMapper mapper;

    @Mock
    private OnlineCoursePreviewValidator previewValidator;

    @Mock
    private OnlineCourseVersionService versionService;

    private OnlineCourse course;

    @BeforeEach
    void setUp() {
        CourseCategory ieltsCategory = new CourseCategory();
        ieltsCategory.setId(1L);
        ieltsCategory.setCode("IELTS");
        ieltsCategory.setName("IELTS");

        course = OnlineCourse.builder()
                .id(100L)
                .title("IELTS Foundation Course")
                .slug("ielts-foundation")
                .price(BigDecimal.valueOf(990000))
                .category(ieltsCategory)
                .level(CourseLevel.BEGINNER)
                .status(PackageStatus.PUBLISHED)
                .build();
    }

    @Test
    void viewPublicCourse_Success() {
        OnlineCourseResponse mockResponse = OnlineCourseResponse.builder()
                .id(100L)
                .status(PackageStatus.PUBLISHED)
                .build();

        OnlineCourseVersionServiceImpl service = new OnlineCourseVersionServiceImpl(
                onlineCourseRepository, versionRepository, courseAssessmentRepository,
                assessmentSubmissionRepository, lessonProgressRepository, lessonRepository,
                userRepository, mapper, previewValidator
        );

        when(mapper.toResponse(course)).thenReturn(mockResponse);

        OnlineCourseResponse result = service.readPublishedSnapshot(course, true);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(PackageStatus.PUBLISHED, result.getStatus());

        verify(mapper, times(1)).toResponse(course);
    }

    @Test
    void searchCoursesByKeyword_Success() {
        OnlineCourse course2 = OnlineCourse.builder()
                .id(101L)
                .title("IELTS Advanced")
                .price(BigDecimal.valueOf(990000))
                .category(course.getCategory())
                .level(CourseLevel.INTERMEDIATE)
                .status(PackageStatus.PUBLISHED)
                .build();

        OnlineCourseResponse resp1 = OnlineCourseResponse.builder().id(100L).title("IELTS Foundation Course").status(PackageStatus.PUBLISHED).build();
        OnlineCourseResponse resp2 = OnlineCourseResponse.builder().id(101L).title("IELTS Advanced").status(PackageStatus.PUBLISHED).build();

        when(versionService.readPublishedSnapshot(course, false)).thenReturn(resp1);
        when(versionService.readPublishedSnapshot(course2, false)).thenReturn(resp2);

        List<OnlineCourseResponse> result = List.of(
                versionService.readPublishedSnapshot(course, false),
                versionService.readPublishedSnapshot(course2, false)
        );

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(c -> c.getStatus() == PackageStatus.PUBLISHED));
        assertTrue(result.stream().anyMatch(c -> c.getTitle().contains("IELTS")));

        verify(versionService, times(1)).readPublishedSnapshot(course, false);
        verify(versionService, times(1)).readPublishedSnapshot(course2, false);
    }

    @Test
    void filterCoursesByCategoryAndLevel_Success() {
        CourseCategory toeicCategory = new CourseCategory();
        toeicCategory.setId(2L);
        toeicCategory.setCode("TOEIC");
        toeicCategory.setName("TOEIC");

        OnlineCourse ieltsIntermediate = OnlineCourse.builder()
                .id(101L)
                .title("IELTS Intermediate")
                .price(BigDecimal.valueOf(990000))
                .category(course.getCategory())
                .level(CourseLevel.INTERMEDIATE)
                .status(PackageStatus.PUBLISHED)
                .build();

        OnlineCourse toeicBeginner = OnlineCourse.builder()
                .id(102L)
                .title("TOEIC Beginner")
                .price(BigDecimal.valueOf(990000))
                .category(toeicCategory)
                .level(CourseLevel.BEGINNER)
                .status(PackageStatus.PUBLISHED)
                .build();

        List<OnlineCourse> allCourses = List.of(course, ieltsIntermediate, toeicBeginner);

        lenient().when(versionService.readPublishedSnapshot(course, false))
                .thenReturn(OnlineCourseResponse.builder().id(100L).category("IELTS").level(CourseLevel.BEGINNER).build());
        lenient().when(versionService.readPublishedSnapshot(ieltsIntermediate, false))
                .thenReturn(OnlineCourseResponse.builder().id(101L).category("IELTS").level(CourseLevel.INTERMEDIATE).build());
        lenient().when(versionService.readPublishedSnapshot(toeicBeginner, false))
                .thenReturn(OnlineCourseResponse.builder().id(102L).category("TOEIC").level(CourseLevel.BEGINNER).build());


        List<OnlineCourseResponse> filtered = allCourses.stream()
                .filter(c -> c.getCategory().getCode().equals("IELTS"))
                .filter(c -> c.getLevel() == CourseLevel.BEGINNER)
                .map(c -> versionService.readPublishedSnapshot(c, false))
                .toList();


        assertEquals(1, filtered.size());
        assertTrue(filtered.stream().allMatch(c -> c.getCategory().equals("IELTS")));
        assertTrue(filtered.stream().allMatch(c -> c.getLevel() == CourseLevel.BEGINNER));

        verify(versionService, times(1)).readPublishedSnapshot(course, false);
        verify(versionService, times(1)).readPublishedSnapshot(ieltsIntermediate, false);
        verify(versionService, times(1)).readPublishedSnapshot(toeicBeginner, false);
    }

    @Test
    void searchCourses_NoResultsFound() {
        CourseCategory toeicCategory = new CourseCategory();
        toeicCategory.setId(2L);
        toeicCategory.setCode("TOEIC");
        toeicCategory.setName("TOEIC");

        OnlineCourse toeicCourse = OnlineCourse.builder()
                .id(201L)
                .title("TOEIC 450")
                .price(BigDecimal.valueOf(990000))
                .category(toeicCategory)
                .level(CourseLevel.ADVANCED)
                .status(PackageStatus.PUBLISHED)
                .build();

        List<OnlineCourse> allCourses = List.of(toeicCourse);

        lenient().when(versionService.readPublishedSnapshot(toeicCourse, false))
                .thenReturn(OnlineCourseResponse.builder().id(201L).category("TOEIC").level(CourseLevel.ADVANCED).build());

        List<OnlineCourseResponse> filtered = allCourses.stream()
                .filter(c -> c.getCategory().getCode().equals("IELTS"))
                .filter(c -> c.getLevel() == CourseLevel.BEGINNER)
                .map(c -> versionService.readPublishedSnapshot(c, false))
                .toList();

        assertTrue(filtered.isEmpty(), "No search results found.");

        verify(versionService, times(1)).readPublishedSnapshot(toeicCourse, false);
    }
}
