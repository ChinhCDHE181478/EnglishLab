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
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlacementTestService {
    private static final String TEST_CODE = "IELTS_PLACEMENT_MOCK_1";

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
        AiEvaluationResult aiResult = evaluateProductiveSkills(request);
        BigDecimal productiveBand = normalizeBand(aiResult == null ? null : aiResult.getEstimatedScore());
        BigDecimal writingBand = extractBand(aiResult, "writingBand", productiveBand);
        BigDecimal speakingBand = extractBand(aiResult, "speakingBand", productiveBand);
        String status = aiResult == null ? "OBJECTIVE_EVALUATED" : "COMPLETED";

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

    private AiEvaluationResult evaluateProductiveSkills(PlacementTestSubmissionRequest request) {
        String writing = request.getWritingAnswers() == null ? "" : writeJson(objectMapper.valueToTree(request.getWritingAnswers()));
        String speaking = safe(request.getSpeakingTranscript());
        String prompt = """
                You are evaluating an IELTS placement test for course placement, not issuing an official IELTS result.
                Score the learner on the IELTS 0-9 band scale using half-band increments.
                Evaluate Writing for task response, coherence, lexical resource, grammar.
                Evaluate Speaking for fluency/coherence, lexical resource, grammar, pronunciation evidence when audio is available.
                Return concise JSON feedback with keys: estimatedScore, writingBand, speakingBand, strengths, weaknesses, recommendations.

                WRITING RESPONSES:
                %s

                SPEAKING TRANSCRIPT:
                %s
                """.formatted(writing, speaking);
        try {
            if (request.getSpeakingAudioUrl() != null && !request.getSpeakingAudioUrl().isBlank()) {
                return audioStorageService.loadStoredAudioFromUrl(request.getSpeakingAudioUrl())
                        .map(audio -> aiEvaluationClient.evaluateWithAudio(prompt, audio.bytes(), audio.contentType()))
                        .orElseGet(() -> aiEvaluationClient.evaluate(prompt));
            }
            return aiEvaluationClient.evaluate(prompt);
        } catch (RuntimeException exception) {
            return null;
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
        if (correct >= 32) return band(7.5);
        if (correct >= 30) return band(7);
        if (correct >= 26) return band(6.5);
        if (correct >= 23) return band(6);
        if (correct >= 18) return band(5.5);
        if (correct >= 16) return band(5);
        if (correct >= 13) return band(4.5);
        if (correct >= 11) return band(4);
        return band(3.5);
    }

    private BigDecimal readingBand(int correct) {
        if (correct >= 39) return band(9);
        if (correct >= 37) return band(8.5);
        if (correct >= 35) return band(8);
        if (correct >= 33) return band(7.5);
        if (correct >= 30) return band(7);
        if (correct >= 27) return band(6.5);
        if (correct >= 23) return band(6);
        if (correct >= 19) return band(5.5);
        if (correct >= 15) return band(5);
        if (correct >= 13) return band(4.5);
        if (correct >= 10) return band(4);
        return band(3.5);
    }

    private BigDecimal normalizeBand(BigDecimal value) {
        if (value == null) return null;
        double bounded = Math.max(0, Math.min(9, value.doubleValue()));
        return BigDecimal.valueOf(Math.round(bounded * 2) / 2.0).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal extractBand(AiEvaluationResult result, String field, BigDecimal fallback) {
        if (result == null || result.getFeedbackJson() == null || result.getFeedbackJson().isBlank()) return fallback;
        try {
            JsonNode value = objectMapper.readTree(result.getFeedbackJson()).path(field);
            return value.isNumber() ? normalizeBand(value.decimalValue()) : fallback;
        } catch (IOException exception) {
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
}
