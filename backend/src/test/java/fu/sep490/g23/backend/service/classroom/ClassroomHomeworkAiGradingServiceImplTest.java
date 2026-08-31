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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClassroomHomeworkAiGradingServiceImplTest {

    @Test
    void tryAutoGrade_PreservesCompleteStructuredFeedback() {
        AiEvaluationClient client = mock(AiEvaluationClient.class);
        HomeworkAttachmentStorageService storageService = mock(HomeworkAttachmentStorageService.class);
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

        boolean graded = new ClassroomHomeworkAiGradingServiceImpl(client, storageService).tryAutoGrade(submission);

        assertThat(graded).isTrue();
        assertThat(submission.getScore()).isEqualByComparingTo("7.78");
        assertThat(submission.getTeacherFeedback()).isEqualTo("Bài viết bám đúng đề.");
        assertThat(submission.getAiFeedbackJson()).isEqualTo(feedbackJson);
        assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.GRADED);
    }

    @Test
    void tryAutoGrade_SpeakingSendsStoredAudioInsteadOfMetadataText() {
        AiEvaluationClient client = mock(AiEvaluationClient.class);
        HomeworkAttachmentStorageService storageService = mock(HomeworkAttachmentStorageService.class);
        byte[] audioBytes = new byte[]{1, 2, 3};
        String attachmentUrl = "/api/classroom-homework/attachments/homework-audio.webm";
        when(storageService.loadStoredAttachmentFromUrl(attachmentUrl)).thenReturn(Optional.of(
                new HomeworkAttachmentStorageService.StoredHomeworkAttachment(
                        "homework-audio.webm", "audio/webm", audioBytes.length, audioBytes
                )
        ));
        when(client.evaluateWithAudio(contains("actual spoken response"), same(audioBytes), eq("audio/webm")))
                .thenReturn(AiEvaluationResult.builder()
                        .estimatedScore(BigDecimal.valueOf(6.5))
                        .feedbackJson("{\"summary\":\"Phát âm khá rõ.\"}")
                        .build());

        ClassroomHomework homework = speakingHomework();
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .homework(homework)
                .student(User.builder().id(2L).build())
                .textAnswer("Speaking mock test: recording duration 125 seconds")
                .attachmentUrl(attachmentUrl)
                .status(HomeworkSubmissionStatus.SUBMITTED)
                .build();

        boolean graded = new ClassroomHomeworkAiGradingServiceImpl(client, storageService)
                .tryAutoGrade(submission);

        assertThat(graded).isTrue();
        assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.GRADED);
        verify(client, never()).evaluate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void tryAutoGrade_SpeakingWithoutStoredAudioRemainsSubmitted() {
        AiEvaluationClient client = mock(AiEvaluationClient.class);
        HomeworkAttachmentStorageService storageService = mock(HomeworkAttachmentStorageService.class);
        String attachmentUrl = "https://example.com/untrusted-audio.webm";
        when(storageService.loadStoredAttachmentFromUrl(attachmentUrl)).thenReturn(Optional.empty());
        ClassroomHomeworkSubmission submission = ClassroomHomeworkSubmission.builder()
                .homework(speakingHomework())
                .student(User.builder().id(2L).build())
                .textAnswer("Speaking mock test metadata")
                .attachmentUrl(attachmentUrl)
                .status(HomeworkSubmissionStatus.SUBMITTED)
                .build();

        boolean graded = new ClassroomHomeworkAiGradingServiceImpl(client, storageService)
                .tryAutoGrade(submission);

        assertThat(graded).isFalse();
        assertThat(submission.getStatus()).isEqualTo(HomeworkSubmissionStatus.SUBMITTED);
        assertThat(submission.getScore()).isNull();
        verify(client, never()).evaluate(org.mockito.ArgumentMatchers.anyString());
        verify(client, never()).evaluateWithAudio(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private ClassroomHomework speakingHomework() {
        AssessmentRubric rubric = AssessmentRubric.builder()
                .name("IELTS Speaking")
                .skill(AssessmentSkill.SPEAKING)
                .scoringScale("0-9")
                .build();
        rubric.addCriterion(RubricCriterion.builder()
                .name("Fluency")
                .description("Fluency and coherence")
                .bandDescriptors("Band 7: speaks at length")
                .displayOrder(1)
                .build());
        return ClassroomHomework.builder()
                .id(11L)
                .title("Speaking mock test")
                .instruction("Answer the speaking prompts.")
                .maxScore(BigDecimal.TEN)
                .gradingMode(HomeworkGradingMode.AI)
                .skill(AssessmentSkill.SPEAKING)
                .rubric(rubric)
                .build();
    }
}
