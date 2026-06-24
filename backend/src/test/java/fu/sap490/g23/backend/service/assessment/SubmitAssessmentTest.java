package fu.sap490.g23.backend.service.assessment;

import fu.sap490.g23.backend.service.assessment.impl.AiAssessmentServiceImpl;
import fu.sap490.g23.backend.dto.request.assessment.AssessmentSubmissionRequest;
import fu.sap490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sap490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.service.ai.AiEvaluationClient;
import fu.sap490.g23.backend.service.ai.AiEvaluationResult;
import fu.sap490.g23.backend.service.course.CourseProgressService;
import fu.sap490.g23.backend.service.course.CourseProgressionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitAssessmentTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;

    @Mock
    private PackageEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseProgressionGuard courseProgressionGuard;

    @Mock
    private AiEvaluationClient aiEvaluationClient;

    @Mock
    private AssessmentSubmissionRepository submissionRepository;

    @Mock
    private CourseProgressService courseProgressService;

    @Mock
    private AssessmentPassingThresholdResolver passingThresholdResolver;

    @InjectMocks
    private AiAssessmentServiceImpl aiAssessmentService;

    private User student;
    private OnlineCourse course;
    private LearningPackage learningPackage;
    private PackageEnrollment enrollment;
    private CourseAssessment assessment;
    private AssessmentSubmissionRequest request;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setEmail("student@test.com");

        learningPackage = new LearningPackage();
        learningPackage.setId(1L);

        course = new OnlineCourse();
        course.setId(10L);
        course.setLearningPackage(learningPackage);

        enrollment = new PackageEnrollment();
        enrollment.setStudent(student);
        enrollment.setLearningPackage(learningPackage);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        assessment = new CourseAssessment();
        assessment.setId(100L);
        assessment.setOnlineCourse(course);
        assessment.setAiEvaluationMode(AiEvaluationMode.RUBRIC_FEEDBACK);
        AssessmentRubric rubric = new AssessmentRubric();
        rubric.setSkill(AssessmentSkill.WRITING);
        assessment.setRubric(rubric);
        assessment.setSkill(AssessmentSkill.WRITING);

        request = new AssessmentSubmissionRequest();
        request.setSubmittedText("My answer");
    }

    /**
     * Mục đích: Kiểm tra trường hợp nộp bài thành công, hệ thống gọi AI chấm điểm và lưu kết quả.
     * Kỳ vọng: Gọi AI client, lưu db, cập nhật lại tiến độ khóa học.
     */
    @Test
    void submitAssessment_Success_WritesToDbAndRefreshesProgress() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));
        
        AiEvaluationResult mockAiResult = AiEvaluationResult.builder()
                .estimatedScore(BigDecimal.valueOf(8.0))
                .feedbackJson("{}")
                .provider("TestProvider")
                .model("TestModel")
                .rawResponse("Raw")
                .build();
        when(aiEvaluationClient.evaluate(anyString())).thenReturn(mockAiResult);

        AssessmentSubmission mockSubmission = new AssessmentSubmission();
        mockSubmission.setId(999L);
        mockSubmission.setAssessment(assessment);
        when(submissionRepository.save(any(AssessmentSubmission.class))).thenReturn(mockSubmission);

        // Act
        AiAssessmentSubmissionResponse result = aiAssessmentService.submitAssessment(assessment.getId(), request, student.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(999L, result.getId());
        verify(courseProgressionGuard, times(1)).ensureAssessmentCanBeSubmitted(student, assessment);
        verify(submissionRepository, times(1)).save(any(AssessmentSubmission.class));
        verify(courseProgressService, times(1)).refreshEnrollmentProgress(enrollment, course, student);
    }

    /**
     * Mục đích: Ngăn chặn nộp bài nếu bài test chưa được thiết lập chế độ chấm điểm bằng AI.
     * Kỳ vọng: Ném ngoại lệ "Bài đánh giá này chưa bật phản hồi tự động".
     */
    @Test
    void submitAssessment_Failure_AiEvaluationModeNone_ThrowsException() {
        // Arrange
        assessment.setAiEvaluationMode(AiEvaluationMode.NONE);
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.submitAssessment(assessment.getId(), request, student.getEmail());
        });
        assertEquals("Bài đánh giá này chưa bật phản hồi tự động.", exception.getMessage());
    }

    /**
     * Mục đích: Chặn việc nộp bài thi trống (không có text, audio hay json).
     * Kỳ vọng: Ném ngoại lệ yêu cầu nhập nội dung.
     */
    @Test
    void submitAssessment_Failure_NoSubmissionContent_ThrowsException() {
        // Arrange
        request.setSubmittedText(null);
        request.setSubmittedAudioUrl(null);
        request.setObjectiveAnswersJson(null);
        
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.of(enrollment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.submitAssessment(assessment.getId(), request, student.getEmail());
        });
        assertEquals("Vui lòng nhập nội dung bài làm trước khi nộp.", exception.getMessage());
    }

    /**
     * Mục đích: Ngăn chặn nộp bài nếu học viên chưa đăng ký khóa học.
     * Kỳ vọng: Ném ngoại lệ báo chưa đăng ký.
     */
    @Test
    void submitAssessment_Failure_NotEnrolled_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(enrollmentRepository.findByStudentAndLearningPackage(student, learningPackage)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.submitAssessment(assessment.getId(), request, student.getEmail());
        });
        assertEquals("Bạn cần đăng ký khóa học trước khi làm bài đánh giá.", exception.getMessage());
    }
}
