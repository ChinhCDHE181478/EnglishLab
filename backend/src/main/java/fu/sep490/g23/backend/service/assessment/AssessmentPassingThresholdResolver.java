package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AssessmentPassingThresholdResolver {

    public BigDecimal resolve(CourseAssessment assessment) {
        if (assessment == null) {
            return null;
        }

        OnlineCourse course = assessment.getOnlineCourse();
        if (assessment.getType() == AssessmentType.MODULE_TEST
                && IeltsBandScale.usesBandScale(assessment)
                && course != null
                && course.getTargetBand() != null) {
            return IeltsBandScale.normalizeThreshold(
                    assessment,
                    BigDecimal.valueOf(course.getTargetBand() - 0.5D)
            );
        }

        if (assessment.getPassingScore() != null) {
            return IeltsBandScale.normalizeThreshold(assessment, assessment.getPassingScore());
        }

        return null;
    }

    public String buildDisplayLabel(CourseAssessment assessment) {
        BigDecimal threshold = resolve(assessment);
        if (threshold == null) {
            return null;
        }

        return "Ngưỡng đạt: " + formatThreshold(threshold);
    }

    public boolean isScorePassing(BigDecimal score, CourseAssessment assessment) {
        if (score == null) {
            return false;
        }
        BigDecimal passingThreshold = resolve(assessment);
        if (passingThreshold == null) {
            return true;
        }
        return score.compareTo(passingThreshold) >= 0;
    }

    private String formatThreshold(BigDecimal threshold) {
        return threshold.stripTrailingZeros().scale() <= 0
                ? threshold.setScale(0, RoundingMode.HALF_UP).toPlainString()
                : threshold.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }
}
