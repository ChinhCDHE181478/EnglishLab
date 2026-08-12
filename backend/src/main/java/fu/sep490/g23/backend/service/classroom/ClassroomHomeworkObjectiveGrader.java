package fu.sep490.g23.backend.service.classroom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.Set;

@Component
public class ClassroomHomeworkObjectiveGrader {

    private static final Set<String> LEARNER_HIDDEN_ANSWER_FIELDS = Set.of(
            "answerKey", "correctAnswer", "correctAnswers", "correctOption", "correctOptionId",
            "acceptedAnswer", "acceptedAnswers"
    );

    private final ObjectMapper objectMapper;

    public ClassroomHomeworkObjectiveGrader() {
        this(new ObjectMapper());
    }

    ClassroomHomeworkObjectiveGrader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean supports(ClassroomHomework homework) {
        if (homework == null || homework.getActivityType() != HomeworkActivityType.SKILL_PRACTICE) {
            return false;
        }
        JsonNode answerKey = readAnswerKey(homework);
        return answerKey != null && answerKey.isObject() && !answerKey.isEmpty();
    }

    public ObjectiveScore score(ClassroomHomework homework, String submittedText) {
        JsonNode answerKey = readAnswerKey(homework);
        JsonNode responses = readResponses(submittedText);
        if (answerKey == null || !answerKey.isObject() || answerKey.isEmpty()) {
            throw new IllegalArgumentException("Bài trắc nghiệm chưa có đáp án để chấm tự động.");
        }
        if (responses == null || !responses.isObject()) {
            throw new IllegalArgumentException("Định dạng đáp án bài trắc nghiệm không hợp lệ.");
        }

        int correct = 0;
        int total = answerKey.size();
        Iterator<String> questionKeys = answerKey.fieldNames();
        while (questionKeys.hasNext()) {
            String questionKey = questionKeys.next();
            if (matches(answerKey.get(questionKey), responses.get(questionKey))) correct++;
        }

        BigDecimal maxScore = homework.getMaxScore() == null ? BigDecimal.TEN : homework.getMaxScore();
        BigDecimal score = total == 0
                ? BigDecimal.ZERO
                : maxScore.multiply(BigDecimal.valueOf(correct))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new ObjectiveScore(score, correct, total);
    }

    public String toLearnerActivityConfig(String activityConfigJson) {
        JsonNode config = readJson(activityConfigJson);
        if (config == null || !config.isObject()) {
            return null;
        }
        removeLearnerAnswerFields(config);
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void removeLearnerAnswerFields(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.remove(LEARNER_HIDDEN_ANSWER_FIELDS);
            objectNode.elements().forEachRemaining(this::removeLearnerAnswerFields);
            return;
        }
        if (node.isArray()) {
            node.elements().forEachRemaining(this::removeLearnerAnswerFields);
        }
    }

    private JsonNode readAnswerKey(ClassroomHomework homework) {
        String rawAnswerKey = homework.getAssessmentBankItem() == null
                ? null
                : homework.getAssessmentBankItem().getObjectiveAnswerKey();
        if (hasText(rawAnswerKey)) return readJson(rawAnswerKey);

        JsonNode config = readJson(homework.getActivityConfigJson());
        return config == null ? null : config.path("answerKey");
    }

    private JsonNode readResponses(String submittedText) {
        JsonNode payload = readJson(submittedText);
        if (payload == null) return null;
        JsonNode responses = payload.path("responses");
        return responses.isObject() ? responses : payload;
    }

    private boolean matches(JsonNode acceptedNode, JsonNode submittedNode) {
        if (submittedNode == null || submittedNode.isNull()) return false;
        String submitted = submittedNode.asText("").trim();
        if (acceptedNode != null && acceptedNode.isArray()) {
            for (JsonNode accepted : acceptedNode) {
                if (accepted.asText("").trim().equalsIgnoreCase(submitted)) return true;
            }
            return false;
        }
        return acceptedNode != null && acceptedNode.asText("").trim().equalsIgnoreCase(submitted);
    }

    private JsonNode readJson(String value) {
        if (!hasText(value)) return null;
        try {
            return objectMapper.readTree(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record ObjectiveScore(BigDecimal score, int correctCount, int totalCount) {
    }
}
