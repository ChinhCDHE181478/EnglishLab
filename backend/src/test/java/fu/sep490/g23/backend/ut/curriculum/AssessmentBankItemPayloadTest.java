package fu.sep490.g23.backend.ut.curriculum;

import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentBankItemPayloadTest {

    @Test
    void synchronizesTransientScoreChangesIntoContentData() {
        AssessmentBankItem item = AssessmentBankItem.builder()
                .title("IELTS Writing Task 2")
                .type(AssessmentType.MODULE_TEST)
                .skill(AssessmentSkill.WRITING)
                .aiEvaluationMode(AiEvaluationMode.ESTIMATED_BAND)
                .maxScore(BigDecimal.valueOf(9))
                .passingScore(BigDecimal.valueOf(6.5))
                .build();

        item.synchronizeContentData();

        assertThat(item.getContentData())
                .containsEntry("maxScore", BigDecimal.valueOf(9))
                .containsEntry("passingScore", BigDecimal.valueOf(6.5));
    }
}
