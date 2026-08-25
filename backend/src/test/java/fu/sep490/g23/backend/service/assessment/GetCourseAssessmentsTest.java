package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.service.assessment.impl.AiAssessmentServiceImpl;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCourseAssessmentsTest {

    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;

    @Mock
    private AssessmentSubmissionRepository submissionRepository;

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private OnlineCourseEnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssessmentPassingThresholdResolver passingThresholdResolver;

    @Mock
    private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;

    @Mock
    private OnlineCourseVersionService onlineCourseVersionService;

    @InjectMocks
    private AiAssessmentServiceImpl aiAssessmentService;

    private User student;
    private OnlineCourse course;
    private LearningPackage learningPackage;
    private OnlineCourseEnrollment enrollment;
    private CourseAssessment assessment;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setEmail("student@test.com");

        learningPackage = new LearningPackage();
        learningPackage.setId(1L);

        course = new OnlineCourse();
        course.setId(10L);
        course.setLearningPackage(learningPackage);

        enrollment = new OnlineCourseEnrollment();
        enrollment.setStudent(student);
        enrollment.setLearningPackage(learningPackage);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        assessment = new CourseAssessment();
        assessment.setId(100L);
        assessment.setOnlineCourse(course);
        assessment.setActive(true);
        assessment.setType(AssessmentType.MODULE_TEST);
        assessment.setProgressKey("assessment-100");
    }

    /**
     * Mục đích: Lấy danh sách bài kiểm tra của khóa học thành công.
     * Kỳ vọng: Trả về danh sách bài kiểm tra, kiểm tra quyền truy cập (phải được enrolled).
     */
    @Test
    void getCourseAssessments_Success_ReturnsAssessments() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);
        when(onlineCourseVersionService.getLatestPublishedAssessmentIds(enrollment)).thenReturn(List.of(assessment.getId()));
        when(courseAssessmentRepository.findAllById(List.of(assessment.getId()))).thenReturn(List.of(assessment));
        when(submissionRepository.findTop2ByAssessmentProgressKeyAndStudentOrderBySubmittedAtDesc(
                assessment.getProgressKey(),
                student
        )).thenReturn(List.of());

        // Act
        List<CourseAssessmentResponse> result = aiAssessmentService.getCourseAssessments(course.getId(), student.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(assessment.getId(), result.get(0).getId());
    }

    @Test
    void getCourseAssessments_UsesLatestPublishedVersionInsteadOfEnrollmentPurchaseVersion() {
        OnlineCourseVersion versionOne = OnlineCourseVersion.builder()
                .id(20L)
                .onlineCourse(course)
                .versionNumber(1)
                .status(CourseVersionStatus.RETIRED)
                .assessmentIdsJson("[100]")
                .build();
        enrollment.setCourseVersion(versionOne);
        assessment.setActive(false);

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);
        when(onlineCourseVersionService.getLatestPublishedAssessmentIds(enrollment)).thenReturn(List.of(assessment.getId()));
        when(courseAssessmentRepository.findAllById(List.of(assessment.getId()))).thenReturn(List.of(assessment));
        when(submissionRepository.findTop2ByAssessmentProgressKeyAndStudentOrderBySubmittedAtDesc(
                assessment.getProgressKey(),
                student
        )).thenReturn(List.of());

        List<CourseAssessmentResponse> result = aiAssessmentService.getCourseAssessments(
                course.getId(),
                student.getEmail()
        );

        assertEquals(List.of(assessment.getId()), result.stream().map(CourseAssessmentResponse::getId).toList());
        verify(courseAssessmentRepository, never())
                .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course);
    }

    @Test
    void getCourseAssessments_CarriesSubmissionAcrossAssessmentVersions() {
        assessment.setProgressKey("module-1-writing");
        CourseAssessment previousAssessment = new CourseAssessment();
        previousAssessment.setId(90L);
        previousAssessment.setOnlineCourse(course);
        previousAssessment.setProgressKey(assessment.getProgressKey());
        previousAssessment.setType(AssessmentType.MODULE_TEST);
        AssessmentSubmission historicalSubmission = AssessmentSubmission.builder()
                .id(901L)
                .assessment(previousAssessment)
                .student(student)
                .status(SubmissionStatus.PASSED)
                .build();

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);
        when(onlineCourseVersionService.getLatestPublishedAssessmentIds(enrollment))
                .thenReturn(List.of(assessment.getId()));
        when(courseAssessmentRepository.findAllById(List.of(assessment.getId())))
                .thenReturn(List.of(assessment));
        when(submissionRepository.findTop2ByAssessmentProgressKeyAndStudentOrderBySubmittedAtDesc(
                assessment.getProgressKey(),
                student
        )).thenReturn(List.of(historicalSubmission));

        List<CourseAssessmentResponse> result = aiAssessmentService.getCourseAssessments(
                course.getId(),
                student.getEmail()
        );

        assertEquals(901L, result.get(0).getLatestSubmission().getId());
        assertEquals(90L, result.get(0).getLatestSubmission().getAssessmentId());
        assertEquals(SubmissionStatus.PASSED, result.get(0).getLatestSubmission().getStatus());
    }

    /**
     * Mục đích: Kiểm tra trường hợp lấy danh sách bài test nhưng không tìm thấy user.
     * Kỳ vọng: Ném ngoại lệ "Student not found".
     */
    @Test
    void getCourseAssessments_Failure_StudentNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.getCourseAssessments(course.getId(), "notfound@test.com");
        });
        assertEquals("Student not found", exception.getMessage());
    }

    /**
     * Mục đích: Kiểm tra trường hợp khóa học không tồn tại.
     * Kỳ vọng: Ném ngoại lệ "Course not found".
     */
    @Test
    void getCourseAssessments_Failure_CourseNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.getCourseAssessments(99L, student.getEmail());
        });
        assertEquals("Course not found", exception.getMessage());
    }

    /**
     * Mục đích: Ngăn chặn truy cập nếu học viên chưa đăng ký khóa học.
     * Kỳ vọng: Ném ngoại lệ "Student is not enrolled in this online course".
     */
    @Test
    void getCourseAssessments_Failure_NotEnrolled_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course))
                .thenThrow(new RuntimeException("Bạn cần đăng ký khóa học trước khi làm bài đánh giá."));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.getCourseAssessments(course.getId(), student.getEmail());
        });
        assertEquals("Bạn cần đăng ký khóa học trước khi làm bài đánh giá.", exception.getMessage());
    }

    /**
     * Mục đích: Ngăn chặn truy cập nếu khóa học đã bị hủy đăng ký (CANCELLED).
     * Kỳ vọng: Ném ngoại lệ "Enrollment is not active".
     */
    @Test
    void getCourseAssessments_Failure_EnrollmentCancelled_ThrowsException() {
        // Arrange
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(onlineCourseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course))
                .thenThrow(new RuntimeException("Bạn đã hủy đăng ký khóa học này. Vui lòng đăng ký lại để tiếp tục."));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.getCourseAssessments(course.getId(), student.getEmail());
        });
        assertEquals("Bạn đã hủy đăng ký khóa học này. Vui lòng đăng ký lại để tiếp tục.", exception.getMessage());
    }
}
