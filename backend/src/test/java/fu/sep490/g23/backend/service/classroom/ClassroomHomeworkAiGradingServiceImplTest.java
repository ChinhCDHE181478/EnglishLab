package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.RubricCriterion;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomHomeworkAiGradingServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClassroomHomeworkAiGradingServiceImplTest {

    @Test
    void tryAutoGrade_PreservesCompleteStructuredFeedback() {
        AiEvaluationClient client = mock(AiEvaluationClient.class);
        String feedbackJson = """
                {"estimatedScore":7,"summary":"Bài viết bám đúng đề.","criteria":[{"name":"Task Response","score":7,"feedback":"Luận điểm rõ."}],"strengths":["Bố cục tốt."],"weaknesses":["Ví dụ còn chung."],"suggestions":["Thêm dẫn chứng cụ thể."]}
                """;
        when(client.evaluate(contains("Task Response"))).thenReturn(AiEvaluationResult.builder()
                .estimatedScore(BigDecimal.valueOf(7))
                .feedbackJson(feedbackJson)
                .build());

        AssessmentRubric rubric = AssessmentRubric.builder()
                .name("IELTS Writing")
                .skill(AssessmentSkill.WRITING)
                .scoringScale("0-9")
                .build();
        rubric.addCriterion(RubricCriterion.builder()
                .name("Task Response")
                .description("Đáp ứng yêu cầu đề bài")
                .bandDescriptors("Band 7: đáp ứng tốt")
                .displayOrder(1)
                .build());
        ClassroomHomework homework = ClassroomHomework.builder()
                .id(10L)
                .title("Writing Task 2")
                .instruction("Discuss both views.")
                .maxScore(BigDecimal.TEN)
                .gradingMode(HomeworkGradingMode.AI)
                .skill(AssessmentSkill.WRITING)
                .rubric(rubric)
                .build();
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .homework(homework)
                .student(User.builder().id(2L).build())
                .textAnswer("This essay discusses both views with examples.")
                .status(HomeworkSubmissionStatus.SUBMITTED)
                .build();

        boolean graded = new ClassroomHomeworkAiGradingServiceImpl(client).tryAutoGrade(submission);

        assertThat(graded).isTrue();
        assertThat(submission.getScore()).isEqualByComparingTo("7.78");
        assertThat(submission.getTeacherFeedback()).isEqualTo("Bài viết bám đúng đề.");
        assertThat(submission.getAiFeedbackJson()).isEqualTo(feedbackJson);
        assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.GRADED);
    }
}
