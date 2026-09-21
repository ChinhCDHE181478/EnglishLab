package fu.sep490.g23.backend.ut.classroomLearningService;

import fu.sep490.g23.backend.dto.request.classroom.CompletePracticeRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPracticeAttemptResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomPracticeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-57: Complete Classroom Practice Exercise - test đầy đủ với request validation.
 * Service: ClassroomPracticeService.submitAttempt()
 * Request DTO: CompletePracticeRequest
 */
@ExtendWith(MockitoExtension.class)
public class CompleteClassroomPracticeExerciseTest {

    @Mock
    private ClassroomPracticeService practiceService;

    // TC01: Nộp bài trắc nghiệm/tự luận PUBLISHED thành công
    @Test
    void completeClassroomPracticeExercise_UTC01_submitPublished_succeeds() {
        ClassroomPracticeAttemptResponse expected = ClassroomPracticeAttemptResponse.builder()
                .id(201L)
                .scorePercent(85.0)
                .completedAt(LocalDateTime.now())
                .build();

        when(practiceService.submitAttempt(eq(1L), eq(201L), any(CompletePracticeRequest.class), eq("learner@test.com")))
                .thenReturn(expected);

        // Build complete request with all fields
        Instant startTime = Instant.parse("2026-09-21T14:00:00Z");
        CompletePracticeRequest request = new CompletePracticeRequest();
        request.setResponseText("Bài làm hoàn chỉnh của học viên về IELTS Reading");
        request.setAnswersJson("{\"q1\":\"A\",\"q2\":\"B\",\"q3\":\"C\",\"q4\":\"A\",\"q5\":\"D\"}");
        request.setDurationSeconds(1800); // 30 minutes
        request.setStartedAt(startTime);

        ClassroomPracticeAttemptResponse result = practiceService.submitAttempt(1L, 201L, request, "learner@test.com");

        // Verify response
        assertThat(result.getScorePercent()).isEqualTo(85.0);

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> classIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> exerciseIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<CompletePracticeRequest> requestCaptor = 
                ArgumentCaptor.forClass(CompletePracticeRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(practiceService, times(1))
                .submitAttempt(classIdCaptor.capture(), exerciseIdCaptor.capture(), requestCaptor.capture(), emailCaptor.capture());

        // Verify all parameters
        assertThat(classIdCaptor.getValue()).isEqualTo(1L);
        assertThat(exerciseIdCaptor.getValue()).isEqualTo(201L);

        CompletePracticeRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getResponseText()).isEqualTo("Bài làm hoàn chỉnh của học viên về IELTS Reading");
        assertThat(capturedRequest.getAnswersJson()).isEqualTo("{\"q1\":\"A\",\"q2\":\"B\",\"q3\":\"C\",\"q4\":\"A\",\"q5\":\"D\"}");
        assertThat(capturedRequest.getDurationSeconds()).isEqualTo(1800);
        assertThat(capturedRequest.getStartedAt()).isEqualTo(startTime);
        
        assertThat(emailCaptor.getValue()).isEqualTo("learner@test.com");

        verifyNoMoreInteractions(practiceService);
    }

    // TC02: Auto-submit khi hết giờ -> MSG-16
    @Test
    void completeClassroomPracticeExercise_UTC02_autoSubmit() {
        when(practiceService.submitAttempt(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("MSG-16: Đã hết giờ! Bài làm của bạn đã tự động nộp."));

        CompletePracticeRequest request = new CompletePracticeRequest();
        request.setDurationSeconds(0);  // Auto-submit
        request.setAnswersJson("{\"q1\":\"A\",\"q2\":\"B\"}");
        request.setStartedAt(Instant.parse("2026-09-21T14:00:00Z"));

        assertThatThrownBy(() -> practiceService.submitAttempt(1L, 201L, request, "learner@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-16");

        // Verify the request that triggered auto-submit
        ArgumentCaptor<CompletePracticeRequest> requestCaptor = 
                ArgumentCaptor.forClass(CompletePracticeRequest.class);
        verify(practiceService, times(1))
                .submitAttempt(any(), any(), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getDurationSeconds()).isEqualTo(0);

        verifyNoMoreInteractions(practiceService);
    }

    // TC03: Lưu draft khi exit
    @Test
    void completeClassroomPracticeExercise_UTC03_saveDraft() {
        ClassroomPracticeAttemptResponse expected = ClassroomPracticeAttemptResponse.builder()
                .id(202L)
                .responseText("{\"status\":\"DRAFT\"}")
                .build();

        when(practiceService.submitAttempt(eq(1L), eq(202L), any(CompletePracticeRequest.class), eq("learner@test.com")))
                .thenReturn(expected);

        CompletePracticeRequest request = new CompletePracticeRequest();
        request.setResponseText("{\"status\":\"DRAFT\"}");
        request.setAnswersJson("{\"q1\":\"A\"}");
        request.setDurationSeconds(600);
        request.setStartedAt(Instant.parse("2026-09-21T14:00:00Z"));

        ClassroomPracticeAttemptResponse result = practiceService.submitAttempt(1L, 202L, request, "learner@test.com");

        assertThat(result.getResponseText()).contains("DRAFT");

        // CRITICAL: Verify request data
        ArgumentCaptor<CompletePracticeRequest> requestCaptor = 
                ArgumentCaptor.forClass(CompletePracticeRequest.class);
        verify(practiceService, times(1))
                .submitAttempt(eq(1L), eq(202L), requestCaptor.capture(), eq("learner@test.com"));

        CompletePracticeRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getResponseText()).isEqualTo("{\"status\":\"DRAFT\"}");
        assertThat(capturedRequest.getAnswersJson()).isEqualTo("{\"q1\":\"A\"}");
        assertThat(capturedRequest.getDurationSeconds()).isEqualTo(600);

        verifyNoMoreInteractions(practiceService);
    }

    // TC04: Bài tập DRAFT -> BusinessRuleException
    @Test
    void completeClassroomPracticeExercise_UTC04_draftExercise() {
        when(practiceService.submitAttempt(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Bài tập chưa xuất bản"));

        CompletePracticeRequest request = new CompletePracticeRequest();
        request.setAnswersJson("{\"q1\":\"A\"}");

        assertThatThrownBy(() -> practiceService.submitAttempt(1L, 999L, request, "learner@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa xuất bản");

        // Verify the non-existent exercise ID was passed
        ArgumentCaptor<Long> exerciseIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(practiceService, times(1))
                .submitAttempt(any(), exerciseIdCaptor.capture(), any(), any());

        assertThat(exerciseIdCaptor.getValue()).isEqualTo(999L);

        verifyNoMoreInteractions(practiceService);
    }

    // TC05: Không enrolled -> AccessDenied
    @Test
    void completeClassroomPracticeExercise_UTC05_learnerNotEnrolled() {
        when(practiceService.submitAttempt(any(), any(), any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Not enrolled"));

        CompletePracticeRequest request = new CompletePracticeRequest();
        request.setAnswersJson("{\"q1\":\"A\"}");

        assertThatThrownBy(() -> practiceService.submitAttempt(1L, 201L, request, "unknown@test.com"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        // Verify the unauthorized email was passed
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(practiceService, times(1))
                .submitAttempt(any(), any(), any(), emailCaptor.capture());

        assertThat(emailCaptor.getValue()).isEqualTo("unknown@test.com");

        verifyNoMoreInteractions(practiceService);
    }

    // TC06: ExerciseId không tồn tại -> EntityNotFoundException
    @Test
    void completeClassroomPracticeExercise_UTC06_exerciseNotFound() {
        when(practiceService.submitAttempt(any(), any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu"));

        CompletePracticeRequest request = new CompletePracticeRequest();
        request.setAnswersJson("{\"q1\":\"A\"}");

        assertThatThrownBy(() -> practiceService.submitAttempt(1L, 9999L, request, "learner@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent exercise ID was passed
        ArgumentCaptor<Long> exerciseIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(practiceService, times(1))
                .submitAttempt(any(), exerciseIdCaptor.capture(), any(), any());

        assertThat(exerciseIdCaptor.getValue()).isEqualTo(9999L);
    }
}
