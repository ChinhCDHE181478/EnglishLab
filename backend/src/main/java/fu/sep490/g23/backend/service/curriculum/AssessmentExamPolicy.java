package fu.sep490.g23.backend.service.curriculum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;

import java.math.BigDecimal;
import java.util.Locale;

public final class AssessmentExamPolicy {

    public static final BigDecimal IELTS_MAX_SCORE = BigDecimal.valueOf(9);
    public static final BigDecimal TOEIC_MAX_SCORE = BigDecimal.valueOf(990);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AssessmentExamPolicy() {
    }

    public static AiEvaluationMode resolveEvaluationMode(
            AssessmentType type,
            AssessmentSkill skill,
            AiEvaluationMode requestedMode
    ) {
        if (type == AssessmentType.MOCK_TEST && isEnglishSkill(skill)) {
            return AiEvaluationMode.ESTIMATED_BAND;
        }
        return requestedMode;
    }

    public static BigDecimal resolveMockExamMaximum(AssessmentType type, String uiConfigJson) {
        if (type != AssessmentType.MOCK_TEST) {
            return null;
        }
        return "TOEIC".equals(resolveExamType(uiConfigJson)) ? TOEIC_MAX_SCORE : IELTS_MAX_SCORE;
    }

    static String resolveExamType(String uiConfigJson) {
        if (uiConfigJson == null || uiConfigJson.isBlank()) {
            return "IELTS";
        }
        try {
            JsonNode config = OBJECT_MAPPER.readTree(uiConfigJson);
            String examType = config.path("examType").asText("").trim().toUpperCase(Locale.ROOT);
            if ("TOEIC".equals(examType)
                    || config.path("type").asText("").toLowerCase(Locale.ROOT).startsWith("toeic_")) {
                return "TOEIC";
            }
            return "IELTS";
        } catch (Exception ignored) {
            return "IELTS";
        }
    }

    private static boolean isEnglishSkill(AssessmentSkill skill) {
        return skill == AssessmentSkill.LISTENING
                || skill == AssessmentSkill.READING
                || skill == AssessmentSkill.WRITING
                || skill == AssessmentSkill.SPEAKING;
    }
}
