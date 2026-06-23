package fu.sap490.g23.backend.service.assessment;

import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class IeltsBandScale {

    public static final BigDecimal MAX = BigDecimal.valueOf(9.0);

    private IeltsBandScale() {
    }

    public static boolean usesBandScale(CourseAssessment assessment) {
        if (assessment == null) {
            return false;
        }
        return usesBandScale(
                assessment.getType(),
                assessment.getSkill(),
                assessment.getAiEvaluationMode()
        );
    }

    public static boolean usesBandScale(
            AssessmentType type,
            AssessmentSkill skill,
            AiEvaluationMode evaluationMode
    ) {
        if (skill == AssessmentSkill.LISTENING || skill == AssessmentSkill.READING) {
            return false;
        }
        if (evaluationMode == AiEvaluationMode.ESTIMATED_BAND) {
            return true;
        }
        if (type != AssessmentType.MODULE_TEST && type != AssessmentType.MOCK_TEST) {
            return false;
        }
        return true;
    }

    public static BigDecimal resolveScoreCap(CourseAssessment assessment) {
        if (assessment == null) {
            return null;
        }
        if (!usesBandScale(assessment)) {
            return assessment.getMaxScore();
        }
        BigDecimal configured = assessment.getMaxScore();
        if (configured == null || configured.compareTo(MAX) > 0) {
            return MAX;
        }
        return configured;
    }

    public static BigDecimal normalizeBand(BigDecimal value) {
        if (value == null) {
            return null;
        }
        double bounded = Math.max(0, Math.min(MAX.doubleValue(), value.doubleValue()));
        return BigDecimal.valueOf(Math.round(bounded * 2) / 2.0).setScale(1, RoundingMode.HALF_UP);
    }

    public static BigDecimal normalizeThreshold(CourseAssessment assessment, BigDecimal raw) {
        if (raw == null) {
            return null;
        }
        if (usesBandScale(assessment)) {
            return normalizeBand(raw);
        }
        return raw.setScale(1, RoundingMode.HALF_UP);
    }

    public static BigDecimal clampBandScore(BigDecimal value, CourseAssessment assessment) {
        if (value == null) {
            return null;
        }
        BigDecimal cap = resolveScoreCap(assessment);
        if (cap != null && value.compareTo(cap) > 0) {
            return cap;
        }
        if (usesBandScale(assessment)) {
            return normalizeBand(value);
        }
        return value;
    }

    public static BigDecimal resolveDefaultMaxScore(
            AssessmentType type,
            AssessmentSkill skill,
            AiEvaluationMode evaluationMode
    ) {
        if (usesBandScale(type, skill, evaluationMode)) {
            return MAX;
        }
        return BigDecimal.TEN;
    }

    public static BigDecimal normalizeConfiguredMaxScore(
            BigDecimal maxScore,
            AssessmentType type,
            AssessmentSkill skill,
            AiEvaluationMode evaluationMode
    ) {
        if (maxScore == null) {
            return resolveDefaultMaxScore(type, skill, evaluationMode);
        }
        if (!usesBandScale(type, skill, evaluationMode)) {
            return maxScore;
        }
        if (maxScore.compareTo(MAX) > 0) {
            return MAX;
        }
        return maxScore;
    }

    public static BigDecimal normalizeConfiguredPassingScore(
            BigDecimal passingScore,
            AssessmentType type,
            AssessmentSkill skill,
            AiEvaluationMode evaluationMode
    ) {
        if (passingScore == null) {
            return null;
        }
        if (!usesBandScale(type, skill, evaluationMode)) {
            return passingScore;
        }
        return normalizeBand(passingScore);
    }
}
