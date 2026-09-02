package fu.sep490.g23.backend.service.assessment.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.assessment.MockTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.MockTestAttemptResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.MockTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.entity.curriculum.enums.ContentBankType;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.MockTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;
import fu.sep490.g23.backend.service.assessment.MockTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MockTestServiceImpl implements MockTestService {
    private final AssessmentBankItemRepository assessmentBankRepository;
    private final MockTestAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final AssessmentAudioStorageService audioStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public MockTestAttemptResponse submitMockTest(Long mockTestId, MockTestSubmissionRequest request, String studentEmail) {
        validateSubmission(request);
        AssessmentBankItem mockTest = assessmentBankRepository
                .findByIdAndTypeAndStatus(mockTestId, AssessmentType.MOCK_TEST, "PUBLISHED")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi thử đã xuất bản."));
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));

        AssessmentSkill skill = mockTest.getSkill() == null ? AssessmentSkill.MIXED : mockTest.getSkill();
        ObjectiveScore objectiveScore = scoreObjective(request.getObjectiveAnswersJson(), mockTest.getObjectiveAnswerKey());
        boolean objective = objectiveScore.total() > 0;
        BigDecimal percent = objective
                ? BigDecimal.valueOf(objectiveScore.correct() * 100.0 / objectiveScore.total()).setScale(2, RoundingMode.HALF_UP)
                : null;
        BigDecimal score = objective ? resolveScore(mockTest, objectiveScore) : null;
        String aiFeedback = null;
        String status = objective ? "COMPLETED" : "SUBMITTED";

        if (isProductiveSkill(skill)) {
            Optional<AssessmentAudioStorageService.StoredAssessmentAudio> speakingAudio = resolveSpeakingAudio(skill, request);
            try {
                AiEvaluationResult result = evaluateProductiveMockTest(mockTest, request, speakingAudio);
                if (result == null || result.getEstimatedScore() == null) {
                    throw new IllegalStateException("AI không trả về điểm cho bài thi thử.");
                }
                score = normalizeAiScore(result.getEstimatedScore(), mockTest.getMaxScore());
                aiFeedback = normalizeFeedback(result.getFeedbackJson(), score);
                status = "COMPLETED";
            } catch (RuntimeException exception) {
                aiFeedback = failedFeedback();
                status = "FAILED";
            }
        }

        MockTestAttempt attempt = MockTestAttempt.builder()
                .assessmentBankItem(mockTest)
                .student(student)
                .skill(skill)
                .objectiveAnswers(safe(request.getObjectiveAnswersJson()))
                .submittedText(safe(request.getSubmittedText()))
                .submittedAudioUrl(safe(request.getSubmittedAudioUrl()))
                .correctCount(objective ? objectiveScore.correct() : null)
                .totalQuestions(objective ? objectiveScore.total() : null)
                .score(score)
                .aiFeedback(aiFeedback)
                .status(status)
                .submittedAt(LocalDateTime.now())
                .build();
        return toResponse(attemptRepository.save(attempt));
    }

    private boolean isProductiveSkill(AssessmentSkill skill) {
        return skill == AssessmentSkill.WRITING || skill == AssessmentSkill.SPEAKING;
    }

    private Optional<AssessmentAudioStorageService.StoredAssessmentAudio> resolveSpeakingAudio(
            AssessmentSkill skill,
            MockTestSubmissionRequest request
    ) {
        if (skill != AssessmentSkill.SPEAKING) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(request.getSubmittedAudioUrl())) {
            throw new IllegalArgumentException("Bài Speaking cần có bản ghi âm để chấm điểm.");
        }
        return Optional.of(audioStorageService.loadStoredAudioFromUrl(request.getSubmittedAudioUrl())
                .orElseThrow(() -> new IllegalArgumentException("Không đọc được bản ghi âm Speaking đã nộp.")));
    }

    private AiEvaluationResult evaluateProductiveMockTest(
            AssessmentBankItem mockTest,
            MockTestSubmissionRequest request,
            Optional<AssessmentAudioStorageService.StoredAssessmentAudio> speakingAudio
    ) {
        String prompt = buildAiPrompt(mockTest, request.getSubmittedText(), speakingAudio.isPresent());
        return speakingAudio
                .map(audio -> aiEvaluationClient.evaluateWithAudio(prompt, audio.bytes(), audio.contentType()))
                .orElseGet(() -> aiEvaluationClient.evaluate(prompt));
    }

    private String buildAiPrompt(AssessmentBankItem mockTest, String submittedText, boolean audioAttached) {
        String rubric = mockTest.getRubric() == null
                ? "Use standard IELTS criteria for the selected skill."
                : mockTest.getRubric().getCriteria().stream()
                        .map(criterion -> "%s (%s%%): %s".formatted(
                                safe(criterion.getName()),
                                criterion.getWeight() == null ? 0 : criterion.getWeight(),
                                safe(criterion.getDescription())))
                        .reduce((left, right) -> left + "\n" + right)
                        .orElse(safe(mockTest.getRubric().getDescription()));
        return """
                Grade this English mock-test submission using only the learner evidence provided.
                Return structured JSON feedback and one numeric estimatedScore.
                Test: %s
                Skill: %s
                Maximum score: %s
                Instructions: %s
                Rubric:
                %s
                Audio attached: %s
                Learner text:
                %s
                """.formatted(
                safe(mockTest.getTitle()),
                mockTest.getSkill() == null ? AssessmentSkill.MIXED : mockTest.getSkill(),
                mockTest.getMaxScore() == null ? BigDecimal.TEN : mockTest.getMaxScore(),
                safe(mockTest.getInstructions()),
                rubric,
                audioAttached ? "yes" : "no",
                safe(submittedText));
    }

    private BigDecimal normalizeAiScore(BigDecimal value, BigDecimal configuredMax) {
        BigDecimal maximum = configuredMax == null ? BigDecimal.TEN : configuredMax;
        return value.max(BigDecimal.ZERO).min(maximum).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeFeedback(String feedbackJson, BigDecimal score) {
        if (StringUtils.hasText(feedbackJson)) {
            JsonNode parsed = readJson(feedbackJson);
            if (parsed != null && parsed.isObject()) {
                return parsed.toString();
            }
        }
        return "{\"summary\":\"Đã chấm bài bằng AI.\",\"score\":" + score.toPlainString() + "}";
    }

    private String failedFeedback() {
        return "{\"summary\":\"Chưa thể chấm bài tự động. Vui lòng thử lại sau.\"}";
    }

    private void validateSubmission(MockTestSubmissionRequest request) {
        if (request == null
                || (!StringUtils.hasText(request.getObjectiveAnswersJson())
                && !StringUtils.hasText(request.getSubmittedText())
                && !StringUtils.hasText(request.getSubmittedAudioUrl()))) {
            throw new IllegalArgumentException("Bài thi chưa có câu trả lời để nộp.");
        }
    }

    private BigDecimal resolveScore(AssessmentBankItem mockTest, ObjectiveScore objectiveScore) {
        if (objectiveScore.total() <= 0) {
            return null;
        }
        BigDecimal maxScore = mockTest.getMaxScore() == null ? BigDecimal.valueOf(objectiveScore.total()) : mockTest.getMaxScore();
        return maxScore
                .multiply(BigDecimal.valueOf(objectiveScore.correct()))
                .divide(BigDecimal.valueOf(objectiveScore.total()), 2, RoundingMode.HALF_UP);
    }

    private ObjectiveScore scoreObjective(String submittedJson, String answerKeyJson) {
        JsonNode answerKey = readJson(answerKeyJson);
        if (answerKey == null || !answerKey.isObject()) {
            return new ObjectiveScore(0, 0);
        }
        JsonNode submitted = readJson(submittedJson);
        if (submitted != null && submitted.has("responses") && submitted.get("responses").isArray()) {
            return scoreResponses(submitted.get("responses"), answerKey);
        }
        java.util.Map<String, String> responses = new java.util.HashMap<>();
        int total = 0;
        int correct = 0;
        var fields = answerKey.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (field.getKey().contains("-")) {
                continue;
            }
            total++;
            if (matches(responses.get(field.getKey()), field.getValue())) {
                correct++;
            }
        }
        return new ObjectiveScore(correct, total);
    }

    private ObjectiveScore scoreResponses(JsonNode responses, JsonNode answerKey) {
        int total = 0;
        int correct = 0;
        for (JsonNode response : responses) {
            String questionNumber = response.path("questionNumber").asText("");
            String answerType = response.path("answerType").asText("");
            String actual = response.path("answer").asText("");
            boolean grouped = "multi_select_letters".equals(answerType) || questionNumber.contains("-");
            if (grouped) {
                Set<String> expected = expectedLetterSet(answerKey, questionNumber);
                if (expected.isEmpty()) {
                    continue;
                }
                Set<String> selected = parseLetterSet(actual);
                total += expected.size();
                for (String letter : selected) {
                    if (expected.contains(letter)) {
                        correct++;
                    }
                }
                continue;
            }
            JsonNode expected = lookupExpected(answerKey, questionNumber);
            if (expected == null || expected.isMissingNode() || expected.isNull()) {
                continue;
            }
            total++;
            if (matches(actual, expected)) {
                correct++;
            }
        }
        return new ObjectiveScore(correct, total);
    }

    private JsonNode lookupExpected(JsonNode answerKey, String questionNumber) {
        JsonNode direct = answerKey.path(questionNumber);
        if (!direct.isMissingNode() && !direct.isNull()) {
            return direct;
        }
        if (questionNumber.contains("-")) {
            return answerKey.path(questionNumber.split("-")[0].trim());
        }
        return direct;
    }

    private Set<String> expectedLetterSet(JsonNode answerKey, String questionNumber) {
        Set<String> expected = new LinkedHashSet<>();
        JsonNode direct = lookupExpected(answerKey, questionNumber);
        addLetters(expected, direct);
        if (!expected.isEmpty()) {
            return expected;
        }
        Arrays.stream(questionNumber.split("-"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(number -> addLetters(expected, answerKey.path(number)));
        return expected;
    }

    private void addLetters(Set<String> expected, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> expected.addAll(parseLetterSet(item.asText(""))));
            return;
        }
        expected.addAll(parseLetterSet(node.asText("")));
    }

    private Set<String> parseLetterSet(String value) {
        Set<String> selected = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return selected;
        }
        Arrays.stream(value.split("[,\\s]+"))
                .map(item -> item.trim().toUpperCase(Locale.ROOT))
                .filter(item -> item.matches("[A-Z]"))
                .forEach(selected::add);
        return selected;
    }

    private boolean matches(String actual, JsonNode expected) {
        if (expected == null || expected.isMissingNode() || expected.isNull()) {
            return false;
        }
        if (expected.isArray()) {
            for (JsonNode candidate : expected) {
                if (normalize(actual).equals(normalize(candidate.asText()))) {
                    return true;
                }
            }
            return false;
        }
        return normalize(actual).equals(normalize(expected.asText()));
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Dữ liệu bài thi thử không hợp lệ.");
        }
    }

    private MockTestAttemptResponse toResponse(MockTestAttempt attempt) {
        AssessmentBankItem mockTest = attempt.getAssessmentBankItem();
        return MockTestAttemptResponse.builder()
                .id(attempt.getId())
                .mockTestId(mockTest.getId())
                .mockTestTitle(mockTest.getTitle())
                .skill(attempt.getSkill())
                .correctCount(attempt.getCorrectCount())
                .totalQuestions(attempt.getTotalQuestions())
                .score(attempt.getScore())
                .percent(calculatePercent(attempt))
                .aiFeedbackJson(attempt.getAiFeedback())
                .status(attempt.getStatus())
                .submittedText(attempt.getSubmittedText())
                .submittedAudioUrl(attempt.getSubmittedAudioUrl())
                .objectiveAnswersJson(attempt.getObjectiveAnswers())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    private BigDecimal calculatePercent(MockTestAttempt attempt) {
        if (attempt.getCorrectCount() == null || attempt.getTotalQuestions() == null
                || attempt.getTotalQuestions() <= 0) {
            return null;
        }
        return BigDecimal.valueOf(attempt.getCorrectCount() * 100.0 / attempt.getTotalQuestions())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ObjectiveScore(int correct, int total) {
    }
}
