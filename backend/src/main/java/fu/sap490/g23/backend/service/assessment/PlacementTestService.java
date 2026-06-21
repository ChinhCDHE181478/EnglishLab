package fu.sap490.g23.backend.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sap490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sap490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.service.ai.AiEvaluationClient;
import fu.sap490.g23.backend.service.ai.AiEvaluationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlacementTestService {
    private static final String TEST_CODE = "IELTS_PLACEMENT_MOCK_1";
    private static final int MAX_ATTEMPTS = 3;
    private static final Pattern SPEAKING_METADATA_PATTERN = Pattern.compile("speaking mock test:|part prompts shown to the learner:|recording duration seconds:|voice signal detected:", Pattern.CASE_INSENSITIVE);
    private static final Set<String> WRITING_TASK_1_KEYWORDS = Set.of(
            "corn", "ethanol", "fuel", "process", "production", "produce", "diagram", "stages", "ferment", "fermentation", "liquid", "milling", "cook", "cooking", "purify", "purification"
    );
    private static final Set<String> WRITING_TASK_2_KEYWORDS = Set.of(
            "physical", "mental", "strength", "sport", "sports", "athlete", "athletes", "success", "training", "performance", "competition", "competitive"
    );
    private static final Set<String> SPEAKING_TOPIC_KEYWORDS = Set.of(
            "from", "live", "home", "hometown", "films", "film", "movie", "movies", "watch",
            "leisure", "activity", "activities", "work", "adults", "children", "parents",
            "generation", "generations", "free", "time", "enjoy"
    );

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository attemptRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final AssessmentAudioStorageService audioStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Map<String, Object> getTest(String studentEmail) {
        User student = requireStudent(studentEmail);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("testCode", TEST_CODE);
        response.put("title", "Bài đánh giá đầu vào IELTS");
        response.put("description", "Một phiên thi liên tục gồm Listening, Reading, Writing và Speaking.");
        long attemptCount = attemptRepository.countByStudentAndTestCode(student, TEST_CODE);
        response.put("attemptCount", attemptCount);
        response.put("maxAttempts", MAX_ATTEMPTS);
        response.put("canRetake", attemptCount < MAX_ATTEMPTS);
        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("listening", toPlainObject(withoutAnswerKey(loadJson("placement-test/mock-1-listening.json"))));
        sections.put("reading", toPlainObject(withoutAnswerKey(loadJson("assessment-data/ielts_mock_2025_january_reading_test_1.json"))));
        sections.put("writing", toPlainObject(loadJson("assessment-data/ielts_mock_2025_january_writing_test_1.json")));
        sections.put("speaking", toPlainObject(loadJson("placement-test/mock-1-speaking.json")));
        response.put("sections", sections);
        attemptRepository.findTopByStudentAndTestCodeOrderBySubmittedAtDesc(student, TEST_CODE)
                .ifPresent(attempt -> response.put("latestAttempt", toResponse(attempt)));
        return response;
    }

    @Transactional
    public PlacementTestAttemptResponse submit(PlacementTestSubmissionRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);
        if (attemptRepository.countByStudentAndTestCode(student, TEST_CODE) >= MAX_ATTEMPTS) {
            throw new IllegalStateException("Bạn đã dùng hết 3 lượt làm bài đánh giá đầu vào.");
        }
        validateSubmission(request);

        JsonNode listeningConfig = loadJson("placement-test/mock-1-listening.json");
        JsonNode readingConfig = loadJson("assessment-data/ielts_mock_2025_january_reading_test_1.json");
        JsonNode listeningAnswers = objectMapper.valueToTree(request.getListeningAnswers());
        JsonNode readingAnswers = objectMapper.valueToTree(request.getReadingAnswers());
        JsonNode writingAnswers = objectMapper.valueToTree(request.getWritingAnswers());
        JsonNode deviceCheck = objectMapper.valueToTree(request.getDeviceCheck());
        ObjectiveScore listening = scoreObjective(listeningAnswers, listeningConfig.path("answerKey"));
        ObjectiveScore reading = scoreObjective(readingAnswers, readingConfig.path("answerKey"));

        BigDecimal listeningBand = listeningBand(listening.correct());
        BigDecimal readingBand = readingBand(reading.correct());
        JsonNode writingConfig = loadJson("assessment-data/ielts_mock_2025_january_writing_test_1.json");
        JsonNode speakingConfig = loadJson("placement-test/mock-1-speaking.json");
        AiEvaluationResult aiResult = evaluateProductiveSkills(request, writingConfig, speakingConfig);
        BigDecimal productiveBand = normalizeBand(aiResult == null ? null : aiResult.getEstimatedScore());
        BigDecimal writingBand = extractBand(aiResult, "writingBand", productiveBand);
        BigDecimal speakingBand = extractBand(aiResult, "speakingBand", productiveBand);
        String status = aiResult == null || (writingBand == null && speakingBand == null) ? "OBJECTIVE_EVALUATED" : "COMPLETED";

        BigDecimal overall = averageAvailable(listeningBand, readingBand, writingBand, speakingBand);
        ObjectNode answers = objectMapper.createObjectNode();
        answers.set("listening", listeningAnswers);
        answers.set("reading", readingAnswers);
        answers.set("writing", writingAnswers);
        answers.put("speakingTranscript", safe(request.getSpeakingTranscript()));
        answers.put("speakingAudioUrl", safe(request.getSpeakingAudioUrl()));

        PlacementTestAttempt attempt = PlacementTestAttempt.builder()
                .student(student)
                .testCode(TEST_CODE)
                .answersJson(writeJson(answers))
                .deviceCheckJson(writeJson(deviceCheck))
                .listeningScore(listeningBand)
                .readingScore(readingBand)
                .writingScore(writingBand)
                .speakingScore(speakingBand)
                .overallScore(overall)
                .correctListening(listening.correct())
                .correctReading(reading.correct())
                .aiFeedbackJson(aiResult == null ? fallbackFeedback() : aiResult.getFeedbackJson())
                .status(status)
                .submittedAt(LocalDateTime.now())
                .build();
        return toResponse(attemptRepository.save(attempt));
    }

    private AiEvaluationResult evaluateProductiveSkills(PlacementTestSubmissionRequest request, JsonNode writingConfig, JsonNode speakingConfig) {
        String writing = request.getWritingAnswers() == null ? "" : writeJson(objectMapper.valueToTree(request.getWritingAnswers()));
        String speaking = safe(request.getSpeakingTranscript());
        String prompt = """
                You are evaluating an IELTS placement test for course placement, not issuing an official IELTS result.
                Score the learner on the IELTS 0-9 band scale using half-band increments.
                Evaluate Writing for task response, coherence, lexical resource, grammar.
                Evaluate Speaking for fluency/coherence, lexical resource, grammar, pronunciation evidence when audio is available.
                If a response is clearly off-topic, nonsensical, irrelevant to the prompt, or made of filler unrelated to the actual task, assign 0.0 for that skill.
                If there is not enough real evidence to judge Speaking reliably, set speakingBand to null and explain that the submission should not be scored yet.
                If there is not enough real evidence to judge Writing reliably, set writingBand to 0.0 when the writing is present but clearly irrelevant, or null only when no meaningful writing content exists.
                Return concise JSON feedback with keys: estimatedScore, writingBand, speakingBand, strengths, weaknesses, recommendations.

                WRITING TASKS:
                %s

                WRITING RESPONSES:
                %s

                SPEAKING TASK:
                %s

                SPEAKING TRANSCRIPT:
                %s
                """.formatted(writeJson(writingConfig), writing, writeJson(speakingConfig), speaking);
        try {
            AiEvaluationResult aiResult;
            if (request.getSpeakingAudioUrl() != null && !request.getSpeakingAudioUrl().isBlank()) {
                aiResult = audioStorageService.loadStoredAudioFromUrl(request.getSpeakingAudioUrl())
                        .map(audio -> aiEvaluationClient.evaluateWithAudio(prompt, audio.bytes(), audio.contentType()))
                        .orElseGet(() -> aiEvaluationClient.evaluate(prompt));
            } else {
                aiResult = aiEvaluationClient.evaluate(prompt);
            }
            return applyProductiveGuards(aiResult, request);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private AiEvaluationResult applyProductiveGuards(AiEvaluationResult aiResult, PlacementTestSubmissionRequest request) {
        if (aiResult == null) {
            return null;
        }

        try {
            ObjectNode root = aiResult.getFeedbackJson() == null || aiResult.getFeedbackJson().isBlank()
                    ? objectMapper.createObjectNode()
                    : (ObjectNode) objectMapper.readTree(aiResult.getFeedbackJson());

            BigDecimal writingBand = readBand(root.path("writingBand"), normalizeBand(aiResult.getEstimatedScore()));
            BigDecimal speakingBand = readBand(root.path("speakingBand"), normalizeBand(aiResult.getEstimatedScore()));

            WritingEvidence writingEvidence = evaluateWritingEvidence(request.getWritingAnswers());
            SpeakingEvidence speakingEvidence = evaluateSpeakingEvidence(request, aiResult.isAudioInputAnalyzed());

            if (writingEvidence.offTopicAllTasks()) {
                writingBand = band(0);
                appendGuardFeedback(root,
                        "Phần Writing đang lệch đề nặng hoặc nội dung không liên quan tới cả hai task, nên bị chấm 0.",
                        "Viết lại đúng trọng tâm: Task 1 phải mô tả quy trình sản xuất ethanol từ ngô; Task 2 phải bàn về physical strength và mental strength trong thể thao.");
            } else if (writingEvidence.hasSevereProblem()) {
                writingBand = minBand(writingBand, band(2.5));
                appendGuardFeedback(root,
                        "Phần Writing có ít nhất một task quá ngắn hoặc lệch đề rõ rệt, nên điểm bị hạ mạnh.",
                        "Hoàn thành đầy đủ cả hai task, bám đúng đề và phát triển ý rõ ràng trước khi nộp lại.");
            }

            if (speakingEvidence.insufficientEvidence()) {
                speakingBand = null;
                appendGuardFeedback(root,
                        speakingEvidence.message(),
                        "Hãy nộp lại bài nói với bản ghi thật rõ hoặc transcript thực sự phản ánh câu trả lời của bạn.");
            } else if (speakingEvidence.offTopic()) {
                speakingBand = band(0);
                appendGuardFeedback(root,
                        "Phần Speaking lệch đề nặng hoặc nội dung nói không liên quan tới các câu hỏi đã cho, nên bị chấm 0.",
                        "Trả lời trực tiếp câu hỏi Part 1, mô tả đúng cue card ở Part 2, và bám chủ đề leisure / work / activities ở Part 3.");
            }

            BigDecimal productiveAverage = averageAvailable(writingBand, speakingBand);
            aiResult.setEstimatedScore(productiveAverage);
            if (productiveAverage == null) {
                root.putNull("estimatedScore");
            } else {
                root.put("estimatedScore", productiveAverage);
            }

            if (writingBand == null) {
                root.putNull("writingBand");
            } else {
                root.put("writingBand", writingBand);
            }

            if (speakingBand == null) {
                root.putNull("speakingBand");
            } else {
                root.put("speakingBand", speakingBand);
            }

            aiResult.setFeedbackJson(objectMapper.writeValueAsString(root));
            return aiResult;
        } catch (Exception exception) {
            return aiResult;
        }
    }

    private ObjectiveScore scoreObjective(JsonNode submitted, JsonNode answerKey) {
        int total = 0;
        int correct = 0;
        Iterator<Map.Entry<String, JsonNode>> fields = answerKey.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            total++;
            if (matches(submitted == null ? null : submitted.get(entry.getKey()), entry.getValue())) {
                correct++;
            }
        }
        return new ObjectiveScore(correct, total);
    }

    private boolean matches(JsonNode actual, JsonNode expected) {
        if (actual == null || actual.isNull() || actual.asText().isBlank()) return false;
        if (expected.isArray()) {
            if (actual.isArray()) {
                Set<String> actualValues = new HashSet<>();
                actual.forEach(value -> actualValues.add(normalize(value.asText())));
                Set<String> expectedValues = new HashSet<>();
                expected.forEach(value -> expectedValues.add(normalize(value.asText())));
                return actualValues.equals(expectedValues);
            }
            for (JsonNode candidate : expected) {
                if (normalize(actual.asText()).equals(normalize(candidate.asText()))) return true;
            }
            return false;
        }
        return normalize(actual.asText()).equals(normalize(expected.asText()));
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT)
                .replaceAll("[£$,.]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private BigDecimal listeningBand(int correct) {
        if (correct >= 39) return band(9);
        if (correct >= 37) return band(8.5);
        if (correct >= 35) return band(8);
        if (correct >= 33) return band(7.5);
        if (correct >= 30) return band(7);
        if (correct >= 27) return band(6.5);
        if (correct >= 23) return band(6);
        if (correct >= 20) return band(5.5);
        if (correct >= 16) return band(5);
        if (correct >= 13) return band(4.5);
        if (correct >= 10) return band(4);
        if (correct >= 7) return band(3.5);
        if (correct >= 5) return band(3);
        if (correct >= 3) return band(2.5);
        return band(0);
    }

    private BigDecimal readingBand(int correct) {
        if (correct >= 40) return band(9);
        if (correct >= 39) return band(8.5);
        if (correct >= 38) return band(8);
        if (correct >= 36) return band(7.5);
        if (correct >= 34) return band(7);
        if (correct >= 32) return band(6.5);
        if (correct >= 30) return band(6);
        if (correct >= 27) return band(5.5);
        if (correct >= 23) return band(5);
        if (correct >= 19) return band(4.5);
        if (correct >= 15) return band(4);
        if (correct >= 12) return band(3.5);
        if (correct >= 8) return band(3);
        if (correct >= 5) return band(2.5);
        return band(0);
    }

    private BigDecimal normalizeBand(BigDecimal value) {
        return IeltsBandScale.normalizeBand(value);
    }

    private BigDecimal extractBand(AiEvaluationResult result, String field, BigDecimal fallback) {
        if (result == null || result.getFeedbackJson() == null || result.getFeedbackJson().isBlank()) return fallback;
        try {
            JsonNode value = objectMapper.readTree(result.getFeedbackJson()).path(field);
            return readBand(value, fallback);
        } catch (IOException exception) {
            return fallback;
        }
    }

    private BigDecimal readBand(JsonNode value, BigDecimal fallback) {
        if (value == null || value.isMissingNode()) {
            return fallback;
        }
        if (value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return normalizeBand(value.decimalValue());
        }
        String raw = safe(value.asText());
        if (raw.isBlank()) {
            return null;
        }
        try {
            return normalizeBand(new BigDecimal(raw));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private BigDecimal averageAvailable(BigDecimal... values) {
        List<BigDecimal> available = Arrays.stream(values).filter(Objects::nonNull).toList();
        if (available.isEmpty()) return null;
        BigDecimal total = available.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return normalizeBand(total.divide(BigDecimal.valueOf(available.size()), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal band(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal minBand(BigDecimal current, BigDecimal cap) {
        if (cap == null) return current;
        if (current == null) return cap;
        return current.compareTo(cap) <= 0 ? current : cap;
    }

    private WritingEvidence evaluateWritingEvidence(Map<String, Object> writingAnswers) {
        String task1 = safe(asText(writingAnswers == null ? null : writingAnswers.get("task_1")));
        String task2 = safe(asText(writingAnswers == null ? null : writingAnswers.get("task_2")));

        int task1Words = countWords(task1);
        int task2Words = countWords(task2);
        int task1Hits = countKeywordHits(task1, WRITING_TASK_1_KEYWORDS);
        int task2Hits = countKeywordHits(task2, WRITING_TASK_2_KEYWORDS);

        boolean task1OffTopic = task1Words >= 25 && task1Hits == 0;
        boolean task2OffTopic = task2Words >= 40 && task2Hits == 0;
        boolean task1TooShort = task1Words < 40;
        boolean task2TooShort = task2Words < 60;

        return new WritingEvidence(task1OffTopic, task2OffTopic, task1TooShort, task2TooShort);
    }

    private SpeakingEvidence evaluateSpeakingEvidence(PlacementTestSubmissionRequest request, boolean audioAnalyzed) {
        String transcript = safe(request.getSpeakingTranscript());
        boolean hasAudioUrl = request.getSpeakingAudioUrl() != null && !request.getSpeakingAudioUrl().isBlank();
        boolean metadataOnlyTranscript = SPEAKING_METADATA_PATTERN.matcher(transcript).find();
        int transcriptWords = countWords(transcript);

        if (!audioAnalyzed) {
            if ((!hasAudioUrl && transcriptWords < 20) || metadataOnlyTranscript) {
                return new SpeakingEvidence(false, true,
                        "Phần Speaking chưa có đủ bằng chứng nói thật để chấm: transcript quá ít hoặc chỉ là metadata của bài thi.");
            }
            if (transcriptWords < 20) {
                return new SpeakingEvidence(false, true,
                        "Phần Speaking quá ngắn nên chưa đủ bằng chứng để chấm đáng tin cậy.");
            }
            if (countKeywordHits(transcript, SPEAKING_TOPIC_KEYWORDS) == 0) {
                return new SpeakingEvidence(true, false,
                        "Phần Speaking lệch khỏi chủ đề của đề bài.");
            }
        }

        return new SpeakingEvidence(false, false, "");
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Arrays.stream(text.trim().split("\\s+")).filter(token -> !token.isBlank()).count();
    }

    private int countKeywordHits(String text, Set<String> keywords) {
        String normalizedText = " " + normalizeForRelevance(text) + " ";
        int hits = 0;
        for (String keyword : keywords) {
            String needle = " " + normalizeForRelevance(keyword) + " ";
            if (normalizedText.contains(needle)) {
                hits++;
            }
        }
        return hits;
    }

    private String normalizeForRelevance(String value) {
        return safe(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void appendGuardFeedback(ObjectNode root, String weakness, String suggestion) {
        appendArrayText(root, "weaknesses", weakness);
        appendArrayText(root, "recommendations", suggestion);
        appendArrayText(root, "suggestions", suggestion);
    }

    private void appendArrayText(ObjectNode root, String field, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        var node = root.withArray(field);
        for (JsonNode item : node) {
            if (value.equalsIgnoreCase(item.asText())) {
                return;
            }
        }
        node.add(value);
    }

    private JsonNode withoutAnswerKey(JsonNode source) {
        ObjectNode copy = source.deepCopy();
        copy.remove("answerKey");
        return copy;
    }

    private JsonNode loadJson(String path) {
        try {
            String content = new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
            return objectMapper.readTree(content);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tải dữ liệu bài đánh giá đầu vào.", exception);
        }
    }

    private String writeJson(JsonNode node) {
        if (node == null) return null;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu dữ liệu bài đánh giá đầu vào.", exception);
        }
    }

    private void validateSubmission(PlacementTestSubmissionRequest request) {
        if (request == null || request.getListeningAnswers() == null || request.getReadingAnswers() == null
                || request.getWritingAnswers() == null) {
            throw new RuntimeException("Bài thi chưa có đủ dữ liệu của bốn kỹ năng.");
        }
        if (request.getDeviceCheck() == null || !Boolean.TRUE.equals(request.getDeviceCheck().get("completed"))) {
            throw new RuntimeException("Bạn cần hoàn thành kiểm tra thiết bị trước khi nộp bài.");
        }
        if ((request.getSpeakingTranscript() == null || request.getSpeakingTranscript().isBlank())
                && (request.getSpeakingAudioUrl() == null || request.getSpeakingAudioUrl().isBlank())) {
            throw new RuntimeException("Phần Speaking cần có bản ghi âm hoặc nội dung trả lời.");
        }
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
    }

    private PlacementTestAttemptResponse toResponse(PlacementTestAttempt attempt) {
        return PlacementTestAttemptResponse.builder()
                .id(attempt.getId())
                .testCode(attempt.getTestCode())
                .listeningScore(attempt.getListeningScore())
                .readingScore(attempt.getReadingScore())
                .writingScore(attempt.getWritingScore())
                .speakingScore(attempt.getSpeakingScore())
                .overallScore(attempt.getOverallScore())
                .correctListening(attempt.getCorrectListening())
                .correctReading(attempt.getCorrectReading())
                .aiFeedbackJson(attempt.getAiFeedbackJson())
                .status(attempt.getStatus())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    private String fallbackFeedback() {
        return "{\"message\":\"Đã lưu bài và chấm hai kỹ năng khách quan. Writing và Speaking sẽ được chấm lại khi dịch vụ AI sẵn sàng.\"}";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toPlainObject(JsonNode node) {
        return objectMapper.convertValue(node, Map.class);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ObjectiveScore(int correct, int total) {
    }

    private record WritingEvidence(boolean task1OffTopic, boolean task2OffTopic, boolean task1TooShort, boolean task2TooShort) {
        private boolean offTopicAllTasks() {
            return task1OffTopic && task2OffTopic;
        }

        private boolean hasSevereProblem() {
            return task1OffTopic || task2OffTopic || task1TooShort || task2TooShort;
        }
    }

    private record SpeakingEvidence(boolean offTopic, boolean insufficientEvidence, String message) {
    }
}
