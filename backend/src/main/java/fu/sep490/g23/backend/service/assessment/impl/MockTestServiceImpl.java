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
import fu.sep490.g23.backend.service.assessment.MockTestService;
import fu.sep490.g23.backend.service.curriculum.ContentBankIdResolver;
import fu.sep490.g23.backend.service.curriculum.ContentBankLinkSync;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MockTestServiceImpl implements MockTestService {
    private final AssessmentBankItemRepository assessmentBankRepository;
    private final MockTestAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final ContentBankIdResolver contentBankIdResolver;
    private final ContentBankLinkSync contentBankLinkSync;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public MockTestAttemptResponse submitMockTest(Long mockTestId, MockTestSubmissionRequest request, String studentEmail) {
        validateSubmission(request);
        Long resolvedId = contentBankIdResolver.resolve(ContentBankType.ASSESSMENT, mockTestId).orElse(mockTestId);
        AssessmentBankItem mockTest = assessmentBankRepository
                .findByIdAndTypeAndStatusAndActiveTrue(resolvedId, AssessmentType.MOCK_TEST, "PUBLISHED")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi thử đã xuất bản."));
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));

        ObjectiveScore objectiveScore = scoreObjective(request == null ? null : request.getObjectiveAnswersJson(), mockTest.getObjectiveAnswerKey());
        boolean objective = objectiveScore.total() > 0;
        BigDecimal percent = objective
                ? BigDecimal.valueOf(objectiveScore.correct() * 100.0 / objectiveScore.total()).setScale(2, RoundingMode.HALF_UP)
                : null;
        BigDecimal score = objective ? resolveScore(mockTest, objectiveScore) : null;

        MockTestAttempt attempt = MockTestAttempt.builder()
                .assessmentBankItem(mockTest)
                .legacyAssessmentBankItemId(requireLegacyAssessmentId(mockTest))
                .student(student)
                .skill(mockTest.getSkill() == null ? AssessmentSkill.MIXED : mockTest.getSkill())
                .objectiveAnswersJson(safe(request == null ? null : request.getObjectiveAnswersJson()))
                .submittedText(safe(request == null ? null : request.getSubmittedText()))
                .submittedAudioUrl(safe(request == null ? null : request.getSubmittedAudioUrl()))
                .correctCount(objective ? objectiveScore.correct() : null)
                .totalQuestions(objective ? objectiveScore.total() : null)
                .score(score)
                .percent(percent)
                .status(objective ? "COMPLETED" : "SUBMITTED")
                .submittedAt(LocalDateTime.now())
                .build();
        return toResponse(attemptRepository.save(attempt));
    }

    private Long requireLegacyAssessmentId(AssessmentBankItem mockTest) {
        Long legacyId = contentBankLinkSync.legacyIdForAssessment(mockTest);
        if (legacyId == null) {
            throw new IllegalStateException("Thiếu ánh xạ legacy cho đề thi thử. Chạy lại migration Slice 3 hoặc tạo map.");
        }
        return legacyId;
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
                .percent(attempt.getPercent())
                .status(attempt.getStatus())
                .submittedText(attempt.getSubmittedText())
                .submittedAudioUrl(attempt.getSubmittedAudioUrl())
                .objectiveAnswersJson(attempt.getObjectiveAnswersJson())
                .submittedAt(attempt.getSubmittedAt())
                .build();
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
