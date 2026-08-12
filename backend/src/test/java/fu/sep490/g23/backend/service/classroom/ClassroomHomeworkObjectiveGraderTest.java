package fu.sep490.g23.backend.service.classroom;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkActivityType;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassroomHomeworkObjectiveGraderTest {

    private final ClassroomHomeworkObjectiveGrader grader = new ClassroomHomeworkObjectiveGrader(new ObjectMapper());

    @Test
    void score_UsesAnswerKeyAndAcceptsConfiguredAlternativeAnswers() {
        ClassroomHomework homework = ClassroomHomework.builder()
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .activityConfigJson("""
                        {"answerKey":{"1":"A","2":["B","C"],"3":"D"}}
                        """)
                .maxScore(BigDecimal.TEN)
                .build();

        ClassroomHomeworkObjectiveGrader.ObjectiveScore result = grader.score(
                homework,
                "{\"responses\":{\"1\":\"a\",\"2\":\"C\",\"3\":\"A\"}}"
        );

        assertThat(grader.supports(homework)).isTrue();
        assertThat(result.correctCount()).isEqualTo(2);
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.score()).isEqualByComparingTo("6.67");
    }

    @Test
    void score_UsesAssessmentBankAnswerKeyWhenHomeworkReferencesPublishedAssessment() {
        ClassroomHomework homework = ClassroomHomework.builder()
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .assessmentBankItem(AssessmentBankItem.builder()
                        .objectiveAnswerKey("{\"1\":\"TRUE\"}")
                        .build())
                .maxScore(BigDecimal.valueOf(5))
                .build();

        ClassroomHomeworkObjectiveGrader.ObjectiveScore result = grader.score(homework, "{\"1\":\"true\"}");

        assertThat(result.score()).isEqualByComparingTo("5.00");
    }

    @Test
    void score_RejectsObjectiveHomeworkWithoutAnswerKey() {
        ClassroomHomework homework = ClassroomHomework.builder()
                .activityType(HomeworkActivityType.SKILL_PRACTICE)
                .activityConfigJson("{\"questions\":[]}")
                .build();

        assertThat(grader.supports(homework)).isFalse();
        assertThatThrownBy(() -> grader.score(homework, "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa có đáp án");
    }

    @Test
    void supports_DoesNotAutoGradeMixedHomeworkWithSubjectiveContent() {
        ClassroomHomework homework = ClassroomHomework.builder()
                .activityType(HomeworkActivityType.MIXED)
                .activityConfigJson("{\"answerKey\":{\"1\":\"A\"}}")
                .build();

        assertThat(grader.supports(homework)).isFalse();
    }

    @Test
    void toLearnerActivityConfig_RemovesAnswerKeyButKeepsQuestions() {
        String learnerConfig = grader.toLearnerActivityConfig("""
                {"questions":[{"number":1,"prompt":"Question","correctAnswer":"A"}],"answerKey":{"1":"A"}}
                """);

        assertThat(learnerConfig).contains("questions", "Question");
        assertThat(learnerConfig).doesNotContain("answerKey", "correctAnswer", "\"1\":\"A\"");
    }

    @Test
    void toLearnerActivityConfig_DoesNotExposeMalformedTeacherConfig() {
        assertThat(grader.toLearnerActivityConfig("{\"answerKey\":"))
                .isNull();
    }
}
