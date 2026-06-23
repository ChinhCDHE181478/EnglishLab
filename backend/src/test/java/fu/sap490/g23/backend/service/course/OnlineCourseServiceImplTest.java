package fu.sap490.g23.backend.service.course;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.exception.CourseUnavailableException;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sap490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.repository.course.LessonRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sap490.g23.backend.service.mail.CourseEnrollmentMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineCourseServiceImplTest {

    @Mock
    private OnlineCourseRepository onlineCourseRepository;
    @Mock
    private LearningPackageRepository learningPackageRepository;
    @Mock
    private PackageTypeRepository packageTypeRepository;
    @Mock
    private CourseCategoryRepository courseCategoryRepository;
    @Mock
    private PackageEnrollmentRepository enrollmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private VocabularyProgressRepository vocabularyProgressRepository;
    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;
    @Mock
    private AssessmentRubricRepository assessmentRubricRepository;
    @Mock
    private AssessmentSubmissionRepository assessmentSubmissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseProgressionGuard courseProgressionGuard;
    @Mock
    private OnlineCourseMapper mapper;
    @Mock
    private BunnyStreamService bunnyStreamService;
    @Mock
    private CourseProgressService courseProgressService;
    @Mock
    private CourseEnrollmentMailService courseEnrollmentMailService;
    @Mock
    private YouTubeTranscriptService youTubeTranscriptService;

    private OnlineCourseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OnlineCourseServiceImpl(
                onlineCourseRepository,
                learningPackageRepository,
                packageTypeRepository,
                courseCategoryRepository,
                enrollmentRepository,
                lessonRepository,
                lessonProgressRepository,
                vocabularyProgressRepository,
                courseAssessmentRepository,
                assessmentRubricRepository,
                assessmentSubmissionRepository,
                userRepository,
                mapper,
                bunnyStreamService,
                courseProgressService,
                courseProgressionGuard,
                courseEnrollmentMailService,
                youTubeTranscriptService
        );
    }

    @Test
    void registerCourse_rejectsWhenCourseIsNotPublished() {
        User student = User.builder().email("learner@example.com").build();
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesByIdAndLearningPackageDeletedFalseAndLearningPackageStatus(99L, PackageStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        CourseUnavailableException exception = assertThrows(
                CourseUnavailableException.class,
                () -> service.registerCourse(99L, student.getEmail())
        );

        assertEquals("Course not found or not available for enrollment", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(PackageEnrollment.class));
        verify(courseEnrollmentMailService, never()).sendEnrollmentSuccessEmail(any(), any(), any());
    }

    @Test
    void registerCourse_rejectsDirectEnrollmentForPaidCourse() {
        User student = User.builder().email("learner@example.com").build();
        LearningPackage learningPackage = LearningPackage.builder()
                .id(10L).status(PackageStatus.PUBLISHED).deleted(false)
                .price(BigDecimal.valueOf(499000)).build();
        OnlineCourse course = OnlineCourse.builder().id(5L).learningPackage(learningPackage).build();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesByIdAndLearningPackageDeletedFalseAndLearningPackageStatus(course.getId(), PackageStatus.PUBLISHED))
                .thenReturn(Optional.of(course));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.registerCourse(course.getId(), student.getEmail()));

        assertEquals("Khóa học trả phí chỉ được kích hoạt sau khi thanh toán thành công.", exception.getMessage());
        verify(enrollmentRepository, never()).save(any(PackageEnrollment.class));
    }

    @Test
    void registerCourse_createsEnrollmentForPublishedCourse() {
        User student = User.builder().email("learner@example.com").build();
        LearningPackage learningPackage = LearningPackage.builder()
                .id(10L)
                .status(PackageStatus.PUBLISHED)
                .deleted(false)
                .title("IELTS Intensive")
                .slug("ielts-intensive")
                .build();
        OnlineCourse course = OnlineCourse.builder()
                .id(5L)
                .learningPackage(learningPackage)
                .build();
        PackageEnrollment savedEnrollment = PackageEnrollment.builder()
                .id(100L)
                .student(student)
                .learningPackage(learningPackage)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(0)
                .build();
        OnlineCourseResponse response = OnlineCourseResponse.builder()
                .id(course.getId())
                .registered(true)
                .enrollmentId(savedEnrollment.getId())
                .build();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesByIdAndLearningPackageDeletedFalseAndLearningPackageStatus(course.getId(), PackageStatus.PUBLISHED))
                .thenReturn(Optional.of(course));
        when(learningPackageRepository.findByIdAndDeletedFalseAndStatusForUpdate(learningPackage.getId(), PackageStatus.PUBLISHED))
                .thenReturn(Optional.of(learningPackage));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(PackageEnrollment.class))).thenReturn(savedEnrollment);
        when(mapper.toResponse(course, true, 0, savedEnrollment.getId())).thenReturn(response);

        OnlineCourseResponse result = service.registerCourse(course.getId(), student.getEmail());

        assertEquals(savedEnrollment.getId(), result.getEnrollmentId());
        verify(enrollmentRepository).save(any(PackageEnrollment.class));
        verify(courseEnrollmentMailService).sendEnrollmentSuccessEmail(student, course, savedEnrollment);
    }

    @Test
    void registerCourse_returnsExistingEnrollmentWithoutSavingAgain() {
        User student = User.builder().email("learner@example.com").build();
        LearningPackage learningPackage = LearningPackage.builder()
                .id(10L)
                .status(PackageStatus.PUBLISHED)
                .deleted(false)
                .title("IELTS Intensive")
                .slug("ielts-intensive")
                .build();
        OnlineCourse course = OnlineCourse.builder()
                .id(5L)
                .learningPackage(learningPackage)
                .build();
        PackageEnrollment existingEnrollment = PackageEnrollment.builder()
                .id(100L)
                .student(student)
                .learningPackage(learningPackage)
                .status(EnrollmentStatus.ACTIVE)
                .progressPercent(40)
                .build();
        OnlineCourseResponse response = OnlineCourseResponse.builder()
                .id(course.getId())
                .registered(true)
                .enrollmentId(existingEnrollment.getId())
                .progressPercent(existingEnrollment.getProgressPercent())
                .build();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesByIdAndLearningPackageDeletedFalseAndLearningPackageStatus(course.getId(), PackageStatus.PUBLISHED))
                .thenReturn(Optional.of(course));
        when(learningPackageRepository.findByIdAndDeletedFalseAndStatusForUpdate(learningPackage.getId(), PackageStatus.PUBLISHED))
                .thenReturn(Optional.of(learningPackage));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(existingEnrollment));
        when(mapper.toResponse(course, true, existingEnrollment.getProgressPercent(), existingEnrollment.getId())).thenReturn(response);

        OnlineCourseResponse result = service.registerCourse(course.getId(), student.getEmail());

        assertEquals(existingEnrollment.getId(), result.getEnrollmentId());
        assertEquals(existingEnrollment.getProgressPercent(), result.getProgressPercent());
        verify(enrollmentRepository, never()).save(any(PackageEnrollment.class));
        verify(courseEnrollmentMailService, never()).sendEnrollmentSuccessEmail(any(), any(), any());
    }

    @Test
    void registerCourse_reactivatesCancelledEnrollment() {
        User student = User.builder().email("learner@example.com").build();
        LearningPackage learningPackage = LearningPackage.builder()
                .id(10L)
                .status(PackageStatus.PUBLISHED)
                .deleted(false)
                .title("IELTS Intensive")
                .slug("ielts-intensive")
                .build();
        OnlineCourse course = OnlineCourse.builder()
                .id(5L)
                .learningPackage(learningPackage)
                .build();
        PackageEnrollment cancelledEnrollment = PackageEnrollment.builder()
                .id(100L)
                .student(student)
                .learningPackage(learningPackage)
                .status(EnrollmentStatus.CANCELLED)
                .progressPercent(25)
                .build();
        OnlineCourseResponse response = OnlineCourseResponse.builder()
                .id(course.getId())
                .registered(true)
                .enrollmentId(cancelledEnrollment.getId())
                .progressPercent(cancelledEnrollment.getProgressPercent())
                .build();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findWithModulesByIdAndLearningPackageDeletedFalseAndLearningPackageStatus(course.getId(), PackageStatus.PUBLISHED))
                .thenReturn(Optional.of(course));
        when(learningPackageRepository.findByIdAndDeletedFalseAndStatusForUpdate(learningPackage.getId(), PackageStatus.PUBLISHED))
                .thenReturn(Optional.of(learningPackage));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(cancelledEnrollment));
        when(enrollmentRepository.save(cancelledEnrollment)).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(course, true, cancelledEnrollment.getProgressPercent(), cancelledEnrollment.getId())).thenReturn(response);

        OnlineCourseResponse result = service.registerCourse(course.getId(), student.getEmail());

        assertEquals(EnrollmentStatus.ACTIVE, cancelledEnrollment.getStatus());
        assertEquals(cancelledEnrollment.getId(), result.getEnrollmentId());
        verify(enrollmentRepository).save(cancelledEnrollment);
        verify(courseEnrollmentMailService).sendEnrollmentSuccessEmail(student, course, cancelledEnrollment);
    }

    @Test
    void removeModule_deletesAssessmentsWhenThereAreNoStudentSubmissions() {
        CourseModule module = CourseModule.builder()
                .id(11L)
                .title("Mô-đun 1")
                .build();
        CourseAssessment assessment = CourseAssessment.builder()
                .id(21L)
                .module(module)
                .title("Bài kiểm tra cuối mô-đun")
                .build();

        when(courseAssessmentRepository.findByModule(module)).thenReturn(List.of(assessment));
        when(assessmentSubmissionRepository.existsByAssessmentId(assessment.getId())).thenReturn(false);

        ReflectionTestUtils.invokeMethod(service, "ensureModuleCanBeRemoved", module);

        verify(courseAssessmentRepository).deleteAll(List.of(assessment));
        verify(courseAssessmentRepository).flush();
    }

    @Test
    void removeModule_rejectsWhenAnAssessmentHasStudentSubmissions() {
        CourseModule module = CourseModule.builder()
                .id(11L)
                .title("Mô-đun 1")
                .build();
        CourseAssessment assessment = CourseAssessment.builder()
                .id(21L)
                .module(module)
                .title("Bài kiểm tra cuối mô-đun")
                .build();

        when(courseAssessmentRepository.findByModule(module)).thenReturn(List.of(assessment));
        when(assessmentSubmissionRepository.existsByAssessmentId(assessment.getId())).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "ensureModuleCanBeRemoved", module)
        );

        assertEquals(
                "Không thể xóa mô-đun \"Mô-đun 1\" vì đã có bài làm học viên trong bài kiểm tra \"Bài kiểm tra cuối mô-đun\".",
                exception.getMessage()
        );
        verify(courseAssessmentRepository, never()).deleteAll(any());
        verify(courseAssessmentRepository, never()).flush();
    }
}
