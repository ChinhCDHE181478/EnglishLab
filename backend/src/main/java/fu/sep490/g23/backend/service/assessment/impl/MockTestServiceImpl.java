package fu.sap490.g23.backend.service.assessment.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.request.assessment.MockTestSubmissionRequest;
import fu.sap490.g23.backend.dto.response.assessment.MockTestAttemptResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.MockTestAttempt;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sap490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.MockTestAttemptRepository;
import fu.sap490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sap490.g23.backend.service.assessment.MockTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MockTestServiceImpl implements MockTestService {
    private final AssessmentBankItemRepository assessmentBankRepository;
    private final MockTestAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public MockTestAttemptResponse submitMockTest(Long mockTestId, MockTestSubmissionRequest request, String studentEmail) {
        validateSubmission(request);
        AssessmentBankItem mockTest = assessmentBankRepository
                .findByIdAndTypeAndStatusAndActiveTrue(mockTestId, AssessmentType.MOCK_TEST, "PUBLISHED")
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
        java.util.Map<String, String> responses = new java.util.HashMap<>();
        if (submitted != null && submitted.has("responses")) {
            for (JsonNode response : submitted.withArray("responses")) {
                responses.put(response.path("questionNumber").asText(), response.path("answer").asText(""));
            }
        }

        int total = 0;
        int correct = 0;
        var fields = answerKey.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            total++;
            if (matches(responses.get(field.getKey()), field.getValue())) {
                correct++;
            }
        }
        return new ObjectiveScore(correct, total);
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
