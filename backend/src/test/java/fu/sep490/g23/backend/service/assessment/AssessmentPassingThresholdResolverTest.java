package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentPassingThresholdResolverTest {

    private final AssessmentPassingThresholdResolver resolver = new AssessmentPassingThresholdResolver();

    @Test
    void resolve_usesTargetBandMinusHalfForModuleTestWhenPassingScoreNotConfigured() {
        OnlineCourse course = OnlineCourse.builder().targetBand(6.5D).build();
        CourseAssessment assessment = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .onlineCourse(course)
                .maxScore(null)
                .build();

        assertEquals(new BigDecimal("6.0"), resolver.resolve(assessment));
        assertEquals("Ngưỡng đạt: 6", resolver.buildDisplayLabel(assessment));
    }

    @Test
    void resolve_usesConfiguredPassingScoreWhenProvided() {
        OnlineCourse course = OnlineCourse.builder().targetBand(6.5D).build();
        CourseAssessment assessment = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .onlineCourse(course)
                .passingScore(new BigDecimal("5.0"))
                .maxScore(new BigDecimal("10"))
                .build();

        assertEquals(new BigDecimal("6.0"), resolver.resolve(assessment));
        assertEquals("Ngưỡng đạt: 6", resolver.buildDisplayLabel(assessment));
    }

    @Test
    void resolve_usesConfiguredPassingScoreEvenWhenHigherThanTargetBand() {
        OnlineCourse course = OnlineCourse.builder().targetBand(6.5D).build();
        CourseAssessment assessment = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .onlineCourse(course)
                .passingScore(new BigDecimal("6.5"))
                .maxScore(null)
                .build();

        assertEquals(new BigDecimal("6.0"), resolver.resolve(assessment));
    }

    @Test
    void resolve_returnsNullWhenNoPassingScoreOrTargetBand() {
        CourseAssessment quiz = CourseAssessment.builder()
                .type(AssessmentType.QUIZ)
                .maxScore(new BigDecimal("10"))
                .build();
        CourseAssessment moduleTest = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .maxScore(new BigDecimal("9"))
                .build();

        assertNull(resolver.resolve(quiz));
        assertNull(resolver.resolve(moduleTest));
        assertNull(resolver.buildDisplayLabel(quiz));
    }

    @Test
    void isScorePassing_allowsHalfBandBelowTargetBand() {
        OnlineCourse course = OnlineCourse.builder().targetBand(6.0D).build();
        CourseAssessment assessment = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .onlineCourse(course)
                .maxScore(null)
                .build();

        assertTrue(resolver.isScorePassing(new BigDecimal("5.5"), assessment));
        assertFalse(resolver.isScorePassing(new BigDecimal("5.0"), assessment));
    }

    @Test
    void isScorePassing_comparesAgainstTargetBandMinusHalf() {
        OnlineCourse course = OnlineCourse.builder().targetBand(6.5D).build();
        CourseAssessment assessment = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .onlineCourse(course)
                .maxScore(null)
                .build();

        assertTrue(resolver.isScorePassing(new BigDecimal("6.0"), assessment));
        assertFalse(resolver.isScorePassing(new BigDecimal("5.5"), assessment));
    }
}
