package fu.sep490.g23.backend.ut.learningExperienceService;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestDefinition;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.assessment.impl.PlacementTestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link PlacementTestServiceImpl}.
 * - Case 1: Khởi tạo bài thi đánh giá đầu vào - PRE-3 (kiểm tra thiết bị đã hoàn thành).
 * - Case 2: Nộp bài thi & chấm điểm thành công - AI Engine hoạt động bình thường.
 * - Case 3: Tự động nộp bài khi hết giờ - dùng saved_draft_answers[].
 * - Case 4: Nộp bài thất bại do AI Engine bị sập - graceful fallback objective-only.
 */
@ExtendWith(MockitoExtension.class)
class TakePlacementTestTest {

    @Mock private UserRepository userRepository;
    @Mock private PlacementTestAttemptRepository attemptRepository;
    @Mock private AiEvaluationClient aiEvaluationClient;
    @Mock private AssessmentAudioStorageService audioStorageService;
    @Mock private PlacementTestDefinitionService definitionService;
    @Mock private ContentBankItemRepository contentBankItemRepository;

    private PlacementTestServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User learner;
    private String learnerEmail;
    private PlacementTestDefinition activeDefinition;

    // Text fixtures dùng chung
    private static final String WRITING_TASK_1 =
            "The diagram illustrates the process of ethanol fuel production from corn as raw material. "
            + "In the first stage, corn is harvested and milled into fine powder, then cooked with water "
            + "to produce a liquid mash. The next stage shows the fermentation process and purification "
            + "stages that yield the final ethanol fuel.";
    private static final String WRITING_TASK_2 =
            "Many people argue that mental strength is more important than physical strength in the "
            + "success of athletes. While physical strength enables athletes to perform at high levels in "
            + "training and competition, mental strength carries them through injuries and defeats. Top "
            + "sports athletes credit their success to the mental side of competition, the discipline to "
            + "train every day, and the ability to focus under pressure during the most important events.";
    private static final String SPEAKING_TRANSCRIPT =
            "I am from Hanoi and I live in my home town. In my free time I enjoy watching films and "
            + "movies with my parents. These leisure activities help me relax after work.";

    private void stubAllConfigs() throws Exception {
        when(definitionService.getConfig(activeDefinition, "listening"))
                .thenReturn(objectMapper.readTree(activeDefinition.getListeningConfigJson()));
        when(definitionService.getConfig(activeDefinition, "reading"))
                .thenReturn(objectMapper.readTree(activeDefinition.getReadingConfigJson()));
        when(definitionService.getConfig(activeDefinition, "writing"))
                .thenReturn(objectMapper.readTree(activeDefinition.getWritingConfigJson()));
        when(definitionService.getConfig(activeDefinition, "speaking"))
                .thenReturn(objectMapper.readTree(activeDefinition.getSpeakingConfigJson()));
    }

    @BeforeEach
    void setUp() {
        learnerEmail = "learner@englishlab.com";
        learner = User.builder()
                .id(1L)
                .email(learnerEmail)
                .fullName("Nguyen Van A")
                .currentBand(null)
                .build();

        // answerKey: 5 câu Listening + 5 câu Reading
        String listeningConfig = "{\"answerKey\":{\"q1\":\"A\",\"q2\":\"B\",\"q3\":\"C\",\"q4\":\"D\",\"q5\":\"E\"}}";
        String readingConfig   = "{\"answerKey\":{\"q1\":\"true\",\"q2\":\"false\",\"q3\":\"true\",\"q4\":\"false\",\"q5\":\"true\"}}";
        String writingConfig   = "{\"tasks\":[{\"id\":\"task_1\"},{\"id\":\"task_2\"}]}";
        String speakingConfig  = "{\"parts\":[{\"id\":\"part_1\"}]}";
        String toeicConfig     = "{\"listening\":{\"parts\":[]},\"reading\":{\"parts\":[]}}";

        activeDefinition = PlacementTestDefinition.builder()
                .id(10L)
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .title("Bài đánh giá đầu vào IELTS")
                .description("Một phiên đánh giá gồm Nghe, Đọc, Viết và Nói được gửi để điểm bất đồng.")
                .examType("IELTS")
                .status("PUBLISHED")
                .maxAttempts(3)
                .listeningConfigJson(listeningConfig)
                .readingConfigJson(readingConfig)
                .writingConfigJson(writingConfig)
                .speakingConfigJson(speakingConfig)
                .toeicConfigJson(toeicConfig)
                .updatedAt(LocalDateTime.now())
                .build();

        lenient().when(userRepository.findByEmail(learnerEmail)).thenReturn(Optional.of(learner));
        lenient().when(definitionService.getDefinition()).thenReturn(activeDefinition);

        service = new PlacementTestServiceImpl(
                userRepository, attemptRepository, aiEvaluationClient,
                audioStorageService, definitionService, contentBankItemRepository);
    }


    // Case 1: Khoi tao bai thi danh gia dau vao
    @Test
    void initializePlacementTest_LearnerCompletedDeviceCheck_ReturnsFullTestConfig() throws Exception {
        // Arrange
        when(attemptRepository.countByStudentAndTestCode(learner, PlacementTestDefinitionService.TEST_CODE))
                .thenReturn(0L);
        when(attemptRepository.findTopByStudentAndTestCodeOrderBySubmittedAtDesc(
                learner, PlacementTestDefinitionService.TEST_CODE))
                .thenReturn(Optional.empty());

        when(definitionService.getConfig(activeDefinition, "listening"))
                .thenReturn(objectMapper.readTree(activeDefinition.getListeningConfigJson()));
        when(definitionService.getConfig(activeDefinition, "reading"))
                .thenReturn(objectMapper.readTree(activeDefinition.getReadingConfigJson()));
        when(definitionService.getConfig(activeDefinition, "writing"))
                .thenReturn(objectMapper.readTree(activeDefinition.getWritingConfigJson()));
        when(definitionService.getConfig(activeDefinition, "speaking"))
                .thenReturn(objectMapper.readTree(activeDefinition.getSpeakingConfigJson()));
        when(definitionService.getConfig(activeDefinition, "toeic"))
                .thenReturn(objectMapper.readTree(activeDefinition.getToeicConfigJson()));

        // Act
        Map<String, Object> response = service.getTest(learnerEmail);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.get("testCode")).isEqualTo(PlacementTestDefinitionService.TEST_CODE);
        assertThat(response.get("examType")).isEqualTo("IELTS");
        assertThat(response.get("maxAttempts")).isEqualTo(3);
        assertThat(response.get("attemptCount")).isEqualTo(0L);
        assertThat(response.get("canRetake")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        Map<String, Object> sections = (Map<String, Object>) response.get("sections");
        assertThat(sections).containsKeys("listening", "reading", "writing", "speaking", "toeic");

        // Bảo mật: listening/reading/toeic KHÔNG lộ answerKey
        assertThat((Map<String, Object>) sections.get("listening")).doesNotContainKey("answerKey");
        assertThat((Map<String, Object>) sections.get("reading")).doesNotContainKey("answerKey");
        assertThat((Map<String, Object>) sections.get("toeic")).doesNotContainKey("answerKey");

        assertThat(response).doesNotContainKey("latestAttempt");
    }

    // =================================================================
    // Case 2: Nộp bài thi & chấm điểm thành công
    // PRE: lần làm bài đang IN_PROGRESS, AI Engine Service hoạt động bình thường
    // Input: attempt_id (learner + request), danh sách câu trả lời answers[]
    // =================================================================
    @Test
    void submitPlacementTest_AllAnswersProvided_GradingSuccessful() throws Exception {
        // Arrange - Listening/Reading: 5/5 đúng -> Listening 3.0, Reading 2.5
        Map<String, Object> listeningAnswers = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) listeningAnswers.put("q" + i, String.valueOf((char)('A' + i - 1)));

        Map<String, Object> readingAnswers = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) readingAnswers.put("q" + i, i % 2 == 1 ? "true" : "false");

        Map<String, Object> writingAnswers = new LinkedHashMap<>();
        writingAnswers.put("task_1", WRITING_TASK_1);
        writingAnswers.put("task_2", WRITING_TASK_2);

        Map<String, Object> deviceCheck = new LinkedHashMap<>();
        deviceCheck.put("completed", true);
        deviceCheck.put("microphone", true);
        deviceCheck.put("fullscreen", true);

        PlacementTestSubmissionRequest request = new PlacementTestSubmissionRequest();
        request.setExamType("IELTS");
        request.setListeningAnswers(listeningAnswers);
        request.setReadingAnswers(readingAnswers);
        request.setWritingAnswers(writingAnswers);
        request.setSpeakingTranscript(SPEAKING_TRANSCRIPT);
        request.setDeviceCheck(deviceCheck);

        when(attemptRepository.countByStudentAndTestCode(learner, PlacementTestDefinitionService.TEST_CODE))
                .thenReturn(0L);
        stubAllConfigs();

        AiEvaluationResult aiResult = AiEvaluationResult.builder()
                .estimatedScore(new BigDecimal("7.0"))
                .feedbackJson("{\"estimatedScore\":7.0,\"writingBand\":7.0,\"speakingBand\":7.0}")
                .provider("mock-ai").model("gpt-eval-1").audioInputAnalyzed(false).build();
        when(aiEvaluationClient.evaluate(anyString())).thenReturn(aiResult);

        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PlacementTestAttemptResponse response = service.submit(request, learnerEmail);

        // Assert
        assertThat(response.getLearnerId()).isEqualTo(learner.getId());
        assertThat(response.getExamType()).isEqualTo("IELTS");
        assertThat(response.getCorrectListening()).isEqualTo(5);
        assertThat(response.getCorrectReading()).isEqualTo(5);
        assertThat(response.getListeningScore()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(response.getReadingScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(response.getWritingScore()).isEqualByComparingTo(new BigDecimal("7.0"));
        assertThat(response.getSpeakingScore()).isEqualByComparingTo(new BigDecimal("7.0"));
        assertThat(response.getOverallScore()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getSubmittedAt()).isNotNull();
        assertThat(response.getExpiresAt())
                .isCloseTo(response.getSubmittedAt().plusDays(180), within(5, ChronoUnit.SECONDS));
    }

    // =================================================================
    // Case 3: Tự động nộp bài khi hết giờ (isExpired = true)
    // PRE: Thời gian đếm ngược chạm mức 0
    // Input: saved_draft_answers[] (chỉ một phần, do hết giờ trước khi hoàn tất)
    // =================================================================
    @Test
    void submitPlacementTest_TimeExpired_AutoSubmitWithSavedDraft() throws Exception {
        // Arrange: saved_draft_answers[] - chỉ có 3/5 câu listening + 2/5 câu reading
        Map<String, Object> savedDraftListening = new LinkedHashMap<>();
        savedDraftListening.put("q1", "A");
        savedDraftListening.put("q2", "B");
        savedDraftListening.put("q3", "C");

        Map<String, Object> savedDraftReading = new LinkedHashMap<>();
        savedDraftReading.put("q1", "true");
        savedDraftReading.put("q2", "false");

        Map<String, Object> emptyWriting = new LinkedHashMap<>();

        Map<String, Object> deviceCheck = new LinkedHashMap<>();
        deviceCheck.put("completed", true);
        deviceCheck.put("autoSubmit", true);

        PlacementTestSubmissionRequest request = new PlacementTestSubmissionRequest();
        request.setExamType("IELTS");
        request.setListeningAnswers(savedDraftListening);
        request.setReadingAnswers(savedDraftReading);
        request.setWritingAnswers(emptyWriting);
        request.setSpeakingTranscript("I am from Hanoi.");
        request.setDeviceCheck(deviceCheck);

        when(attemptRepository.countByStudentAndTestCode(learner, PlacementTestDefinitionService.TEST_CODE))
                .thenReturn(0L);

        AiEvaluationResult aiResult = AiEvaluationResult.builder()
                .estimatedScore(new BigDecimal("5.0"))
                .feedbackJson("{\"estimatedScore\":5.0,\"writingBand\":null,\"speakingBand\":null," +
                        "\"message\":\"Đã hết giờ! Bài làm của bạn đã tự động nộp.\"}")
                .provider("mock-ai")
                .audioInputAnalyzed(false)
                .build();
        when(aiEvaluationClient.evaluate(anyString())).thenReturn(aiResult);

        stubAllConfigs();

        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PlacementTestAttemptResponse response = service.submit(request, learnerEmail);

        // Assert
        assertThat(response.getLearnerId()).isEqualTo(learner.getId());
        assertThat(response.getCorrectListening()).isEqualTo(3);
        assertThat(response.getListeningScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(response.getCorrectReading()).isEqualTo(2);
        assertThat(response.getReadingScore()).isEqualByComparingTo(new BigDecimal("0"));
        assertThat(response.getWritingScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(response.getSpeakingScore()).isNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAiFeedbackJson()).contains("hết giờ").contains("tự động nộp");
        assertThat(response.getSubmittedAt()).isNotNull();
        assertThat(response.getExpiresAt())
                .isCloseTo(response.getSubmittedAt().plusDays(180), within(5, ChronoUnit.SECONDS));
    }

    // =================================================================
    // Case 4: Nộp bài thất bại do AI Engine Service bị sập
    // PRE: AI Engine throw RuntimeException -> service fallback objective-only
    // =================================================================
    @Test
    void submitPlacementTest_AIEngineDown_ObjectiveOnlyFallback() throws Exception {
        // Arrange
        Map<String, Object> listeningAnswers = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) listeningAnswers.put("q" + i, String.valueOf((char)('A' + i - 1)));

        Map<String, Object> readingAnswers = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) readingAnswers.put("q" + i, i % 2 == 1 ? "true" : "false");

        Map<String, Object> writingAnswers = new LinkedHashMap<>();
        writingAnswers.put("task_1", WRITING_TASK_1);
        writingAnswers.put("task_2", WRITING_TASK_2);

        Map<String, Object> deviceCheck = new LinkedHashMap<>();
        deviceCheck.put("completed", true);
        deviceCheck.put("microphone", true);
        deviceCheck.put("fullscreen", true);

        PlacementTestSubmissionRequest request = new PlacementTestSubmissionRequest();
        request.setExamType("IELTS");
        request.setListeningAnswers(listeningAnswers);
        request.setReadingAnswers(readingAnswers);
        request.setWritingAnswers(writingAnswers);
        request.setSpeakingTranscript(SPEAKING_TRANSCRIPT);
        request.setDeviceCheck(deviceCheck);

        when(attemptRepository.countByStudentAndTestCode(learner, PlacementTestDefinitionService.TEST_CODE))
                .thenReturn(0L);
        stubAllConfigs();

        // AI Engine Service BỊ SẬP
        doThrow(new RuntimeException("AI Engine Service unavailable: connection timeout"))
                .when(aiEvaluationClient).evaluate(anyString());

        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act - service không throw ra ngoài
        PlacementTestAttemptResponse response = service.submit(request, learnerEmail);

        // Assert - Listening/Reading chấm bình thường, Writing/Speaking = null
        assertThat(response).isNotNull();
        assertThat(response.getCorrectListening()).isEqualTo(5);
        assertThat(response.getCorrectReading()).isEqualTo(5);
        assertThat(response.getListeningScore()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(response.getReadingScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(response.getWritingScore()).isNull();
        assertThat(response.getSpeakingScore()).isNull();
        // overall = (3.0 + 2.5) / 2 = 2.75 -> round half-band = 3.0
        assertThat(response.getOverallScore()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(response.getStatus()).isEqualTo("OBJECTIVE_EVALUATED");
        assertThat(response.getSubmittedAt()).isNotNull();
        assertThat(response.getExpiresAt())
                .isCloseTo(response.getSubmittedAt().plusDays(180), within(5, ChronoUnit.SECONDS));
        assertThat(response.getAiFeedbackJson()).contains("message");
    }
}
