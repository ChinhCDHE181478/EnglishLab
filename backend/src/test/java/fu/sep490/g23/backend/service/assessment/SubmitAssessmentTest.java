package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.service.assessment.impl.AiAssessmentServiceImpl;
import fu.sep490.g23.backend.dto.request.assessment.AssessmentSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
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
    private OnlineCourseEnrollmentRepository enrollmentRepository;

    @Mock
    private OnlineCourseVersionService onlineCourseVersionService;

    @Mock
    private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;

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
    private OnlineCourseEnrollment enrollment;
    private CourseAssessment assessment;
    private AssessmentSubmissionRequest request;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setEmail("student@test.com");

        course = new OnlineCourse();
        course.setId(10L);

        enrollment = new OnlineCourseEnrollment();
        enrollment.setStudent(student);
        enrollment.setOnlineCourse(course);
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
        request.setSubmittedText("My answer develops a clear argument with relevant evidence.");
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
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);
        when(enrollmentRepository.findByStudentAndOnlineCourse(student, course)).thenReturn(Optional.of(enrollment));
        
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
        verify(onlineCourseVersionService, times(1))
                .assertAssessmentBelongsToEnrollment(enrollment, assessment.getId());
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
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);

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
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.submitAssessment(assessment.getId(), request, student.getEmail());
        });
        assertEquals("Vui lòng nhập nội dung bài làm trước khi nộp.", exception.getMessage());
    }

    @Test
    void submitAssessment_InsufficientWriting_ReturnsZeroWithoutCallingAi() {
        request.setSubmittedText("1");
        assessment.setAiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND);
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);
        when(enrollmentRepository.findByStudentAndOnlineCourse(student, course)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssessmentSubmission.class))).thenAnswer(invocation -> {
            AssessmentSubmission saved = invocation.getArgument(0);
            saved.setId(1001L);
            return saved;
        });

        AiAssessmentSubmissionResponse result = aiAssessmentService.submitAssessment(
                assessment.getId(),
                request,
                student.getEmail()
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getAiScore()));
        assertTrue(result.getAiFeedbackJson().contains("Bài làm quá ngắn"));
        verify(aiEvaluationClient, never()).evaluate(anyString());
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
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course))
                .thenThrow(new RuntimeException("Bạn cần đăng ký khóa học trước khi làm bài đánh giá."));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAssessmentService.submitAssessment(assessment.getId(), request, student.getEmail());
        });
        assertEquals("Bạn cần đăng ký khóa học trước khi làm bài đánh giá.", exception.getMessage());
    }

    @Test
    void submitAssessment_ObjectiveSnapshot_ClearsStaleWritingRubricWithoutLiveBankRefresh() {
        AssessmentBankItem bankItem = AssessmentBankItem.builder()
                .title("Ngân hàng đề đã được chỉnh sửa sau khi xuất bản")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.LISTENING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .instructions("Làm đủ các câu Listening.")
                .timeLimitMinutes(40)
                .build();
        assessment.setAssessmentBankItem(bankItem);
        assessment.setTitle("Snapshot đề Listening đã xuất bản");
        assessment.setSkill(AssessmentSkill.LISTENING);
        assessment.setAiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND);
        request.setSubmittedText(null);
        request.setObjectiveAnswersJson("{\"responses\":[]}");

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);
        when(enrollmentRepository.findByStudentAndOnlineCourse(student, course)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssessmentSubmission.class))).thenAnswer(invocation -> {
            AssessmentSubmission saved = invocation.getArgument(0);
            saved.setId(1000L);
            return saved;
        });

        AiAssessmentSubmissionResponse result = aiAssessmentService.submitAssessment(
                assessment.getId(),
                request,
                student.getEmail()
        );

        assertNotNull(result);
        assertEquals(AssessmentSkill.LISTENING, assessment.getSkill());
        assertEquals("Snapshot đề Listening đã xuất bản", assessment.getTitle());
        assertNull(assessment.getRubric());
        verify(aiEvaluationClient, never()).evaluate(anyString());
    }

    @Test
    void submitAssessment_VocabularyQuiz_UsesDeterministicAnswerKey() {
        assessment.setType(AssessmentType.QUIZ);
        assessment.setSkill(AssessmentSkill.VOCABULARY);
        assessment.setRubric(null);
        assessment.setObjectiveAnswerKey("{\"1\":\"A\"}");
        assessment.setMaxScore(BigDecimal.ONE);
        request.setSubmittedText(null);
        request.setObjectiveAnswersJson("{\"responses\":[{\"questionNumber\":\"1\",\"part\":\"part_1\",\"answerType\":\"single_choice\",\"answer\":\"A\"}]}");

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(courseAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course)).thenReturn(enrollment);
        when(enrollmentRepository.findByStudentAndOnlineCourse(student, course)).thenReturn(Optional.empty());
        when(passingThresholdResolver.resolve(assessment)).thenReturn(BigDecimal.ONE);
        when(submissionRepository.save(any(AssessmentSubmission.class))).thenAnswer(invocation -> {
            AssessmentSubmission saved = invocation.getArgument(0);
            saved.setId(1002L);
            return saved;
        });

        AiAssessmentSubmissionResponse result = aiAssessmentService.submitAssessment(
                assessment.getId(),
                request,
                student.getEmail()
        );

        assertEquals(0, BigDecimal.ONE.compareTo(result.getAiScore()));
        assertTrue(result.getAiFeedbackJson().contains("\"correctCount\":1"));
        verify(aiEvaluationClient, never()).evaluate(anyString());
    }
}
