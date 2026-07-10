package fu.sap490.g23.backend.service.assessment;

import fu.sap490.g23.backend.entity.assessment.CourseAssessment;
import fu.sap490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sap490.g23.backend.entity.assessment.enums.AssessmentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IeltsBandScaleTest {

    @Test
    void normalizeBand_roundsToNearestHalfBand() {
        assertEquals(new BigDecimal("6.5"), IeltsBandScale.normalizeBand(new BigDecimal("6.3")));
        assertEquals(new BigDecimal("6.0"), IeltsBandScale.normalizeBand(new BigDecimal("6.2")));
        assertEquals(new BigDecimal("7.0"), IeltsBandScale.normalizeBand(new BigDecimal("6.8")));
    }

    @Test
    void normalizeBand_clampsToNine() {
        assertEquals(new BigDecimal("9.0"), IeltsBandScale.normalizeBand(new BigDecimal("10")));
        assertEquals(new BigDecimal("9.0"), IeltsBandScale.normalizeBand(new BigDecimal("9.4")));
        assertEquals(new BigDecimal("9.0"), IeltsBandScale.normalizeBand(new BigDecimal("9.0")));
    }

    @Test
    void resolveScoreCap_usesNineForBandAssessmentsWithLegacyMaxTen() {
        CourseAssessment assessment = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .maxScore(new BigDecimal("10"))
                .build();

        assertEquals(IeltsBandScale.MAX, IeltsBandScale.resolveScoreCap(assessment));
    }

    @Test
    void resolveScoreCap_keepsObjectiveMaxForListeningModuleTest() {
        CourseAssessment assessment = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.LISTENING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .maxScore(new BigDecimal("40"))
                .build();

        assertEquals(new BigDecimal("40"), IeltsBandScale.resolveScoreCap(assessment));
    }

    @Test
    void usesBandScale_detectsEstimatedBandAndProductiveModuleTests() {
        CourseAssessment writing = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .build();
        CourseAssessment listening = CourseAssessment.builder()
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.LISTENING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .build();

        assertTrue(IeltsBandScale.usesBandScale(writing));
        assertFalse(IeltsBandScale.usesBandScale(listening));
    }
}
