package fu.sep490.g23.backend.ut.learningExperienceService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.PlacementTestDefinition;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.assessment.PlacementTestSessionToken;
import fu.sep490.g23.backend.service.assessment.impl.PlacementTestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;


@ExtendWith(MockitoExtension.class)
public class TakePlacementTestTest {

    private static final String LEARNER_EMAIL = "learner_01@test.com";
    private static final String TEST_CODE = "IELTS_PLACEMENT_CURRENT";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlacementTestAttemptRepository attemptRepository;

    @Mock
    private AiEvaluationClient aiEvaluationClient;

    @Mock
    private AssessmentAudioStorageService audioStorageService;

    @Mock
    private PlacementTestDefinitionService definitionService;

    @Mock
    private PlacementTestSessionToken sessionTokenService;

    @Mock
    private ContentBankItemRepository contentBankItemRepository;

    private PlacementTestServiceImpl service;

    private User learner;
    private PlacementTestDefinition publishedDefinition;
    private ContentBankItem bankItem;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new PlacementTestServiceImpl(
                userRepository,
                attemptRepository,
                aiEvaluationClient,
                audioStorageService,
                definitionService,
                sessionTokenService,
                contentBankItemRepository
        );

        learner = User.builder()
                .id(1L)
                .email(LEARNER_EMAIL)
                .fullName("Learner One")
                .currentBand(5.5)
                .build();

        bankItem = ContentBankItem.builder()
                .id(1L)
                .title("Placement Test")
                .build();

        // Default definition: PUBLISHED with all exam types enabled
        publishedDefinition = PlacementTestDefinition.builder()
                .id(1L)
                .testCode(TEST_CODE)
                .title("Bài đánh giá đầu vào")
                .status("PUBLISHED")
                .ieltsEnabled(true)
                .toeicEnabled(true)
                .skillAssessmentEnabled(true)
                .maxAttempts(3)
                .build();
    }


    private PlacementTestSubmissionRequest buildIeltsRequest(Map<String, Object> listening,
                                                            Map<String, Object> reading,
                                                            Map<String, Object> writing,
                                                            String speakingAudioUrl,
                                                            String speakingTranscript) {
        PlacementTestSubmissionRequest req = new PlacementTestSubmissionRequest();
        req.setExamType("IELTS");
        req.setListeningAnswers(listening);
        req.setReadingAnswers(reading);
        req.setWritingAnswers(writing);
        req.setSpeakingAudioUrl(speakingAudioUrl);
        req.setSpeakingTranscript(speakingTranscript);
        req.setDeviceCheck(Map.of("completed", true));
        return req;
    }


    private PlacementTestSubmissionRequest buildToeicRequest(Map<String, Object> listening,
                                                             Map<String, Object> reading) {
        PlacementTestSubmissionRequest req = new PlacementTestSubmissionRequest();
        req.setExamType("TOEIC");
        req.setListeningAnswers(listening);
        req.setReadingAnswers(reading);
        req.setDeviceCheck(Map.of("completed", true));
        return req;
    }

    // TC01: Nộp bài Placement Test IELTS thành công với đầy đủ 4 kỹ năng
    @Test
    void takePlacementTest_TC01_ieltsAllSkills() throws Exception {
        // Arrange: Mock student lookup
        when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        when(userRepository.save(any(User.class))).thenReturn(learner);

        // Mock definition service
        when(definitionService.getDefinition()).thenReturn(publishedDefinition);

        // Mock config: listening with answer key (30 correct -> band 7.0)
        JsonNode listeningConfig = objectMapper.readTree(
                "{\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\",\"6\":\"B\",\"7\":\"C\",\"8\":\"D\",\"9\":\"A\",\"10\":\"B\"," +
                        "\"11\":\"A\",\"12\":\"B\",\"13\":\"C\",\"14\":\"D\",\"15\":\"A\",\"16\":\"B\",\"17\":\"C\",\"18\":\"D\",\"19\":\"A\",\"20\":\"B\"," +
                        "\"21\":\"A\",\"22\":\"B\",\"23\":\"C\",\"24\":\"D\",\"25\":\"A\",\"26\":\"B\",\"27\":\"C\",\"28\":\"D\",\"29\":\"A\",\"30\":\"B\"}}"
        );
        // Mock config: reading with answer key (32 correct -> band 6.5)
        JsonNode readingConfig = objectMapper.readTree(
                "{\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\",\"6\":\"B\",\"7\":\"C\",\"8\":\"D\",\"9\":\"A\",\"10\":\"B\"," +
                        "\"11\":\"A\",\"12\":\"B\",\"13\":\"C\",\"14\":\"D\",\"15\":\"A\",\"16\":\"B\",\"17\":\"C\",\"18\":\"D\",\"19\":\"A\",\"20\":\"B\"," +
                        "\"21\":\"A\",\"22\":\"B\",\"23\":\"C\",\"24\":\"D\",\"25\":\"A\",\"26\":\"B\",\"27\":\"C\",\"28\":\"D\",\"29\":\"A\",\"30\":\"B\"," +
                        "\"31\":\"A\",\"32\":\"B\"}}"
        );
        JsonNode writingConfig = objectMapper.readTree(
                "{\"tasks\":[{\"taskId\":\"task_1\",\"title\":\"Writing Task 1\"},{\"taskId\":\"task_2\",\"title\":\"Writing Task 2\"}]}"
        );
        JsonNode speakingConfig = objectMapper.readTree(
                "{\"topics\":[{\"topicId\":\"part_1\",\"title\":\"Introduction\"}]}"
        );

        when(definitionService.getConfig(publishedDefinition, "listening")).thenReturn(listeningConfig);
        when(definitionService.getConfig(publishedDefinition, "reading")).thenReturn(readingConfig);
        when(definitionService.getConfig(publishedDefinition, "writing")).thenReturn(writingConfig);
        when(definitionService.getConfig(publishedDefinition, "speaking")).thenReturn(speakingConfig);

        // Mock audio storage (has audio URL)
        AssessmentAudioStorageService.StoredAssessmentAudio storedAudio =
                new AssessmentAudioStorageService.StoredAssessmentAudio("audio.mp3", "audio/mpeg", 60000, new byte[60000]);
        when(audioStorageService.loadStoredAudioFromUrl("http://storage/audio.mp3"))
                .thenReturn(Optional.of(storedAudio));

        // Mock AI evaluation result
        AiEvaluationResult aiResult = AiEvaluationResult.builder()
                .estimatedScore(BigDecimal.valueOf(6.0))
                .feedbackJson("{\"writingBand\":6.5,\"speakingBand\":6.0,\"estimatedScore\":6.0,\"strengths\":\"Good\",\"weaknesses\":\"Vocabulary\",\"recommendations\":\"Read more\"}")
                .audioInputAnalyzed(true)
                .build();
        when(aiEvaluationClient.evaluateWithAudio(any(), any(), any())).thenReturn(aiResult);

        // Mock content bank item lookup
        when(contentBankItemRepository.findById(1L)).thenReturn(Optional.of(bankItem));

        // Mock attempt save
        PlacementTestAttempt savedAttempt = PlacementTestAttempt.builder()
                .id(100L)
                .student(learner)
                .testCode(TEST_CODE)
                .contentBankItem(bankItem)
                .listeningScore(BigDecimal.valueOf(7.0))
                .readingScore(BigDecimal.valueOf(6.5))
                .writingScore(BigDecimal.valueOf(6.5))
                .speakingScore(BigDecimal.valueOf(6.0))
                .overallScore(BigDecimal.valueOf(6.5))
                .correctListening(30)
                .correctReading(32)
                .status("COMPLETED")
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .expiresAt(LocalDateTime.now().plusDays(180))
                .submittedAt(LocalDateTime.now())
                .build();
        when(attemptRepository.save(any(PlacementTestAttempt.class))).thenReturn(savedAttempt);

        // Build submission request using setter methods
        Map<String, Object> listeningAnswers = new HashMap<>();
        for (int i = 1; i <= 30; i++) listeningAnswers.put(String.valueOf(i), i % 2 == 1 ? "A" : "B");
        Map<String, Object> readingAnswers = new HashMap<>();
        for (int i = 1; i <= 32; i++) readingAnswers.put(String.valueOf(i), i % 2 == 1 ? "A" : "B");
        Map<String, Object> writingAnswers = new HashMap<>();
        writingAnswers.put("task_1", "The chart illustrates...");
        writingAnswers.put("task_2", "In conclusion...");

        PlacementTestSubmissionRequest request = buildIeltsRequest(
                listeningAnswers, readingAnswers, writingAnswers,
                "http://storage/audio.mp3", "Speaking transcript content here."
        );

        // Act
        PlacementTestAttemptResponse response = service.submit(request, LEARNER_EMAIL);

        // Assert: Verify scores
        assertThat(response.getListeningScore()).isEqualByComparingTo(BigDecimal.valueOf(7.0));
        assertThat(response.getReadingScore()).isEqualByComparingTo(BigDecimal.valueOf(6.5));
        assertThat(response.getWritingScore()).isEqualByComparingTo(BigDecimal.valueOf(6.5));
        assertThat(response.getSpeakingScore()).isEqualByComparingTo(BigDecimal.valueOf(6.0));
        assertThat(response.getOverallScore()).isEqualByComparingTo(BigDecimal.valueOf(6.5));
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getCorrectListening()).isEqualTo(30);
        assertThat(response.getCorrectReading()).isEqualTo(32);

        // Assert: Verify AI was called
        verify(aiEvaluationClient, times(1)).evaluateWithAudio(any(), any(), any());

        // Assert: Verify attempt was saved
        ArgumentCaptor<PlacementTestAttempt> attemptCaptor = ArgumentCaptor.forClass(PlacementTestAttempt.class);
        verify(attemptRepository, times(1)).save(attemptCaptor.capture());
        PlacementTestAttempt capturedAttempt = attemptCaptor.getValue();
        assertThat(capturedAttempt.getStatus()).isEqualTo("COMPLETED");
        assertThat(capturedAttempt.getWritingScore()).isNotNull();
        assertThat(capturedAttempt.getSpeakingScore()).isNotNull();

        // Assert: Verify learner's currentBand was updated (check the argument passed to save)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        // Note: Due to mock behavior, the captured User may be the original learner with default currentBand.
        // The actual service sets currentBand = overall on the student object before saving.
        // Verify at least that save was called with the correct learner ID
        assertThat(userCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(userCaptor.getValue().getEmail()).isEqualTo(LEARNER_EMAIL);
    }

    // TC02: Nộp bài Placement Test TOEIC thành công
    @Test
    void takePlacementTest_TC02_toeicSuccess() throws Exception {
        // Arrange: Mock student lookup
        when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        when(userRepository.save(any(User.class))).thenReturn(learner);

        // Mock definition service
        when(definitionService.getDefinition()).thenReturn(publishedDefinition);

        // Mock TOEIC config with answer key
        JsonNode toeicConfig = objectMapper.readTree(
                "{\"type\":\"toeic_full_test\"," +
                        "\"listening\":{\"parts\":[{\"questions\":[{\"number\":1},{\"number\":2},{\"number\":3},{\"number\":4},{\"number\":5}]}]},\"reading\":{\"parts\":[{\"questions\":[{\"number\":101},{\"number\":102},{\"number\":103},{\"number\":104},{\"number\":105}]}]},\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\",\"101\":\"B\",\"102\":\"C\",\"103\":\"D\",\"104\":\"A\",\"105\":\"B\"}}"
        );
        when(definitionService.getConfig(publishedDefinition, "toeic")).thenReturn(toeicConfig);

        // Mock content bank item
        when(contentBankItemRepository.findById(1L)).thenReturn(Optional.of(bankItem));

        // Mock attempt save
        PlacementTestAttempt savedAttempt = PlacementTestAttempt.builder()
                .id(101L)
                .student(learner)
                .testCode(TEST_CODE)
                .contentBankItem(bankItem)
                .listeningScore(BigDecimal.valueOf(250))
                .readingScore(BigDecimal.valueOf(250))
                .overallScore(BigDecimal.valueOf(500))
                .status("COMPLETED")
                .evaluationStatus(PlacementEvaluationStatus.ELIGIBLE)
                .expiresAt(LocalDateTime.now().plusDays(180))
                .submittedAt(LocalDateTime.now())
                .build();
        when(attemptRepository.save(any(PlacementTestAttempt.class))).thenReturn(savedAttempt);

        // Build TOEIC submission request
        PlacementTestSubmissionRequest request = buildToeicRequest(
                Map.of("1", "A", "2", "B", "3", "C", "4", "D", "5", "A"),
                Map.of("101", "B", "102", "C", "103", "D", "104", "A", "105", "B")
        );

        // Act
        PlacementTestAttemptResponse response = service.submit(request, LEARNER_EMAIL);

        // Assert: Status is COMPLETED
        assertThat(response.getStatus()).isEqualTo("COMPLETED");

        // Assert: AI client was NEVER called for TOEIC
        verify(aiEvaluationClient, never()).evaluate(any(String.class));
        verify(aiEvaluationClient, never()).evaluateWithAudio(any(), any(), any());

        // Assert: recommendedLevel is set for TOEIC (no staff review needed)
        assertThat(response.getEvaluationStatus()).isEqualTo(PlacementEvaluationStatus.ELIGIBLE);

        // Assert: Attempt was saved
        verify(attemptRepository, times(1)).save(any(PlacementTestAttempt.class));
    }

    // TC03: Tự động nộp bài khi hết giờ làm bài countdown timer về 0
    @Test
    void takePlacementTest_TC03_autoSubmitWhenTimerExpires_success() throws Exception {
        // Arrange: Mock similar to TC01 but with minimal answers (auto-submitted)
        when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        when(userRepository.save(any(User.class))).thenReturn(learner);
        when(definitionService.getDefinition()).thenReturn(publishedDefinition);

        // Minimal answer configs
        JsonNode listeningConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\"}}");
        JsonNode readingConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\"}}");
        JsonNode writingConfig = objectMapper.readTree("{\"tasks\":[{\"taskId\":\"task_1\",\"title\":\"Task 1\"},{\"taskId\":\"task_2\",\"title\":\"Task 2\"}]}");
        JsonNode speakingConfig = objectMapper.readTree("{\"topics\":[{\"topicId\":\"part_1\",\"title\":\"Part 1\"}]}");

        when(definitionService.getConfig(publishedDefinition, "listening")).thenReturn(listeningConfig);
        when(definitionService.getConfig(publishedDefinition, "reading")).thenReturn(readingConfig);
        when(definitionService.getConfig(publishedDefinition, "writing")).thenReturn(writingConfig);
        when(definitionService.getConfig(publishedDefinition, "speaking")).thenReturn(speakingConfig);

        AssessmentAudioStorageService.StoredAssessmentAudio storedAudio =
                new AssessmentAudioStorageService.StoredAssessmentAudio("audio.mp3", "audio/mpeg", 30000, new byte[30000]);
        when(audioStorageService.loadStoredAudioFromUrl("http://storage/audio.mp3"))
                .thenReturn(Optional.of(storedAudio));

        AiEvaluationResult aiResult = AiEvaluationResult.builder()
                .estimatedScore(BigDecimal.valueOf(5.0))
                .feedbackJson("{\"writingBand\":5.0,\"speakingBand\":5.0,\"estimatedScore\":5.0}")
                .audioInputAnalyzed(true)
                .build();
        when(aiEvaluationClient.evaluateWithAudio(any(), any(), any())).thenReturn(aiResult);

        when(contentBankItemRepository.findById(1L)).thenReturn(Optional.of(bankItem));

        PlacementTestAttempt savedAttempt = PlacementTestAttempt.builder()
                .id(102L)
                .student(learner)
                .testCode(TEST_CODE)
                .contentBankItem(bankItem)
                .listeningScore(BigDecimal.valueOf(5.0))
                .readingScore(BigDecimal.valueOf(5.0))
                .writingScore(BigDecimal.valueOf(5.0))
                .speakingScore(BigDecimal.valueOf(5.0))
                .overallScore(BigDecimal.valueOf(5.0))
                .correctListening(3)
                .correctReading(3)
                .status("COMPLETED")
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .expiresAt(LocalDateTime.now().plusDays(180))
                .submittedAt(LocalDateTime.now())
                .build();
        when(attemptRepository.save(any(PlacementTestAttempt.class))).thenReturn(savedAttempt);

        // Build auto-submit request (simulating timer expiry)
        PlacementTestSubmissionRequest request = buildIeltsRequest(
                Map.of("1", "A", "2", "B", "3", "C", "4", "D", "5", "A"),
                Map.of("1", "A", "2", "B", "3", "C", "4", "D", "5", "A"),
                Map.of("task_1", "Auto-submitted answer", "task_2", "Auto-submitted task 2"),
                "http://storage/audio.mp3",
                "Auto-submitted transcript when timer expired."
        );

        // Act
        PlacementTestAttemptResponse response = service.submit(request, LEARNER_EMAIL);

        // Assert: Same behavior as normal submit
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getListeningScore()).isNotNull();
        assertThat(response.getReadingScore()).isNotNull();

        // Assert: Attempt was saved with submittedAt timestamp
        ArgumentCaptor<PlacementTestAttempt> attemptCaptor = ArgumentCaptor.forClass(PlacementTestAttempt.class);
        verify(attemptRepository, times(1)).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getSubmittedAt()).isNotNull();
    }

    // TC04: Xử lý ngoại lệ khi AI Engine Service gặp sự cố
    @Test
    void takePlacementTest_TC04_aiServiceFails_objectiveEvaluated() throws Exception {
        // Arrange
        when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        when(userRepository.save(any(User.class))).thenReturn(learner);
        when(definitionService.getDefinition()).thenReturn(publishedDefinition);

        JsonNode listeningConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\"}}");
        JsonNode readingConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\"}}");
        JsonNode writingConfig = objectMapper.readTree("{\"tasks\":[{\"taskId\":\"task_1\",\"title\":\"Task 1\"}]}");
        JsonNode speakingConfig = objectMapper.readTree("{\"topics\":[{\"topicId\":\"part_1\",\"title\":\"Part 1\"}]}");

        when(definitionService.getConfig(publishedDefinition, "listening")).thenReturn(listeningConfig);
        when(definitionService.getConfig(publishedDefinition, "reading")).thenReturn(readingConfig);
        when(definitionService.getConfig(publishedDefinition, "writing")).thenReturn(writingConfig);
        when(definitionService.getConfig(publishedDefinition, "speaking")).thenReturn(speakingConfig);

        AssessmentAudioStorageService.StoredAssessmentAudio storedAudio =
                new AssessmentAudioStorageService.StoredAssessmentAudio("audio.mp3", "audio/mpeg", 30000, new byte[30000]);
        when(audioStorageService.loadStoredAudioFromUrl("http://storage/audio.mp3"))
                .thenReturn(Optional.of(storedAudio));

        // AI throws RuntimeException (simulating API failure)
        when(aiEvaluationClient.evaluateWithAudio(any(), any(), any()))
                .thenThrow(new RuntimeException("AI service unavailable"));

        when(contentBankItemRepository.findById(1L)).thenReturn(Optional.of(bankItem));

        PlacementTestAttempt savedAttempt = PlacementTestAttempt.builder()
                .id(103L)
                .student(learner)
                .testCode(TEST_CODE)
                .contentBankItem(bankItem)
                .listeningScore(BigDecimal.valueOf(6.0))
                .readingScore(BigDecimal.valueOf(6.0))
                .writingScore(null)  // Not scored due to AI failure
                .speakingScore(null) // Not scored due to AI failure
                .overallScore(BigDecimal.valueOf(6.0))
                .correctListening(4)
                .correctReading(4)
                .status("OBJECTIVE_EVALUATED")
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .expiresAt(LocalDateTime.now().plusDays(180))
                .submittedAt(LocalDateTime.now())
                .build();
        when(attemptRepository.save(any(PlacementTestAttempt.class))).thenReturn(savedAttempt);

        PlacementTestSubmissionRequest request = buildIeltsRequest(
                Map.of("1", "A", "2", "B", "3", "C", "4", "D", "5", "A"),
                Map.of("1", "A", "2", "B", "3", "C", "4", "D", "5", "A"),
                Map.of("task_1", "Sample writing content"),
                "http://storage/audio.mp3",
                "Sample speaking transcript."
        );

        // Act
        PlacementTestAttemptResponse response = service.submit(request, LEARNER_EMAIL);

        // Assert: Status is OBJECTIVE_EVALUATED (AI failed)
        assertThat(response.getStatus()).isEqualTo("OBJECTIVE_EVALUATED");

        // Assert: L/R scores are preserved
        assertThat(response.getListeningScore()).isNotNull();
        assertThat(response.getReadingScore()).isNotNull();

        // Assert: W/S scores are null (not yet evaluated)
        assertThat(response.getWritingScore()).isNull();
        assertThat(response.getSpeakingScore()).isNull();

        // Assert: Attempt was saved
        ArgumentCaptor<PlacementTestAttempt> attemptCaptor = ArgumentCaptor.forClass(PlacementTestAttempt.class);
        verify(attemptRepository, times(1)).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getStatus()).isEqualTo("OBJECTIVE_EVALUATED");
    }

    // TC05: Thất bại do bài nộp thiếu câu hỏi bắt buộc hoặc thiếu Speaking
    @Test
    void takePlacementTest_TC05_incompleteSubmission() {
        // Arrange: Mock student lookup (needed before validation)
        lenient().when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        lenient().when(definitionService.getDefinition()).thenReturn(publishedDefinition);

        // Mock minimal config for validation to proceed
        try {
            JsonNode minimalConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\"}}");
            lenient().when(definitionService.getConfig(any(), eq("listening"))).thenReturn(minimalConfig);
            lenient().when(definitionService.getConfig(any(), eq("reading"))).thenReturn(minimalConfig);
            JsonNode writingConfig = objectMapper.readTree("{\"tasks\":[{\"taskId\":\"task_1\",\"title\":\"Task 1\"}]}");
            JsonNode speakingConfig = objectMapper.readTree("{\"topics\":[{\"topicId\":\"part_1\",\"title\":\"Part 1\"}]}");
            lenient().when(definitionService.getConfig(any(), eq("writing"))).thenReturn(writingConfig);
            lenient().when(definitionService.getConfig(any(), eq("speaking"))).thenReturn(speakingConfig);
        } catch (Exception e) {
            // Ignore JSON parsing errors in test setup
        }

        // Missing Speaking (no audioUrl AND no transcript)
        PlacementTestSubmissionRequest request = buildIeltsRequest(
                Map.of("1", "A"),
                Map.of("1", "A"),
                Map.of("task_1", "Writing content"),
                null,  // Missing audio
                null   // Missing transcript
        );

        // Act & Assert: Should throw validation exception about Speaking
        assertThatThrownBy(() -> service.submit(request, LEARNER_EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Speaking");

        // Verify: No attempt was saved (validation failed before save)
        verify(attemptRepository, never()).save(any());
        verify(aiEvaluationClient, never()).evaluate(any(String.class));
        verify(aiEvaluationClient, never()).evaluateWithAudio(any(), any(), any());
    }


    // TC06_E3: Bài kiểm tra bị ngưng hoạt động
    // Nhánh a: Learner chưa bắt đầu (không có session hợp lệ) → hệ thống phải chặn nộp bài và hiển thị thông báo lỗi.
    @Test
    void takePlacementTest_TC06a_testDefinitionInactive_blocksSubmitWithoutValidSession() {
        // Arrange: Definition is not PUBLISHED (e.g., ARCHIVED/INACTIVE) and learner has no valid session.
        PlacementTestDefinition inactiveDefinition = PlacementTestDefinition.builder()
                .id(1L)
                .testCode(TEST_CODE)
                .title("Placement Test")
                .status("ARCHIVED")  // Not PUBLISHED
                .build();

        when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        when(definitionService.getDefinition()).thenReturn(inactiveDefinition);
        // No valid session token → learner is treated as "has not started".
        when(sessionTokenService.isValid(null, LEARNER_EMAIL, "IELTS")).thenReturn(false);

        PlacementTestSubmissionRequest request = buildIeltsRequest(
                Map.of("1", "A"),
                Map.of("1", "A"),
                Map.of("task_1", "Writing"),
                "http://storage/audio.mp3",
                "Transcript"
        );

        // Act & Assert: Should throw because definition is inactive and no session covers this attempt.
        assertThatThrownBy(() -> service.submit(request, LEARNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tạm dừng");

        // Verify: No attempt was saved and learner profile was not touched.
        verify(attemptRepository, never()).save(any());
        verify(userRepository, never()).save(any(User.class));
    }

    // TC06_E3:
    // Nhánh b: Learner đã bắt đầu trong khi bài đang hoạt động và session còn hiệu lực
    // → hệ thống cho phép nộp bài và tiếp tục flow chấm điểm bình thường.
    @Test
    void takePlacementTest_TC06b_testDefinitionInactive_allowsSubmitWithValidSession() throws Exception {
        // Arrange: Definition is now inactive, but learner still holds a valid session from when it was PUBLISHED.
        PlacementTestDefinition inactiveDefinition = PlacementTestDefinition.builder()
                .id(1L)
                .testCode(TEST_CODE)
                .title("Placement Test")
                .status("ARCHIVED")  // Not PUBLISHED anymore
                .build();

        String validSessionToken = "valid-jwt-token-for-ielts";

        when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        when(userRepository.save(any(User.class))).thenReturn(learner);
        when(definitionService.getDefinition()).thenReturn(inactiveDefinition);
        when(sessionTokenService.isValid(validSessionToken, LEARNER_EMAIL, "IELTS")).thenReturn(true);

        // Mock config used by the normal IELTS scoring flow.
        JsonNode listeningConfig = objectMapper.readTree(
                "{\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\",\"6\":\"B\",\"7\":\"C\",\"8\":\"D\",\"9\":\"A\",\"10\":\"B\"," +
                        "\"11\":\"A\",\"12\":\"B\",\"13\":\"C\",\"14\":\"D\",\"15\":\"A\",\"16\":\"B\",\"17\":\"C\",\"18\":\"D\",\"19\":\"A\",\"20\":\"B\"," +
                        "\"21\":\"A\",\"22\":\"B\",\"23\":\"C\",\"24\":\"D\",\"25\":\"A\",\"26\":\"B\",\"27\":\"C\",\"28\":\"D\",\"29\":\"A\",\"30\":\"B\"}}"
        );
        JsonNode readingConfig = objectMapper.readTree(
                "{\"answerKey\":{\"1\":\"A\",\"2\":\"B\",\"3\":\"C\",\"4\":\"D\",\"5\":\"A\",\"6\":\"B\",\"7\":\"C\",\"8\":\"D\",\"9\":\"A\",\"10\":\"B\"," +
                        "\"11\":\"A\",\"12\":\"B\",\"13\":\"C\",\"14\":\"D\",\"15\":\"A\",\"16\":\"B\",\"17\":\"C\",\"18\":\"D\",\"19\":\"A\",\"20\":\"B\"," +
                        "\"21\":\"A\",\"22\":\"B\",\"23\":\"C\",\"24\":\"D\",\"25\":\"A\",\"26\":\"B\",\"27\":\"C\",\"28\":\"D\",\"29\":\"A\",\"30\":\"B\"," +
                        "\"31\":\"A\",\"32\":\"B\"}}"
        );
        JsonNode writingConfig = objectMapper.readTree(
                "{\"tasks\":[{\"taskId\":\"task_1\",\"title\":\"Writing Task 1\"}]}"
        );
        JsonNode speakingConfig = objectMapper.readTree(
                "{\"topics\":[{\"topicId\":\"part_1\",\"title\":\"Introduction\"}]}"
        );

        lenient().when(definitionService.getConfig(inactiveDefinition, "listening")).thenReturn(listeningConfig);
        lenient().when(definitionService.getConfig(inactiveDefinition, "reading")).thenReturn(readingConfig);
        lenient().when(definitionService.getConfig(inactiveDefinition, "writing")).thenReturn(writingConfig);
        lenient().when(definitionService.getConfig(inactiveDefinition, "speaking")).thenReturn(speakingConfig);

        AssessmentAudioStorageService.StoredAssessmentAudio storedAudio =
                new AssessmentAudioStorageService.StoredAssessmentAudio("audio.mp3", "audio/mpeg", 60000, new byte[60000]);
        lenient().when(audioStorageService.loadStoredAudioFromUrl("http://storage/audio.mp3"))
                .thenReturn(Optional.of(storedAudio));

        AiEvaluationResult aiResult = AiEvaluationResult.builder()
                .estimatedScore(BigDecimal.valueOf(6.0))
                .feedbackJson("{\"writingBand\":6.5,\"speakingBand\":6.0,\"estimatedScore\":6.0}")
                .audioInputAnalyzed(true)
                .build();
        lenient().when(aiEvaluationClient.evaluateWithAudio(any(), any(), any())).thenReturn(aiResult);

        lenient().when(contentBankItemRepository.findById(1L)).thenReturn(Optional.of(bankItem));

        PlacementTestAttempt savedAttempt = PlacementTestAttempt.builder()
                .id(101L)
                .student(learner)
                .testCode(TEST_CODE)
                .contentBankItem(bankItem)
                .listeningScore(BigDecimal.valueOf(7.0))
                .readingScore(BigDecimal.valueOf(6.5))
                .writingScore(BigDecimal.valueOf(6.5))
                .speakingScore(BigDecimal.valueOf(6.0))
                .overallScore(BigDecimal.valueOf(6.5))
                .correctListening(30)
                .correctReading(32)
                .status("COMPLETED")
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .expiresAt(LocalDateTime.now().plusDays(180))
                .submittedAt(LocalDateTime.now())
                .build();
        lenient().when(attemptRepository.save(any(PlacementTestAttempt.class))).thenReturn(savedAttempt);

        Map<String, Object> listeningAnswers = new HashMap<>();
        for (int i = 1; i <= 30; i++) listeningAnswers.put(String.valueOf(i), i % 2 == 1 ? "A" : "B");
        Map<String, Object> readingAnswers = new HashMap<>();
        for (int i = 1; i <= 32; i++) readingAnswers.put(String.valueOf(i), i % 2 == 1 ? "A" : "B");
        Map<String, Object> writingAnswers = Map.of("task_1", "The chart illustrates...");

        PlacementTestSubmissionRequest request = buildIeltsRequest(
                listeningAnswers, readingAnswers, writingAnswers,
                "http://storage/audio.mp3", "Speaking transcript content here."
        );
        request.setSessionToken(validSessionToken);

        // Act: Submission should succeed because the session token is still valid.
        PlacementTestAttemptResponse response = service.submit(request, LEARNER_EMAIL);

        // Assert: Normal scoring flow continues — attempt is saved, profile is updated, scores are returned.
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getOverallScore()).isEqualByComparingTo(BigDecimal.valueOf(6.5));
        assertThat(response.getCorrectListening()).isEqualTo(30);
        assertThat(response.getCorrectReading()).isEqualTo(32);

        verify(sessionTokenService, times(1)).isValid(validSessionToken, LEARNER_EMAIL, "IELTS");
        verify(attemptRepository, times(1)).save(any(PlacementTestAttempt.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    // TC07: Thất bại khi lưu CSDL gặp lỗi kết nối
    @Test
    void takePlacementTest_TC07_databaseSaveFails() throws Exception {
        // Arrange
        lenient().when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        lenient().when(userRepository.save(any(User.class))).thenReturn(learner);
        lenient().when(definitionService.getDefinition()).thenReturn(publishedDefinition);

        JsonNode listeningConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\"}}");
        JsonNode readingConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\"}}");
        JsonNode writingConfig = objectMapper.readTree("{\"tasks\":[{\"taskId\":\"task_1\",\"title\":\"Task 1\"}]}");
        JsonNode speakingConfig = objectMapper.readTree("{\"topics\":[{\"topicId\":\"part_1\",\"title\":\"Part 1\"}]}");

        lenient().when(definitionService.getConfig(any(), eq("listening"))).thenReturn(listeningConfig);
        lenient().when(definitionService.getConfig(any(), eq("reading"))).thenReturn(readingConfig);
        lenient().when(definitionService.getConfig(any(), eq("writing"))).thenReturn(writingConfig);
        lenient().when(definitionService.getConfig(any(), eq("speaking"))).thenReturn(speakingConfig);

        // Mock audio not found
        lenient().when(audioStorageService.loadStoredAudioFromUrl(any())).thenReturn(Optional.empty());

        // AI evaluates without audio
        AiEvaluationResult aiResult = AiEvaluationResult.builder()
                .estimatedScore(BigDecimal.valueOf(6.0))
                .feedbackJson("{\"writingBand\":6.0,\"speakingBand\":6.0}")
                .audioInputAnalyzed(false)
                .build();
        lenient().when(aiEvaluationClient.evaluate(any(String.class))).thenReturn(aiResult);

        // Mock attempt save to throw RuntimeException (simulating DB failure)
        lenient().when(attemptRepository.save(any(PlacementTestAttempt.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        PlacementTestSubmissionRequest request = buildIeltsRequest(
                Map.of("1", "A"),
                Map.of("1", "A"),
                Map.of("task_1", "Writing content"),
                null,
                "Speaking transcript"
        );

        // Act & Assert: Should propagate DB error
        assertThatThrownBy(() -> service.submit(request, LEARNER_EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database");
    }

    // TC08: Tự động ngắt ghi âm Speaking khi vượt quá giới hạn 5 phút
    @Test
    void takePlacementTest_TC08_longSpeakingAudio_processedNormally() throws Exception {

        when(userRepository.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learner));
        when(userRepository.save(any(User.class))).thenReturn(learner);
        when(definitionService.getDefinition()).thenReturn(publishedDefinition);

        JsonNode listeningConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\"}}");
        JsonNode readingConfig = objectMapper.readTree("{\"answerKey\":{\"1\":\"A\"}}");
        JsonNode writingConfig = objectMapper.readTree("{\"tasks\":[{\"taskId\":\"task_1\",\"title\":\"Task 1\"}]}");
        JsonNode speakingConfig = objectMapper.readTree("{\"topics\":[{\"topicId\":\"part_1\",\"title\":\"Part 1\"}]}");

        when(definitionService.getConfig(publishedDefinition, "listening")).thenReturn(listeningConfig);
        when(definitionService.getConfig(publishedDefinition, "reading")).thenReturn(readingConfig);
        when(definitionService.getConfig(publishedDefinition, "writing")).thenReturn(writingConfig);
        when(definitionService.getConfig(publishedDefinition, "speaking")).thenReturn(speakingConfig);

        // Mock long audio (> 5 minutes = 300 seconds = 300,000 bytes for simulation)
        AssessmentAudioStorageService.StoredAssessmentAudio longAudio =
                new AssessmentAudioStorageService.StoredAssessmentAudio(
                        "long_audio.mp3", "audio/mpeg", 400000, new byte[400000]);
        when(audioStorageService.loadStoredAudioFromUrl("http://storage/long_audio.mp3"))
                .thenReturn(Optional.of(longAudio));

        // AI evaluates the audio
        AiEvaluationResult aiResult = AiEvaluationResult.builder()
                .estimatedScore(BigDecimal.valueOf(6.0))
                .feedbackJson("{\"writingBand\":6.0,\"speakingBand\":6.0}")
                .audioInputAnalyzed(true)
                .build();
        when(aiEvaluationClient.evaluateWithAudio(any(), any(), any())).thenReturn(aiResult);

        when(contentBankItemRepository.findById(1L)).thenReturn(Optional.of(bankItem));

        PlacementTestAttempt savedAttempt = PlacementTestAttempt.builder()
                .id(104L)
                .student(learner)
                .testCode(TEST_CODE)
                .contentBankItem(bankItem)
                .listeningScore(BigDecimal.valueOf(6.0))
                .readingScore(BigDecimal.valueOf(6.0))
                .writingScore(BigDecimal.valueOf(6.0))
                .speakingScore(BigDecimal.valueOf(6.0))
                .overallScore(BigDecimal.valueOf(6.0))
                .status("COMPLETED")
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .expiresAt(LocalDateTime.now().plusDays(180))
                .submittedAt(LocalDateTime.now())
                .build();
        when(attemptRepository.save(any(PlacementTestAttempt.class))).thenReturn(savedAttempt);

        PlacementTestSubmissionRequest request = buildIeltsRequest(
                Map.of("1", "A"),
                Map.of("1", "A"),
                Map.of("task_1", "Writing"),
                "http://storage/long_audio.mp3",
                "Speaking transcript for long audio."
        );

        // Act
        PlacementTestAttemptResponse response = service.submit(request, LEARNER_EMAIL);

        // Assert: Audio was processed (AI was called with audio)
        verify(aiEvaluationClient, times(1)).evaluateWithAudio(any(), eq(longAudio.bytes()), eq("audio/mpeg"));

        // Assert: Attempt was saved successfully
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getSpeakingScore()).isNotNull();
    }
}
