package fu.sep490.g23.backend.ut.learningExperienceService;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.dto.response.commerce.CommerceCourseItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestDefinition;
import fu.sep490.g23.backend.entity.commerce.CartItem;
import fu.sep490.g23.backend.entity.commerce.WishlistItem;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.commerce.CartItemRepository;
import fu.sep490.g23.backend.repository.commerce.WishlistItemRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.assessment.impl.PlacementTestServiceImpl;
import fu.sep490.g23.backend.service.commerce.impl.StudentCommerceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link PlacementTestServiceImpl}.
 * - Case 1: Khß╗ƒi tß║ío b├ái thi ─æ├ính gi├í ─æß║ºu v├áo - PRE-3 (kiß╗âm tra thiß║┐t bß╗ï ─æ├ú ho├án th├ánh).
 * - Case 2: Nß╗Öp b├ái thi & chß║Ñm ─æiß╗âm th├ánh c├┤ng - AI Engine hoß║ít ─æß╗Öng b├¼nh th╞░ß╗¥ng.
 * - Case 3: Tß╗▒ ─æß╗Öng nß╗Öp b├ái khi hß║┐t giß╗¥ (isExpired = true) - d├╣ng saved_draft_answers[].
 * - Case 4: Nß╗Öp b├ái thß║Ñt bß║íi do AI Engine Service bß╗ï sß║¡p - graceful fallback objective-only.
 */
@ExtendWith(MockitoExtension.class)
class TakePlacementTestTest {

    @Mock private UserRepository userRepository;
    @Mock private PlacementTestAttemptRepository attemptRepository;
    @Mock private AiEvaluationClient aiEvaluationClient;
    @Mock private AssessmentAudioStorageService audioStorageService;
    @Mock private PlacementTestDefinitionService definitionService;

    private PlacementTestServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User learner;
    private String learnerEmail;
    private PlacementTestDefinition activeDefinition;

    // Text fixtures d├╣ng chung (─æß╗º d├ái + on-topic ─æß╗â v╞░ß╗út guard)
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

        // answerKey: 5 c├óu Listening + 5 c├óu Reading (─æß╗º ─æß╗â v╞░ß╗út band 2.5/3.0)
        String listeningConfig = "{\"answerKey\":{\"q1\":\"A\",\"q2\":\"B\",\"q3\":\"C\",\"q4\":\"D\",\"q5\":\"E\"}}";
        String readingConfig   = "{\"answerKey\":{\"q1\":\"true\",\"q2\":\"false\",\"q3\":\"true\",\"q4\":\"false\",\"q5\":\"true\"}}";
        String writingConfig   = "{\"tasks\":[{\"id\":\"task_1\"},{\"id\":\"task_2\"}]}";
        String speakingConfig  = "{\"parts\":[{\"id\":\"part_1\"}]}";
        String toeicConfig     = "{\"listening\":{\"parts\":[]},\"reading\":{\"parts\":[]}}";

        activeDefinition = PlacementTestDefinition.builder()
                .id(10L)
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .title("B├ái ─æ├ính gi├í ─æß║ºu v├áo IELTS")
                .description("Mß╗Öt phi├¬n ─æ├ính gi├í gß╗ôm Nghe, ─Éß╗ìc, Viß║┐t v├á N├│i ─æß╗â gß╗úi ├╜ ─æiß╗âm bß║»t ─æß║ºu ph├╣ hß╗úp.")
                .examType("IELTS")
                .maxAttempts(3)
                .active(true)
                .listeningConfigJson(listeningConfig)
                .readingConfigJson(readingConfig)
                .writingConfigJson(writingConfig)
                .speakingConfigJson(speakingConfig)
                .toeicConfigJson(toeicConfig)
                .updatedAt(java.time.LocalDateTime.now())
                .build();

        lenient().when(userRepository.findByEmail(learnerEmail)).thenReturn(Optional.of(learner));
        lenient().when(definitionService.getDefinition()).thenReturn(activeDefinition);

        service = new PlacementTestServiceImpl(
                userRepository, attemptRepository, aiEvaluationClient,
                audioStorageService, definitionService);
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

        // getConfig trß║ú JsonNode ß╗⌐ng vß╗¢i mß╗ùi kß╗╣ n─âng (bß╗Å answerKey cho objective sections)
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

        // Bß║úo mß║¡t: listening/reading/toeic KH├öNG lß╗Ö answerKey
        assertThat((Map<String, Object>) sections.get("listening")).doesNotContainKey("answerKey");
        assertThat((Map<String, Object>) sections.get("reading")).doesNotContainKey("answerKey");
        assertThat((Map<String, Object>) sections.get("toeic")).doesNotContainKey("answerKey");

        assertThat(response).doesNotContainKey("latestAttempt");
    }

    // =================================================================
    // Case 2: Nß╗Öp b├ái thi & chß║Ñm ─æiß╗âm th├ánh c├┤ng
    // PRE: l╞░ß╗út l├ám b├ái ─æang IN_PROGRESS, AI Engine Service hoß║ít ─æß╗Öng b├¼nh th╞░ß╗¥ng
    // Input: attempt_id (learner + request), danh s├ích c├óu trß║ú lß╗¥i answers[]
    // =================================================================
    @Test
    void submitPlacementTest_AllAnswersProvided_GradingSuccessful() throws Exception {
        // Arrange - Listening/Reading: 5/5 ─æ├║ng ΓåÆ Listening 3.0, Reading 2.5
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
    // Case 3: Tß╗▒ ─æß╗Öng nß╗Öp b├ái khi hß║┐t giß╗¥ (isExpired = true)
    // PRE: Thß╗¥i gian ─æß║┐m ng╞░ß╗úc chß║ím mß╗æc 0
    // Input: attempt_id, saved_draft_answers[] (chß╗ë mß╗Öt phß║ºn, do hß║┐t giß╗¥ tr╞░ß╗¢c khi ho├án tß║Ñt)
    // Hß╗ç thß╗æng ├⌐p buß╗Öc submit, chß║Ñm ─æiß╗âm dß╗▒a tr├¬n draft ─æ├ú l╞░u,
    // trß║ú vß╗ü kß║┐t quß║ú k├¿m message "─É├ú hß║┐t giß╗¥! B├ái l├ám cß╗ºa bß║ín ─æ├ú tß╗▒ ─æß╗Öng nß╗Öp."
    // =================================================================
    @Test
    void submitPlacementTest_TimeExpired_AutoSubmitWithSavedDraft() throws Exception {
        // Arrange: saved_draft_answers[] - chß╗ë c├│ 3/5 c├óu listening + 2/5 c├óu reading (do hß║┐t giß╗¥)
        Map<String, Object> savedDraftListening = new LinkedHashMap<>();
        savedDraftListening.put("q1", "A");
        savedDraftListening.put("q2", "B");
        savedDraftListening.put("q3", "C");
        // q4, q5 ch╞░a kß╗ïp trß║ú lß╗¥i ΓåÆ null trong answerKey (kh├┤ng t├¡nh)

        Map<String, Object> savedDraftReading = new LinkedHashMap<>();
        savedDraftReading.put("q1", "true");
        savedDraftReading.put("q2", "false");
        // q3, q4, q5 ch╞░a trß║ú lß╗¥i

        // Writing/Speaking rß╗ùng v├¼ hß║┐t giß╗¥ tr╞░ß╗¢c khi l├ám
        Map<String, Object> emptyWriting = new LinkedHashMap<>();
        // empty ΓåÆ guard set writingBand = null

        Map<String, Object> deviceCheck = new LinkedHashMap<>();
        deviceCheck.put("completed", true);
        deviceCheck.put("autoSubmit", true);  // ─æ├ính dß║Ñu auto-submit

        PlacementTestSubmissionRequest request = new PlacementTestSubmissionRequest();
        request.setExamType("IELTS");
        request.setListeningAnswers(savedDraftListening);
        request.setReadingAnswers(savedDraftReading);
        request.setWritingAnswers(emptyWriting);
        request.setSpeakingTranscript("I am from Hanoi."); // ngß║»n ΓåÆ kh├┤ng ─æß╗º evidence chß║Ñm speaking
        request.setDeviceCheck(deviceCheck);

        when(attemptRepository.countByStudentAndTestCode(learner, PlacementTestDefinitionService.TEST_CODE))
                .thenReturn(0L);

        // AI Engine Service hoß║ít ─æß╗Öng b├¼nh th╞░ß╗¥ng, trß║ú feedback ─æ╞ín giß║ún
        AiEvaluationResult aiResult = AiEvaluationResult.builder()
                .estimatedScore(new BigDecimal("5.0"))
                .feedbackJson("{\"estimatedScore\":5.0,\"writingBand\":null,\"speakingBand\":null," +
                        "\"message\":\"─É├ú hß║┐t giß╗¥! B├ái l├ám cß╗ºa bß║ín ─æ├ú tß╗▒ ─æß╗Öng nß╗Öp.\"}")
                .provider("mock-ai")
                .audioInputAnalyzed(false)
                .build();
        when(aiEvaluationClient.evaluate(anyString())).thenReturn(aiResult);

        stubAllConfigs();

        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PlacementTestAttemptResponse response = service.submit(request, learnerEmail);

        // Assert - hß╗ç thß╗æng vß║½n ├⌐p buß╗Öc submit th├ánh c├┤ng vß╗¢i saved_draft_answers[]
        assertThat(response.getLearnerId()).isEqualTo(learner.getId());
        assertThat(response.getCorrectListening()).isEqualTo(3);
        assertThat(response.getListeningScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(response.getCorrectReading()).isEqualTo(2);
        assertThat(response.getReadingScore()).isEqualByComparingTo(new BigDecimal("0"));
        assertThat(response.getWritingScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(response.getSpeakingScore()).isNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAiFeedbackJson()).contains("─É├ú hß║┐t giß╗¥").contains("tß╗▒ ─æß╗Öng nß╗Öp");
        assertThat(response.getSubmittedAt()).isNotNull();
        assertThat(response.getExpiresAt())
                .isCloseTo(response.getSubmittedAt().plusDays(180), within(5, ChronoUnit.SECONDS));
    }

    // =================================================================
    // Case 4: Nß╗Öp b├ái thß║Ñt bß║íi do AI Engine Service bß╗ï sß║¡p
    // PRE: AI Engine throw RuntimeException ΓåÆ service fallback objective-only
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

        // AI Engine Service Bß╗è Sß║¼P
        doThrow(new RuntimeException("AI Engine Service unavailable: connection timeout"))
                .when(aiEvaluationClient).evaluate(anyString());

        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act - service kh├┤ng throw ra ngo├ái
        PlacementTestAttemptResponse response = service.submit(request, learnerEmail);

        // Assert - Listening/Reading chß║Ñm b├¼nh th╞░ß╗¥ng, Writing/Speaking = null
        assertThat(response).isNotNull();
        assertThat(response.getCorrectListening()).isEqualTo(5);
        assertThat(response.getCorrectReading()).isEqualTo(5);
        assertThat(response.getListeningScore()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(response.getReadingScore()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(response.getWritingScore()).isNull();
        assertThat(response.getSpeakingScore()).isNull();
        // overall = (3.0 + 2.5) / 2 = 2.75 ΓåÆ round half-band = 3.0
        assertThat(response.getOverallScore()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(response.getStatus()).isEqualTo("OBJECTIVE_EVALUATED");
        // Bß║ún ghi vß║½n l╞░u ΓåÆ learner thß╗¡ lß║íi ─æ╞░ß╗úc
        assertThat(response.getSubmittedAt()).isNotNull();
        assertThat(response.getExpiresAt())
                .isCloseTo(response.getSubmittedAt().plusDays(180), within(5, ChronoUnit.SECONDS));
        assertThat(response.getAiFeedbackJson()).contains("message");
    }

    @ExtendWith(MockitoExtension.class)
    public static class AddCourseToCartTest {

        @Mock
        private CartItemRepository cartItemRepository;

        @Mock
        private WishlistItemRepository wishlistItemRepository;

        @Mock
        private OnlineCourseRepository onlineCourseRepository;

        @Mock
        private PackageEnrollmentRepository packageEnrollmentRepository;

        @Mock
        private UserRepository userRepository;

        private StudentCommerceServiceImpl service;

        // ---- Mock data ----
        private User student;
        private OnlineCourse course;
        private LearningPackage learningPackage;

        @BeforeEach
        void setUp() {
            service = new StudentCommerceServiceImpl(
                    cartItemRepository,
                    wishlistItemRepository,
                    onlineCourseRepository,
                    packageEnrollmentRepository,
                    userRepository
            );

            student = User.builder()
                    .id(1L)
                    .email("learner@englishlab.com")
                    .fullName("Nguyen Van A")
                    .build();

            learningPackage = LearningPackage.builder()
                    .id(10L)
                    .title("IELTS Foundation Course")
                    .slug("ielts-foundation")
                    .price(BigDecimal.valueOf(990000))
                    .salePrice(BigDecimal.valueOf(790000))
                    .duration("3 thang")
                    .status(PackageStatus.PUBLISHED)
                    .deleted(false)
                    .build();

            course = OnlineCourse.builder()
                    .id(100L)
                    .learningPackage(learningPackage)
                    .level(CourseLevel.BEGINNER)
                    .totalLessons(40)
                    .totalHours(60)
                    .modules(new ArrayList<>())
                    .build();
        }

        @Test
        void addCourseToCart_Success() {
            // Arrange - mock data cho tß║Ñt cß║ú dependency
            when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
            when(onlineCourseRepository.findById(course.getId())).thenReturn(Optional.of(course));
            // learner ch╞░a sß╗ƒ hß╗»u kh├│a hß╗ìc (cß║ú 2 lß║ºn gß╗ìi trong service ─æß╗üu trß║ú empty)
            when(packageEnrollmentRepository.findByStudentAndLearningPackage(student, learningPackage))
                    .thenReturn(Optional.empty());
            // ch╞░a c├│ trong giß╗Å h├áng
            when(cartItemRepository.findByStudentAndOnlineCourseId(student, course.getId()))
                    .thenReturn(Optional.empty());

            // mock h├ánh vi save() trß║ú vß╗ü CartItem c├│ id v├á addedAt
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
                CartItem toSave = invocation.getArgument(0);
                toSave.setId(500L);
                toSave.setAddedAt(LocalDateTime.of(2026, 8, 10, 21, 0));
                return toSave;
            });

            // Act
            CommerceCourseItemResponse response = service.addToCart(course.getId(), student.getEmail());

            // Assert - response trß║ú vß╗ü ─æ├║ng th├┤ng tin kh├│a hß╗ìc
            assertNotNull(response);
            assertEquals(course.getId(), response.getId());
            assertEquals(learningPackage.getSlug(), response.getSlug());
            assertEquals(learningPackage.getTitle(), response.getTitle());
            assertEquals(PackageStatus.PUBLISHED.name(), response.getStatus());
            assertEquals(BigDecimal.valueOf(790000), response.getSalePrice());
            assertEquals(BigDecimal.valueOf(990000), response.getOriginalPrice());
            assertNotNull(response.getAddedAt());

            // Assert - cart item ─æ╞░ß╗úc l╞░u ─æ├║ng student + course
            ArgumentCaptor<CartItem> cartCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(cartCaptor.capture());
            CartItem savedItem = cartCaptor.getValue();
            assertEquals(student, savedItem.getStudent());
            assertEquals(course, savedItem.getOnlineCourse());

            // Assert - wishlist item cß╗ºa c├╣ng kh├│a hß╗ìc phß║úi bß╗ï x├│a
            verify(wishlistItemRepository, times(1))
                    .deleteByStudentAndOnlineCourseId(student, course.getId());

            // Kh├┤ng save wishlist mß╗¢i
            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        }

        /**
         * Mß╗Ñc ─æ├¡ch: ─Éß║úm bß║úo khi kh├│a hß╗ìc ─æ├ú tß╗ôn tß║íi trong giß╗Å h├áng th├¼ service n├⌐m
         * RuntimeException vß╗¢i message "Kh├│a hß╗ìc ─æ├ú c├│ trong giß╗Å h├áng." v├á KH├öNG l╞░u
         * cart/wishlist mß╗¢i.
         */
        @Test
        void addCourseToCart_AlreadyInCart() {
            // Arrange - mock data cho dependency
            when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
            when(onlineCourseRepository.findById(course.getId())).thenReturn(Optional.of(course));
            // learner ch╞░a sß╗ƒ hß╗»u kh├│a hß╗ìc (pass qua assertNotEnrolled)
            when(packageEnrollmentRepository.findByStudentAndLearningPackage(student, learningPackage))
                    .thenReturn(Optional.empty());
            // KH├ôA Hß╗îC ─É├â C├ô TRONG GIß╗Ä H├ÇNG - ─æ├óy l├á ─æiß╗üu kiß╗çn k├¡ch hoß║ít exception
            CartItem existingCartItem = CartItem.builder()
                    .id(500L)
                    .student(student)
                    .onlineCourse(course)
                    .addedAt(LocalDateTime.of(2026, 8, 9, 10, 0))
                    .build();
            when(cartItemRepository.findByStudentAndOnlineCourseId(student, course.getId()))
                    .thenReturn(Optional.of(existingCartItem));

            // Act & Assert - phß║úi n├⌐m RuntimeException vß╗¢i ─æ├║ng message tiß║┐ng Viß╗çt
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> service.addToCart(course.getId(), student.getEmail()));

            assertEquals("Kh├│a hß╗ìc ─æ├ú c├│ trong giß╗Å h├áng.", exception.getMessage());

            // Assert - KH├öNG ─æ╞░ß╗úc l╞░u cart item mß╗¢i
            verify(cartItemRepository, never()).save(any(CartItem.class));

            // Assert - KH├öNG ─æ╞░ß╗úc x├│a wishlist (v├¼ ch╞░a tß╗¢i b╞░ß╗¢c ─æ├│)
            verify(wishlistItemRepository, never())
                    .deleteByStudentAndOnlineCourseId(student, course.getId());

            // Assert - KH├öNG ─æ╞░ß╗úc tß║ío wishlist mß╗¢i
            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        }

        /**
         * Mß╗Ñc ─æ├¡ch: Hß╗ìc vi├¬n ─æ├ú ─æ─âng k├╜/sß╗ƒ hß╗»u kh├│a hß╗ìc n├áy (PackageEnrollment status = ACTIVE/COMPLETED)
         * th├¼ service phß║úi n├⌐m RuntimeException vß╗¢i message "Bß║ín ─æ├ú sß╗ƒ hß╗»u kh├│a hß╗ìc n├áy."
         * v├á KH├öNG thß╗▒c hiß╗çn bß║Ñt kß╗│ thao t├íc l╞░u cart/wishlist n├áo.
         */
        @Test
        void addCourseToCart_AlreadyEnrolled() {
            // Arrange - mock data cho dependency
            when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
            when(onlineCourseRepository.findById(course.getId())).thenReturn(Optional.of(course));

            // Hß╗ìc vi├¬n ─É├â Sß╗₧ Hß╗«U kh├│a hß╗ìc (PackageEnrollment status = ACTIVE) - ─æ├óy l├á ─æiß╗üu kiß╗çn k├¡ch hoß║ít exception
            PackageEnrollment activeEnrollment = PackageEnrollment.builder()
                    .id(999L)
                    .student(student)
                    .learningPackage(learningPackage)
                    .status(EnrollmentStatus.ACTIVE)
                    .progressPercent(50)
                    .registeredAt(LocalDateTime.of(2026, 7, 1, 9, 0))
                    .build();
            // cß║ú 2 lß║ºn gß╗ìi trong service (assertNotEnrolled + isRegistered) ─æß╗üu trß║ú vß╗ü enrollment ACTIVE
            when(packageEnrollmentRepository.findByStudentAndLearningPackage(student, learningPackage))
                    .thenReturn(Optional.of(activeEnrollment));

            // Act & Assert - phß║úi n├⌐m RuntimeException vß╗¢i ─æ├║ng message tiß║┐ng Viß╗çt
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> service.addToCart(course.getId(), student.getEmail()));

            assertEquals("Bß║ín ─æ├ú sß╗ƒ hß╗»u kh├│a hß╗ìc n├áy.", exception.getMessage());

            // Assert - KH├öNG ─æ╞░ß╗úc t├¼m cart item
            verify(cartItemRepository, never())
                    .findByStudentAndOnlineCourseId(any(User.class), any(Long.class));

            // Assert - KH├öNG ─æ╞░ß╗úc l╞░u cart item mß╗¢i
            verify(cartItemRepository, never()).save(any(CartItem.class));

            // Assert - KH├öNG ─æ╞░ß╗úc x├│a wishlist
            verify(wishlistItemRepository, never())
                    .deleteByStudentAndOnlineCourseId(any(User.class), any(Long.class));

            // Assert - KH├öNG ─æ╞░ß╗úc tß║ío wishlist mß╗¢i
            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        }

        /**
         * Mß╗Ñc ─æ├¡ch: Kh├│a hß╗ìc ch╞░a ─æ╞░ß╗úc xuß║Ñt bß║ún (LearningPackage status != PUBLISHED)
         * th├¼ service phß║úi n├⌐m RuntimeException vß╗¢i message "Kh├│a hß╗ìc ch╞░a ─æ╞░ß╗úc xuß║Ñt bß║ún."
         * v├á KH├öNG thß╗▒c hiß╗çn bß║Ñt kß╗│ thao t├íc l╞░u cart/wishlist n├áo.
         */
        @Test
        void addCourseToCart_CourseNotPublished() {
            // Arrange - tß║ío mß╗Öt kh├│a hß╗ìc vß╗¢i LearningPackage status = DRAFT (ch╞░a xuß║Ñt bß║ún)
            LearningPackage draftPackage = LearningPackage.builder()
                    .id(20L)
                    .title("IELTS Advanced Course")
                    .slug("ielts-advanced")
                    .price(BigDecimal.valueOf(1290000))
                    .salePrice(BigDecimal.valueOf(990000))
                    .duration("4 thang")
                    .status(PackageStatus.DRAFT)   // <-- ch╞░a xuß║Ñt bß║ún
                    .deleted(false)
                    .build();

            OnlineCourse draftCourse = OnlineCourse.builder()
                    .id(200L)
                    .learningPackage(draftPackage)
                    .level(CourseLevel.ADVANCED)
                    .totalLessons(60)
                    .totalHours(80)
                    .modules(new ArrayList<>())
                    .build();

            // mock data cho dependency
            when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
            when(onlineCourseRepository.findById(draftCourse.getId())).thenReturn(Optional.of(draftCourse));

            // Act & Assert - phß║úi n├⌐m RuntimeException vß╗¢i ─æ├║ng message tiß║┐ng Viß╗çt
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> service.addToCart(draftCourse.getId(), student.getEmail()));

            assertEquals("Kh├│a hß╗ìc ch╞░a ─æ╞░ß╗úc xuß║Ñt bß║ún.", exception.getMessage());

            // Assert - KH├öNG ─æ╞░ß╗úc kiß╗âm tra enrollment (v├¼ dß╗½ng ß╗ƒ requireVisibleCourse)
            verify(packageEnrollmentRepository, never())
                    .findByStudentAndLearningPackage(any(User.class), any(LearningPackage.class));

            // Assert - KH├öNG ─æ╞░ß╗úc t├¼m cart item
            verify(cartItemRepository, never())
                    .findByStudentAndOnlineCourseId(any(User.class), any(Long.class));

            // Assert - KH├öNG ─æ╞░ß╗úc l╞░u cart item mß╗¢i
            verify(cartItemRepository, never()).save(any(CartItem.class));

            // Assert - KH├öNG ─æ╞░ß╗úc x├│a wishlist
            verify(wishlistItemRepository, never())
                    .deleteByStudentAndOnlineCourseId(any(User.class), any(Long.class));

            // Assert - KH├öNG ─æ╞░ß╗úc tß║ío wishlist mß╗¢i
            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        }
    }
}
